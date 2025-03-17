plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.mkayuni.bassbroker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mkayuni.bassbroker"
        minSdk = 24
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Core AndroidX libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose UI dependencies
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Retrofit for network requests
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // ThreeTenABP for date/time handling
    implementation("com.jakewharton.threetenabp:threetenabp:1.3.1")

    // MPAndroidChart for charting
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Coroutines for async operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // WorkManager for background tasks
    implementation("androidx.work:work-runtime-ktx:2.8.1")

    // ViewModel and LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")

    // Material Components
    implementation("com.google.android.material:material:1.10.0")

    // DataStore for preferences
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Compose Activity integration
    implementation("androidx.activity:activity-compose:1.8.2")

    // Compose integration with ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // TensorFlow Lite for ML
    implementation("org.tensorflow:tensorflow-lite:2.12.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.3")

    // TensorFlow Lite Select Ops (Flex Delegate)
    implementation("org.tensorflow:tensorflow-lite-select-tf-ops:2.12.0")

    // TensorFlow Lite GPU Delegate (optional)
    implementation("org.tensorflow:tensorflow-lite-gpu:2.12.0")

    // TensorFlow Lite Metadata
    implementation("org.tensorflow:tensorflow-lite-metadata:0.4.3")

    // Apache Commons Math for statistical calculations
    implementation("org.apache.commons:commons-math3:3.6.1")

    // Additional dependencies for debugging and logging
    implementation("com.jakewharton.timber:timber:5.0.1")

    implementation ("androidx.compose.material:material-icons-extended:1.5.0")
}