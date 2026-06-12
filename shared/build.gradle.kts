import java.util.Properties
import kotlin.apply

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.metro)
    alias(libs.plugins.kotlin.serialization)
}

val localProperties =
    Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }
val baseUrl: String =
    localProperties.getProperty("BASE_URL")?.trim().orEmpty()

// local.properties 의 BASE_URL 을 commonMain 의 AppConfig 상수로 생성한다.
// iOS(MainViewController)·Android(App) 가 모두 이 값을 사용해 BASE_URL 을 local.properties 한곳에서 관리한다.
val generatedConfigDir = layout.buildDirectory.dir("generated/appConfig/kotlin")

val generateAppConfig =
    tasks.register("generateAppConfig") {
        val outputDir = generatedConfigDir
        val url = baseUrl
        inputs.property("baseUrl", url)
        outputs.dir(outputDir)
        doLast {
            val pkgDir =
                outputDir
                    .get()
                    .asFile
                    .resolve("com/ruleup/shared")
            pkgDir.mkdirs()
            pkgDir.resolve("AppConfig.kt").writeText(
                """
                package com.ruleup.shared

                object AppConfig {
                    const val BASE_URL: String = "$url"
                }
                """.trimIndent() + "\n",
            )
        }
    }

kotlin {
    android {
        namespace = "com.ruleup.shared"
        compileSdk = 37
        minSdk = 24
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
        }
    }
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            // Compose/Skiko 의 시스템 프레임워크 의존성을 모두 번들하도록 동적 프레임워크로 빌드한다.
            isStatic = false
            binaryOption("bundleId", "com.ruleup.shared")
        }
    }

    compilerOptions {
        // core:ui 가 -Xexplicit-backing-fields(실험 기능)로 컴파일되어 pre-release 메타데이터를
        // 가지므로, 이를 소비하기 위해 pre-release 체크를 건너뛴다.
        freeCompilerArgs.add("-Xskip-prerelease-check")
    }

    sourceSets {
        commonMain {
            // generateAppConfig 가 만든 AppConfig.kt 를 commonMain 에 등록한다.
            kotlin.srcDir(generatedConfigDir)
        }
        commonMain.dependencies {
            // Metro AppGraph 가 집계할 모든 기여 모듈(데이터/도메인/프레젠테이션/코어)
            implementation(project(":core:domain"))
            implementation(project(":core:entity"))
            implementation(project(":core:ui"))
            implementation(project(":core:network"))
            implementation(project(":core:datastore"))
            implementation(project(":onboarding:domain"))
            implementation(project(":onboarding:data"))
            implementation(project(":onboarding:presentation"))
            implementation(project(":challenge:domain"))
            implementation(project(":challenge:data"))
            implementation(project(":challenge:presentation"))

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)

            // 백스택(navigation3-runtime)은 멀티플랫폼. NavDisplay(navigation3-ui)는 iOS 미지원이라 androidMain 에만 둔다.
            implementation(libs.androidx.navigation3.runtime)

            implementation(libs.metrox.viewmodel)
            implementation(libs.metrox.viewmodel.compose)
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            // NavDisplay + ViewModelStore 엔트리 데코레이터(Android 전용 렌더링)
            implementation(libs.androidx.navigation3.ui)
            implementation(libs.androidx.lifecycle.viewmodel.navigation3)
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
    dependsOn(generateAppConfig)
}
