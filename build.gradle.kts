
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.firebase.appdistribution) apply false
}

// 전 모듈 ktlint 설정.
subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    // Compose 전용 룰셋(io.nlopez.compose.rules).
    val catalog = rootProject.extensions.getByType<org.gradle.api.artifacts.VersionCatalogsExtension>().named("libs")
    dependencies {
        add("ktlintRuleset", catalog.findLibrary("ktlint-compose").get())
    }
}
