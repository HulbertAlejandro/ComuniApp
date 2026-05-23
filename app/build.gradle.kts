import java.util.Properties
import com.android.build.api.dsl.ApplicationExtension
// Mantenemos el import por si acaso, pero usaremos la clase explícita abajo para ir sobre seguro
import com.google.firebase.appdistribution.gradle.AppDistributionExtension

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.devtools.ksp)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.firebase.appdistribution)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

configure<ApplicationExtension> {
    namespace = "com.miempresa.comuniapp"

    // Subimos a SDK 35/36 según lo que pida tu entorno para mitigar advertencias de compatibilidad vieja
    compileSdk = 36

    defaultConfig {
        applicationId = "com.miempresa.comuniapp"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val openRouterKey = localProperties.getProperty("OPENROUTER_API_KEY") ?: ""
        buildConfigField("String", "OPENROUTER_API_KEY", "\"$openRouterKey\"")
    }

    buildTypes {
        getByName("debug") {
            isDebuggable = true

            (this as ExtensionAware).extensions.configure<AppDistributionExtension>("firebaseAppDistribution") {
                artifactType = "APK"
                releaseNotes = "ComuniApp Debug — Rama activa: ${getGitBranch()}"
                groups = "testers-comuniapp-interno"
            }
        }

        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // SOLUCIÓN DEFINITIVA: Mismo enfoque agnóstico del azúcar sintáctico de Gradle
            (this as ExtensionAware).extensions.configure<AppDistributionExtension>("firebaseAppDistribution") {
                artifactType = "APK"
                releaseNotes = "ComuniApp v${defaultConfig.versionName} — Build oficial de Entrega"
                groups = "testers-comuniapp-interno"
                serviceCredentialsFile = "app/credentials/firebase-service-account.json"
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Compose
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    // Networking & APIs
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // WorkManager
    implementation(libs.androidx.work.runtime)
    implementation(libs.hilt.work)
    ksp(libs.hilt.compiler.work)

    // Firebase
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.storage)

    // Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // DataStore
    implementation(libs.data.store)

    // Mapbox
    implementation(libs.maps.android)
    implementation(libs.maps.compose)

    // Icons
    implementation(libs.material.icons.extended)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

fun getGitBranch(): String {
    return try {
        val process = ProcessBuilder("git", "rev-parse", "--abbrev-ref", "HEAD")
            .redirectErrorStream(true)
            .start()
        process.inputStream.bufferedReader().readLine()?.trim() ?: "unknown"
    } catch (_: Exception) {
        "unknown"
    }
}