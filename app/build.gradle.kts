plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Cargar GEMINI_API_KEY desde local.properties o variable de entorno (no hardcodear secretos)
val geminiApiKey: String = run {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localFile.readLines()
            .map { it.trim() }
            .firstOrNull { it.startsWith("GEMINI_API_KEY=") }
            ?.substringAfter("=")
            ?: System.getenv("GEMINI_API_KEY")
            ?: ""
    } else {
        System.getenv("GEMINI_API_KEY") ?: ""
    }
}

android {
    namespace = "com.waveapp.tarotgemini"
    // compileSdk debe ser un entero en Kotlin DSL
    compileSdk = 36

    defaultConfig {
        applicationId = "com.waveapp.tarotgemini"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Exponer la API key como BuildConfig.GEMINI_API_KEY (valor vacío si no está definida localmente)
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    // quitar kotlinOptions de aquí; lo configuramos usando tasks.withType más abajo
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// Configurar kotlinOptions (jvmTarget) de forma segura
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")

    // Gemini AI SDK
    implementation("com.google.ai.client.generativeai:generativeai:0.1.2")

    // Coroutines para operaciones asíncronas
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
