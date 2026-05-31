plugins {
    id("ruleup.android.data")
}

android {
    namespace = "com.ruleup.feature.onboarding.data"
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":feature:onboarding:domain"))
}
