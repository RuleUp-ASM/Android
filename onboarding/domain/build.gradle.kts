import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.ruleup.onboarding.domain"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // 인증 포트 대역을 onboarding:presentation 이 함께 쓴다
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
    // 계정 정보는 profile 소유
    api(project(":profile:domain"))
    // 진단 로깅(사용자에게 노출하지 않는 실패 원인).
    implementation(project(":observability:domain"))
    // 이벤트 카탈로그가 BizEvent 를 돌려주므로 공개 시그니처에 나온다.
    api(project(":logging:domain"))
    implementation(libs.kotlinx.coroutines.core)
    // UseCase 의 @Inject 생성자(런타임 Hilt 컴포넌트에서 제공).
    implementation(libs.javax.inject)

    testImplementation(kotlin("test-junit"))
    testImplementation(testFixtures(project(":onboarding:domain")))

    // coroutines 가 implementation 이라 testFixtures 컴파일 경로엔 오지 않는다.
    testFixturesImplementation(libs.kotlinx.coroutines.core)
    testImplementation(testFixtures(project(":observability:domain")))
    testImplementation(testFixtures(project(":logging:domain")))
}
