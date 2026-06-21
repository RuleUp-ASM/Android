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
        namespace = "com.ruleup.verification.data"
        compileSdk = 37
        minSdk = 24
        // commonTest(DTO 직렬화 테스트, 명세 부록 A Phase 0 수용)를 JVM 호스트에서 실행한다.
        withHostTest {}
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
        }
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        androidMain {
            // AGP KMP 라이브러리 플러그인은 Ktorfit(KSP)이 kspAndroidMain 으로 생성한
            // createXxxApi() 확장과 Room 생성 DAO/Impl 을 컴파일 소스셋에 자동 등록하지 않으므로 직접 추가한다.
            kotlin.srcDir("build/generated/ksp/android/androidMain/kotlin")
        }
        commonMain.dependencies {
            implementation(project(":verification:domain"))
            implementation(project(":core:network"))
            implementation(project(":core:domain"))
            implementation(project(":core:entity"))
            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            // 신호 타임스탬프(epoch millis) → ISO 변환(kotlin.time.Instant)
            implementation(libs.kotlinx.datetime)
        }
        androidMain.dependencies {
            // Geofence(GeofencingClient) + 보조 측위(FusedLocationProviderClient)
            implementation(libs.play.services.location)
            implementation(libs.kotlinx.coroutines.play.services)
            // 로컬 버퍼(단일 진실원) — geofence_transition / location_sample / geofence_target / progress_cache
            implementation(libs.androidx.room.runtime)
            // 백그라운드 sync 오케스트레이션(WorkManager PeriodicWork + WorkerFactory)
            implementation(libs.androidx.work.runtime)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.test)
        }
    }

    // iOS: createVerificationApi() 는 타깃별 KSP 생성물이라 intermediate(iosMain)에서 참조 불가.
    // 각 leaf 소스셋에 공유 DI 모듈(src/iosShared)과 해당 타깃 KSP 생성물을 등록한다(androidMain 패턴의 iOS 대응).
    listOf("iosArm64", "iosSimulatorArm64").forEach { target ->
        sourceSets.getByName("${target}Main").kotlin.srcDir("src/iosShared/kotlin")
        sourceSets.getByName("${target}Main").kotlin.srcDir("build/generated/ksp/$target/${target}Main/kotlin")
    }
}

// Room 어노테이션 프로세서(KSP)를 androidMain 컴파일에 건다. (ktorfit 과 동일 kspAndroidMain 구성)
dependencies {
    add("kspAndroidMain", libs.androidx.room.compiler)
}

ksp {
    // Java 대신 Kotlin DAO/구현을 생성해 KMP androidMain(Kotlin 전용) 소스셋에 바로 등록되게 한다.
    arg("room.generateKotlin", "true")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
    if (name == "compileAndroidMain") dependsOn("kspAndroidMain")
    if (name.startsWith("compileKotlinIos")) {
        dependsOn("kspKotlin${name.removePrefix("compileKotlin")}")
    }
}

// KSP(Ktorfit/Room) 생성 코드는 소스셋에 등록돼 있어 ktlint 가 린트·입력 스냅샷 대상으로 삼는다.
// 생성물은 린트에서 제외하고, ktlint 태스크를 ksp 태스크에 의존시켜
// "uses this output without declaring dependency" 태스크 검증 실패를 없앤다.
extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    filter {
        exclude { it.file.path.contains("/generated/") }
    }
}
tasks
    .matching { it.name.startsWith("runKtlintCheckOver") || it.name.startsWith("runKtlintFormatOver") }
    .configureEach {
        dependsOn(tasks.matching { it.name.startsWith("ksp") })
    }
