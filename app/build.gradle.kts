plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// versionCode/versionName vengono dalla CI (VERSION_CODE = numero build, monotòno crescente),
// così l'app installata sa se sul repo c'è una versione più recente. Fallback per build locali.
val appVersionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1
val appVersionName = System.getenv("VERSION_NAME") ?: "1.0.$appVersionCode"

android {
    namespace = "com.personal.spese"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.personal.spese"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        // Chiave di firma STABILE (release.keystore nel repo): stessa firma a ogni build →
        // aggiornamenti in-place senza disinstallare. Per un'app personale offline la chiave
        // committata è un compromesso accettabile (serve solo a consentire gli update).
        create("release") {
            storeFile = file("release.keystore")
            storePassword = "spese-release"
            keyAlias = "spese"
            keyPassword = "spese-release"
        }
    }

    buildTypes {
        release {
            // R8: shrinking + ottimizzazione. Le keep-rule per kotlinx.serialization
            // sono in proguard-rules.pro; Room/Compose/Glance/DataStore portano le proprie.
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true // espone BuildConfig.VERSION_CODE/NAME al controllo aggiornamenti
    }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)

    // Predisposte per milestone successive (widget / export)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.kotlinx.serialization.json)
}
