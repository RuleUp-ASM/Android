package com.ruleup.android_ruleup.deeplink

import android.app.Application
import android.net.Uri
import com.ruleup.challenge.domain.navigation.ChallengeInvitePage
import com.ruleup.challenge.domain.navigation.WatcherAcceptPage
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.observability.domain.test.testObservability
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 외부에서 들어오는 초대 링크 세 갈래 — `/inv`(친구) · `/c`(챌린지 멤버) · `/w`(감시자).
 *
 * 셋이 같은 도메인이라 **접두사 하나로 갈린다.** 잘못 갈라지면 감시자가 방에 가입되거나 그 반대가
 * 되고, 토큰을 잘못 잘라내면 수락이 통째로 막힌 채 "초대가 잘못됐다"로만 보인다.
 */
@RunWith(RobolectricTestRunner::class)
// 실제 App 은 카카오 SDK 초기화까지 한다 — URI 파싱만 보는 테스트가 그것 때문에 죽으면 안 된다.
@Config(application = Application::class)
class InviteLinkTest {
    @Test
    fun `초대 링크는 토큰을 그대로 실어 수락 화면으로 간다`() {
        val route = resolveStartRoute(uri("https://android.ruleup.co.kr/w/wtk_8f3a"), testObservability())

        assertEquals(WatcherAcceptPage.PATH, route?.path)
        assertEquals("wtk_8f3a", route?.args?.get(WatcherAcceptPage.ARG_TOKEN))
    }

    @Test
    fun `토큰 없는 초대 링크는 목적지로 삼지 않는다`() {
        // 수락할 대상이 없는데 화면을 띄우면 사용자가 빈 오류만 본다.
        assertNull(resolveStartRoute(uri("https://android.ruleup.co.kr/w"), testObservability()))
    }

    @Test
    fun `앱 사용 중에 열어도 같은 수락 화면으로 간다`() {
        // 콜드스타트와 실행 중 진입이 갈리면 같은 링크가 다르게 동작한다.
        val route = resolveNewIntentRoute(uri("https://android.ruleup.co.kr/w/wtk_1"), testObservability())

        assertEquals(WatcherAcceptPage.PATH, route?.path)
    }

    @Test
    fun `챌린지 초대 링크는 토큰을 그대로 실어 초대 화면으로 간다`() {
        val route = resolveStartRoute(uri("https://android.ruleup.co.kr/c/cinv_9d2f"), testObservability())

        assertEquals(ChallengeInvitePage.PATH, route?.path)
        assertEquals("cinv_9d2f", route?.args?.get(ChallengeInvitePage.ARG_TOKEN))
    }

    @Test
    fun `챌린지 초대와 감시자 초대는 서로 다른 화면으로 간다`() {
        // 접두사 하나로 갈린다 — 섞이면 감시자가 방에 가입되거나 그 반대가 된다.
        val challenge = resolveStartRoute(uri("https://android.ruleup.co.kr/c/t1"), testObservability())
        val watcher = resolveStartRoute(uri("https://android.ruleup.co.kr/w/t1"), testObservability())

        assertEquals(ChallengeInvitePage.PATH, challenge?.path)
        assertEquals(WatcherAcceptPage.PATH, watcher?.path)
    }

    @Test
    fun `친구 초대 링크는 감시자 수락으로 새지 않는다`() {
        // 두 링크가 같은 도메인이라 접두사 하나로 갈린다.
        assertNull(resolveStartRoute(uri("https://android.ruleup.co.kr/inv/abc"), testObservability()))
    }

    private fun uri(value: String): Uri = Uri.parse(value)
}

/**
 * 알림 딥링크(`ruleup://`) → 앱 라우트.
 *
 * 서버가 알림 타입을 늘리는 것은 정상이라 **모르는 링크가 오는 것도 정상**이다 — 그때 엉뚱한
 * 화면으로 보내는 것보다 아무 데도 안 가는 편이 낫다.
 *
 * 반대로 실재하는 화면인데 못 가면 알림을 눌러도 아무 일이 없는 것처럼 보인다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class RuleUpSchemeResolverTest {
    private val resolver = RuleUpSchemeResolver()

    @Test
    fun `챌린지 딥링크는 방 상세로 간다`() {
        val route = resolver.resolve("ruleup://challenge/c_301")

        assertEquals(AppRoutes.CHALLENGE_DETAIL, route?.path)
        assertEquals("c_301", route?.args?.get("challengeId"))
    }

    @Test
    fun `모더레이션 거부는 수정 화면으로 간다`() {
        // 거부 후 사용자가 할 일이 "고치는 것"이라 목록이 아니라 편집으로 보낸다.
        val route = resolver.resolve("ruleup://challenge/c_301/edit")

        assertEquals(AppRoutes.CHALLENGE_SETTINGS, route?.path)
    }

    @Test
    fun `티어 알림은 내 티어 화면으로 간다`() {
        assertEquals(AppRoutes.MY_TIER, resolver.resolve("ruleup://mypage/tier")?.path)
    }

    @Test
    fun `약관 개정 알림은 동의 화면으로 간다`() {
        assertEquals(AppRoutes.MY_AGREEMENTS, resolver.resolve("ruleup://terms/t_7")?.path)
    }

    @Test
    fun `모르는 딥링크는 아무 데도 보내지 않는다`() {
        assertNull(resolver.resolve("ruleup://something/new"))
    }

    @Test
    fun `부정행위 검출 알림은 제재 이력으로 간다`() {
        // 검출은 자동 제재(CHALLENGE_KICK)로 제재 이력에 남는다. 전용 화면은 아직 없다.
        assertEquals(AppRoutes.MY_SANCTIONS, resolver.resolve("ruleup://mypage/cheat-history")?.path)
    }

    @Test
    fun `폐기된 인증 상세 링크는 제자리에 둔다`() {
        // 2026-09-07 개정으로 VERIFICATION_RESULT 는 방 상세로 간다. 이 링크를 실은 옛 알림이
        // 보관 6개월 동안 남아 있으므로 받아만 두고 버린다 — 없는 화면으로 보내면 빈 화면이다.
        assertNull(resolver.resolve("ruleup://verification/v_88"))
    }

    @Test
    fun `다른 스킴은 이 해석기가 다루지 않는다`() {
        // https 앱링크는 별도 경로가 처리한다 — 여기서 가로채면 초대 링크가 엉뚱하게 풀린다.
        assertNull(resolver.resolve("https://android.ruleup.co.kr/c/tok"))
    }

    @Test
    fun `식별자 없는 챌린지 링크는 버린다`() {
        assertNull(resolver.resolve("ruleup://challenge"))
    }
}
