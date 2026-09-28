import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.ruleup.challenge.domain"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // 공유 챌린지 저장소 테스트 대역.
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
    // 공개 시그니처의 core:domain 타입.
    api(project(":core:domain"))
    // 이벤트 카탈로그가 BizEvent 를 돌려주므로 공개 시그니처에 나온다.
    api(project(":logging:domain"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)

    testImplementation(kotlin("test-junit"))
    testImplementation(testFixtures(project(":challenge:domain")))
}
