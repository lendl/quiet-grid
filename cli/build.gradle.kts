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
    // Main.kt's CLI resolves its default --out path ("shared/src/commonMain/composeResources/files") relative
    // to the working directory, matching CLAUDE.md's convention that Gradle tasks are
    // invoked from the repo root. Pin workingDir to the root project so that
    // `./gradlew :cli:run ...` from the repo root resolves default asset paths correctly
    // instead of silently nesting them under cli/.
    workingDir = rootProject.projectDir
}

tasks.named<Test>("test") {
    // Pin test working directory to the root project so that relative asset paths
    // in tests (e.g., "shared/src/commonMain/composeResources/files") resolve correctly.
    workingDir = rootProject.projectDir
    maxHeapSize = "4g"
}

val bankTestSourceSet = sourceSets.create("bankTest") {
    compileClasspath += sourceSets.main.get().output + sourceSets.test.get().output
    runtimeClasspath += sourceSets.main.get().output + sourceSets.test.get().output
}

kotlin {
    target.compilations.named("bankTest") {
        associateWith(target.compilations.getByName("main"))
        associateWith(target.compilations.getByName("test"))
    }
}

configurations.named("bankTestImplementation") { extendsFrom(configurations.testImplementation.get()) }
configurations.named("bankTestRuntimeOnly") { extendsFrom(configurations.testRuntimeOnly.get()) }

tasks.register<Test>("bankTest") {
    description = "Checks every committed puzzle bank, Journey file and word list."
    group = "verification"
    testClassesDirs = bankTestSourceSet.output.classesDirs
    classpath = bankTestSourceSet.runtimeClasspath
    workingDir = rootProject.projectDir
    maxHeapSize = "4g"
}

dependencies {
    implementation(project(":engine"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.13.0")
    implementation("org.apache.lucene:lucene-analysis-common:10.6.0")

    testImplementation("junit:junit:4.13.2")
}

tasks.named("jacocoTestReport") {
    dependsOn(tasks.named("test"))
}
