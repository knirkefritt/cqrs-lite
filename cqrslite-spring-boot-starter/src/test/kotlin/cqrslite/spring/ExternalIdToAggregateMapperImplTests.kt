package cqrslite.spring

import cqrslite.core.AggregateRoot
import cqrslite.core.ExternalIdToAggregateMapperImpl
import cqrslite.core.RepositoryImpl
import cqrslite.core.SessionImpl
import cqrslite.spring.harness.ContainerizedPostgresDatabase
import cqrslite.spring.harness.TestContext
import cqrslite.spring.testaggregates.Giraffe
import cqrslite.spring.testaggregates.Hippo
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.util.*
import javax.naming.ConfigurationException

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(classes = [TestContext::class])
@ActiveProfiles("test")
@ExtendWith(ContainerizedPostgresDatabase::class)
class ExternalIdToAggregateMapperImplTests {
    @Autowired
    private lateinit var lookup: ExternalIdToAggregateMapperImpl

    @Autowired
    private lateinit var repository: RepositoryImpl

    @Test
    fun get_or_create_aggregate_does_not_exist_returns_same_aggregate_on_both_invocations() {
        runBlocking {
            val session = SessionImpl(repository)

            val externalId = "hippo-id-${UUID.randomUUID()}"

            val aggregate1 = lookup.getOrCreateAggregate(externalId, session, Hippo::class.java)
            val aggregate2 = lookup.getOrCreateAggregate(externalId, session, Hippo::class.java)

            assert(aggregate1 == aggregate2)
        }
    }

    @Test
    fun get_or_create_aggregate_does_not_exist_different_external_id_returns_different_aggregates_based_on_external_id() {
        runBlocking {
            val session = SessionImpl(repository)

            val aggregate1 = lookup.getOrCreateAggregate("hippo-id-${UUID.randomUUID()}", session, Hippo::class.java)
            val aggregate2 = lookup.getOrCreateAggregate("hippo-id-${UUID.randomUUID()}", session, Hippo::class.java)

            assert(aggregate1 != aggregate2)
        }
    }

    @Test
    fun find_aggregate_aggregate_exists_returns_existing_aggregate() {
        val externalId = "existing-aggregate-id--${UUID.randomUUID()}"

        runBlocking {
            val aggregate1 = newSuspendedTransaction {
                val session = SessionImpl(repository)
                val aggregate1 = lookup.getOrCreateAggregate(externalId, session, Hippo::class.java)
                aggregate1.someUseCase()
                session.commit()
                aggregate1
            }

            val aggregate2 = newSuspendedTransaction {
                val anotherSession = SessionImpl(repository)
                lookup.findAggregate(externalId, anotherSession, Hippo::class.java)
            }
            assert(aggregate1.id == aggregate2?.id)
        }
    }

    @Test
    fun find_aggregate_aggregate_does_not_exists_returns_null() {
        runBlocking {
            val aggregate2 = newSuspendedTransaction {
                val anotherSession = SessionImpl(repository)
                lookup.findAggregate("non-existing-aggregate--${UUID.randomUUID()}", anotherSession, Hippo::class.java)
            }
            assert(aggregate2 == null)
        }
    }

    @Test
    fun get_or_create_aggregate_does_not_exist_same_external_id_different_type_returns_different_aggregates_based_on_types() {
        val externalId = "same-ext-id-${UUID.randomUUID()}"

        runBlocking {
            val session = SessionImpl(repository)

            lookup.getOrCreateAggregate(externalId, session, Hippo::class.java)
            lookup.getOrCreateAggregate(externalId, session, Giraffe::class.java)
        }
    }

    class IAmAnUnknownAggregate(id: UUID) : AggregateRoot(id)

    @Test
    fun try_to_get_unknown_aggregate_fails_with_configuration_exception() {
        runBlocking {
            val session = SessionImpl(repository)

            assertThrows<ConfigurationException> {
                lookup.getOrCreateAggregate(
                    "whatever-id-${UUID.randomUUID()}",
                    session,
                    IAmAnUnknownAggregate::class.java,
                )
            }
        }
    }

    @Test
    fun map_mapping_does_not_exist_returns_success() {
        runBlocking {
            val session = SessionImpl(repository)
            val aggregate = lookup.getOrCreateAggregate("hippo-id-${UUID.randomUUID()}", session, Hippo::class.java)

            val success = lookup.mapToAggregate("another-id-${UUID.randomUUID()}", aggregate.id, Hippo::class.java)

            assert(success)
        }
    }

    @Test
    fun map_mapping_exist_returns_success() {
        runBlocking {
            val session = SessionImpl(repository)
            val aggregate = lookup.getOrCreateAggregate("hippo-id-${UUID.randomUUID()}", session, Hippo::class.java)

            val externalId = "another-id-${UUID.randomUUID()}"

            lookup.mapToAggregate(externalId, aggregate.id, Hippo::class.java)
            val success = lookup.mapToAggregate(externalId, aggregate.id, Hippo::class.java)

            assert(success)
        }
    }

    @Test
    fun map_same_external_id_but_different_aggregates_can_handle_mapping_for_both() {
        runBlocking {
            val session = SessionImpl(repository)

            val externalId = "external-id-${UUID.randomUUID()}"
            val hippo = lookup.getOrCreateAggregate(externalId, session, Hippo::class.java)
            val giraffe = lookup.getOrCreateAggregate(externalId, session, Giraffe::class.java)

            val externalId1 = "another-id-${UUID.randomUUID()}"
            lookup.mapToAggregate(externalId1, giraffe.id, Giraffe::class.java)
            val success = lookup.mapToAggregate(externalId1, hippo.id, Hippo::class.java)

            assert(success)
        }
    }

    @Test
    fun map_id_already_mapped_to_another_aggregate_throws_already_mapped_exception() {
        runBlocking {
            val session = SessionImpl(repository)

            val hippoOne = lookup.getOrCreateAggregate(
                "external-id-${UUID.randomUUID()}",
                session,
                Hippo::class.java,
            )

            val alreadyMappedId = "another-id-${UUID.randomUUID()}"
            lookup.mapToAggregate(alreadyMappedId, hippoOne.id, Hippo::class.java)

            runBlocking {
                var handled = false

                try {
                    val hippoTwo = lookup.getOrCreateAggregate(
                        "external-id-${UUID.randomUUID()}",
                        session,
                        Hippo::class.java,
                    )
                    lookup.mapToAggregate(alreadyMappedId, hippoTwo.id, Hippo::class.java)
                } catch (_: ExternalIdToAggregateMapperImpl.AlreadyMappedToDifferentAggregate) {
                    handled = true
                }
                assert(handled)
            }
        }
    }
}
