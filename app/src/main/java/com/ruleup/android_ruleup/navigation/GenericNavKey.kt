package com.ruleup.android_ruleup.navigation

import androidx.navigation3.runtime.NavKey
import com.ruleup.domain.navigation.NavRoute
import kotlinx.serialization.Serializable

/**
 * Navigation3 의 단일 백스택 엔트리 타입.
 *
 * 모든 destination 이 본 타입 하나로 표현되며, 실제 화면 분기는 [path] 값으로 결정한다.
 * [args] 는 NavRoute.args 와 동일한 String 맵 형태로 보존되어, 백스택 직렬화/복원 시
 * Navigation3 의 typed-route 메커니즘이 그대로 동작한다.
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
