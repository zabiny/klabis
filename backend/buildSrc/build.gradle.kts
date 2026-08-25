plugins {
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    // Version pinned to match the org.openapi.generator Gradle plugin version in backend/build.gradle.kts —
    // KlabisSpringCodegen subclasses SpringCodegen from this exact artifact, so the two must move together.
    implementation("org.openapitools:openapi-generator:7.18.0")

    testImplementation(platform("org.junit:junit-bom:5.11.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.26.3")
}

tasks.test {
    useJUnitPlatform()
}
