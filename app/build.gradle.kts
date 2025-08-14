plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.example.ibero"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.ibero"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

// Dependencias por defecto de Android Studio (algunas ya estaban)
    implementation(libs.androidx.core.ktx) // Ya estaba
    implementation(libs.androidx.appcompat) // Nueva
    implementation(libs.android.material) // Nueva
    implementation(libs.androidx.constraintlayout) // Nueva

    // Kotlin Coroutines para manejo asíncrono
    implementation(libs.kotlinx.coroutines.core) // Nueva
    implementation(libs.kotlinx.coroutines.android) // Nueva

    // Android Architecture Components - Lifecycle (ViewModel y LiveData)
    implementation(libs.androidx.lifecycle.viewmodel.ktx) // Nueva
    implementation(libs.androidx.lifecycle.livedata.ktx) // Nueva
    implementation(libs.androidx.lifecycle.runtime.ktx) // Ya estaba o usa la nueva si cambiaste el nombre
    implementation(libs.androidx.lifecycle.runtime.ktx.v2) // Si usaste v2 para evitar el conflicto

    // Room Persistence Library (Base de datos local SQLite)
    implementation(libs.androidx.room.runtime) // Nueva
    kapt(libs.androidx.room.compiler) // Nueva (asegúrate de tener el plugin `kotlin-kapt` en tu `plugins` bloque)
    implementation(libs.androidx.room.ktx) // Nueva

    // Retrofit (para llamadas a API, por ejemplo, a Google Apps Script)
    implementation(libs.square.retrofit) // Nueva
    implementation(libs.square.retrofit.converter.gson) // Nueva

    // Librería para cargar imágenes
    implementation(libs.bumptech.glide) // Nueva
    kapt(libs.bumptech.glide.compiler) // Nueva (requiere el plugin `kotlin-kapt`)


    // Dependencias de testing y Compose existentes
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.squareup.okhttp)
    implementation(libs.squareup.okhttp.logging.interceptor)

    // OkHttp para llamadas a la API
    implementation(libs.okhttp.v4100)

    // Coroutines para gestión de hilos
    implementation(libs.kotlinx.coroutines.core.v171)
    implementation(libs.kotlinx.coroutines.android.v171)

}