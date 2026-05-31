import java.util.Properties

plugins {
    id("ruleup.android.data")
}

val localProperties =
    Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) {
            file.inputStream().use { load(it) }
        }
    }

android {
    namespace = "com.ruleup.network"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        buildConfigField("String", "BASE_URL", "\"${localProperties.getProperty("BASE_URL")}\"")
    }
}
