import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.ruleup.verification.domain"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // 공유 인증 저장소 테스트 대역.
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
    api(project(":challenge:domain"))
    api(project(":profile:domain"))
    api(project(":onboarding:domain"))
    // 비즈니스 이벤트 로깅(AnalyticsLogger).
    implementation(project(":observability:domain"))
    // 진행률 캐시 관찰(ProgressCacheStore.observe(): Flow).
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)

    testImplementation(kotlin("test-junit"))
    testImplementation(testFixtures(project(":challenge:domain")))
    testImplementation(testFixtures(project(":core:domain")))
    testImplementation(libs.kotlinx.coroutines.test)

    // 테스트 대역의 코루틴 의존성.
    testFixturesImplementation(libs.kotlinx.coroutines.core)
}
