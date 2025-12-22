@file:Suppress("UnstableApiUsage")

import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    id("org.jetbrains.kotlin.plugin.serialization")
    id("io.github.mlm-games.apk-dist") version "0.4.2"
}

val properties = Properties()
val localPropertiesFile: File = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { properties.load(it) }
}

android {
    namespace = "org.mlm.browkorftv"
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "org.mlm.browkorftv"

        versionCode = libs.versions.versionCode.get().toInt()
        versionName = libs.versions.versionName.get()

        vectorDrawables.useSupportLibrary = true
    }

    val enableApkSplits = (providers.gradleProperty("enableApkSplits").orNull ?: "true").toBoolean()
    val includeUniversalApk = (providers.gradleProperty("includeUniversalApk").orNull ?: "false").toBoolean()
    val targetAbi = providers.gradleProperty("targetAbi").orNull

    splits {
        abi {
            isEnable = enableApkSplits
            reset()
            if (enableApkSplits) {
                if (targetAbi != null) {
                    include(targetAbi)
                } else {
                    include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
                }
            }
            isUniversalApk = includeUniversalApk
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_PATH") ?: "${rootProject.projectDir}/release.keystore")
            storePassword = System.getenv("STORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = false
            enableV4Signing = false
        }
    }

    buildTypes {
        getByName("debug") {
            isDebuggable = true
            isShrinkResources = false
        }
        getByName("release") {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
            buildConfigField("Long", "BUILD_TIME", "${System.currentTimeMillis()}L")
        }
    }

    flavorDimensions += listOf("appstore", "webengine")
    productFlavors {
        create("generic") {
            dimension = "appstore"
            buildConfigField("Boolean", "BUILT_IN_AUTO_UPDATE", "true")
        }
        create("google") {
            dimension = "appstore"
            buildConfigField("Boolean", "BUILT_IN_AUTO_UPDATE", "false")
        }
        create("foss") {
            dimension = "appstore"
            applicationIdSuffix = ".foss"
            buildConfigField("Boolean", "BUILT_IN_AUTO_UPDATE", "false")
        }

        create("geckoIncluded") {
            dimension = "webengine"
        }
        create("geckoExcluded") {
            dimension = "webengine"
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    dependenciesInfo {
        includeInApk = false
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

apkDist {
    artifactNamePrefix.set("browkorftv")
}

// Configure all tasks that are instances of AbstractArchiveTask (From Target)
tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

dependencies {
    implementation(project(":app:common"))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    "geckoIncludedImplementation"(project(":app:gecko"))

    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.webkit)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.lifecycle.runtime)

    implementation(libs.kotlin.coroutines)

    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.annotations)
    ksp(libs.koin.ksp.compiler)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.startup.runtime)
    implementation(libs.ad.block) // TODO: replace with latest adblock-rust by brave

    // UI
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.process)

    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.kmp.settings.ui.compose)
    implementation(libs.kmp.settings.core)

    implementation(libs.androidx.tv.material)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}