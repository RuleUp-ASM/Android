import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.ruleup.onboarding.domain"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
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
    // Page/NavRoute·TokenRepository, User 등 공유 커널이 본 모듈의 공개 시그니처에 노출되므로 api 로 전파한다.
    api(project(":core:domain"))
    api(project(":core:entity"))
    // 비즈니스 이벤트 로깅(AnalyticsLogger). 내부 구현 세부라 implementation 으로 둔다.
    implementation(project(":core:analytics"))
    implementation(libs.kotlinx.coroutines.core)
    // UseCase 의 @Inject 생성자(런타임 Hilt 컴포넌트에서 제공). 도메인은 hilt 런타임 없이 annotation 만.
    implementation(libs.javax.inject)
}
