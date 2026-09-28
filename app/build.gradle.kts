import com.google.firebase.appdistribution.gradle.firebaseAppDistributionDefault
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
    apply(plugin = "com.google.firebase.crashlytics")
    apply(plugin = "com.google.firebase.appdistribution")
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

/** 릴리즈 서명 자격 증명. */
val keystoreProperties =
    Properties().apply {
        val f = rootProject.file("keystore.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }

// rootProject.file() 은 절대경로면 그대로, 상대경로면 루트 기준으로 푼다.
val releaseStoreFile =
    keystoreProperties
        .getProperty("storeFile")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.let { rootProject.file(it) }
        ?.takeIf { it.isFile }

// 서명 없는 릴리즈는 설치도 스토어 업로드도 안 되는 산출물이다.
if (releaseStoreFile == null &&
    gradle.startParameter.taskNames.any { it.contains("assembleRelease") || it.contains("bundleRelease") }
) {
    logger.warn("경고: keystore.properties 를 찾지 못해 release 변형이 서명 없이 빌드된다. CLAUDE.md 「릴리즈 서명」 참고.")
}

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
        // Retrofit base URL
        buildConfigField("String", "BASE_URL", "\"$baseUrl\"")
        // Amplitude 수집 키
        buildConfigField("String", "AMPLITUDE_API_KEY", "\"$amplitudeApiKey\"")
    }

    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // 자격 증명이 없으면 null
            signingConfig = signingConfigs.findByName("release")
            // 릴리즈는 운영 서버로 고정한다
            buildConfigField("String", "BASE_URL", "\"https://prod.ruleup.co.kr/api\"")
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
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        // core:ui 실험 기능 메타데이터 호환.
        freeCompilerArgs.add("-Xskip-prerelease-check")
        jvmTarget = JvmTarget.JVM_11
    }
}

dependencies {
    // :app 이 컴포지션 루트(AppRoot/내비게이션) + 전 feature·core 모듈 집계점.
    implementation(project(":core:domain"))
    implementation(project(":core:device"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":core:network"))
    implementation(project(":core:datastore"))
    implementation(project(":observability:data"))
    implementation(project(":logging:data"))
    // 인스펙터 싱크는 디버그 변형에만 물린다
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
    implementation(project(":notification:domain"))
    implementation(project(":notification:data"))
    implementation(project(":notification:presentation"))
    implementation(project(":report:presentation"))
    implementation(project(":support:domain"))
    implementation(project(":support:data"))
    implementation(project(":support:presentation"))
    implementation(project(":tti:domain"))
    implementation(project(":tti:data"))
    implementation(project(":tti:presentation"))

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
    // 프로세스가 후면으로 내려가는 신호.
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.work)
    ksp(libs.hilt.compiler)

    implementation(libs.kakao.user)
    // KakaoMapSdk.init(앱키) 호출용.
    implementation(libs.kakao.map)

    // FCM 수신(공지 fan-out 등) + 토큰 등록.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    // 프레임 jank 측정(JankStats).
    implementation(libs.androidx.metrics.performance)

    testImplementation(libs.junit)
    testImplementation(libs.konsist)
    // 딥링크 파서가 android.net.Uri 를 쓴다
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(testFixtures(project(":observability:domain")))
    testImplementation(testFixtures(project(":logging:domain")))

    // 인수 테스트는 앱이 실제로 쓰는 Retrofit api·DTO 를 그대로 써서 실서버를 두드린다
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

/** Firebase App Distribution 배포 설정. */
if (project.file("google-services.json").exists()) {
    firebaseAppDistributionDefault {
        artifactType = "APK"

        // 내부 확인용 채널.
        groups = "beta"

        // 무엇이 담긴 빌드인지 테스터가 알 수 있게 적는다.
        releaseNotes =
            (project.findProperty("releaseNotes") as String?)?.takeIf { it.isNotBlank() }
                ?: providers
                    .exec { commandLine("git", "log", "-1", "--no-merges", "--pretty=%h %s") }
                    .standardOutput
                    .asText
                    .map { it.trim() }
                    .getOrElse("")
    }
}
