plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.iaderm"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.iaderm"
        minSdk = 26
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        mlModelBinding = true
    }

    aaptOptions {
        noCompress("tflite")
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.core.ktx)
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)

    // UI Components
    implementation(libs.constraintlayout)
    implementation(libs.coordinatorlayout)
    implementation(libs.material)
    implementation(libs.viewpager2)
    implementation(libs.recyclerview)

    // Room Database (local persistence)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    // Lifecycle (ViewModel + LiveData)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)

    // CameraX
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.mlkit.face.detection)

    // TensorFlow Lite
    implementation(libs.tensorflow.lite)
    implementation(libs.tensorflow.lite.support)
}