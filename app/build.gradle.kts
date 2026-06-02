import java.util.Properties

plugins {
    id("ruleup.android.application")
    id("ruleup.android.compose")
    id("ruleup.android.test")
    id("ruleup.android.hilt")
}

val localProperties =
    Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }

val kakaoNativeAppKey: String = localProperties.getProperty("KAKAO_NATIVE_APP_KEY")?.trim().orEmpty()

val googleRedirectUri: String = localProperties.getProperty("GOOGLE_REDIRECT_URI")?.trim().orEmpty()
val googleRedirectScheme: String = googleRedirectUri.substringBefore(":")

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
        manifestPlaceholders["appAuthRedirectScheme"] = googleRedirectScheme
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
    implementation(project(":navigation"))
    implementation(libs.kakao.user)
}
