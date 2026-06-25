import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.ruleup.verification.data"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        // VerificationSyncWorker 등에서 java.time(Instant) 사용 — minSdk 24 desugaring.
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

ksp {
    // Room 이 Kotlin DAO/구현(_Impl)을 생성하게 한다.
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(project(":verification:domain"))
    implementation(project(":core:network"))
    implementation(project(":core:domain"))
    implementation(project(":core:entity"))

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    // Geofence(GeofencingClient) + 보조 측위(FusedLocationProviderClient)
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.play.services)
    // 로컬 버퍼(단일 진실원) Room
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    // 백그라운드 sync 오케스트레이션(WorkManager) + Hilt 통합(@HiltWorker / HiltWorkerFactory)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    // 움직임·수면 온디바이스 읽기(Health Connect)
    implementation(libs.androidx.health.connect.client)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // java.time desugaring (minSdk 24)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    testImplementation(kotlin("test-junit"))
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlinx.coroutines.test)
}
