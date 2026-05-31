import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidDomainConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("ruleup.android.library")
                apply("ruleup.android.hilt")
            }
        }
    }
}
