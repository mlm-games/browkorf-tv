plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.phlox.tvwebbrowser.webengine.gecko"

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    implementation(project(":app:common"))
    implementation(libs.androidx.appcompat)
    implementation(libs.geckoview)

    testImplementation(libs.junit)
}