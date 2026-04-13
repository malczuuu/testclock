import com.diffplug.spotless.LineEnding

plugins {
    id("internal.errorprone-convention")
    id("internal.idea-convention")
    id("internal.jacoco-convention")
    id("internal.java-library-convention")
    id("internal.mrjar-module-info-convention")
    id("internal.publishing-convention")
    alias(libs.plugins.nmcp)
    alias(libs.plugins.spotless)
}

dependencies {
    api(libs.jspecify)

    testImplementation(platform(libs.junit.bom))
    testImplementation(platform(libs.spock.bom))
    testImplementation(libs.spock.core)
    testRuntimeOnly(libs.junit.platform.launcher)

    errorprone(libs.errorprone.core)
    errorprone(libs.nullaway)
}

// see buildSrc/src/main/kotlin/internal.publishing-convention.gradle.kts
internalPublishing {
    displayName = "TestClock"
    description = "A mutable & playable java.time.Clock for unit tests."
}

nmcp {
    publishAllPublicationsToCentralPortal {
        username = System.getenv("PUBLISHING_USERNAME")
        password = System.getenv("PUBLISHING_PASSWORD")

        publishingType = "USER_MANAGED"
    }
}

spotless {
    java {
        target("**/src/**/*.java")

        // NOTE: decided not to upgrade Google Java Format, as versions 1.29+ require running it on Java 21
        googleJavaFormat("1.28.0")
        forbidWildcardImports()
        endWithNewline()
        lineEndings = LineEnding.UNIX
    }

    kotlin {
        target("**/src/**/*.kt")

        ktfmt("0.60").metaStyle()
        endWithNewline()
        lineEndings = LineEnding.UNIX
    }

    kotlinGradle {
        target("*.gradle.kts", "buildSrc/*.gradle.kts", "buildSrc/src/**/*.gradle.kts")

        ktlint("1.8.0").editorConfigOverride(mapOf("max_line_length" to "120"))
        endWithNewline()
        lineEndings = LineEnding.UNIX
    }

    groovy {
        target("**/src/**/*.groovy")

        greclipse().configProperties("org.eclipse.jdt.core.formatter.indentation.size=2")
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
