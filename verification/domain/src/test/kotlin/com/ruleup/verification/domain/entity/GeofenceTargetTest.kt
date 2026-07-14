package com.ruleup.verification.domain.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

/**
 * [LocationPin] 에 새로 추가된 [LocationPin.address] (표시용 주소, 앵커 목록 시트에서 노출) 를 검증한다.
 * [GeofenceTarget] 자체는 이번 PR 에서 변경되지 않았으므로 별도 커버리지를 추가하지 않는다.
 */
class GeofenceTargetTest {
    @Test
    fun `address 를 생략하면 기본값은 null 이다`() {
        val pin = LocationPin(lat = 37.0, lng = 127.0, radiusM = 600f, label = "헬스장")

        assertNull(pin.address)
    }

    @Test
    fun `address 를 지정하면 그대로 보관된다`() {
        val pin = LocationPin(lat = 37.0, lng = 127.0, radiusM = 600f, label = "헬스장", address = "서울시 중구")

        assertEquals("서울시 중구", pin.address)
    }

    @Test
    fun `address 만 다르면 동등하지 않다 - data class 계약`() {
        val withAddress = LocationPin(lat = 37.0, lng = 127.0, radiusM = 600f, label = "헬스장", address = "서울시 중구")
        val withoutAddress = LocationPin(lat = 37.0, lng = 127.0, radiusM = 600f, label = "헬스장", address = null)

        assertNotEquals(withAddress, withoutAddress)
    }

    @Test
    fun `copy 로 address 만 갈아끼울 수 있다`() {
        val original = LocationPin(lat = 37.0, lng = 127.0, radiusM = 600f, label = "헬스장", address = null)

        val withAddress = original.copy(address = "서울시 중구")

        assertEquals("서울시 중구", withAddress.address)
        assertEquals(original.lat, withAddress.lat)
        assertEquals(original.label, withAddress.label)
    }
}