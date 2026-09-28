import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.ruleup.profile.presentation"
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

    // Compose 가 테마·리소스를 읽어야 렌더된다.
    testOptions {
        unitTests {
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
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(project(":tti:presentation"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":profile:domain"))
    // 로그아웃·탈퇴는 인증 소관
    implementation(project(":onboarding:domain"))
    // 「내가 받는 알림」은 감시자 관계
    implementation(project(":challenge:domain"))
    // 알림 설정·알림함 진입점
    implementation(project(":notification:domain"))
    // 이의 내역은 인증 모듈 소관 개념이다
    implementation(project(":verification:domain"))
    // 신고·차단 화면으로 보내는 경로(BlockListPage)만 쓴다.
    implementation(project(":report:domain"))
    // 설정 허브가 문의 진입점과 새 답변 뱃지를 그린다
    implementation(project(":support:domain"))

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

    // 프로필 이미지 로딩
    implementation(libs.coil.compose)
    implementation(libs.coil.network)

    // 활동 캘린더 월 그리드
    implementation(libs.kizitonwose.calendar.compose)

    // 친구 초대: QR 렌더링(클라 생성) + 카카오톡 공유(사용자 본인 발신)
    implementation(libs.zxing.core)
    implementation(libs.kakao.share)

    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    testImplementation(kotlin("test-junit"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(testFixtures(project(":core:domain")))
    testImplementation(testFixtures(project(":observability:domain")))
    testImplementation(testFixtures(project(":verification:domain")))
    testImplementation(testFixtures(project(":challenge:domain")))
    testImplementation(testFixtures(project(":onboarding:domain")))
    testImplementation(testFixtures(project(":support:domain")))

    // Compose 화면을 JVM 에서 렌더한다
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)

    // manifest 는 반드시 debugImplementation 이다.
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
