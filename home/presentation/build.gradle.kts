import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.ruleup.home.presentation"
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

    // Compose 가 테마·리소스를 읽어야 렌더된다. 없으면 리소스 조회에서 터진다.
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

    // 홈은 집계 화면이라 두 feature 의 domain 을 함께 읽는다(내 챌린지 + 진행률).
    // 동급 feature 끼리의 횡적 결합이 아니라, 화면 하나를 조립하기 위한 하향 의존이다.
    implementation(project(":challenge:domain"))
    implementation(project(":verification:domain"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.core.ktx)

    implementation(libs.coil.compose)
    implementation(libs.coil.network)

    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    testImplementation(kotlin("test-junit"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(testFixtures(project(":core:domain")))
    testImplementation(testFixtures(project(":observability:domain")))

    // Compose 화면을 JVM 에서 렌더한다 — CI(test.yml)가 도는 ./gradlew test 안에 들어온다.
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)

    // manifest 는 반드시 debugImplementation 이다. 유닛 테스트는 debug 변형의 병합 매니페스트를 읽는데,
    // testImplementation 으로 넣으면 클래스만 오고 createComposeRule 이 띄울 ComponentActivity 가 안 실린다.
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
