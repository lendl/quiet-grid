plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("application")
    id("jacoco")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
}

application {
    mainClass.set("com.quietgrid.cli.MainKt")
}

tasks.named<JavaExec>("run") {
    // The `run` task (JavaExec) defaults workingDir to this subproject's directory.
    // Main.kt's CLI resolves its default --out path ("app/src/main/assets") relative
    // to the working directory, matching CLAUDE.md's convention that Gradle tasks are
    // invoked from the repo root. Pin workingDir to the root project so that
    // `./gradlew :cli:run ...` from the repo root resolves default asset paths correctly
    // instead of silently nesting them under cli/.
    workingDir = rootProject.projectDir
}

tasks.named<Test>("test") {
    // Pin test working directory to the root project so that relative asset paths
    // in tests (e.g., "app/src/main/assets") resolve correctly.
    workingDir = rootProject.projectDir
}

dependencies {
    implementation(project(":engine"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    testImplementation("junit:junit:4.13.2")
}

tasks.named("jacocoTestReport") {
    dependsOn(tasks.named("test"))
}
