import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.2.0.RELEASE"
    id("io.spring.dependency-management") version "1.0.8.RELEASE"
    kotlin("jvm") version "1.3.50"
    kotlin("plugin.spring") version "1.3.50"
    kotlin("plugin.jpa") version "1.3.50"
    kotlin("plugin.noarg") version "1.3.50"
    kotlin("plugin.allopen") version "1.3.50"
}



allOpen {
    annotation("javax.persistence.Entity")
    annotation("javax.persistence.Embeddable")
    annotation("javax.persistence.MappedSuperclass")
}

group = "com.delivery"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8

val developmentOnly by configurations.creating
configurations {
    runtimeClasspath {
        extendsFrom(developmentOnly)
    }
}

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.springdoc:springdoc-openapi-ui:1.3.9")
    implementation("org.springdoc:springdoc-openapi-kotlin:1.3.9")
    implementation("io.swagger.core.v3:swagger-annotations:2.1.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.auth0:auth0-spring-security-api:1.2.6")
    implementation("org.joda:joda-money:1.0.1")

    developmentOnly("org.springframework.boot:spring-boot-devtools")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        //        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("io.cucumber:cucumber-java8:5.0.0-RC4")
    testImplementation("io.cucumber:cucumber-java:5.0.0-RC4")
    testImplementation("io.cucumber:cucumber-junit:5.0.0-RC4")
//    testImplementation("io.cucumber:cucumber-junit-platform-engine:5.0.0-RC4")
    testImplementation("io.cucumber:cucumber-spring:5.0.0-RC4")
    testImplementation("org.assertj:assertj-core:3.14.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}
