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
    implementation("redis.clients:jedis:5.2.0")
}

tasks.jar {
    archiveFileName.set("marketplace-keycloak-user-events.jar")
}
