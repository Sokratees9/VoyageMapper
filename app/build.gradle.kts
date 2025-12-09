import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
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

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

android {
    namespace = "org.okane.voyagemapper"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.okane.voyagemapper"
        minSdk = 26
        targetSdk = 35
        versionCode = 7
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["MAPS_API_KEY"] =
            if (project.hasProperty("MAPS_API_KEY"))
                project.property("MAPS_API_KEY") as String
            else
                ""
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = false
    }
}

dependencies {

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.livedata)
    implementation(libs.viewmodel)
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.convertergson)
    implementation(libs.playservicesmaps)
    implementation(libs.androidmaps)
    implementation(libs.location)
    implementation(libs.glide)
    implementation(libs.places)
    implementation(libs.constraintlayout)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.mockito)
    testImplementation(libs.robolectric)
    testImplementation(libs.robo.ext)
    testImplementation(libs.hosuaby)
    testImplementation (libs.json)

    testRuntimeOnly(libs.junit.jupiter.engine)

    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    coreLibraryDesugaring(libs.desugar)

    annotationProcessor(libs.compiler)
}