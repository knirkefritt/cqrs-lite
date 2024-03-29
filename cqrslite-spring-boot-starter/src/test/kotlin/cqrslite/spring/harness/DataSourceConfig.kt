package cqrslite.spring.harness

import com.zaxxer.hikari.HikariDataSource
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
    fun dataSource(): DataSource = DataSourceBuilder
        .create()
        .type(HikariDataSource::class.java)
        .build()

    @ConfigurationProperties(prefix = "spring.datasource.flyway.hikari")
    @Bean
    @FlywayDataSource
    fun flywayDataSource(): DataSource = DataSourceBuilder
        .create()
        .type(HikariDataSource::class.java)
        .build()
}
