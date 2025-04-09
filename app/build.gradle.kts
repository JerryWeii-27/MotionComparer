plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.googlemediapipetest"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.googlemediapipetest"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
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
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.tasks.vision)
//    implementation("com.google.mediapipe:solution-core:0.10.20")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // PictureSelector basic (Necessary)
    implementation(libs.pictureselector)

    // image compress library (Not necessary)
    implementation(libs.compress)

    // uCrop library (Not necessary)
    implementation(libs.ucrop)

    // simple camerax library (Not necessary)
    implementation(libs.camerax)

    // https://mvnrepository.com/artifact/com.github.bumptech.glide/glide
    implementation(libs.glide)

    // https://mvnrepository.com/artifact/com.google.android.exoplayer/exoplayer
    implementation(libs.exoplayer)

    // https://mvnrepository.com/artifact/androidx.media/media
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)

    // https://mvnrepository.com/artifact/androidx.preference/preference-ktx
    implementation(libs.androidx.preference.ktx)
}