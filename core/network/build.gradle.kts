import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.ruleup.network"
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
    implementation(project(":core:domain"))
    implementation(project(":observability:domain"))
    // TokenAuthenticator 가 갱신 토큰(Token)을 직접 다룬다.
    implementation(project(":core:entity"))

    // Retrofit/OkHttp/Json 은 NetworkModule 의 @Provides 시그니처와 data 모듈의 API 생성에 노출되므로 api.
    api(libs.retrofit)
    api(libs.retrofit.converter.kotlinx.serialization)
    api(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.core.ktx)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
