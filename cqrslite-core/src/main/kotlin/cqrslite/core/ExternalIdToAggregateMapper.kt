package cqrslite.core

import cqrslite.core.config.ExternalIdToAggregateMapperConfig
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.UUIDColumnType
import org.jetbrains.exposed.sql.VarCharColumnType
import org.jetbrains.exposed.sql.statements.StatementType
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.*
import javax.naming.ConfigurationException

class ExternalIdToAggregateMapper(
    private val cfg: ExternalIdToAggregateMapperConfig,
) {
    private val getExistingSql = """
            SELECT aggregate_id
            FROM ${cfg.schema}.${cfg.mappingTable}
            WHERE external_id = ? and aggregate_type = ?
    """.trimIndent()

    private val insertNewAggregateOrGetExistingSql = """
            WITH new_row AS (
                INSERT INTO ${cfg.schema}.${cfg.mappingTable} (external_id, aggregate_type, aggregate_id)
                VALUES (?, ?, ?)
                ON CONFLICT (external_id, aggregate_type) DO NOTHING
                RETURNING *
            )
            SELECT aggregate_id FROM new_row
            UNION
            SELECT aggregate_id FROM ${cfg.schema}.${cfg.mappingTable}
            WHERE external_id = ? and aggregate_type = ?
    """.trimIndent()

    suspend fun <T : AggregateRoot> getOrCreateAggregate(externalId: String, session: Session, clazz: Class<T>): T {
        val newAggregateId = UUID.randomUUID()

        val aggregateType =
            cfg.map[clazz] ?: throw ConfigurationException("Missing aggregate ${clazz.name} from config map")

        val insertNewParams = listOf(
            Pair(VarCharColumnType(), externalId),
            Pair(VarCharColumnType(), aggregateType),
            Pair(UUIDColumnType(), newAggregateId),
            Pair(VarCharColumnType(), externalId),
            Pair(VarCharColumnType(), aggregateType),
        )
        return newSuspendedTransaction {
            try {
                val id = exec(
                    insertNewAggregateOrGetExistingSql,
                    args = insertNewParams,
                    transform = { rs -> if (rs.next()) UUID.fromString(rs.getString(1)) else throw NoResult() },
                    explicitStatementType = StatementType.SELECT,
                )
                when (id) {
                    newAggregateId -> {
                        clazz.getConstructor(UUID::class.java).newInstance(newAggregateId)
                            .apply { session.add(this) }
                    }

                    null -> {
                        // if a competing transaction was in progress, then committed, the statement will return null
                        throw NoResult()
                    }

                    else -> {
                        session.find(id, clazz = clazz) ?: throw NoResult()
                    }
                }
            } catch (e: NoResult) {
                // we can end up here if there is a race condition or if a previous use-case did not produce any events
                // for the aggregate
                getOrCreateAggregateMappingExists(externalId, session, clazz)
            }
        }
    }

    private enum class Result {
        Mapped,
        AlreadyMappedToDifferentAggregate,
    }

    suspend fun <T : AggregateRoot> mapToAggregate(externalId: String, aggregateId: UUID, clazz: Class<T>): Boolean {
        val aggregateType =
            cfg.map[clazz] ?: throw ConfigurationException("Missing aggregate ${clazz.name} from config map")

        val mapToParams = listOf(
            Pair(VarCharColumnType(), externalId),
            Pair(VarCharColumnType(), aggregateType),
            Pair(UUIDColumnType(), aggregateId),
            Pair(VarCharColumnType(), externalId),
            Pair(VarCharColumnType(), aggregateType),
        )

        val result = newSuspendedTransaction {
            try {
                val id = exec(
                    insertNewAggregateOrGetExistingSql,
                    args = mapToParams,
                    transform = { rs -> if (rs.next()) UUID.fromString(rs.getString(1)) else throw NoResult() },
                    explicitStatementType = StatementType.SELECT,
                )
                when (id) {
                    aggregateId -> {
                        Result.Mapped
                    }

                    null -> {
                        throw NoResult()
                    }

                    else -> {
                        Result.AlreadyMappedToDifferentAggregate
                    }
                }
            } catch (_: NoResult) {
                val existingId = getIdOfExistingAggregate(externalId, clazz)
                if (existingId == aggregateId) {
                    Result.Mapped
                } else {
                    Result.AlreadyMappedToDifferentAggregate
                }
            }
        }
        return when (result) {
            Result.Mapped -> true
            Result.AlreadyMappedToDifferentAggregate -> throw AlreadyMappedToDifferentAggregate()
        }
    }

    suspend fun <T : AggregateRoot> findAggregate(existingId: String, session: Session, clazz: Class<T>): T? {
        return newSuspendedTransaction {
            try {
                val existingAggregateId = exec(
                    getExistingSql,
                    args = listOf(
                        Pair(VarCharColumnType(), existingId),
                        Pair(VarCharColumnType(), cfg.map[clazz]),
                    ),
                    transform = { rs -> if (rs.next()) UUID.fromString(rs.getString(1)) else throw NoResult() },
                    explicitStatementType = StatementType.SELECT,
                )
                if (existingAggregateId != null) {
                    session.find(existingAggregateId, clazz = clazz)
                } else {
                    throw RuntimeException("This should not occur")
                }
            } catch (_: NoResult) {
                null
            }
        }
    }

    /**
     * The mapping in the mapping table exists, so we do not have to create one, the aggregate however
     * might not exist, for example if a previous use-case created an aggregate but the aggregate did
     * not produce any events (and thus it has no persisted state). We will recreate the aggregate
     * if we can't find any events matching the aggregateId
     */
    private suspend fun <T : AggregateRoot> Transaction.getOrCreateAggregateMappingExists(
        externalId: String,
        session: Session,
        clazz: Class<T>,
    ): T {
        val existingAggregateId = getIdOfExistingAggregate(externalId, clazz)

        var aggregate = session.find(existingAggregateId, clazz = clazz)
        if (aggregate == null) {
            aggregate = clazz.getConstructor(UUID::class.java).newInstance(existingAggregateId) as T
            session.add(aggregate)
        }
        return aggregate
    }

    private fun <T : AggregateRoot> Transaction.getIdOfExistingAggregate(externalId: String, clazz: Class<T>): UUID {
        val existingAggregateId = exec(
            getExistingSql,
            args = listOf(
                Pair(VarCharColumnType(), externalId),
                Pair(VarCharColumnType(), cfg.map[clazz]),
            ),
            transform = { rs ->
                if (rs.next()) UUID.fromString(rs.getString(1)) else throw Error("This should not occur")
            },
            explicitStatementType = StatementType.SELECT,
        )
            ?: throw Error(
                """
                            Should always return a value because we have already made sure
                            that there is an existing aggregate
                """.trimIndent(),
            )
        return existingAggregateId
    }

    class AlreadyMappedToDifferentAggregate : Exception()

    class NoResult : Exception()
}
