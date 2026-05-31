plugins {
    id("ruleup.android.presentation")
}

android {
    namespace = "com.ruleup.feature.onboarding.presentation"
}

dependencies {
    implementation(project(":core:designsystem"))
}
