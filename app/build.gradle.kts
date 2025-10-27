import org.jetbrains.kotlin.gradle.dsl.JvmTarget
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-parcelize")
}

android {
    namespace = "com.applications.player"
    compileSdk = 36
    ndkVersion = "29.0.13846066"

    defaultConfig {
        applicationId = "com.applications.player"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        } // FIX: Replaced Groovy-style call with correct Kotlin DSL property syntax

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
    buildFeatures{
        viewBinding = true
        dataBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    /*kotlinOptions {
        jvmTarget = "11"
    }*/
    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
        }
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
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)


    implementation("androidx.compose.material3:material3:1.5.0-alpha07")
    implementation("com.google.android.material:material:1.14.0-alpha06")

    implementation("androidx.compose.material:material-icons-core-android:1.7.8")


    // Koin
    implementation("io.insert-koin:koin-core:4.1.1")
    implementation("io.insert-koin:koin-android:4.1.1")
    implementation("io.insert-koin:koin-androidx-compose:4.1.1")


    //viewmodel
    implementation("io.insert-koin:koin-androidx-compose:4.1.1") // This might not be needed for your setup
//implementation("io.insert-koin:koin-androidx-viewmodel:3.5.0") // This is the key one!
    implementation("androidx.media3:media3-exoplayer:1.8.0")
    implementation("androidx.media3:media3-ui:1.8.0")

    implementation("androidx.media3:media3-session:1.8.0")

    implementation("androidx.media:media:1.7.0")

    implementation("com.github.MasayukiSuda:Mp4Composer-android:v0.4.1")


    implementation(libs.bundles.coil)

    implementation("io.coil-kt.coil3:coil-video:3.3.0")

    implementation("com.github.bumptech.glide:glide:5.0.5")

    // Example dependencies for the Data module (NOT the UI module)
    //("com.example.torrent:torrent-client:1.0.0")



    implementation ("org.libtorrent4j:libtorrent4j:2.1.0-38")


   implementation ("org.libtorrent4j:libtorrent4j:2.1.0-38")
   implementation ("org.libtorrent4j:libtorrent4j-android-arm:2.1.0-38")
   implementation ("org.libtorrent4j:libtorrent4j-android-arm64:2.1.0-38")
   implementation ("org.libtorrent4j:libtorrent4j-android-x86:2.1.0-38")
   implementation ("org.libtorrent4j:libtorrent4j-android-x86_64:2.1.0-38")


    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")






}