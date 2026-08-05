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
}

/**
 * androidTest 변형에만 매니페스트 placeholder 를 채운다.
 *
 * 이 모듈의 매니페스트는 카카오 redirect 스킴에 `${KAKAO_NATIVE_APP_KEY}` 를, 의존하는 AppAuth
 * 라이브러리는 `${appAuthRedirectScheme}` 를 쓰는데 값을 정의하는 건 `:app` 뿐이다. 앱 병합에서는
 * `:app` 이 채우니 문제가 없지만, **모듈의 androidTest 는 자기 힘으로 테스트 APK 를 만들어야 해서**
 * 그 자리에서 값을 요구한다. Studio 의 Compose 프리뷰가 이 태스크를 돌려 빌드가 깨졌다.
 *
 * `defaultConfig` 에 넣지 않는 이유는 그러면 **앱에 실리는 매니페스트의 값까지 여기서 정해지기**
 * 때문이다. 실제 스킴은 `:app` 이 local.properties 에서 읽은 값이어야 하고, 이 모듈이 끼어들면
 * 로그인 redirect 가 조용히 엉뚱한 스킴으로 나간다.
 *
 * 이 모듈엔 계측 테스트가 없어 만들어지는 테스트 APK 는 비어 있다. 값은 자리만 채우면 된다.
 */
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
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":onboarding:domain"))
    // 진단 로깅(사용자에게 노출하지 않는 실패 원인).
    implementation(project(":observability:domain"))

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
}
