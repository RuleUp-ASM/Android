plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
    jvm { // core:domain 등 기존 kotlin.jvm 모듈이 소비
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
        }
    }
    android { // *:presentation 등 android 모듈이 소비
        namespace = "com.ruleup.entity"
        compileSdk = 37
        minSdk = 24
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
        }
    }
    // 추후 iOS 붙일 때:
    // iosX64(); iosArm64(); iosSimulatorArm64()
}
