plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.metro)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "com.ruleup.shared"
        compileSdk = 37
        minSdk = 24
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
        }
    }
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
            binaryOption("bundleId", "com.ruleup.shared")
        }
    }

    compilerOptions {
        // core:ui 가 -Xexplicit-backing-fields(실험 기능)로 컴파일되어 pre-release 메타데이터를
        // 가지므로, 이를 소비하기 위해 pre-release 체크를 건너뛴다.
        freeCompilerArgs.add("-Xskip-prerelease-check")
    }

    sourceSets {
        commonMain.dependencies {
            // Metro AppGraph 가 집계할 모든 기여 모듈(데이터/도메인/프레젠테이션/코어)
            implementation(project(":core:domain"))
            implementation(project(":core:entity"))
            implementation(project(":core:ui"))
            implementation(project(":core:network"))
            implementation(project(":core:datastore"))
            implementation(project(":onboarding:domain"))
            implementation(project(":onboarding:data"))
            implementation(project(":onboarding:presentation"))
            implementation(project(":challenge:domain"))
            implementation(project(":challenge:data"))
            implementation(project(":challenge:presentation"))

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)

            // 백스택(navigation3-runtime)은 멀티플랫폼. NavDisplay(navigation3-ui)는 iOS 미지원이라 androidMain 에만 둔다.
            implementation(libs.androidx.navigation3.runtime)

            implementation(libs.metrox.viewmodel)
            implementation(libs.metrox.viewmodel.compose)
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            // NavDisplay + ViewModelStore 엔트리 데코레이터(Android 전용 렌더링)
            implementation(libs.androidx.navigation3.ui)
            implementation(libs.androidx.lifecycle.viewmodel.navigation3)
        }
    }
}
