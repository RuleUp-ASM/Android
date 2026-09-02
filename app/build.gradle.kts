import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import kotlin.apply

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

// Firebase(google-services) 플러그인은 google-services.json 이 있어야 동작한다.
// Crashlytics 도 같은 설정에 의존하므로 함께 적용한다(자동 크래시/ANR 수집).
if (project.file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}

val localProperties =
    Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }

val kakaoNativeAppKey: String = localProperties.getProperty("KAKAO_NATIVE_APP_KEY")?.trim().orEmpty()
val baseUrl: String = localProperties.getProperty("BASE_URL")?.trim().orEmpty()
val amplitudeApiKey: String = localProperties.getProperty("AMPLITUDE_API_KEY")?.trim().orEmpty()
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
        minSdk = 26
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
        // Retrofit base URL — Hilt AppModule(@BaseUrl)이 소비한다.
        buildConfigField("String", "BASE_URL", "\"$baseUrl\"")
        // Amplitude 수집 키 — 비어 있으면 ObservabilityAppModule 이 출구를 달지 않는다.
        buildConfigField("String", "AMPLITUDE_API_KEY", "\"$amplitudeApiKey\"")
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
        // core:ui 가 -Xexplicit-backing-fields(실험 기능)로 컴파일되어 pre-release 메타데이터를 가지므로 건너뛴다.
        freeCompilerArgs.add("-Xskip-prerelease-check")
        jvmTarget = JvmTarget.JVM_11
    }
}

dependencies {
    // :app 이 컴포지션 루트(AppRoot/내비게이션) + 전 feature·core 모듈 집계점.
    // Hilt 컴포넌트가 모든 모듈의 @Module/@HiltViewModel 바인딩을 한곳에서 모은다.
    implementation(project(":core:domain"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":core:network"))
    implementation(project(":core:datastore"))
    implementation(project(":observability:data"))
    // 인스펙터 싱크는 디버그 변형에만 물린다 — 릴리스 APK 에 포함되지 않는다.
    debugImplementation(project(":observability:debug"))
    implementation(project(":onboarding:domain"))
    implementation(project(":onboarding:data"))
    implementation(project(":onboarding:presentation"))
    implementation(project(":challenge:domain"))
    implementation(project(":challenge:data"))
    implementation(project(":challenge:presentation"))
    implementation(project(":home:presentation"))
    implementation(project(":profile:domain"))
    implementation(project(":profile:data"))
    implementation(project(":profile:presentation"))
    implementation(project(":verification:domain"))
    implementation(project(":verification:data"))
    implementation(project(":verification:presentation"))

    implementation(project(":report:domain"))
    implementation(project(":report:data"))

    implementation(libs.androidx.work.runtime)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.work)
    ksp(libs.hilt.compiler)

    implementation(libs.kakao.user)
    // KakaoMapSdk.init(앱키) 호출용. 지도 렌더링은 :core:map.
    implementation(libs.kakao.map)

    // FCM 수신(공지 fan-out 등) + 토큰 등록. google-services 설정은 위 조건부 플러그인과 공유한다.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    // 프레임 jank 측정(JankStats). 디버그 빌드에서만 트래킹 → 관측 파이프라인으로 기록.
    implementation(libs.androidx.metrics.performance)

    testImplementation(libs.junit)
    testImplementation(libs.konsist)

    // 인수 테스트는 앱이 실제로 쓰는 Retrofit api·DTO 를 그대로 써서 실서버를 두드린다 —
    // 서버가 계약을 바꾸면 역직렬화에서 터지는 것이 목적이다.
    testImplementation(kotlin("test-junit"))
    testImplementation(libs.retrofit)
    testImplementation(libs.retrofit.converter.kotlinx.serialization)
    testImplementation(libs.okhttp)
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
