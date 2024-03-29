package cqrslite.spring.harness

import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.Extension
import org.junit.jupiter.api.extension.ExtensionContext
import org.testcontainers.containers.PostgreSQLContainer

class ContainerizedPostgresDatabase : Extension, BeforeAllCallback {
    companion object {
        var postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer("postgres:15")
                .withDatabaseName("cqrslite")
                .withUsername("sa")
                .withPassword("sa")

        init {
            postgres.withInitScript("init.sql")
        }
    }

    override fun beforeAll(context: ExtensionContext?) {
        postgres.start()

        /*
         * System.setProperty has global side effects (affects all tests)
         * but it's ok since we are only initializing the db container once for all tests
         * if you need test specific configuration, consider this: https://rieckpil.de/override-spring-boot-configuration-properties-for-tests/
         * or this: https://spring.io/blog/2020/03/27/dynamicpropertysource-in-spring-framework-5-2-5-and-spring-boot-2-2-6
         */
        System.setProperty(
            "spring.datasource.default.hikari.jdbc-url",
            "jdbc:postgresql://localhost:${postgres.firstMappedPort}/${postgres.databaseName}",
        )
        System.setProperty("spring.datasource.default.hikari.username", "test_user")
        System.setProperty("spring.datasource.default.hikari.password", "test_user")

        System.setProperty(
            "spring.datasource.flyway.hikari.jdbc-url",
            "jdbc:postgresql://localhost:${postgres.firstMappedPort}/${postgres.databaseName}",
        )
        System.setProperty("spring.datasource.flyway.hikari.username", postgres.username)
        System.setProperty("spring.datasource.flyway.hikari.password", postgres.password)
        System.setProperty("spring.flyway.defaultSchema", "flyway_versioning")
    }
}
