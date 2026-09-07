package com.ruleup.android_ruleup.navigation

import androidx.navigation3.runtime.NavKey
import com.ruleup.domain.navigation.NavRoute
import kotlinx.serialization.Serializable

/**
 * Navigation3 의 유일한 백스택 엔트리 타입. 화면 분기는 [path] 로 한다.
 * 백스택은 직렬화되어 복원되므로 [args] 도 NavRoute.args 와 같은 String 맵으로 보존한다.
 */
@Serializable
data class GenericNavKey(
    val path: String,
    val args: Map<String, String> = emptyMap(),
) : NavKey {
    fun toNavRoute(): NavRoute = NavRoute(path, args)

    companion object {
        fun of(route: NavRoute): GenericNavKey = GenericNavKey(route.path, route.args)
    }
}
