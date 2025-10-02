plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    // Add the Google services Gradle plugin
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.presstotransmit"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.presstotransmit"
        minSdk = 28
        targetSdk = 36
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

    flavorDimensions += "environment"
    productFlavors {
        create("ptt") {
            applicationId = "com.example.presstotransmit"
        }
        create("example") {
            applicationId = "com.example.Example"
        }
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

    // Import the Firebase BoM (see: https://firebase.google.com/docs/android/learn-more#bom)
    implementation(platform("com.google.firebase:firebase-bom:34.3.0"))

    implementation("com.google.gms:google-services:4.3.8")

    // Firebase Cloud Messaging
    implementation("com.google.firebase:firebase-messaging")

    // For an optimal experience using FCM, add the Firebase SDK
    // for Google Analytics. This is recommended, but not required.
    implementation("com.google.firebase:firebase-analytics")

    implementation("com.google.firebase:firebase-installations:19.0.1")

    val work_version = "2.10.4"
    implementation("androidx.work:work-runtime:$work_version") // (Java only)
    implementation("androidx.work:work-runtime-ktx:$work_version") // Kotlin + coroutines
    implementation("androidx.work:work-rxjava2:$work_version") // optional - RxJava2 support
    implementation("androidx.work:work-gcm:$work_version") // optional - GCMNetworkManager support
    androidTestImplementation("androidx.work:work-testing:$work_version") // optional - Test helpers
    implementation("androidx.work:work-multiprocess:$work_version") // optional - Multiprocess support

    implementation("com.github.SmartWalkieOrg:VoicePing-Walkie-Talkie-AndroidSDK:1.0")
    //implementation("com.squareup.okhttp3:okhttp:3.9.0")
    //implementation("pub.devrel:easypermissions:0.4.2")
    implementation(libs.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}