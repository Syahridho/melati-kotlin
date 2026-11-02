import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Konfigurasi lingkungan (base URL, API key, channel id) dibaca dari
// local.properties — file yang tidak masuk VCS. Fallback ke gradle property /
// -P agar build CI tetap bisa tanpa file itu. Lihat README "Konfigurasi".
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun melatiConfig(name: String, gradleProperty: String = name, fallback: String = ""): String =
    localProperties.getProperty(name)?.takeIf { it.isNotBlank() }
        ?: providers.gradleProperty(gradleProperty).orNull?.takeIf { it.isNotBlank() }
        ?: fallback

android {
    namespace = "com.example.melati"
    compileSdk {
        version = release(37)
    }

    // API key tidak pernah ditulis di source. Sumbernya local.properties
    // (API_KEY=...), fallback -Pmelati.apiKey=... saat build rilis.
    val melatiApiKey = melatiConfig("API_KEY", "melati.apiKey")

    defaultConfig {
        applicationId = "com.example.melati"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // PRD §8: base URL disediakan lewat BuildConfig. Retrofit mewajibkan
        // trailing slash.
        val apiBaseUrl = melatiConfig(
            "API_BASE_URL",
            "melati.baseUrl",
            "https://test.syahridho.my.id/",
        ).let { if (it.endsWith("/")) it else "$it/" }

        buildConfigField("String", "BASE_URL", "\"$apiBaseUrl\"")
        // Server test menolak request tanpa API key (401). Nilai diisi dari
        // local.properties / CI, JANGAN di-commit. Lihat docs/API-ASSUMPTIONS.md.
        buildConfigField("String", "API_KEY", "\"$melatiApiKey\"")
        // PRD §5.1 menyebut channel_id = 1; nilai final datang dari backend dan
        // disetel di local.properties (CHANNEL_ID=...).
        buildConfigField("String", "CHANNEL_ID", "\"${melatiConfig("CHANNEL_ID", "melati.channelId", "1")}\"")
    }

    buildTypes {
        release {
            // AGP 9.3+: satu flag ini mengaktifkan code + optimized resource shrinking.
            // Keep rules diletakkan di src/main/keepRules/*.keep.
            optimization {
                enable = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
