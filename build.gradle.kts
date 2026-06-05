// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.ktlint) apply false
}

// 모든 모듈에 ktlint 적용. CI 는 ./gradlew ktlintCheck 로 검사, 로컬은 ./gradlew ktlintFormat 로 자동 포맷.
subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    // Compose 전용 룰셋(io.nlopez.compose.rules). @Composable 코드에만 규칙이 적용된다.
    val catalog = rootProject.extensions.getByType<org.gradle.api.artifacts.VersionCatalogsExtension>().named("libs")
    dependencies {
        add("ktlintRuleset", catalog.findLibrary("ktlint-compose").get())
    }
}