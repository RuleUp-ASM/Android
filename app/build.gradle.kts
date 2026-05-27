plugins {
    id("ruleup.android.application")
    id("ruleup.android.compose")
    id("ruleup.android.test")
}

android {
    namespace = "com.ruleup.android"

    defaultConfig {
        applicationId = "com.ruleup.android"
        versionCode =
            libs.versions.versionCode
                .get()
                .toInt()
        versionName = libs.versions.versionName.get()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
