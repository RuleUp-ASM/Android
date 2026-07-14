package com.ruleup.map

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * [MapAnchor] 는 [GeofenceMap] 이 앵커 목록을 다시 그릴지 판단하는 근거로 구조적 동등성(`==`)에
 * 의존한다(`GeofenceMapObjects.drawAnchors`: `if (anchors == drawnAnchors) return`). 값이 같으면
 * 같은 리스트로 취급돼 재그림을 건너뛰므로, data class 동등성 계약이 그대로 유지되는지 검증한다.
 */
class MapLatLngTest {
    @Test
    fun `좌표와 반경이 모두 같은 앵커는 동등하다`() {
        val a = MapAnchor(lat = 37.5, lng = 127.0, radiusM = 600f)
        val b = MapAnchor(lat = 37.5, lng = 127.0, radiusM = 600f)

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `위도가 다르면 동등하지 않다`() {
        val a = MapAnchor(lat = 37.5, lng = 127.0, radiusM = 600f)
        val b = MapAnchor(lat = 37.6, lng = 127.0, radiusM = 600f)

        assertNotEquals(a, b)
    }

    @Test
    fun `경도가 다르면 동등하지 않다`() {
        val a = MapAnchor(lat = 37.5, lng = 127.0, radiusM = 600f)
        val b = MapAnchor(lat = 37.5, lng = 127.1, radiusM = 600f)

        assertNotEquals(a, b)
    }

    @Test
    fun `반경만 달라도 동등하지 않다`() {
        val a = MapAnchor(lat = 37.5, lng = 127.0, radiusM = 600f)
        val b = MapAnchor(lat = 37.5, lng = 127.0, radiusM = 601f)

        assertNotEquals(a, b)
    }

    @Test
    fun `앵커 리스트도 원소가 모두 같으면 동등하다 - drawAnchors 재그림 스킵 조건`() {
        val previous = listOf(MapAnchor(37.0, 127.0, 500f), MapAnchor(37.1, 127.1, 800f))
        val next = listOf(MapAnchor(37.0, 127.0, 500f), MapAnchor(37.1, 127.1, 800f))

        assertEquals(previous, next)
    }

    @Test
    fun `앵커 리스트 순서가 다르면 동등하지 않다`() {
        val previous = listOf(MapAnchor(37.0, 127.0, 500f), MapAnchor(37.1, 127.1, 800f))
        val reordered = listOf(MapAnchor(37.1, 127.1, 800f), MapAnchor(37.0, 127.0, 500f))

        assertNotEquals(previous, reordered)
    }

    @Test
    fun `앵커 개수가 다르면 동등하지 않다`() {
        val previous = listOf(MapAnchor(37.0, 127.0, 500f))
        val withOneMore = listOf(MapAnchor(37.0, 127.0, 500f), MapAnchor(37.1, 127.1, 800f))

        assertNotEquals(previous, withOneMore)
    }

    @Test
    fun `copy 는 지정한 필드만 바꾼다`() {
        val original = MapAnchor(lat = 37.0, lng = 127.0, radiusM = 500f)

        val moved = original.copy(lat = 38.0)

        assertEquals(38.0, moved.lat)
        assertEquals(original.lng, moved.lng)
        assertEquals(original.radiusM, moved.radiusM)
    }
}