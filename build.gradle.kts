import com.diffplug.spotless.LineEnding
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.gradle.ext.Gradle
import org.jetbrains.gradle.ext.JUnit
import org.jetbrains.gradle.ext.runConfigurations
import org.jetbrains.gradle.ext.settings

plugins {
    id("jacoco")
    id("java-library")
    id("maven-publish")
    id("signing")
    alias(libs.plugins.errorprone)
    alias(libs.plugins.idea.ext)
    alias(libs.plugins.nmcp)
    alias(libs.plugins.nullmarked)
    alias(libs.plugins.spotless)
}

dependencies {
    api(libs.jspecify)

    testImplementation(libs.assertj.core)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)

    errorprone(libs.errorprone.core)
    errorprone(libs.nullaway)
}

// The project is built using a JDK 25 toolchain, but the main sources are compiled with --release 8.
//
// This means:
// - Gradle can run on any JDK 17+,
// - javac from JDK 25 is used,
// - the produced bytecode and available Java API for main sources are restricted to Java 8.
//
// This setup lets us use a modern JDK for tooling (ErrorProne, NullAway) while keeping Java 8 binary compatibility for
// library consumers.
//
// Tests are NOT compiled with --release 8, so they may use newer Java APIs (e.g. JUnit 6).

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters")
    options.encoding = "UTF-8"
    options.errorprone {
        if (isTestTask()) {
            disableAllChecks = true
        }

        error("NullAway")
        option("NullAway:OnlyNullMarked", "true")
        option("NullAway:JSpecifyMode", "true")
    }
}

tasks.named<JavaCompile>("compileJava") {
    options.release = 8
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    testLogging {
        events(TestLogEvent.FAILED, TestLogEvent.PASSED, TestLogEvent.SKIPPED)
        exceptionFormat = TestExceptionFormat.SHORT
        showStandardStreams = true
    }

    systemProperty("user.language", "en")
    systemProperty("user.country", "US")
}

tasks.withType<Javadoc>().configureEach {
    javadocTool = javaToolchains.javadocToolFor { languageVersion = JavaLanguageVersion.of(8) }
}

tasks.withType<Jar>().configureEach {
    manifest {
        attributes["Implementation-Title"] = project.name
        attributes["Implementation-Version"] = project.version
        attributes["Created-By"] = "Gradle ${gradle.gradleVersion}"
    }
    from("${rootProject.rootDir}/LICENSE") {
        into("META-INF/")
        rename { "LICENSE.txt" }
    }
}

nullmarked {
    verifyOnly = true
    verify {
        strict()
    }
}

// Adds compilation of module-info.java with Java 9, which transforms output into a multi-release JAR for supporting
// Java Platform Module System if used by Java 9+.

val main9SourceSet = sourceSets.create("main9") {
    java.srcDirs("src/main9/java", "src/main/java")
}

configurations.named(main9SourceSet.compileClasspathConfigurationName) {
    extendsFrom(configurations.named("compileClasspath").get())
}
configurations.named(main9SourceSet.runtimeClasspathConfigurationName) {
    extendsFrom(configurations.named("runtimeClasspath").get())
}

tasks.named<JavaCompile>("compileMain9Java") {
    options.release = 9
}

tasks.named<Jar>("jar") {
    into("META-INF/versions/9") {
        from(main9SourceSet.output) {
            include("module-info.class")
        }
    }
    manifest {
        attributes["Multi-Release"] = "true"
    }
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))

    reports {
        xml.required = true
        html.required = true
        csv.required = false
    }
}

tasks.named<Task>("check") {
    finalizedBy(tasks.named("jacocoTestReport"))
}

idea {
    project {
        settings {
            runConfigurations {
                create<Gradle>("Clean [testclock]") {
                    taskNames = listOf("clean")
                    projectPath = rootProject.rootDir.absolutePath
                }
                create<Gradle>("Build [testclock]") {
                    taskNames = listOf("spotlessApply build")
                    projectPath = rootProject.rootDir.absolutePath
                }
                create<Gradle>("Format Code [testclock]") {
                    taskNames = listOf("spotlessApply")
                    projectPath = rootProject.rootDir.absolutePath
                }
                create<JUnit>("JUnit [testclock]") {
                    moduleName = "testclock.test"
                    workingDirectory = rootProject.rootDir.absolutePath
                    packageName = "io.github.malczuuu.testclock"
                }
            }
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            from(components["java"])

            pom {
                name = "TestClock"
                description = "A mutable & playable java.time.Clock for unit tests."
                url = "https://github.com/malczuuu/testclock"
                inceptionYear = "2026"
                licenses {
                    license {
                        name = "Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0"
                    }
                }
                developers {
                    developer {
                        id = "damianmalczewski"
                        name = "Damian Malczewski"
                        url = "https://github.com/damianmalczewski"
                    }
                }
                scm {
                    connection = "scm:git:https://github.com/malczuuu/testclock.git"
                    developerConnection = "scm:git:git@github.com:malczuuu/testclock.git"
                    url = "https://github.com/malczuuu/testclock"
                }
                issueManagement {
                    system = "GitHub Issues"
                    url = "https://github.com/malczuuu/testclock/issues"
                }
            }
        }
    }
}

signing {
    if (project.getBooleanProperty("sign")) {
        useInMemoryPgpKeys(System.getenv("SIGNING_KEY"), System.getenv("SIGNING_PASSWORD"))
        sign(publishing.publications["maven"])
    }
}

// Usage:
//   ./gradlew printVersion
tasks.register<DefaultTask>("printVersion") {
    description = "Prints the current project version to the console."
    group = "help"

    val projectName = project.name
    val projectVersion = project.version.toString()

    doLast {
        println("$projectName version: $projectVersion")
    }
}

tasks.withType<PublishToMavenLocal>().configureEach {
    finalizedBy("printVersion")
}

nmcp {
    publishAllPublicationsToCentralPortal {
        username = System.getenv("PUBLISHING_USERNAME")
        password = System.getenv("PUBLISHING_PASSWORD")

        publishingType = "USER_MANAGED"
    }
}

spotless {
    val licenseHeader = "${rootProject.rootDir}/gradle/license-header.java"

    java {
        target("**/src/**/*.java")
        licenseHeaderFile(licenseHeader)

        googleJavaFormat().maxLineLength(100)
        forbidWildcardImports()
        endWithNewline()
        lineEndings = LineEnding.UNIX
    }
    format("javaMisc") {
        target("**/src/**/package-info.java", "**/src/**/module-info.java")

        // License headers in these files are not formatted with standard java group, so we need to use custom settings.
        // The regex is designed to find out where the code starts in these files, so the license header can be placed
        // before it.
        //
        // The code starts with either:
        //
        // - any annotation (ex. @NullMarked before package declaration),
        // - package, module or import declaration,
        // - "/**" in case of a pre-package (or pre-module) JavaDoc.
        val delimiter = "^(@|package|import|module|/\\*\\*)"

        licenseHeaderFile(licenseHeader, delimiter)
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**")

        ktlint().editorConfigOverride(mapOf("max_line_length" to "100"))
        endWithNewline()
        lineEndings = LineEnding.UNIX
    }
    format("yaml") {
        target("**/*.yml", "**/*.yaml")

        trimTrailingWhitespace()
        leadingTabsToSpaces(2)
        endWithNewline()
        lineEndings = LineEnding.UNIX
    }
    format("misc") {
        target("**/.gitattributes", "**/.gitignore")

        trimTrailingWhitespace()
        leadingTabsToSpaces(4)
        endWithNewline()
        lineEndings = LineEnding.UNIX
    }
}

defaultTasks("spotlessApply", "build")

/**
 * Retrieves a boolean property from the project, interpreting any value other than `"false"` as
 * `true`.
 *
 * @param name The name of the property to retrieve.
 * @param defaultValue The default boolean value to return if the property is not set. Defaults to
 *   `false`.
 * @return The boolean value of the property, or [defaultValue] if the property is not set.
 * @receiver Gradle [Project] from which the property is read.
 */
fun Project.getBooleanProperty(name: String, defaultValue: Boolean = false): Boolean =
    if (hasProperty(name)) findProperty(name)?.toString() != "false" else defaultValue

/**
 * Determines whether this task is a test task. For simplicity, a task is considered a test task if
 * its name contains "test" (case-insensitive).
 *
 * @return `true` if the task name matches a test task pattern, `false` otherwise.
 */
fun Task.isTestTask(): Boolean = name.matches(Regex(".*[tT]est.*"))
