import org.apache.tools.ant.util.JavaEnvUtils.VERSION_1_8
//import org.jetbrains.kotlin.storage.CacheResetOnProcessCanceled.enabled

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.gms.google-services")
    id("kotlin-parcelize")
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"



}

android {
    namespace = "com.example.argusapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.argusapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

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
        viewBinding = true

    }
    composeOptions {
        kotlinCompilerExtensionVersion = "..."
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

}
configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-stdlib:1.9.0")
        force("org.jetbrains.kotlin:kotlin-stdlib-common:1.9.0")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.0")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.0")
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
    implementation(libs.material)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.room.common.jvm)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.play.services.location)
    implementation(libs.play.services.maps)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-analytics")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("org.osmdroid:osmdroid-android:6.1.14")




}