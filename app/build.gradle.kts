plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("androidx.navigation.safeargs.kotlin")
}

android {
    namespace = "com.gigakin.stockbuddy"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.gigakin.stockbuddy"
        minSdk = 28        // NFR-22: Android 9+
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0-mvp"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // BuildConfig fields for demo-mode limits (Section 4, MVP Scope doc).
        // These are deliberately build-time constants, not stored config, so they're
        // trivial to raise/strip out before a real pilot. See util/DemoLimits.kt.
        buildConfigField("int", "DEMO_MAX_ITEMS", "50")
        buildConfigField("int", "DEMO_MAX_CATEGORIES", "10")
        buildConfigField("int", "DEMO_MAX_SESSIONS", "25")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        // Common need once native .so libs from hardware SDKs are added (NFR-23).
        jniLibs.useLegacyPackaging = false
    }
}

dependencies {
    // Core / AndroidX
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.fragment:fragment-ktx:1.8.1")
    implementation("androidx.activity:activity-ktx:1.9.0")

    // Material Design 3 (NFR-10a)
    implementation("com.google.android.material:material:1.12.0")

    // Lifecycle / ViewModel (NFR-27 layered architecture)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")

    // Navigation Component
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

    // Room (NFR-15: local SQLite via Room)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // CSV (Bulk Linking / Export — default library decision from scope doc Section 7)
    implementation("com.opencsv:opencsv:5.9")

    // JSON (domain-specific field attributes, Section 1.7)
    implementation("org.json:json:20240303")

    // Chainway SDK — uncomment once the real .aar is placed in app/libs/ (see app/libs/README.md)
    // implementation(name = "chainway-sdk", ext = "aar")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
