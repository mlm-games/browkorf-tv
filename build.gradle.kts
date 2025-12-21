import com.android.build.gradle.BaseExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

subprojects {

    plugins.withType<com.android.build.gradle.BasePlugin> {
        configure<BaseExtension> {
            compileSdkVersion(libs.versions.compileSdk.get().toInt())

            defaultConfig {
                minSdk = libs.versions.minSdk.get().toInt()
                targetSdk = libs.versions.targetSdk.get().toInt()
            }

            compileOptions {
                sourceCompatibility = JavaVersion.toVersion(libs.versions.jvmTarget.get())
                targetCompatibility = JavaVersion.toVersion(libs.versions.jvmTarget.get())
            }
        }
    }

    pluginManager.withPlugin("org.jetbrains.kotlin.android") {
        extensions.configure<KotlinBaseExtension> {
            jvmToolchain(libs.versions.jvmTarget.get().toInt())
        }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}