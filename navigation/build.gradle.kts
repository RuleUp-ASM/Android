plugins {
    id("ruleup.android.presentation")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.ruleup.navigation"
}

dependencies {
    implementation(project(":core:navigation"))
    implementation(project(":feature:onboarding:presentation"))
}
