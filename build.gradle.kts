plugins {
    id("com.android.application") version "9.5.1" apply false
    id("com.android.test") version "9.5.1" apply false
    id("com.android.kotlin.multiplatform.library") version "9.5.1" apply false
    id("org.jetbrains.compose") version "1.12.2" apply false
    id("org.jetbrains.kotlin.jvm") version "2.4.21" apply false
    id("org.jetbrains.kotlin.multiplatform") version "2.4.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
    id("com.android.compose.screenshot") version "0.0.1-alpha16" apply false
    id("com.google.devtools.ksp") version "2.3.13" apply false
    id("io.github.takahirom.roborazzi") version "1.77.0" apply false
    id("androidx.baselineprofile") version "1.5.0" apply false
    id("dev.detekt") version "2.0.0-alpha.6" apply false
}

subprojects {
    if (name == "baselineprofile") return@subprojects
    apply(plugin = "dev.detekt")
    extensions.configure<dev.detekt.gradle.extensions.DetektExtension> {
        buildUponDefaultConfig.set(true)
        ignoreFailures.set(true)
        parallel.set(true)
        config.setFrom(rootProject.file("config/detekt/detekt.yml"))
        source.setFrom(files("src/main/java", "src/main/kotlin", "src/commonMain/kotlin", "src/jvmMain/kotlin", "src/androidMain/kotlin", "src/iosMain/kotlin"))
    }
    dependencies.add("detektPlugins", "io.nlopez.compose.rules:detekt:0.6.8")
}
