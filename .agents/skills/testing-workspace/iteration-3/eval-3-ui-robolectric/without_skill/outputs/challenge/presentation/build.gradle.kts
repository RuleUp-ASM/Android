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

    // Robolectric 이 화면을 그리려면 머지된 리소스가 필요하다 — painterResource(ic_arrow_back) 가
    // 이 플래그 없이는 유닛 테스트에서 리소스를 찾지 못한다.
    testOptions {
        unitTests.isIncludeAndroidResources = true
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

    // Compose 화면을 JVM 에서 그려 검증하는 UI 테스트(Robolectric).
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    // createAndroidComposeRule<ComponentActivity> 가 띄울 액티비티를 debug 매니페스트에 넣어준다.
    // Robolectric 유닛 테스트는 debug 변형의 머지된 매니페스트를 쓰므로 testImplementation 이 아니다.
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
