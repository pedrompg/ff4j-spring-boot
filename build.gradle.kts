import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("java")
}

group = "com.github.pedrompg"
version = "0.1"
java.sourceCompatibility = JavaVersion.VERSION_11

val ff4jVersion = "1.8.7"
val lombokVersion = "1.18.12"
val logbackVersion = "0.1.5"
val testContainersVersion = "1.14.3"

repositories {
    mavenCentral()
}

dependencies {
    // PostgreSQL
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    runtimeOnly("org.postgresql:postgresql")

    // Logback
    runtimeOnly("ch.qos.logback.contrib:logback-json-classic:$logbackVersion")
    runtimeOnly("ch.qos.logback.contrib:logback-jackson:$logbackVersion")
    runtimeOnly("net.logstash.logback:logstash-logback-encoder:6.3")

    // FF4J
    implementation("org.ff4j:ff4j-spring-boot-starter:$ff4jVersion")
    implementation("org.ff4j:ff4j-store-springjdbc:$ff4jVersion")

    // Junit
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation(group = "org.assertj", name = "assertj-core", version = "3.17.0")

    testImplementation("org.testcontainers:testcontainers:$testContainersVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testContainersVersion")
    testImplementation("org.testcontainers:postgresql:$testContainersVersion")
}

tasks {
    withType<Test> {
        useJUnitPlatform()
        failFast = true
        testLogging {
            lifecycle {
                events = mutableSetOf(TestLogEvent.FAILED, TestLogEvent.PASSED, TestLogEvent.SKIPPED)
                exceptionFormat = TestExceptionFormat.FULL
                showExceptions = true
                showCauses = true
                showStackTraces = true
                showStandardStreams = true
            }
            info.events = lifecycle.events
            info.exceptionFormat = lifecycle.exceptionFormat
        }

        val failedTests = mutableListOf<TestDescriptor>()
        val skippedTests = mutableListOf<TestDescriptor>()

        // See https://github.com/gradle/gradle/issues/5431
        addTestListener(object : TestListener {
            override fun beforeSuite(suite: TestDescriptor) {}
            override fun beforeTest(testDescriptor: TestDescriptor) {}
            override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {
                when (result.resultType) {
                    TestResult.ResultType.FAILURE -> failedTests.add(testDescriptor)
                    TestResult.ResultType.SKIPPED -> skippedTests.add(testDescriptor)
                    else -> Unit
                }
            }

            override fun afterSuite(suite: TestDescriptor, result: TestResult) {
                if (suite.parent == null) { // root suite
                    logger.lifecycle("----")
                    logger.lifecycle("Test result: ${result.resultType}")
                    logger.lifecycle(
                            "Test summary: ${result.testCount} tests, " +
                                    "${result.successfulTestCount} succeeded, " +
                                    "${result.failedTestCount} failed, " +
                                    "${result.skippedTestCount} skipped"
                    )
                    failedTests.takeIf { it.isNotEmpty() }?.prefixedSummary("\tFailed Tests")
                    skippedTests.takeIf { it.isNotEmpty() }?.prefixedSummary("\tSkipped Tests:")
                }
            }

            private infix fun List<TestDescriptor>.prefixedSummary(subject: String) {
                logger.lifecycle(subject)
                forEach { test -> logger.lifecycle("\t\t${test.displayName()}") }
            }

            private fun TestDescriptor.displayName() = parent?.let { "${it.name} - $name" } ?: name
        })
    }

    named("cleanTest") {
        group = "verification"
    }

    withType<JavaCompile> {
        options.isIncremental = true
        options.isFork = true
    }
}