import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.ruleup.challenge.presentation"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            // Robolectric 이 Compose 화면을 그리려면 테마·드로어블을 읽어야 한다. 없으면 리소스 조회에서 터진다.
            isIncludeAndroidResources = true
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
        freeCompilerArgs.add("-Xskip-prerelease-check")
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":observability:domain"))
    implementation(project(":challenge:domain"))
    // 대상 앱 설정은 verification 소관 — 그쪽 domain 계약을 직접 쓴다(core 포트 복제 제거).
    implementation(project(":verification:domain"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    implementation(libs.coil.compose)
    implementation(libs.coil.network)

    // 감시자 초대 카드 카카오톡 공유 (사용자 본인 발신)
    implementation(libs.kakao.share)

    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    testImplementation(kotlin("test-junit"))

    // UI 층(Robolectric + Compose). 계측 테스트로 두면 에뮬레이터 워크플로가 필요해 PR 마다 못 돈다.
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    // createComposeRule 이 띄우는 ComponentActivity 는 **병합 매니페스트**에 있어야 한다.
    // 유닛 테스트는 debug 변형의 병합 매니페스트를 읽으므로 testImplementation 으로는 들어가지 않는다.
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
