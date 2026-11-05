plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.secretsGradle)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.androidx.room)
}

android {
    namespace = "com.simplyawakeremake"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.marklackey.android.awake"
        minSdk = 24
        targetSdk = 36
        versionCode = 18
        versionName = "11.3.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }
    flavorDimensions += "env"

    productFlavors {
        create("dev") {
            dimension = "env"

            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            buildConfigField("String", "FIREBASE_ENV", "\"dev\"")
        }

        create("prod") {
            dimension = "env"
            buildConfigField("String", "FIREBASE_ENV", "\"prod\"")
        }
    }

    signingConfigs {
        create("release") {
            // Identifiants lus depuis les propriétés Gradle (~/.gradle/gradle.properties ou -P)
            // puis depuis les variables d'environnement (CI). Jamais dans le dépôt.
            fun secret(name: String): String? =
                (project.findProperty(name) as String?)?.takeIf { it.isNotBlank() }
                    ?: System.getenv(name)?.takeIf { it.isNotBlank() }

            val storePath = secret("RELEASE_STORE_FILE")
            val storePass = secret("RELEASE_STORE_PASSWORD")
            val alias = secret("RELEASE_KEY_ALIAS")
            val keyPass = secret("RELEASE_KEY_PASSWORD") ?: storePass

            if (storePath != null && storePass != null && alias != null && keyPass != null) {
                storeFile = file(storePath)
                storePassword = storePass
                keyAlias = alias
                keyPassword = keyPass
            } else {
                logger.warn(
                    "Release signing not configured (RELEASE_STORE_FILE, RELEASE_STORE_PASSWORD, " +
                        "RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD): release artifacts will be unsigned."
                )
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Signé seulement si le keystore est configuré, sinon build non signé (dev, CI sans secrets).
            signingConfigs.getByName("release").takeIf { it.storeFile != null }?.let { signingConfig = it }
        }

        debug {
            isMinifyEnabled = false
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
        buildConfig = true
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    room {
        schemaDirectory("$projectDir/schemas")
    }
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.ui.tooling)

    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    implementation(libs.google.accompanist.systemuicontroller)
    implementation(libs.accompanist.permissions)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    implementation(libs.logging.interceptor)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.glide)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)

    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.androidx.core.testing)
    testImplementation(libs.turbine)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.bundles.room)
    ksp(libs.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.firebase.auth)
    implementation(libs.play.services.auth)
    implementation(libs.kotlinx.coroutines.play.services)
}
