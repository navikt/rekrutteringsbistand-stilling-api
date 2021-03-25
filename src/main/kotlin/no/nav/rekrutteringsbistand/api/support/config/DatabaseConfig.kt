package no.nav.rekrutteringsbistand.api.support.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil
import org.flywaydb.core.Flyway
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

import javax.sql.DataSource

@Configuration
@Profile("dev", "prod") // TODO: Kan ta bort profil her
class DatabaseConfig {

    @Value("\${database.url}")
    private val databaseUrl: String? = null

    @Value("\${database.navn}")
    private val databaseNavn: String? = null

    @Value("\${vault.mount-path}")
    private val mountPath: String? = null

    @Bean
    fun userDataSource(): DataSource {
        return dataSource("user")
    }

    private fun dataSource(user: String): HikariDataSource {
        val config = HikariConfig()
        config.jdbcUrl = databaseUrl
        config.maximumPoolSize = 2
        config.minimumIdle = 1
        return HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(config, mountPath, dbRole(user))
    }

    @Bean
    fun flywayMigrationStrategy(): FlywayMigrationStrategy {
        return FlywayMigrationStrategy {
            Flyway.configure()
                    .dataSource(dataSource("admin"))
                    .initSql(String.format("SET ROLE \"%s\"", dbRole("admin")))
                    .load()
                    .migrate()
        }
    }

    private fun dbRole(role: String): String {
        return arrayOf(databaseNavn, role).joinToString("-")
    }

}
