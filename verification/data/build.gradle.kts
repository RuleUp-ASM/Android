import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

val localProperties =
    Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }

// 카카오 로컬(키워드 장소검색) REST 키. KakaoLocalModule 의 인터셉터가 KakaoAK 헤더로 소비.
val kakaoRestApiKey: String = localProperties.getProperty("KAKAO_REST_API_KEY")?.trim().orEmpty()

android {
    namespace = "com.ruleup.verification.data"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
        buildConfigField("String", "KAKAO_REST_API_KEY", "\"$kakaoRestApiKey\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
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
    // 카카오 로컬 전용 OkHttpClient(KakaoAK 헤더). Retrofit/converter/Json 은 :core:network 가 api 로 노출.
    implementation(libs.okhttp)

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

    testImplementation(kotlin("test-junit"))
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlinx.coroutines.test)
}
