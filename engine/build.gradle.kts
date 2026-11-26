import java.time.Duration
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithSimulatorTests

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("jacoco")
}

val iosTargetsEnabled = providers.gradleProperty("quietgrid.ios").orNull == "true"

kotlin {
    explicitApiWarning()

    jvm {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
    }
    if (iosTargetsEnabled) {
        iosArm64()
        iosSimulatorArm64()
    }

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.13.0")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        jvmTest.dependencies {
            implementation("junit:junit:4.13.2")
            implementation("com.lemonappdev:konsist:0.18.1")
        }
    }
}

if (iosTargetsEnabled) {
    providers.gradleProperty("iosSimulatorDevice").orNull?.let { device ->
        kotlin.targets.withType<KotlinNativeTargetWithSimulatorTests>().configureEach {
            testRuns.named("test") { deviceId = device }
        }
    }
}

tasks.withType<AbstractTestTask>().configureEach {
    timeout.set(Duration.ofMinutes(20))
}

tasks.register("test") {
    group = "verification"
    description = "Runs the engine's unit tests on the JVM."
    dependsOn("jvmTest")
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("jvmTest")
    executionData(layout.buildDirectory.file("jacoco/jvmTest.exec"))
    classDirectories.from(layout.buildDirectory.dir("classes/kotlin/jvm/main"))
    sourceDirectories.from("src/commonMain/kotlin", "src/jvmMain/kotlin")
    reports {
        xml.required.set(true)
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/test/html"))
    }
}
