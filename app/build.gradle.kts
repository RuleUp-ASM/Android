import java.util.Properties
import kotlin.apply

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.metro)
    alias(libs.plugins.kotlin.serialization)
}

// Firebase(google-services) 플러그인은 google-services.json 이 있어야 동작한다.
// 파일이 없으면 processGoogleServices 가 빌드를 깨므로, 콘솔에서 받은 json 을 app/ 에 둘 때만 자동 적용된다.
if (project.file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

val localProperties =
    Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }

val kakaoNativeAppKey: String =
    localProperties.getProperty("KAKAO_NATIVE_APP_KEY")?.trim().orEmpty()

// BASE_URL 은 :shared 의 AppConfig(local.properties 생성)로 단일 관리하므로 app BuildConfig 에선 제거.

// AppAuth(RedirectUriReceiverActivity) 가 사용하는 리다이렉트 scheme. GOOGLE_REDIRECT_URI 의 scheme 부분.
val appAuthRedirectScheme: String =
    localProperties
        .getProperty("GOOGLE_REDIRECT_URI")
        ?.trim()
        .orEmpty()
        .substringBefore(":")

android {
    namespace = "com.ruleup.android_ruleup"
    compileSdk {
        version =
            release(37) {
                minorApiLevel = 0
            }
    }

    defaultConfig {
        applicationId = "com.ruleup.android_ruleup"
        minSdk = 24
        targetSdk = 36
        versionCode =
            libs.versions.versionCode
                .get()
                .toInt()
        versionName = libs.versions.versionName.get()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["KAKAO_NATIVE_APP_KEY"] = kakaoNativeAppKey
        manifestPlaceholders["appAuthRedirectScheme"] = appAuthRedirectScheme
        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"$kakaoNativeAppKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
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
    implementation(project(":shared"))
    // Metro 그래프 생성(createGraphFactory)이 :shared 로 이동(createAppGraph)하면서 바인딩 집계도 :shared 에서
    // 일어난다. 따라서 :app 은 더 이상 집계용으로 전 모듈을 의존할 필요가 없고, 자신이 직접 쓰는 모듈만 둔다.
    // (런타임 클래스는 :shared 의 전이 의존으로 그대로 APK 에 패키징됨.)
    implementation(project(":core:domain")) // 딥링크 파서의 NavRoute
    implementation(project(":onboarding:domain")) // 딥링크 시작 라우트(SplashPage/IntroPromisePage)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    implementation(libs.metrox.viewmodel)
    implementation(libs.metrox.viewmodel.compose)

    implementation(libs.kakao.user)

    testImplementation(libs.junit)
    testImplementation(libs.konsist)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
