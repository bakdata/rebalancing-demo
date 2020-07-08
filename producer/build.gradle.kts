plugins {
    idea
    java
    id("io.freefair.lombok") version "5.1.0"
    id("com.google.cloud.tools.jib") version "1.2.0"
    id("com.commercehub.gradle.plugin.avro") version "0.9.1"
}
group = "com.bakdata.kafka"

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

repositories {
    mavenCentral()
    maven(url = "http://packages.confluent.io/maven/")
}

dependencies {
    val confluentVersion: String by project
    implementation(group = "io.confluent", name = "kafka-streams-avro-serde", version = confluentVersion)
    implementation(group = "info.picocli", name = "picocli", version = "4.0.4")
    implementation(group = "com.bakdata.common-kafka-streams", name = "common-kafka-streams", version = "1.4.3")
    implementation(group = "com.bakdata.kafka", name = "error-handling", version = "1.0.0")

    val kafkaVersion: String by project
    implementation(group = "org.apache.kafka", name = "kafka-streams", version = kafkaVersion)
    implementation(group = "com.google.guava", name = "guava", version = "26.0-jre")

    val junitVersion = "5.4.0"
    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-api", version = junitVersion)
    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-params", version = junitVersion)
    testImplementation(group = "com.bakdata.fluent-kafka-streams-tests", name = "fluent-kafka-streams-tests-junit5", version = "2.1.0") {
        exclude(group = "junit")
    }
    testImplementation(group = "org.assertj", name = "assertj-core", version = "3.13.0")
    testImplementation(group = "org.slf4j", name = "slf4j-log4j12", version = "1.7.25")
    testImplementation(group = "log4j", name = "log4j", version = "1.2.17")
    testImplementation(group = "org.jooq", name = "jool", version = "0.9.14")
}
