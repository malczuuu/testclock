import org.jetbrains.gradle.ext.Gradle
import org.jetbrains.gradle.ext.JUnit
import org.jetbrains.gradle.ext.runConfigurations
import org.jetbrains.gradle.ext.settings

plugins {
    id("org.jetbrains.gradle.plugin.idea-ext")
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
