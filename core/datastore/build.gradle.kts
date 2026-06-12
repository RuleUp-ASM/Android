plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.metro)
}

kotlin {
    android {
        namespace = "com.ruleup.datastore"
        compileSdk = 37
        minSdk = 24
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
        }
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:domain"))
            implementation(project(":core:entity"))
            // KMP 공통 DataStore API (DataStore<Preferences>, edit, key 등)
            // TokenRepositoryImpl 생성자에 DataStore<Preferences> 가 노출되어 app 의 Metro 그래프가
            // 타입을 인지해야 하므로 api 로 전파한다.
            api(libs.androidx.datastore.preferences.core)
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            // Context 기반 파일 팩토리(preferencesDataStoreFile)는 Android 전용
            implementation(libs.androidx.datastore.preferences)
        }
    }
}
