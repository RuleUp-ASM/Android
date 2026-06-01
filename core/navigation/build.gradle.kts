plugins {
    id("ruleup.android.library")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.ruleup.core.navigation"
}

dependencies {
    // NavKey 가 공개 API(키들의 상위 타입)에 노출되므로 api 로 전이 노출
    api(libs.androidx.navigation3.runtime)
    implementation(libs.kotlinx.serialization.json)
}
