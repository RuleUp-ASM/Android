import java.util.Properties
import kotlin.apply

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
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
// iOS 웹 OAuth(ASWebAuthenticationSession)용. Kakao 는 네이티브 SDK 대신 웹 인가를 쓰므로 REST 키가 필요하다.
val kakaoRestApiKey: String =
    localProperties.getProperty("KAKAO_REST_API_KEY")?.trim().orEmpty()
val kakaoRedirectUri: String =
    localProperties.getProperty("KAKAO_REDIRECT_URI")?.trim().orEmpty()

// KMP Android 라이브러리 플러그인은 buildConfig 생성을 지원하지 않으므로,
// local.properties 의 OAuth 시크릿을 androidMain 소스셋의 Kotlin object 로 생성해 대체한다.
val generatedSecretsDir = layout.buildDirectory.dir("generated/oauthSecrets/kotlin")

val generateOAuthSecrets =
    tasks.register("generateOAuthSecrets") {
        val outputDir = generatedSecretsDir
        val kakao = kakaoNativeAppKey
        val clientId = googleClientId
        val redirect = googleRedirectUri
        val kakaoRest = kakaoRestApiKey
        val kakaoRedirect = kakaoRedirectUri
        // local.properties 값을 입력으로 선언해야 값이 바뀔 때 재생성된다(미선언 시 up-to-date 로 스킵됨).
        inputs.property("kakao", kakao)
        inputs.property("clientId", clientId)
        inputs.property("redirect", redirect)
        inputs.property("kakaoRest", kakaoRest)
        inputs.property("kakaoRedirect", kakaoRedirect)
        outputs.dir(outputDir)
        doLast {
            val pkgDir =
                outputDir
                    .get()
                    .asFile
                    .resolve("com/ruleup/onboarding/presentation")
            pkgDir.mkdirs()
            pkgDir.resolve("OAuthSecrets.kt").writeText(
                """
                package com.ruleup.onboarding.presentation

                internal object OAuthSecrets {
                    const val KAKAO_NATIVE_APP_KEY: String = "$kakao"
                    const val GOOGLE_CLIENT_ID: String = "$clientId"
                    const val GOOGLE_REDIRECT_URI: String = "$redirect"
                    const val KAKAO_REST_API_KEY: String = "$kakaoRest"
                    const val KAKAO_REDIRECT_URI: String = "$kakaoRedirect"
                }
                """.trimIndent() + "\n",
            )
        }
    }

kotlin {
    android {
        namespace = "com.ruleup.onboarding.presentation"
        compileSdk = 37
        minSdk = 24
        // KMP Android 라이브러리는 Android 리소스가 기본 비활성이라, FileProvider 가 참조하는
        // res/xml/onboarding_file_paths.xml 을 AAR 에 포함하려면 명시적으로 켠다.
        androidResources {
            enable = true
        }
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
        }
    }
    iosArm64()
    iosSimulatorArm64()

    compilerOptions {
        // core:ui 가 -Xexplicit-backing-fields(실험 기능)로 컴파일되어 pre-release 메타데이터를
        // 가지므로, 이를 소비하기 위해 pre-release 체크를 건너뛴다.
        freeCompilerArgs.add("-Xskip-prerelease-check")
    }

    sourceSets {
        commonMain {
            // generateOAuthSecrets 가 만든 OAuthSecrets.kt 를 commonMain 에 등록한다(android·iOS actual 모두 참조).
            kotlin.srcDir(generatedSecretsDir)
        }
        commonMain.dependencies {
            implementation(project(":core:entity"))
            implementation(project(":core:ui"))
            implementation(project(":onboarding:domain"))

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.uiToolingPreview)

            implementation(libs.coil.compose)

            implementation(libs.metrox.viewmodel)
            implementation(libs.metrox.viewmodel.compose)
            implementation(libs.jetbrains.lifecycle.runtime.compose)
        }
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.appcompat)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.material)
            // OkHttp 기반 coil 네트워크 페처(JVM 전용)
            implementation(libs.coil.network)

            implementation(libs.kakao.user)
            implementation(libs.app.auth)
        }
        iosMain.dependencies {
            // iOS 네트워크 이미지: Ktor(Darwin) 기반 coil 페처
            implementation(libs.coil.network.ktor)
            implementation(libs.ktor.client.darwin)
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
    dependsOn(generateOAuthSecrets)
}
