import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.ruleup.onboarding.data"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

dependencies {
    implementation(project(":onboarding:domain"))
    implementation(project(":core:network"))
    implementation(project(":core:domain"))

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    // 기기·설치 식별자 영속(DeviceIdentityRepositoryImpl). 토큰 저장소와 파일을 분리해 쓴다.
    implementation(libs.androidx.datastore.preferences)
    // 멀티파트 프로필 이미지 업로드(MultipartBody.Part). Retrofit 이 transitively 제공하나 명시한다.
    implementation(libs.okhttp)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(kotlin("test-junit"))
}
