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
// Crashlytics 도 같은 설정에 의존하므로 함께 적용한다(자동 크래시/ANR 수집).
// App Distribution 도 앱 ID 를 이 파일에서 읽으므로 같은 조건에 묶는다 — 파일이 없는 개발자에게는
// 배포 태스크가 아예 생기지 않고, 대신 빌드가 깨지지도 않는다.
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

/**
 * 릴리즈 서명 자격 증명. 커밋하지 않으므로(.gitignore) CI 와 키를 받지 않은 개발자에게는 없다 —
 * 없으면 서명만 건너뛰고 빌드는 계속 돼야 한다. 키 목록은 CLAUDE.md 「릴리즈 서명」.
 */
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

// 서명 없는 릴리즈는 설치도 스토어 업로드도 안 되는 산출물이다. 조용히 나가면 배포 직전에 안다.
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
        // Retrofit base URL — Hilt AppModule(@BaseUrl)이 소비한다.
        buildConfigField("String", "BASE_URL", "\"$baseUrl\"")
        // Amplitude 수집 키 — 비어 있으면 ObservabilityAppModule 이 출구를 달지 않는다.
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
            // 자격 증명이 없으면 null — 서명이 빠질 뿐 빌드는 통과한다(위 경고 참고).
            signingConfig = signingConfigs.findByName("release")
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
    implementation(project(":notification:domain"))
    implementation(project(":notification:data"))
    implementation(project(":notification:presentation"))
    implementation(project(":report:presentation"))
    implementation(project(":support:domain"))
    implementation(project(":support:data"))
    implementation(project(":support:presentation"))

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
    // 딥링크 파서가 android.net.Uri 를 쓴다 — 순수 JVM 으로는 파싱이 안 된다.
    testImplementation(libs.robolectric)
    testImplementation(testFixtures(project(":observability:domain")))

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

/**
 * Firebase App Distribution 배포 설정.
 *
 * **내려보내는 변형은 debug 다.** 업로드 키(`keystore.properties`)가 없는 개발자·CI 에서도 서명된
 * 산출물이 나오는 유일한 변형이고, `applicationId` 에 suffix 를 붙이지 않아 `google-services.json`
 * 의 앱 ID 와 그대로 맞는다. release 를 배포하려면 키를 먼저 받아야 한다.
 *
 * 앱 ID·프로젝트는 `google-services.json` 에서 읽으므로 여기 적지 않는다 — 두 곳에 적으면 한쪽만
 * 고쳐진다. 업로드 자격 증명은 `firebase login` 또는 `GOOGLE_APPLICATION_CREDENTIALS` 로 준다.
 *
 * 배포: `./gradlew assembleDebug appDistributionUploadDebug`
 */
if (project.file("google-services.json").exists()) {
    firebaseAppDistributionDefault {
        artifactType = "APK"

        // 내부 확인용 채널. release 그룹은 실서버를 보는 빌드를 받는 자리라, 스테이징을
        // 가리키는 이 빌드를 거기로 보내면 테스터가 데이터를 혼동한다.
        groups = "beta"

        // 무엇이 담긴 빌드인지 테스터가 알 수 있게 적는다. `-PreleaseNotes="..."` 로 사람이 쓴
        // 문장을 넘길 수 있고, 없으면 최근 커밋으로 채운다.
        //
        // **머지 커밋은 건너뛴다**(--no-merges) — "Merge pull request #436 from ..." 은 테스터에게
        // 아무것도 말해 주지 않는다. providers.exec 라 배포 태스크가 실제로 도는 순간에만 git 을
        // 부르고, 평소 빌드의 설정 단계는 늦추지 않는다.
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
