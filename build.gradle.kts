import com.android.build.api.dsl.CommonExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.apk.dist) apply false
}

subprojects {

    plugins.withType<com.android.build.gradle.BasePlugin> {
        configure<CommonExtension> {
            compileSdk { version = release(libs.versions.compileSdk.get().toInt()) }
        }
    }

    pluginManager.withPlugin("org.jetbrains.kotlin.android") {
        extensions.configure<KotlinBaseExtension> {
            jvmToolchain(libs.versions.jvmTarget.get().toInt())
        }
    }
}