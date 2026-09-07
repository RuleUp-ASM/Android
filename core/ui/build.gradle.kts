import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.ruleup.ui"
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
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

dependencies {
    api(project(":core:domain"))
    api(project(":observability:domain"))

    // MVI 기반 클래스와 CompositionLocal 이 Compose/ViewModel 을 공개 시그니처로 노출한다.
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.runtime)
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.ui)
    api(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)

    // 걸음·수면 권한 요청 런처(두 feature 화면이 공유). 위치가 OS 다이얼로그를 직접 쓰는 것과 같다.
    // 런처 타입이 공개 시그니처에 나오므로 activity-compose 는 api 로 전파한다.
    api(libs.androidx.activity.compose)
    implementation(libs.androidx.health.connect.client)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
