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
                url = property("internal.pom.url") as String
                inceptionYear = property("internal.pom.inception-year") as String
                licenses {
                    project.findLicenses().forEach {
                        license {
                            name = it.name
                            url = it.url
                            distribution = it.distribution
                            comments = it.comments
                        }
                    }
                }
                developers {
                    project.findDevelopers().forEach {
                        developer {
                            id = it.id
                            name = it.name
                            url = it.url
                            email = it.email
                            organization = it.organization
                            organizationUrl = it.organizationUrl
                        }
                    }
                }
                scm {
                    connection = property("internal.pom.scm.connection") as String
                    developerConnection = property("internal.pom.scm.developer-connection") as String
                    url = property("internal.pom.scm.url") as String
                }
                issueManagement {
                    system = property("internal.pom.issue-management.system") as String
                    url = property("internal.pom.issue-management.url") as String
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

        googleJavaFormat()
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

        ktlint().editorConfigOverride(mapOf("max_line_length" to "120"))
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
 * Represents a single developer entry used in publishing metadata (e.g. POM developers section).
 *
 * All fields except [id] are optional to allow partial definitions in `gradle.properties`. Missing
 * optional values are simply omitted from the generated publication metadata.
 *
 * @property id Unique developer identifier (required).
 * @property name Human-readable developer name.
 * @property email Contact email address.
 * @property url Developer personal or profile URL.
 * @property organization Organization the developer belongs to.
 * @property organizationUrl URL of the organization.
 */
data class Developer(
    val id: String,
    val name: String?,
    val email: String?,
    val url: String?,
    val organization: String?,
    val organizationUrl: String?,
)

/**
 * Represents a single license entry used in publishing metadata (e.g. POM licenses section).
 *
 * All fields except [name] are optional to allow partial definitions in `gradle.properties`.
 * Missing optional values are simply omitted from the generated publication metadata.
 *
 * @property name Human-readable license name.
 * @property url URL to the full license details.
 */
data class License(val name: String?, val url: String?, val distribution: String?, val comments: String?)

/**
 * Evaluates a structured list of developers defined in `gradle.properties`.
 *
 * Developers are resolved using zero-based indexing and the following property naming convention:
 * ```properties
 * internal.pom.developers.0.id=john.doe
 * internal.pom.developers.0.name=JohnDoe
 * internal.pom.developers.0.url=https://johndoe.me
 *
 * internal.pom.developers.1.id=otherdev
 * internal.pom.developers.1.name=Other Developer
 * ```
 *
 * Evaluation stops at the first missing mandatory `id` property. This allows the list to be
 * extended by simply adding the next index.
 *
 * @return An ordered list of resolved [Developer] definitions.
 * @receiver Gradle [Project] from which properties are read.
 */
fun Project.findDevelopers(): List<Developer> {
    val developers = mutableListOf<Developer>()
    var index = 0
    while (true) {
        developers.add(
            Developer(
                id = findProperty("internal.pom.developers.$index.id") as? String ?: break,
                name = findProperty("internal.pom.developers.$index.name") as? String,
                email = findProperty("internal.pom.developers.$index.email") as? String,
                url = findProperty("internal.pom.developers.$index.url") as? String,
                organization = findProperty("internal.pom.developers.$index.organization") as? String,
                organizationUrl = findProperty("internal.pom.developers.$index.organization-url") as? String,
            ),
        )
        index++
    }
    return developers
}

/**
 * Evaluates a structured list of licenses defined in `gradle.properties`.
 *
 * Licenses are resolved using zero-based indexing and the following property naming convention:
 * ```properties
 * internal.pom.licenses.0.name=MIT License
 * internal.pom.licenses.0.url=https://opensource.org/license/MIT
 *
 * internal.pom.licenses.1.name=Apache-2.0 License
 * ```
 *
 * Evaluation stops at the first missing mandatory `name` property. This allows the list to be
 * extended by simply adding the next index.
 *
 * @return An ordered list of resolved [License] definitions.
 * @receiver Gradle [Project] from which properties are read.
 */
fun Project.findLicenses(): List<License> {
    val licenses = mutableListOf<License>()
    var index = 0
    while (true) {
        licenses.add(
            License(
                name = findProperty("internal.pom.licenses.$index.name") as? String ?: break,
                url = findProperty("internal.pom.licenses.$index.url") as? String,
                distribution = findProperty("internal.pom.licenses.$index.distribution") as? String,
                comments = findProperty("internal.pom.licenses.$index.comments") as? String,
            ),
        )
        index++
    }
    return licenses
}

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
