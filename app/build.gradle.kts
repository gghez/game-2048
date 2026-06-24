import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.github.triplet.play")
}

// Local, never-committed config: signing credentials and optional Play Games ids.
// When absent (e.g. CI without secrets), signing/leaderboards degrade gracefully.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.gghez.game2048"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.gghez.game2048"
        minSdk = 24
        targetSdk = 35
        // Tag-driven on CI: the release workflow exports VERSION_CODE / VERSION_NAME
        // from the pushed git tag (vX.Y.Z). Local builds fall back to 1 / "1.0".
        versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionName = System.getenv("VERSION_NAME") ?: "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Google Play Games ids are read from local.properties (never committed).
        // When absent, the generated strings are empty and the app falls back to
        // NoopLeaderboard, so it builds and runs without a Play Console account.
        resValue("string", "game_services_project_id", localProps.getProperty("playGamesAppId", ""))
        resValue("string", "leaderboard_speed", localProps.getProperty("leaderboardSpeed", ""))
        resValue("string", "leaderboard_efficiency", localProps.getProperty("leaderboardEfficiency", ""))
        resValue("string", "leaderboard_time", localProps.getProperty("leaderboardTime", ""))
    }
    signingConfigs {
        // Release signing is configured only when local.properties provides the
        // upload keystore. Otherwise the "release" config is absent; the release
        // build type then fails fast (see below) so we never ship an unsigned AAB.
        val ksPath = localProps.getProperty("RELEASE_STORE_FILE")
        if (ksPath != null) create("release") {
            storeFile = file(ksPath)
            storePassword = localProps.getProperty("RELEASE_STORE_PASSWORD")
            keyAlias = localProps.getProperty("RELEASE_KEY_ALIAS")
            keyPassword = localProps.getProperty("RELEASE_KEY_PASSWORD")
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            // R8 full-mode minification + resource shrinking: drops the unused
            // material-icons-extended graph and obfuscates the release AAB.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

// Fail fast on an unsigned release. Releases are CI-only (tag-driven): CI always
// writes the signing props into local.properties, so a missing "release" signing
// config means a misconfigured release attempt — never ship an unsigned AAB.
// Guard on the resolved task graph so debug builds, unit tests, and any non-release
// task (assembleDebug, testDebugUnitTest, …) are never affected.
gradle.taskGraph.whenReady {
    val buildsRelease = allTasks.any { task ->
        task.project == project &&
            (task.name.contains("Release") &&
                (task.name.startsWith("assemble") ||
                    task.name.startsWith("bundle") ||
                    task.name.startsWith("package")))
    }
    if (buildsRelease && android.signingConfigs.findByName("release") == null) {
        throw GradleException(
            "Release signing not configured: set RELEASE_STORE_FILE/" +
                "RELEASE_STORE_PASSWORD/RELEASE_KEY_ALIAS/RELEASE_KEY_PASSWORD in " +
                "local.properties — releases are CI-only (tag-driven)",
        )
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("com.google.android.gms:play-services-games-v2:21.0.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.0.21")
}

// Gradle Play Publisher: CLI-driven uploads to Google Play.
// The service-account JSON is git-ignored; when it is missing the plugin tasks
// simply fail at call time, so normal builds are unaffected.
play {
    serviceAccountCredentials.set(rootProject.file("play-service-account.json"))
    defaultToAppBundles.set(true)
    track.set("internal")
}
