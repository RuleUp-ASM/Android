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
    implementation(project(":core:domain"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    // 화면이 자기 TTI 구간을 선언한다
    implementation(project(":tti:presentation"))
    implementation(project(":observability:domain"))
    implementation(project(":logging:domain"))
    implementation(project(":challenge:domain"))
    // 대상 앱 설정은 verification 소관
    implementation(project(":verification:domain"))
    // 신고 계약만 쓴다
    implementation(project(":report:domain"))
    // 위치·건강 개별 동의 기록(AccountRepository)과 현행 약관 버전(IntroRepository)
    implementation(project(":profile:domain"))
    implementation(project(":onboarding:domain"))
    // 카드의 미읽음 카운터
    implementation(project(":notification:domain"))

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
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(testFixtures(project(":core:domain")))
    testImplementation(testFixtures(project(":observability:domain")))
    testImplementation(testFixtures(project(":logging:domain")))
    testImplementation(testFixtures(project(":verification:domain")))
    testImplementation(testFixtures(project(":challenge:domain")))
    testImplementation(testFixtures(project(":notification:domain")))
    testImplementation(testFixtures(project(":onboarding:domain")))

    // Compose 화면을 JVM 에서 렌더한다
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)

    // manifest 는 반드시 debugImplementation 이다.
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
