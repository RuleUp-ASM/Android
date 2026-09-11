import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.ruleup.support.domain"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // 설정 허브(profile)와 문의 화면들이 같은 대역을 쓴다 — 모듈마다 베끼면 계약이 갈린다.
    testFixtures {
        enable = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

dependencies {
    // Page/NavRoute 가 본 모듈의 공개 시그니처에 노출되므로 api 로 전파한다.
    api(project(":core:domain"))

    testImplementation(kotlin("test-junit"))
}
