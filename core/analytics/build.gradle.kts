import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.ruleup.analytics"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

dependencies {
    // 순수 계약(AnalyticsLogger/AnalyticsEvent)은 api-only 모듈에 두고, 여기선 Firebase 구현만 담는다.
    // api 로 재노출해 core:analytics 를 의존하는 app 이 계약 타입도 함께 보게 한다.
    api(project(":core:analytics-api"))

    // Firebase Analytics native SDK. google-services.json + google-services 플러그인(app 모듈)으로 초기화된다.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
