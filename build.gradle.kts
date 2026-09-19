plugins {
    java
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "dn"
version = "0.0.1-SNAPSHOT"
description = "Marketplace"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

extra["springCloudVersion"] = "2025.1.2"
// Testcontainers не входит в BOM Spring Boot 4.1 — тянем свой
extra["testcontainersVersion"] = "1.21.3"

dependencies {
    // --- Web / ядро ---
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-websocket")

    // --- Persistence ---
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    runtimeOnly("org.postgresql:postgresql")

    // --- Redis: кэш каталога + Pub/Sub (транспорт событий Keycloak, решение №5) ---
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // --- Security: Keycloak как SSOT, JWT resource server ---
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    // --- Фоновые задачи ---
    implementation("org.springframework.boot:spring-boot-starter-batch")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    // @EnableRetry в MarketplaceApplication без этой зависимости не компилируется
    implementation("org.springframework.retry:spring-retry")

    // --- Внешние интеграции ---
    implementation("com.stripe:stripe-java:24.22.0")
    implementation("io.minio:minio:8.5.17")
    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j")

    // --- Observability: application.yml уже открывает /actuator/prometheus ---
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // --- Мапперы: Entity <-> DTO ---
    implementation("org.mapstruct:mapstruct:1.6.3")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")
    // Порядок важен: должен генерироваться после Lombok, иначе MapStruct не увидит @Getter/@Setter
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")

    // --- Тесты ---
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-redis-test")
    testImplementation("org.springframework.boot:spring-boot-starter-batch-test")
    testImplementation("org.springframework.boot:spring-boot-starter-mail-test")
    testImplementation("org.springframework.boot:spring-boot-starter-websocket-test")
    testImplementation("org.springframework.security:spring-security-test")

    // Testcontainers: интеграционные тесты на реальных Postgres и Redis
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")

    // ArchUnit: автоматический контроль правил изоляции доменов (решение №2)
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.1")

    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
        mavenBom("org.testcontainers:testcontainers-bom:${property("testcontainersVersion")}")
    }
}

tasks.withType<Test> {
    useJUnitPlatform {
        // -PfastTests — прогон без Docker: дешёвый гейт для раундов агентского цикла.
        // Тег "it" наследуется от AbstractIntegrationTest, поэтому разделение не зависит
        // от того, назвали класс *Test или *IT, и не разъедется при следующем переименовании.
        if (providers.gradleProperty("fastTests").isPresent) {
            excludeTags("it")
        }
    }

    // archunit.properties читает форкнутая JVM тестов, а -D на JVM Gradle туда не долетает.
    // Без этого проброса переключить FreezingArchRule снаружи (из CI) невозможно.
    providers.gradleProperty("archunitAllowStoreCreation").orNull?.let {
        systemProperty("freeze.store.default.allowStoreCreation", it)
    }
}
