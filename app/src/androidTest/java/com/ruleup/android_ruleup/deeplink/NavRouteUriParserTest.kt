package com.ruleup.android_ruleup.deeplink

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 딥링크 경계 파서. `android.net.Uri` 가 프레임워크 타입이라 계측 테스트로 둔다.
 *
 * 여기서 고정하는 건 **화면 목록이 아니라 인자 신뢰 규칙**이다. 화면 허용 목록은 제거됐고,
 * 방어는 "외부가 정해서는 안 되는 인자를 버린다"로 옮겼다(#161).
 */
@RunWith(AndroidJUnit4::class)
class NavRouteUriParserTest {
    private fun parse(url: String) = Uri.parse(url).toNavRoute()

    @Test
    fun `app_접두사를_떼고_등록_경로_형식으로_만든다`() {
        val route = parse("https://android.ruleup.co.kr/app/challenge/notices/detail?challengeId=ch1&noticeId=n1")

        assertEquals("challenge/notices/detail", route?.path)
        assertEquals(mapOf("challengeId" to "ch1", "noticeId" to "n1"), route?.args)
    }

    @Test
    fun `canManage_는_버리고_식별자_인자는_남긴다`() {
        val route = parse("https://android.ruleup.co.kr/app/challenge/notices/detail?challengeId=ch1&canManage=true")

        assertEquals(mapOf("challengeId" to "ch1"), route?.args)
    }

    @Test
    fun `signupToken_은_외부에서_주입될_수_없다`() {
        val route = parse("https://android.ruleup.co.kr/app/profile/icon?signupToken=forged")

        assertEquals("profile/icon", route?.path)
        assertEquals(emptyMap<String, String>(), route?.args)
    }

    @Test
    fun `지오펜스_설정_인자는_버리고_challengeId_만_남긴다`() {
        val route =
            parse(
                "https://android.ruleup.co.kr/app/verification/location" +
                    "?challengeId=ch1&defaultRadiusM=1&dwellMinutes=1&targetPackages=com.evil",
            )

        assertEquals(mapOf("challengeId" to "ch1"), route?.args)
    }

    @Test
    fun `감시자_초대는_앱_경로가_아니다`() {
        assertNull(parse("https://android.ruleup.co.kr/w/token123"))
    }

    @Test
    fun `친구_초대는_앱_경로가_아니다`() {
        assertNull(parse("https://android.ruleup.co.kr/inv/CODE12"))
    }

    @Test
    fun `app_접두사가_없으면_해석하지_않는다`() {
        assertNull(parse("https://android.ruleup.co.kr/challenge/detail?challengeId=ch1"))
    }

    @Test
    fun `app_접두사만_있고_경로가_없으면_해석하지_않는다`() {
        assertNull(parse("https://android.ruleup.co.kr/app"))
    }
}
