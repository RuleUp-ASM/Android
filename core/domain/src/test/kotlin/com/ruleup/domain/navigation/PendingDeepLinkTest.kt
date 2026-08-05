package com.ruleup.domain.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class PendingDeepLinkTest {
    private val route = NavRoute(path = "challenge/detail", args = mapOf("id" to "1"))
    private val allPrivate = RouteAccessPolicy { true }
    private val allPublic = RouteAccessPolicy { false }

    @Test
    fun `보류된 목적지가 없으면 평소 진입이다`() {
        assertEquals(PendingDeepLinkEntry.None, PendingDeepLink().consumeFor(authenticated = true, policy = allPrivate))
    }

    @Test
    fun `인증됐으면 목적지를 연다`() {
        val pending = PendingDeepLink().apply { set(route) }

        assertEquals(PendingDeepLinkEntry.Open(route), pending.consumeFor(authenticated = true, policy = allPrivate))
    }

    @Test
    fun `미인증이면 로그인이 필요한 목적지를 버린다`() {
        // 세션 없이 띄우면 API 가 401 을 받고 사용자는 목적지가 아니라 로그인 화면을 본다.
        val pending = PendingDeepLink().apply { set(route) }

        assertEquals(PendingDeepLinkEntry.Dropped(route), pending.consumeFor(authenticated = false, policy = allPrivate))
    }

    @Test
    fun `미인증이어도 로그인이 필요 없는 목적지는 연다`() {
        val pending = PendingDeepLink().apply { set(route) }

        assertEquals(PendingDeepLinkEntry.Open(route), pending.consumeFor(authenticated = false, policy = allPublic))
    }

    @Test
    fun `버려진 목적지도 남지 않는다`() {
        // 남겨 두면 다음 진입에서 사용자가 열지도 않은 링크로 이동한다.
        val pending = PendingDeepLink().apply { set(route) }
        pending.consumeFor(authenticated = false, policy = allPrivate)

        assertEquals(PendingDeepLinkEntry.None, pending.consumeFor(authenticated = true, policy = allPrivate))
    }

    @Test
    fun `같은 목적지로 두 번 이동하지 않는다`() {
        val pending = PendingDeepLink().apply { set(route) }
        pending.consumeFor(authenticated = true, policy = allPrivate)

        assertEquals(PendingDeepLinkEntry.None, pending.consumeFor(authenticated = true, policy = allPrivate))
    }
}
