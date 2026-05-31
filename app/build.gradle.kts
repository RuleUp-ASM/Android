import java.util.Properties

plugins {
    id("ruleup.android.application")
    id("ruleup.android.compose")
    id("ruleup.android.test")
    id("ruleup.android.hilt")
}

val kakaoNativeAppKey: String =
    Properties()
        .apply {
            val f = rootProject.file("local.properties")
            if (f.exists()) f.inputStream().use { load(it) }
        }.getProperty("KAKAO_NATIVE_APP_KEY")
        ?.trim()
        .orEmpty()

android {
    namespace = "com.ruleup.android"

    defaultConfig {
        applicationId = "com.ruleup.android"
        versionCode =
            libs.versions.versionCode
                .get()
                .toInt()
        versionName = libs.versions.versionName.get()

        manifestPlaceholders["KAKAO_NATIVE_APP_KEY"] = kakaoNativeAppKey
        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"$kakaoNativeAppKey\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:designsystem"))
    implementation(project(":feature:onboarding:data"))
    implementation(project(":feature:onboarding:presentation"))
}
