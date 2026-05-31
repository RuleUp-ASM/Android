plugins {
    `kotlin-dsl`
}

group = "com.ruleup.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {
    compileOnly(libs.bundles.gradle.plugins)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "ruleup.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "ruleup.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "ruleup.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("androidHilt") {
            id = "ruleup.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("jvmLibrary") {
            id = "ruleup.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("androidDomain") {
            id = "ruleup.android.domain"
            implementationClass = "AndroidDomainConventionPlugin"
        }
        register("androidTest") {
            id = "ruleup.android.test"
            implementationClass = "AndroidTestConventionPlugin"
        }
        register("androidPresentation") {
            id = "ruleup.android.presentation"
            implementationClass = "AndroidPresentationConventionPlugin"
        }
        register("androidData") {
            id = "ruleup.android.data"
            implementationClass = "AndroidDataConventionPlugin"
        }
    }
}
