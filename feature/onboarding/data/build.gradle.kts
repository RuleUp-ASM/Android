plugins {
    id("ruleup.android.data")
}

android {
    namespace = "com.ruleup.feature.onboarding.data"
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:datastore"))
    implementation(project(":core:model"))
    implementation(project(":feature:onboarding:domain"))
    implementation(libs.kakao.user)
    implementation(libs.app.auth)
}
