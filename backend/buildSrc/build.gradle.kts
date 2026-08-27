plugins {
    `java-library`
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    // KlabisSpringCodegen subclasses SpringCodegen from this exact artifact, so the two must move together.
    implementation("org.openapitools:openapi-generator:7.18.0")
    // GenerateTask type used by the openApiModule(...) extension function (src/main/kotlin/OpenApiModule.kt).
    // Brings org.openapi.generator's task types/classes onto the root project's classpath directly —
    // backend/build.gradle.kts no longer applies that plugin via `plugins { id(...) }` itself, since
    // buildSrc's own runtime classpath is shared with the root project's build script classpath.
    implementation("org.openapitools:openapi-generator-gradle-plugin:7.18.0")

    testImplementation(platform("org.junit:junit-bom:5.11.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.26.3")
}

tasks.test {
    useJUnitPlatform()
}
