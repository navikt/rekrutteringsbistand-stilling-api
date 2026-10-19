import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.4.21"
    kotlin("plugin.spring") version "2.4.21"
    id("org.springframework.boot") version "4.1.1"
    id("io.github.ben-manes.versions") version "0.66.0" // Gir kommandoen "./gradlew dependencyUpdates" som du kan be din AI bruke ved oppgradering av avhengigheter
}

group = "no.nav.tag"
version = "0.0.1-SNAPSHOT"

kotlin {
    jvmToolchain(25)
}

repositories {
    mavenCentral()
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    maven("https://jitpack.io")
    maven("https://packages.confluent.io/maven/")
}

dependencies {
    implementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
    // Ingen kode bruker valideringsannotasjoner, men token-validation-* drar inn jakarta.validation-api.
    // Uten Hibernate Validator fra denne starteren feiler binding av @ConfigurationProperties ved oppstart
    // med NoProviderFoundException.
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("io.github.resilience4j:resilience4j-kotlin:2.4.0")
    implementation("io.github.resilience4j:resilience4j-retry:2.4.0")
    implementation("org.springframework.boot:spring-boot-flyway")
    implementation("org.flywaydb:flyway-core")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    runtimeOnly("org.postgresql:postgresql")
    // Rapids & Rivers krever 9.0. Vi deklarerer samme versjon, så byggefila viser hva som faktisk kjører.
    implementation("net.logstash.logback:logstash-logback-encoder:9.0")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("no.nav.security:token-validation-spring:6.0.13")
    implementation("com.github.navikt:rapids-and-rivers:2026091610031789545782")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.wiremock:wiremock-standalone:3.13.2")
    testImplementation("no.nav.security:token-validation-spring-test:6.0.13")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.mockito.kotlin:mockito-kotlin:6.4.0")
    testImplementation("org.awaitility:awaitility")
    testImplementation("com.github.navikt.rapids-and-rivers:rapids-and-rivers-test:2026091610031789545782")

    // Constraints løfter transitive avhengigheter til versjoner med sikkerhetsfikser.
    // Vanlige versjonskrav (ikke strictly) virker som minsteversjon, så Gradle velger fortsatt
    // en nyere versjon hvis Spring Boot eller andre avhengigheter ber om det.
    // Sjekk om de fortsatt trengs med: ./gradlew dependencyInsight --dependency <navn> --configuration runtimeClasspath
    constraints {
        // Spring Boot 4.1.1 styrer Tomcat 11.0.24, som har kritiske sårbarheter fikset i 11.0.25.
        // Alle Tomcat-modulene må ha samme versjon. Fjern når Spring Boot styrer Tomcat >= 11.0.25.
        listOf(
            "org.apache.tomcat.embed:tomcat-embed-core",
            "org.apache.tomcat.embed:tomcat-embed-el",
            "org.apache.tomcat.embed:tomcat-embed-websocket",
        ).forEach { modul ->
            implementation("$modul:11.0.26") {
                because("CVE-2026-65905, CVE-2026-65182 og CVE-2026-68525 i Tomcat 11.0.24 fra Spring Boot 4.1.1")
            }
        }
        // kafka-clients drar inn sårbar lz4-java (4.2.1 gir 1.10.1, 4.2.2 gir 1.11.2, 4.3.1 gir 1.10.2).
        // Fjern når kafka-clients fra Spring Boot bruker lz4-java >= 1.11.4.
        implementation("at.yawk.lz4:lz4-java:1.11.4") {
            because("CVE-2026-59949 og CVE-2026-106449 til CVE-2026-106453 i lz4-java fra kafka-clients")
        }
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    // Tvinger Docker API-versjon for Testcontainers. Ellers faller docker-java tilbake til 1.32,
    // som nyere Docker og Colima avviser (de krever >= 1.44).
    systemProperty("api.version", "1.44")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("rekrutteringsbistand-stilling-api.jar")
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return !isStable
}

val navTidsstempelVersjon = "^[0-9]{22}$".toRegex()

tasks.named<DependencyUpdatesTask>("dependencyUpdates").configure {
    rejectVersionIf {
        // Avviser ustabile kandidater (alpha, beta, RC, M osv.) med mindre vi allerede er på en ustabil versjon
        val ustabil = isNonStable(candidate.version) && !isNonStable(currentVersion)

        // Nav-biblioteker som rapids-and-rivers bruker 22-sifrede tidsstempelversjoner, men har hatt andre
        // versjonsformater tidligere (f.eks. 2022.05.24-08.59.xxx på jitpack). Gradle tolker de 22-sifrede
        // versjonene som tekst og rangerer dem lavere enn eldre tallversjoner, så gamle formater må avvises.
        val gammeltVersjonsformat = navTidsstempelVersjon.matches(currentVersion) &&
            !navTidsstempelVersjon.matches(candidate.version)

        ustabil || gammeltVersjonsformat
    }
}
