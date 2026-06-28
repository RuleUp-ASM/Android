import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.ruleup.map"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
        // :core:ui 가 실험 기능(explicit backing fields)으로 pre-release 메타데이터를 내보내므로,
        // 이를 의존하는 모듈은 다른 presentation 모듈과 동일하게 prerelease 체크를 건너뛴다.
        freeCompilerArgs.add("-Xskip-prerelease-check")
    }
}

dependencies {
    // 공용 UI 헬퍼(SingleClickHelper 등) 재사용.
    implementation(project(":core:ui"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)

    // 지도(Kakao Map SDK v2). 네이티브 앱키는 :app 의 KakaoMapSdk.init 에서 1회 주입.
    implementation(libs.kakao.map)
    // "현재 위치" 단발 측위(FusedLocation)
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.activity.compose)
}
