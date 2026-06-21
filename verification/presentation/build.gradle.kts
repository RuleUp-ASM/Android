plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.metro)
}

kotlin {
    android {
        namespace = "com.ruleup.verification.presentation"
        compileSdk = 37
        minSdk = 24
        // 렌더 규칙(failureReason 카피/CTA, todayStatus) 순수 로직 테스트를 JVM 호스트에서 실행.
        withHostTest {}
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
        }
    }
    iosArm64()
    iosSimulatorArm64()

    compilerOptions {
        // core:ui 가 -Xexplicit-backing-fields(실험 기능)로 컴파일되어 pre-release 메타데이터를
        // 가지므로, 이를 소비하기 위해 pre-release 체크를 건너뛴다.
        freeCompilerArgs.add("-Xskip-prerelease-check")
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:entity"))
            implementation(project(":core:ui"))
            implementation(project(":core:map"))
            implementation(project(":verification:domain"))

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.uiToolingPreview)

            implementation(libs.kotlinx.datetime)

            implementation(libs.metrox.viewmodel)
            implementation(libs.metrox.viewmodel.compose)
            implementation(libs.jetbrains.lifecycle.runtime.compose)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.appcompat)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.material)
        }
    }
}
