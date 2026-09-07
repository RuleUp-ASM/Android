import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.ruleup.notification.domain"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // 알림 레드닷을 쓰는 화면이 여럿이라 NotificationRepository 대역을 한 곳에 둔다.
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
