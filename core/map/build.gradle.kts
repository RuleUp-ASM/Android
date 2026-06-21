plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    android {
        namespace = "com.ruleup.map"
        compileSdk = 37
        minSdk = 24
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
        }
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
        }
        androidMain.dependencies {
            // Google Maps Compose(GoogleMap+Marker+Circle) — POC. 한국 POI 검색은 Phase 2(카카오/네이버) 교체.
            implementation(libs.maps.compose)
            implementation(libs.play.services.maps)
            // "현재 위치" 단발 측위(FusedLocation)
            implementation(libs.play.services.location)
            implementation(libs.kotlinx.coroutines.play.services)
            implementation(libs.androidx.activity.compose)
        }
    }
}
