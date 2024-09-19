plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
}

android {
    namespace = "org.marklackey.android.awake"
    compileSdk = 34

    defaultConfig {
        applicationId = "org.marklackey.android.awake"
        minSdk = 24
        targetSdk = 34
        versionCode = 7
        versionName = "2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Android
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation (libs.koin.androidx.compose)
    implementation (libs.androidx.navigation.compose)
    implementation(libs.androidx.activity.compose)
    implementation (libs.google.accompanist.systemuicontroller)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation (libs.androidx.ui.tooling)
    implementation(libs.androidx.material3)

    // Media 3
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // External libraries
    implementation(libs.koin.core)
    implementation(libs.insert.koin.koin.core)
    implementation(platform(libs.koin.bom))
    implementation(libs.logging.interceptor)
    implementation(libs.androidx.runtime.rxjava3)
    implementation (libs.rxandroid)
    implementation(libs.koin.android)
    implementation (libs.retrofit)
    implementation (libs.converter.gson)
    implementation (libs.adapter.rxjava3)
    implementation(libs.rxjava3.rxkotlin)
    implementation (libs.glide)

}