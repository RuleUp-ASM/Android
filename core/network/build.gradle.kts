plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.metro)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "com.ruleup.network"
        compileSdk = 37
        minSdk = 24
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
        }
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:domain"))
            // HttpClient·Json·Ktorfit 이 NetworkModule 의 @Provides 시그니처로 노출되어 app 의 Metro 그래프와
            // data 모듈이 타입을 인지해야 하므로 api 로 전파한다.
            api(libs.ktor.client.core)
            api(libs.kotlinx.serialization.json)
            api(libs.ktorfit.lib.light)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            // OkHttp 엔진(자동 선택) + Android Context 기반 이미지 리더
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.core.ktx)
        }
        iosMain.dependencies {
            // Darwin 엔진(HttpClient {} 가 자동 선택)
            implementation(libs.ktor.client.darwin)
        }
    }
}
