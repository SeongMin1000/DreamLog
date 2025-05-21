plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.dreamlog"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.dreamlog"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val key = project.rootProject.file("local.properties")
            .reader()
            .useLines { lines ->
                lines.firstOrNull { it.startsWith("openai.api.key=") }
                    ?.substringAfter("=")
                    ?.trim()
            } ?: "키없음"

        println("✅ 불러온 키: $key")

        buildConfigField("String", "OPENAI_API_KEY", "\"$key\"")
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        viewBinding = true
        dataBinding = true
        buildConfig = true
    }
    packagingOptions {
        resources {
            excludes += "**/*.tflite"
        }
    }

}

dependencies {
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // OkHttp 로깅 (선택)
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // 코루틴 (비동기 처리)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation ("com.google.android.material:material:1.11.0")
    implementation ("com.github.divyanshub024:AndroidDraw:v0.1")
    implementation ("org.tensorflow:tensorflow-lite:2.7.0")
    implementation ("org.tensorflow:tensorflow-lite-support:0.3.0")
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.firebase.auth.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}