plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.metro)
}

kotlin {
    // domain 모듈(jvm 타깃 보유)이 commonMain 에서 의존하므로 jvm 타깃을 맞춰준다.
    // jvm 에는 Firebase/스텁 바인딩이 없지만(그래프 미구성), 인터페이스·이벤트는 그대로 컴파일된다.
    jvm {
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
        }
    }
    android {
        namespace = "com.ruleup.analytics"
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
            // 비즈니스 이벤트 로깅 추상화만 노출 — Firebase 의존성 없음.
            // KMP 로거(iOS 스텁/공통 진단 로그용)
            implementation(libs.kermit)
        }
        androidMain.dependencies {
            // Firebase Analytics native SDK. google-services.json + google-services 플러그인(app 모듈)으로 초기화된다.
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.analytics)
        }
        // iosMain — Firebase 의존성 없음(스텁). 추후 Firebase iOS SDK 연결 시 추가.
    }
}
