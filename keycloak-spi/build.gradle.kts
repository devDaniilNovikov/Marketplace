plugins {
    java
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.keycloak:keycloak-server-spi:26.0.7")
    compileOnly("org.keycloak:keycloak-server-spi-private:26.0.7")
    compileOnly("org.keycloak:keycloak-core:26.0.7")
    // Логгер сервера Keycloak; в дистрибутиве есть, в jar SPI не кладём
    compileOnly("org.jboss.logging:jboss-logging:3.6.1.Final")
    implementation("redis.clients:jedis:5.2.0")
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.jar {
    archiveFileName.set("marketplace-keycloak-user-events.jar")
}

tasks.test {
    useJUnitPlatform()
}
