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
if (project.file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

val localProperties =
    Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }

val kakaoNativeAppKey: String = localProperties.getProperty("KAKAO_NATIVE_APP_KEY")?.trim().orEmpty()
val mapsApiKey: String = localProperties.getProperty("MAPS_API_KEY")?.trim().orEmpty()
val baseUrl: String = localProperties.getProperty("BASE_URL")?.trim().orEmpty()
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
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"$kakaoNativeAppKey\"")
        // Retrofit base URL — Hilt AppModule(@BaseUrl)이 소비한다.
        buildConfigField("String", "BASE_URL", "\"$baseUrl\"")
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
        // verification:data·challenge:presentation 이 java.time desugaring 을 쓰므로 앱에서도 활성화한다.
        isCoreLibraryDesugaringEnabled = true
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
    implementation(project(":shared"))
    // Hilt 컴포넌트가 :shared 의 전이 의존(전 모듈)을 집계한다. :app 은 직접 쓰는 모듈만 둔다.
    implementation(project(":core:domain")) // 딥링크 파서의 NavRoute, NavigationHelper/MessageHelper
    implementation(project(":core:network")) // @BaseUrl 한정자(AppModule)
    implementation(project(":core:analytics")) // AnalyticsLogger 주입
    implementation(project(":onboarding:domain")) // 딥링크 시작 라우트
    implementation(project(":verification:domain")) // SyncScheduler(App 주입)

    implementation(libs.androidx.work.runtime)
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

    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.work)
    ksp(libs.hilt.compiler)

    implementation(libs.kakao.user)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    testImplementation(libs.junit)
    testImplementation(libs.konsist)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
