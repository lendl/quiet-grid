import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithSimulatorTests

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
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
            api("androidx.room:room-runtime:2.8.5")
            api("androidx.datastore:datastore-preferences-core:1.3.0")
            api("com.squareup.okio:okio:3.17.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
        }
        androidMain.dependencies {
            implementation("androidx.sqlite:sqlite-framework:2.6.2")
        }
        if (iosTargetsEnabled) {
            iosMain.dependencies {
                implementation("androidx.sqlite:sqlite-bundled:2.6.2")
            }
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

dependencies {
    add("kspAndroid", "androidx.room:room-compiler:2.8.5")
    if (iosTargetsEnabled) {
        add("kspIosArm64", "androidx.room:room-compiler:2.8.5")
        add("kspIosSimulatorArm64", "androidx.room:room-compiler:2.8.5")
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
