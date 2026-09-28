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
    fun `미인증이면 로그인이 필요한 목적지를 열지 않고 보류한다`() {
        val pending = PendingDeepLink().apply { set(route) }

        assertEquals(PendingDeepLinkEntry.Deferred(route), pending.consumeFor(authenticated = false, policy = allPrivate))
    }

    @Test
    fun `미인증이어도 로그인이 필요 없는 목적지는 연다`() {
        val pending = PendingDeepLink().apply { set(route) }

        assertEquals(PendingDeepLinkEntry.Open(route), pending.consumeFor(authenticated = false, policy = allPublic))
    }

    @Test
    fun `보류한 목적지는 로그인 뒤 한 번만 꺼낸다`() {
        val pending = PendingDeepLink().apply { set(route) }
        pending.consumeFor(authenticated = false, policy = allPrivate)

        assertEquals(route, pending.consumeAfterLogin())
        assertEquals(null, pending.consumeAfterLogin())
    }

    @Test
    fun `같은 목적지로 두 번 이동하지 않는다`() {
        val pending = PendingDeepLink().apply { set(route) }
        pending.consumeFor(authenticated = true, policy = allPrivate)

        assertEquals(PendingDeepLinkEntry.None, pending.consumeFor(authenticated = true, policy = allPrivate))
    }
}
