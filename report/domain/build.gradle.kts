import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.ruleup.report.domain"
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
    // core:domain 도 coroutines 도 쓰지 않는다 — 신고·차단은 1회 조회뿐이라 Flow 가 없고,
    // 공유 커널(Category·Tier) 타입도 시그니처에 나오지 않는다.
    testImplementation(kotlin("test-junit"))
}
