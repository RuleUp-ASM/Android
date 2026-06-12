plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.metro)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktorfit)
}

kotlin {
    android {
        namespace = "com.ruleup.challenge.data"
        compileSdk = 37
        minSdk = 24
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
        }
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        androidMain {
            // AGP KMP 라이브러리 플러그인은 Ktorfit(KSP)이 kspAndroidMain 으로 생성한
            // createXxxApi() 확장을 컴파일 소스셋에 자동 등록하지 않으므로 직접 추가한다.
            kotlin.srcDir("build/generated/ksp/android/androidMain/kotlin")
        }
        commonMain.dependencies {
            implementation(project(":challenge:domain"))
            implementation(project(":core:network"))
            implementation(project(":core:domain"))
            implementation(project(":core:entity"))
            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
        }
    }

    // iOS: createChallengeApi() 는 타깃별 KSP 생성물이라 intermediate(iosMain)에서 참조 불가.
    // 각 leaf 소스셋에 공유 DI 모듈(src/iosShared)과 해당 타깃 KSP 생성물을 등록한다(androidMain 패턴의 iOS 대응).
    listOf("iosArm64", "iosSimulatorArm64").forEach { target ->
        sourceSets.getByName("${target}Main").kotlin.srcDir("src/iosShared/kotlin")
        sourceSets.getByName("${target}Main").kotlin.srcDir("build/generated/ksp/$target/${target}Main/kotlin")
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
    if (name == "compileAndroidMain") dependsOn("kspAndroidMain")
    if (name.startsWith("compileKotlinIos")) {
        dependsOn("kspKotlin${name.removePrefix("compileKotlin")}")
    }
}
