plugins {
    id("ruleup.android.library")
}

android {
    namespace = "com.ruleup.core.ui"
}

dependencies {
    api(libs.androidx.lifecycle.viewmodel.compose)
    api(libs.kotlinx.coroutines.core)
}
