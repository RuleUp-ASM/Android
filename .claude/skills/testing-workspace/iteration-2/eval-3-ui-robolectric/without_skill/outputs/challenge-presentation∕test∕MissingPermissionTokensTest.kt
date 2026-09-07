package com.ruleup.challenge.presentation.detail

import com.ruleup.verification.domain.entity.PermissionState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 화면이 "무엇이 꺼져 있나"를 세는 규칙. 권한 배너와 참여 게이트가 **같은 계산**을 보므로,
 * 조합 폭발을 UI 로 끌고 가지 않고 여기서 축별로 고정한다.
 */
class MissingPermissionTokensTest {
    @Test
    fun `아직 못 물었으면 빈 목록이다`() {
        // 모른다고 참여를 잠그면 조회 실패가 곧 차단이 된다.
        val state = roomState().copy(permissions = null, setup = setup(requiredPermissions = listOf("LOCATION")))

        assertEquals(emptyList(), state.missingPermissionTokens())
    }

    @Test
    fun `꺼진 권한만 남긴다`() {
        val state =
            roomState().copy(
                setup = setup(requiredPermissions = listOf("LOCATION", "PACKAGE_USAGE_STATS")),
                permissions = permissions(location = PermissionState.DENIED),
            )

        assertEquals(listOf("LOCATION"), state.missingPermissionTokens())
    }

    @Test
    fun `앱이 모르는 토큰은 꺼진 것으로 세지 않는다`() {
        // 서버가 토큰을 추가했을 때 그 하나 때문에 구버전 앱이 통째로 잠기면 안 된다.
        val state =
            roomState().copy(
                setup = setup(requiredPermissions = listOf("NEW_FANCY_PERMISSION")),
                permissions = permissions(),
            )

        assertEquals(emptyList(), state.missingPermissionTokens())
    }

    @Test
    fun `셋업 응답이 상세보다 우선한다`() {
        // 상세의 목록은 방 생성 시점 스냅샷이라 셋업이 최신이다.
        val state =
            roomState(detail = detail(requiredPermissions = listOf("PACKAGE_USAGE_STATS"))).copy(
                setup = setup(requiredPermissions = listOf("LOCATION")),
                permissions = permissions(location = PermissionState.DENIED, usageStats = PermissionState.DENIED),
            )

        assertEquals(listOf("LOCATION"), state.missingPermissionTokens())
    }

    @Test
    fun `셋업이 없으면 상세의 목록으로 떨어진다`() {
        val state =
            roomState(detail = detail(requiredPermissions = listOf("PACKAGE_USAGE_STATS"))).copy(
                setup = null,
                permissions = permissions(usageStats = PermissionState.DENIED),
            )

        assertEquals(listOf("PACKAGE_USAGE_STATS"), state.missingPermissionTokens())
    }
}
