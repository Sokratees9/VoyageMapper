import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Load properties from local.properties manually
val localProps = Properties()
val localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) {
    localProps.load(localPropsFile.inputStream())
    localProps.forEach { key, value ->
        project.extensions.extraProperties[key.toString()] = value
    }
}

android {
    namespace = "org.okane.voyagemapper"
    compileSdk = 34

    defaultConfig {
        applicationId = "org.okane.voyagemapper"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["MAPS_API_KEY"] =
            if (project.hasProperty("MAPS_API_KEY"))
                project.property("MAPS_API_KEY") as String
            else
                ""
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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
    implementation(libs.livedata)
    implementation(libs.viewmodel)
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.convertergson)
    implementation(libs.playservicesmaps)
    implementation(libs.androidmaps)
    implementation(libs.desugar)
    implementation(libs.location)
    implementation(libs.glide)
    annotationProcessor(libs.compiler)
}