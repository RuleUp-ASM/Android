import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

class AndroidPresentationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("ruleup.android.library")
                apply("ruleup.android.compose")
                apply("ruleup.android.hilt")
            }

            dependencies {
                add("implementation", project(":core:ui"))
                add("implementation", libs.findBundle("presentation").get())
            }
        }
    }
}
