import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

// Read the shared Google Maps API key from gradle.properties
val mapsApiKey = providers.gradleProperty("MAPS_API_KEY").get()

android {
    namespace = "uws.ac.uk.bookmark"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    buildFeatures {
        buildConfig = true
        resValues = true
    }

    defaultConfig {
        applicationId = "uws.ac.uk.bookmark"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        // load api keys from local.properties
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")

        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use {
                localProperties.load(it)
            }
        }

        // nyt books api key
        val nytApiKey = localProperties.getProperty("NYT_API_KEY", "")
        buildConfigField("String", "NYT_API_KEY", "\"$nytApiKey\"")

        // google books api key
        val googleBooksApiKey = localProperties.getProperty("GOOGLE_BOOKS_API_KEY", "")
        buildConfigField("String", "GOOGLE_BOOKS_API_KEY", "\"$googleBooksApiKey\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

// Makes the API key available to Google Maps and Places
        resValue("string", "google_maps_key", mapsApiKey)

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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")

// Google Maps - display the map and markers
    implementation("com.google.android.gms:play-services-maps:20.1.0")

// Google Places - search for nearby libraries and bookshops
    implementation("com.google.android.libraries.places:places:6.0.2")

}
