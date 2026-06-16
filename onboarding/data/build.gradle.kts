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
        namespace = "com.ruleup.onboarding.data"
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
            // (compileAndroidMain 은 commonMain + androidMain 을 함께 컴파일하므로
            //  commonMain 의 DI 모듈에서 생성 확장을 참조할 수 있다.)
            kotlin.srcDir("build/generated/ksp/android/androidMain/kotlin")
        }
        commonMain.dependencies {
            implementation(project(":onboarding:domain"))
            implementation(project(":core:network"))
            implementation(project(":core:domain"))
            implementation(project(":core:entity"))
            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
        }
    }

    // iOS: createXxxApi() 는 타깃별 KSP 생성물이라 intermediate(iosMain)에서 참조할 수 없다.
    // 각 leaf 소스셋에 (1) 손수 작성한 공유 DI 모듈(src/iosShared) 과 (2) 해당 타깃 KSP 생성물을 등록해
    // leaf 컴파일에서 함께 컴파일되도록 한다(androidMain 패턴의 iOS 대응).
    listOf("iosArm64", "iosSimulatorArm64").forEach { target ->
        sourceSets.getByName("${target}Main").kotlin.srcDir("src/iosShared/kotlin")
        sourceSets.getByName("${target}Main").kotlin.srcDir("build/generated/ksp/$target/${target}Main/kotlin")
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
    if (name == "compileAndroidMain") dependsOn("kspAndroidMain")
    // 각 iOS 컴파일은 동일 타깃 KSP 산출물에 의존한다. 예: compileKotlinIosSimulatorArm64 -> kspKotlinIosSimulatorArm64
    if (name.startsWith("compileKotlinIos")) {
        dependsOn("kspKotlin${name.removePrefix("compileKotlin")}")
    }
}
