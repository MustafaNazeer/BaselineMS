import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.devtools.ksp") version "1.9.24-1.0.20"
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.mustafanazeer.baselinems"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mustafanazeer.baselinems"
        minSdk = 31
        targetSdk = 35
        versionCode = 2
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keystorePropsFile = rootProject.file("app/keystore.properties")
            if (keystorePropsFile.exists()) {
                val props = Properties().apply {
                    FileInputStream(keystorePropsFile).use { stream -> load(stream) }
                }
                val storeFileProp = props.getProperty("storeFile")
                    ?: throw GradleException("app/keystore.properties is missing the storeFile property.")
                val storePasswordProp = props.getProperty("storePassword")
                    ?: throw GradleException("app/keystore.properties is missing the storePassword property.")
                val keyAliasProp = props.getProperty("keyAlias")
                    ?: throw GradleException("app/keystore.properties is missing the keyAlias property.")
                val keyPasswordProp = props.getProperty("keyPassword")
                    ?: throw GradleException("app/keystore.properties is missing the keyPassword property.")
                storeFile = rootProject.file(storeFileProp)
                storePassword = storePasswordProp
                keyAlias = keyAliasProp
                keyPassword = keyPasswordProp
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            val keystorePropsFile = rootProject.file("app/keystore.properties")
            if (keystorePropsFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                signingConfig = null
            }
        }
    }

    gradle.taskGraph.whenReady {
        val hasReleaseTask = allTasks.any { task ->
            val name = task.name
            name.contains("Release", ignoreCase = true) &&
                (name.startsWith("assemble") || name.startsWith("bundle") || name.startsWith("package"))
        }
        if (hasReleaseTask) {
            val keystorePropsFile = rootProject.file("app/keystore.properties")
            if (!keystorePropsFile.exists()) {
                throw GradleException(
                    "Missing app/keystore.properties for the release signing config. " +
                        "Copy app/keystore.properties.example to app/keystore.properties and fill in the four values. " +
                        "See docs/observability/signing-runbook.md for the keystore generation steps."
                )
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    sourceSets {
        getByName("debug") {
            assets.srcDirs("$projectDir/schemas")
        }
    }

    packaging {
        resources {
            excludes += setOf("META-INF/AL2.0", "META-INF/LGPL2.1")
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation(composeBom)
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    implementation("androidx.camera:camera-core:1.4.2")
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")
    implementation("com.google.mlkit:face-detection:16.1.7")

    implementation("com.patrykandpatrick.vico:compose-m3:1.14.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("androidx.room:room-testing:2.6.1")
    testImplementation("org.robolectric:robolectric:4.12.2")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("io.mockk:mockk:1.13.11")
}
