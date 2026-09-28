package com.ruleup.android_ruleup.deeplink

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ruleup.domain.navigation.NavRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/** 진입 URI 파서. */
@RunWith(AndroidJUnit4::class)
class NavRouteUriParserTest {
    private fun parse(url: String) = Uri.parse(url).toNavRoute()

    @Test
    fun `app_접두사를_떼고_등록_경로_형식으로_만든다`() {
        val route = parse("https://android.ruleup.co.kr/app/challenge/detail?challengeId=ch1&from=push")

        assertEquals("challenge/detail", route?.path)
        assertEquals(mapOf("challengeId" to "ch1", "from" to "push"), route?.args)
    }

    @Test
    fun `인자를_걸러내지_않는다`() {
        // 필터링을 없앤 근거는 매니페스트에 /app 필터가 없다는 것이다.
        val route =
            parse(
                "https://android.ruleup.co.kr/app/verification/location" +
                    "?challengeId=ch1&defaultRadiusM=1&dwellMinutes=1",
            )

        assertEquals(
            mapOf("challengeId" to "ch1", "defaultRadiusM" to "1", "dwellMinutes" to "1"),
            route?.args,
        )
    }

    @Test
    fun `앱이_만든_URI_는_그대로_되파싱된다`() {
        // 알림이 조립한 목적지가 왕복해서 같은 NavRoute 로 돌아와야 한다.
        val original = NavRoute("challenge/detail", mapOf("challengeId" to "ch1", "from" to "push"))

        val roundTrip = original.toAppLinkUri().toNavRoute()

        assertEquals(original.path, roundTrip?.path)
        assertEquals(original.args, roundTrip?.args)
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
