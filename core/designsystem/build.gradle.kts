import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.ruleup.designsystem"
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
        // RuleUpColor 등 테마가 explicit backing fields(실험 기능)를 쓴다.
        freeCompilerArgs.add("-Xexplicit-backing-fields")
    }
}

dependencies {
    // 카테고리 → 색/아이콘 매핑(CategoryVisuals)이 InterestCategory 를 공개 시그니처로 노출한다.
    api(project(":core:domain"))

    // 디자인 시스템이므로 Compose 를 api 로 전파한다.
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.runtime)
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
}
