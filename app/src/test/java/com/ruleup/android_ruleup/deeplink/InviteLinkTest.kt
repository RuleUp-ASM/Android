package com.ruleup.android_ruleup.deeplink

import android.app.Application
import android.net.Uri
import com.ruleup.challenge.domain.navigation.ChallengeInvitePage
import com.ruleup.challenge.domain.navigation.WatcherAcceptPage
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
