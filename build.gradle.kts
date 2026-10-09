import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.4.21"
    kotlin("plugin.spring") version "2.4.21"
    id("org.springframework.boot") version "4.1.1"
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
    implementation("org.springframework.data:spring-data-jdbc")
    runtimeOnly("org.postgresql:postgresql")
    implementation("io.opentelemetry.instrumentation:opentelemetry-logback-mdc-1.0:2.28.1-alpha")
    implementation("net.logstash.logback:logstash-logback-encoder:8.1")
    implementation("org.codehaus.janino:janino")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("no.nav.security:token-validation-spring:6.0.12")
    implementation("com.nimbusds:nimbus-jose-jwt:10.10")
    implementation("net.minidev:json-smart")
    implementation("com.github.navikt:rapids-and-rivers:2026091610031789545782")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.wiremock:wiremock-standalone:3.13.2")
    testImplementation("no.nav.security:token-validation-spring-test:6.0.12")
    testImplementation("com.squareup.okhttp3:mockwebserver:5.5.0")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.mockito.kotlin:mockito-kotlin:6.4.0")
    testImplementation("org.awaitility:awaitility")
    testImplementation("com.github.navikt.rapids-and-rivers:rapids-and-rivers-test:2026091610031789545782")

    constraints {
        implementation("at.yawk.lz4:lz4-java") {
            version {
                strictly("1.11.4")
            }
        }
        implementation("org.apache.tomcat.embed:tomcat-embed-core") {
            version {
                strictly("11.0.26")
            }
        }
        implementation("org.apache.tomcat.embed:tomcat-embed-el") {
            version {
                strictly("11.0.26")
            }
        }
        implementation("org.apache.tomcat.embed:tomcat-embed-websocket") {
            version {
                strictly("11.0.26")
            }
        }
        implementation("org.apache.tomcat:tomcat-annotations-api") {
            version {
                strictly("11.0.26")
            }
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
    systemProperty("api.version", "1.44")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("rekrutteringsbistand-stilling-api.jar")
}
