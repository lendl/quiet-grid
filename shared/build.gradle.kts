import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithSimulatorTests

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

val iosTargetsEnabled = providers.gradleProperty("quietgrid.ios").orNull == "true"

kotlin {
    android {
        namespace = "com.quietgrid.shared"
        compileSdk = 37
        minSdk = 26
        withHostTest { }
        compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
    }
    if (iosTargetsEnabled) {
        listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
            target.binaries.framework {
                baseName = "Shared"
                isStatic = true
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":engine"))
            implementation("org.jetbrains.compose.runtime:runtime:1.12.2")
            implementation("org.jetbrains.compose.foundation:foundation:1.12.2")
            implementation("org.jetbrains.compose.material3:material3:1.9.0")
            implementation("org.jetbrains.compose.components:components-resources:1.12.2")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.13.0")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

compose.resources {
    packageOfResClass = "com.quietgrid.shared.resources"
}

if (iosTargetsEnabled) {
    providers.gradleProperty("iosSimulatorDevice").orNull?.let { device ->
        kotlin.targets.withType<KotlinNativeTargetWithSimulatorTests>().configureEach {
            testRuns.named("test") { deviceId = device }
        }
    }
}
