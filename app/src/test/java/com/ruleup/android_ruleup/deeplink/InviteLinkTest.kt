package com.ruleup.android_ruleup.deeplink

import android.app.Application
import android.net.Uri
import com.ruleup.challenge.domain.navigation.ChallengeInvitePage
import com.ruleup.challenge.domain.navigation.WatcherAcceptPage
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.observability.domain.test.testObservability
import com.ruleup.profile.domain.navigation.MyCalendarPage
import com.ruleup.support.domain.navigation.InquiryDetailPage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 외부 초대 링크 파싱. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class InviteLinkTest {
    @Test
    fun `초대 링크는 토큰을 그대로 실어 수락 화면으로 간다`() {
        val route = resolveStartRoute(uri("https://android.ruleup.co.kr/w/wtk_8f3a"), testObservability())

        assertEquals(WatcherAcceptPage.PATH, route?.path)
        assertEquals("wtk_8f3a", route?.args?.get(WatcherAcceptPage.ARG_TOKEN))
    }

    @Test
    fun `카카오톡 카드에서 앱 실행으로 들어오면 감시자 수락 화면으로 간다`() {
        // 웹 주소로만 열면 서버가 앱 설치 여부와 상관없이 플레이스토어로 보낸다(#574).
        val route = resolveStartRoute(uri("kakaoabc123://kakaolink?invite=w&token=wtk_8f3a"), testObservability())

        assertEquals(WatcherAcceptPage.PATH, route?.path)
        assertEquals("wtk_8f3a", route?.args?.get(WatcherAcceptPage.ARG_TOKEN))
    }

    @Test
    fun `카카오톡 카드에서 앱 실행으로 들어오면 챌린지 초대 화면으로 간다`() {
        val route = resolveNewIntentRoute(uri("kakaoabc123://kakaolink?invite=c&token=cinv_9d2f"), testObservability())

        assertEquals(ChallengeInvitePage.PATH, route?.path)
        assertEquals("cinv_9d2f", route?.args?.get(ChallengeInvitePage.ARG_TOKEN))
    }

    @Test
    fun `카카오 앱 실행 주소라도 토큰이나 종류가 없으면 목적지로 삼지 않는다`() {
        assertNull(resolveStartRoute(uri("kakaoabc123://kakaolink?invite=w"), testObservability()))
        assertNull(resolveStartRoute(uri("kakaoabc123://kakaolink?token=t1"), testObservability()))
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
        // 접두사 하나로 갈린다
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

/** 알림 딥링크 변환. */
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
    fun `이의 결과 알림은 이의 내역으로 간다`() {
        assertEquals(AppRoutes.MY_APPEALS, resolver.resolve("ruleup://me/appeals")?.path)
    }

    @Test
    fun `부정행위 검출 알림은 제재 이력으로 간다`() {
        assertEquals(AppRoutes.MY_SANCTIONS, resolver.resolve("ruleup://mypage/cheat-history")?.path)
    }

    @Test
    fun `폐기된 인증 상세 링크는 제자리에 둔다`() {
        assertNull(resolver.resolve("ruleup://verification/v_88"))
    }

    @Test
    fun `다른 스킴은 이 해석기가 다루지 않는다`() {
        assertNull(resolver.resolve("https://android.ruleup.co.kr/c/tok"))
    }

    @Test
    fun `식별자 없는 챌린지 링크는 버린다`() {
        assertNull(resolver.resolve("ruleup://challenge"))
    }

    @Test
    fun `강퇴 알림은 제재 이력으로 간다`() {
        assertEquals(AppRoutes.MY_SANCTIONS, resolver.resolve("ruleup://me/sanctions")?.path)
    }

    @Test
    fun `실패 예정 알림은 그 날짜의 캘린더를 연다`() {
        // 날짜를 잃으면 이의 기한이 걸린 그 건을 사용자가 손으로 찾아야 한다.
        val route = resolver.resolve("ruleup://me/calendar/2026-09-20")

        assertEquals(AppRoutes.MY_CALENDAR, route?.path)
        assertEquals("2026-09-20", route?.args?.get(MyCalendarPage.ARG_DATE))
    }

    @Test
    fun `날짜 없는 캘린더 링크도 캘린더로 보낸다`() {
        val route = resolver.resolve("ruleup://me/calendar")

        assertEquals(AppRoutes.MY_CALENDAR, route?.path)
        assertNull(route?.args?.get(MyCalendarPage.ARG_DATE))
    }

    @Test
    fun `문의 답변 알림은 그 문의 상세를 연다`() {
        val route = resolver.resolve("ruleup://me/inquiries/inq_1")

        assertEquals(AppRoutes.MY_INQUIRY_DETAIL, route?.path)
        assertEquals("inq_1", route?.args?.get(InquiryDetailPage.ARG_INQUIRY_ID))
    }

    @Test
    fun `식별자 없는 문의 링크는 내역 목록으로 보낸다`() {
        // 상세를 열 수 없다고 탭을 죽이면 답변이 왔다는 사실만 남고 볼 길이 없다.
        assertEquals(AppRoutes.MY_INQUIRIES, resolver.resolve("ruleup://me/inquiries")?.path)
    }

    @Test
    fun `감시 실패 알림은 감시 관계 목록으로 간다`() {
        // 통지 1건짜리 화면이 없다
        assertEquals(AppRoutes.MY_WATCHING, resolver.resolve("ruleup://watching/notices/n_1")?.path)
    }
}
