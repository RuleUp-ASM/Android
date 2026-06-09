import java.util.Properties
import kotlin.apply

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.metro)
}
val localProperties =
    Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }

val kakaoNativeAppKey: String =
    localProperties.getProperty("KAKAO_NATIVE_APP_KEY")?.trim().orEmpty()

val googleClientId: String =
    localProperties.getProperty("GOOGLE_CLIENT_ID")?.trim().orEmpty()

val googleRedirectUri: String =
    localProperties.getProperty("GOOGLE_REDIRECT_URI")?.trim().orEmpty()

// AppAuth 리다이렉트 수신용 scheme (GOOGLE_REDIRECT_URI 의 scheme 부분). 예: com.ruleup.android
val appAuthRedirectScheme: String = googleRedirectUri.substringBefore(":")

android {
    namespace = "com.ruleup.onboarding.presentation"
    compileSdk {
        version =
            release(37) {
                minorApiLevel = 0
            }
    }

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        manifestPlaceholders["KAKAO_NATIVE_APP_KEY"] = kakaoNativeAppKey
        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"$kakaoNativeAppKey\"")

        // AppAuth 가 RedirectUriReceiverActivity 의 intent-filter scheme 으로 사용한다.
        manifestPlaceholders["appAuthRedirectScheme"] = appAuthRedirectScheme
        buildConfigField("String", "GOOGLE_CLIENT_ID", "\"$googleClientId\"")
        buildConfigField("String", "GOOGLE_REDIRECT_URI", "\"$googleRedirectUri\"")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        // core:ui 가 -Xexplicit-backing-fields(실험 기능)로 컴파일되어 pre-release 메타데이터를
        // 가지므로, 이를 소비하기 위해 pre-release 체크를 건너뛴다.
        freeCompilerArgs.add("-Xskip-prerelease-check")
    }
}

dependencies {
    implementation(project(":core:entity"))
    implementation(project(":core:ui"))
    implementation(project(":onboarding:domain"))
    implementation(libs.coil.compose)
    implementation(libs.coil.network)
    implementation(libs.androidx.appcompat)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    implementation(libs.metrox.viewmodel)
    implementation(libs.metrox.viewmodel.compose)

    implementation(libs.kakao.user)
    implementation(libs.app.auth)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
