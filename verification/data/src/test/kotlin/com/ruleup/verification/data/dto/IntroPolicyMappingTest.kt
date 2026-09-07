package com.ruleup.verification.data.dto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 인트로 정책 매핑. 이 값이 **온디바이스 수집 주기와 신호 on/off** 를 정한다 — 잘못 접히면
 * 앱이 신호를 모으지 않고, 사용자는 매일 인증에 실패하면서 이유를 알 수 없다.
 *
 * 그래서 "서버가 말하지 않은 것"은 **켠 쪽·기본 주기**로 떨어뜨린다. 임의로 끄는 것보다
 * 조금 더 모으는 편이 사고가 작다.
 */
class IntroPolicyMappingTest {
    @Test
    fun `주기를 안 주면 명세 기본값 30분으로 본다`() {
        val policy = IntroResponse().toDomain()

        assertEquals(1800, policy.flushIntervalSec)
    }

    @Test
    fun `주기를 주면 그 값을 그대로 쓴다`() {
        val policy = IntroResponse(flushIntervalSec = 600).toDomain()

        assertEquals(600, policy.flushIntervalSec)
    }

    @Test
    fun `켬 여부를 안 주면 켠 것으로 본다`() {
        // 서버가 끄지 않은 신호를 앱이 임의로 끄면 인증이 빈다.
        val policy =
            IntroResponse(collection = CollectionResponse(geofence = CadenceResponse(enabled = null))).toDomain()

        assertTrue(policy.geofence?.enabled == true)
    }

    @Test
    fun `서버가 끄라고 하면 끈다`() {
        val policy =
            IntroResponse(collection = CollectionResponse(geofence = CadenceResponse(enabled = false))).toDomain()

        assertTrue(policy.geofence?.enabled == false)
    }

    @Test
    fun `신호 설정 자체가 없으면 그 신호는 정책이 없는 것으로 둔다`() {
        // "안 왔다"와 "꺼졌다"는 다르다 — 없는 걸 꺼진 것으로 접으면 되돌릴 근거가 사라진다.
        val policy = IntroResponse(collection = null).toDomain()

        assertNull(policy.geofence)
        assertNull(policy.screenTime)
    }

    @Test
    fun `백오프를 안 주면 백오프 정책도 없다`() {
        val policy = IntroResponse(backoff = null).toDomain()

        assertNull(policy.backoff)
    }

    @Test
    fun `백오프를 주되 값이 비면 명세 기본값으로 채운다`() {
        val policy = IntroResponse(backoff = BackoffResponse()).toDomain()

        assertEquals(14400, policy.backoff?.maxSec)
        assertEquals(2.0, policy.backoff?.factor)
    }
}
