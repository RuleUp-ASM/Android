import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import kotlin.apply

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val localProperties =
    Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }

val kakaoNativeAppKey: String = localProperties.getProperty("KAKAO_NATIVE_APP_KEY")?.trim().orEmpty()
val googleClientId: String = localProperties.getProperty("GOOGLE_CLIENT_ID")?.trim().orEmpty()
val googleRedirectUri: String = localProperties.getProperty("GOOGLE_REDIRECT_URI")?.trim().orEmpty()
val kakaoRestApiKey: String = localProperties.getProperty("KAKAO_REST_API_KEY")?.trim().orEmpty()
val kakaoRedirectUri: String = localProperties.getProperty("KAKAO_REDIRECT_URI")?.trim().orEmpty()

android {
    namespace = "com.ruleup.onboarding.presentation"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
        // 카카오·AppAuth 라이브러리 매니페스트가 요구하는 값.
        manifestPlaceholders["KAKAO_NATIVE_APP_KEY"] = kakaoNativeAppKey
        manifestPlaceholders["appAuthRedirectScheme"] = googleRedirectUri.substringBefore(":")
        // local.properties 의 OAuth 시크릿을 BuildConfig 로 노출(OAuthActivity 가 소비).
        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"$kakaoNativeAppKey\"")
        buildConfigField("String", "GOOGLE_CLIENT_ID", "\"$googleClientId\"")
        buildConfigField("String", "GOOGLE_REDIRECT_URI", "\"$googleRedirectUri\"")
        buildConfigField("String", "KAKAO_REST_API_KEY", "\"$kakaoRestApiKey\"")
        buildConfigField("String", "KAKAO_REDIRECT_URI", "\"$kakaoRedirectUri\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Compose 가 테마·리소스를 읽어야 렌더된다.
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

/** androidTest 변형에만 매니페스트 placeholder 를 채운다. */
androidComponents {
    onVariants { variant ->
        variant.androidTest?.manifestPlaceholders?.apply {
            put("KAKAO_NATIVE_APP_KEY", "androidTest")
            put("appAuthRedirectScheme", "androidTest")
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
    implementation(project(":onboarding:domain"))
    implementation(project(":profile:domain"))
    // 진단 로깅(사용자에게 노출하지 않는 실패 원인).
    implementation(project(":observability:domain"))
    implementation(project(":logging:domain"))

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

    implementation(libs.kakao.user)
    implementation(libs.app.auth)

    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    testImplementation(kotlin("test-junit"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(testFixtures(project(":core:domain")))
    testImplementation(testFixtures(project(":observability:domain")))
    testImplementation(testFixtures(project(":logging:domain")))
    testImplementation(testFixtures(project(":onboarding:domain")))

    // Compose 화면을 JVM 에서 렌더한다
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)

    // manifest 는 반드시 debugImplementation 이다.
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
