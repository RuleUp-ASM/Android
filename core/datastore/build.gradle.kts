plugins {
    id("ruleup.android.library")
    id("ruleup.android.hilt")
}

android {
    namespace = "com.ruleup.core.datastore"
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.core)
}
