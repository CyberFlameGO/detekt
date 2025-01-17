import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask

plugins {
    id("releasing")
    alias(libs.plugins.detekt)
    alias(libs.plugins.gradleVersions)
    alias(libs.plugins.sonarqube)
}

allprojects {
    group = "io.gitlab.arturbosch.detekt"
    version = Versions.currentOrSnapshot()

    apply(plugin = "io.gitlab.arturbosch.detekt")

    detekt {
        source = objects.fileCollection().from(
            io.gitlab.arturbosch.detekt.extensions.DetektExtension.DEFAULT_SRC_DIR_JAVA,
            "src/test/java",
            io.gitlab.arturbosch.detekt.extensions.DetektExtension.DEFAULT_SRC_DIR_KOTLIN,
            "src/test/kotlin"
        )
        buildUponDefaultConfig = true
        baseline = file("$rootDir/config/detekt/baseline.xml")

        reports {
            xml.enabled = true
            html.enabled = true
            txt.enabled = true
            sarif.enabled = true
        }
    }

    dependencies {
        detekt(project(":detekt-cli"))
        detektPlugins(project(":custom-checks"))
        detektPlugins(project(":detekt-formatting"))
    }

    tasks.withType<Detekt>().configureEach {
        jvmTarget = "1.8"
    }
    tasks.withType<DetektCreateBaselineTask>().configureEach {
        jvmTarget = "1.8"
    }
}

val analysisDir = file(projectDir)
val baselineFile = file("$rootDir/config/detekt/baseline.xml")
val configFile = file("$rootDir/config/detekt/detekt.yml")
val statisticsConfigFile = file("$rootDir/config/detekt/statistics.yml")

val kotlinFiles = "**/*.kt"
val kotlinScriptFiles = "**/*.kts"
val resourceFiles = "**/resources/**"
val buildFiles = "**/build/**"

val detektFormat by tasks.registering(Detekt::class) {
    description = "Formats whole project."
    parallel = true
    disableDefaultRuleSets = true
    buildUponDefaultConfig = true
    autoCorrect = true
    setSource(analysisDir)
    config.setFrom(listOf(statisticsConfigFile, configFile))
    include(kotlinFiles)
    include(kotlinScriptFiles)
    exclude(resourceFiles)
    exclude(buildFiles)
    baseline.set(baselineFile)
    reports {
        xml.enabled = false
        html.enabled = false
        txt.enabled = false
    }
}

val detektAll by tasks.registering(Detekt::class) {
    description = "Runs the whole project at once."
    parallel = true
    buildUponDefaultConfig = true
    setSource(analysisDir)
    config.setFrom(listOf(statisticsConfigFile, configFile))
    include(kotlinFiles)
    include(kotlinScriptFiles)
    exclude(resourceFiles)
    exclude(buildFiles)
    baseline.set(baselineFile)
    reports {
        xml.enabled = false
        html.enabled = false
        txt.enabled = false
    }
}

val detektProjectBaseline by tasks.registering(DetektCreateBaselineTask::class) {
    description = "Overrides current baseline."
    buildUponDefaultConfig.set(true)
    ignoreFailures.set(true)
    parallel.set(true)
    setSource(analysisDir)
    config.setFrom(listOf(statisticsConfigFile, configFile))
    include(kotlinFiles)
    include(kotlinScriptFiles)
    exclude(resourceFiles)
    exclude(buildFiles)
    baseline.set(baselineFile)
}
