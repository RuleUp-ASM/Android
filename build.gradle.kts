// Top-level build file where you can add configuration options common to all subprojects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
}

subprojects {
    apply(plugin = "dev.detekt")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    plugins.withId("com.android.application") {
        extensions.configure<com.android.build.api.dsl.ApplicationExtension> {
            lint {
                sarifReport = true
                htmlReport = true
                xmlReport = true
            }
        }
    }
    plugins.withId("com.android.library") {
        extensions.configure<com.android.build.api.dsl.LibraryExtension> {
            lint {
                sarifReport = true
                htmlReport = true
                xmlReport = true
            }
        }
    }

    // --- detekt ---
    extensions.configure<dev.detekt.gradle.extensions.DetektExtension> {
        toolVersion = "2.0.0-alpha.3"
        buildUponDefaultConfig = true
        allRules = false
        config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
        baseline = file("$projectDir/detekt-baseline.xml")
        autoCorrect = true
        parallel = true
    }

    tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
        jvmTarget = "21"
        reports {
            html.required.set(true)
            checkstyle.required.set(true)
            sarif.required.set(true)
            markdown.required.set(false)
        }
        exclude("**/build/**", "**/generated/**")
    }

    tasks.withType<dev.detekt.gradle.DetektCreateBaselineTask>().configureEach {
        jvmTarget = "21"
    }

    // --- ktlint ---
    extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set("1.8.0")
        android.set(true)
        ignoreFailures.set(false)
        enableExperimentalRules.set(false)
        filter {
            exclude { it.file.path.contains("/build/") }
            exclude { it.file.path.contains("/generated/") }
        }
        reporters {
            reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.HTML)
            reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
            reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.SARIF)
        }
    }

    dependencies {
        "ktlintRuleset"(rootProject.libs.ktlint.compose)
    }
}
