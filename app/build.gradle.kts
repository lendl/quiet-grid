plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.android.compose.screenshot")
    id("jacoco")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("io.github.takahirom.roborazzi")
    id("androidx.baselineprofile")
}

android {
    namespace = "com.quietgrid.app"
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "com.quietgrid.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 42
        versionName = "1.12.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("release") {
            storeFile = file(System.getenv("ANDROID_KEYSTORE_PATH") ?: "debug.keystore")
            storePassword = System.getenv("ANDROID_STORE_PASSWORD") ?: "android"
            keyAlias = System.getenv("ANDROID_KEY_ALIAS") ?: "androiddebugkey"
            keyPassword = System.getenv("ANDROID_KEY_PASSWORD") ?: "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
            enableUnitTestCoverage = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    experimentalProperties["android.experimental.enableScreenshotTest"] = true

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        disable += "MissingTranslation"
    }
}

abstract class GenerateThemeCountsTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val puzzleFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val counts = sortedMapOf<String, MutableMap<String, MutableMap<String, MutableMap<String, Int>>>>()
        puzzleFiles.files.sortedBy { it.name }.forEach { file ->
            val game = file.name.substringBefore("_puzzles")
            @Suppress("UNCHECKED_CAST")
            val entries = groovy.json.JsonSlurper().parse(file) as List<Map<String, Any?>>
            entries.forEach { entry ->
                val tier = entry["difficulty"] as String? ?: file.name.substringAfter("_puzzles_").substringBefore(".json")
                val locale = entry["locale"] as String? ?: "en"
                val theme = entry["themeId"] as String
                val byTheme = counts.getOrPut(game) { sortedMapOf() }
                    .getOrPut(tier) { sortedMapOf() }
                    .getOrPut(locale) { sortedMapOf() }
                byTheme[theme] = (byTheme[theme] ?: 0) + 1
            }
        }
        val out = outputDir.get().asFile
        out.mkdirs()
        out.resolve("theme_counts.json").writeText(groovy.json.JsonOutput.toJson(counts))
    }
}

abstract class CheckPermissionsTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val mergedManifest: RegularFileProperty

    @get:Input
    abstract val allowedPermissions: SetProperty<String>

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun check() {
        val androidNs = "http://schemas.android.com/apk/res/android"
        val factory = javax.xml.parsers.DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val document = factory.newDocumentBuilder().parse(mergedManifest.get().asFile)
        val declared = listOf("uses-permission", "uses-permission-sdk-23").flatMap { tag ->
            val nodes = document.getElementsByTagName(tag)
            (0 until nodes.length).map { (nodes.item(it) as org.w3c.dom.Element).getAttributeNS(androidNs, "name") }
        }.toSortedSet()
        report.get().asFile.apply { parentFile.mkdirs() }.writeText(declared.joinToString("\n", postfix = "\n"))
        val unexpected = declared - allowedPermissions.get()
        if (unexpected.isNotEmpty()) {
            throw GradleException(
                "Permissions not on the allowlist: ${unexpected.joinToString()}. " +
                    "Remove them with tools:node=\"remove\" in app/src/main/AndroidManifest.xml, " +
                    "or add them to allowedPermissions in app/build.gradle.kts if the app really needs them."
            )
        }
    }
}

val generateThemeCounts = tasks.register<GenerateThemeCountsTask>("generateThemeCounts") {
    puzzleFiles.from(fileTree("src/main/assets") { include("wordsearch_puzzles_*.json", "themeclear_puzzles.json") })
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(generateThemeCounts, GenerateThemeCountsTask::outputDir)
    }
    onVariants(selector().withBuildType("release")) { variant ->
        variant.outputs.forEach { output ->
            (output as com.android.build.api.variant.impl.VariantOutputImpl).outputFileName.set(
                output.versionName.map { "quiet-grid-v$it.apk" }
            )
        }
    }
    onVariants { variant ->
        val variantName = variant.name.replaceFirstChar { it.uppercase() }
        val checkPermissions = tasks.register<CheckPermissionsTask>("check${variantName}Permissions") {
            mergedManifest.set(variant.artifacts.get(com.android.build.api.artifact.SingleArtifact.MERGED_MANIFEST))
            allowedPermissions.set(
                variant.applicationId.map { appId ->
                    setOf(
                        "android.permission.POST_NOTIFICATIONS",
                        "android.permission.VIBRATE",
                        "android.permission.RECEIVE_BOOT_COMPLETED",
                        "$appId.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
                    )
                }
            )
            report.set(layout.buildDirectory.file("reports/permissions/${variant.name}.txt"))
        }
        tasks.matching { it.name == "assemble$variantName" }.configureEach { dependsOn(checkPermissions) }
        tasks.named("check") { dependsOn(checkPermissions) }
    }
}

dependencies {
    implementation(project(":engine"))
    baselineProfile(project(":baselineprofile"))
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-process:2.11.0")
    implementation("androidx.navigation:navigation-compose:2.10.2")
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("com.google.dagger:hilt-android:2.60.1")
    implementation("androidx.hilt:hilt-lifecycle-viewmodel-compose:1.4.0")
    implementation("androidx.work:work-runtime-ktx:2.12.0")
    implementation("androidx.hilt:hilt-work:1.4.0")
    ksp("androidx.hilt:hilt-compiler:1.4.0")
    ksp("com.google.dagger:hilt-android-compiler:2.60.1")
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")
    implementation("androidx.glance:glance-appwidget:1.2.0")
    implementation("androidx.glance:glance-material3:1.2.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.glance:glance-appwidget-testing:1.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    testImplementation("io.mockk:mockk:1.14.11")
    testImplementation("org.robolectric:robolectric:4.17")
    testImplementation(platform("androidx.compose:compose-bom:2026.09.00"))
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("androidx.test.ext:junit:1.3.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.76.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.76.0")
    testImplementation("com.google.dagger:hilt-android-testing:2.60.1")
    kspTest("com.google.dagger:hilt-android-compiler:2.60.1")

    androidTestImplementation(platform("androidx.compose:compose-bom:2026.09.00"))
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    screenshotTestImplementation("com.android.tools.screenshot:screenshot-validation-api:0.0.1-alpha16")
    screenshotTestImplementation("androidx.compose.ui:ui-tooling")
}
