package no.nav.rekrutteringsbistand.api.support.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

import javax.sql.DataSource

@Configuration
@Profile("dev", "prod")
class DatabaseConfig {

    @Value("\${database.hostname}")
    private val databaseHostname: String? = null

    @Value("\${database.port}")
    private val databasePort: String? = null

    @Value("\${database.navn}")
    private val databaseNavn: String? = null

    @Value("\${database.brukernavn}")
    private val databaseBrukernavn: String? = null

    @Value("\${database.passord}")
    private val databasePassord: String? = null

    private fun dataSource(): HikariDataSource {
        val config = HikariConfig()
        config.jdbcUrl = "jdbc:postgresql://$databaseHostname:$databasePort/$databaseNavn"
        config.maximumPoolSize = 2
        config.minimumIdle = 1
        config.username = databaseBrukernavn
        config.password = databasePassord
        config.validate()
        return HikariDataSource(config)
    }

    @Bean
    fun flywayMigrationStrategy(): FlywayMigrationStrategy {
        return FlywayMigrationStrategy {
            Flyway.configure()
                .dataSource(dataSource())
                .load()
                .migrate()
        }
    }

    private fun dbRole(role: String): String {
        return arrayOf(databaseNavn, role).joinToString("-")
    }

}
