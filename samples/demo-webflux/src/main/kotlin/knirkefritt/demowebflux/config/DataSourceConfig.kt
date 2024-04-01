package knirkefritt.demowebflux.config

import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.springframework.boot.autoconfigure.flyway.FlywayDataSource
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import javax.sql.DataSource

@Configuration
class DataSourceConfig {

    @ConfigurationProperties(prefix = "spring.datasource.default.hikari")
    @Bean
    @Primary
    fun dataSource(): DataSource {
        return DataSourceBuilder
            .create()
            .type(HikariDataSource::class.java)
            .build()
    }

    @ConfigurationProperties(prefix = "spring.datasource.flyway.hikari")
    @Bean
    @FlywayDataSource
    fun flywayDataSource(): DataSource {
        return DataSourceBuilder
            .create()
            .type(HikariDataSource::class.java)
            .build()
    }
}

@Configuration
class DatabaseConnection(datasource: DataSource) {

    init {
        Database.connect(datasource)
    }
}