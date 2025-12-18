import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

val properties = Properties()
val localPropertiesFile: File = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { properties.load(it) }
}

// To conditionally apply plugins
var includeFirebase = true

android {
    namespace = "com.phlox.tvwebbrowser"
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.phlox.tvwebbrowser"

        versionCode = libs.versions.versionCode.get().toInt()
        versionName = libs.versions.versionName.get()

        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.incremental" to "true",
                    "room.schemaLocation" to "$projectDir/schemas"
                )
            }
        }
    }

    signingConfigs {
        create("release") {
            storeFile = properties.getProperty("storeFile", null)?.let { rootProject.file(it) }
            storePassword = properties.getProperty("storePassword", "")
            keyAlias = properties.getProperty("keyAlias", "")
            keyPassword = properties.getProperty("keyPassword", "")
        }
    }

    buildTypes {
        getByName("debug") {
            isDebuggable = true
        }
        getByName("release") {
            isDebuggable = false
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = false
        }
    }

    applicationVariants.all {
        val variant = this
        variant.outputs
            .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
            .forEach { output ->
                val flavour = variant.flavorName
                val versionName = variant.versionName
                val arch = output.filters.firstOrNull()?.identifier ?: "universal"
                output.outputFileName = "tvbro-${flavour}-${versionName}(${arch}).apk"
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
            includeFirebase = false
        }

        create("geckoIncluded") {
            dimension = "webengine"
        }
        create("geckoExcluded") {
            dimension = "webengine"
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(project(":app:common"))
    "geckoIncludedImplementation"(project(":app:gecko"))

    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.webkit)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.lifecycle.runtime)

    implementation(libs.kotlin.coroutines)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    // Legacy annotation processor support if needed for Room Java modules, otherwise KSP is enough
    annotationProcessor(libs.androidx.room.compiler)

    // UI
    implementation(libs.segmented.button)
    implementation(libs.ad.block)
    implementation(libs.pinned.section.listview)

    // Firebase (Conditional Logic handled below, but dependencies defined here)
    "googleImplementation"(libs.firebase.core)
    "googleImplementation"(libs.firebase.crashlytics)

    "genericImplementation"(libs.firebase.core)
    "genericImplementation"(libs.firebase.crashlytics)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}

if (includeFirebase) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}