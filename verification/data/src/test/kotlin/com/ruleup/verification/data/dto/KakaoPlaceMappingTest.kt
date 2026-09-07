package com.ruleup.verification.data.dto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 카카오 장소 검색 응답 매핑. 여기서 나온 좌표가 **지오펜스 앵커**가 되므로, 틀리면 사용자는
 * 엉뚱한 곳에 가야 인증이 된다 — 그리고 왜 안 되는지 알 방법이 없다.
 *
 * 특히 카카오는 `x` 가 경도, `y` 가 위도다. 뒤바꿔도 숫자가 들어가 컴파일·파싱은 통과하고,
 * 지도에 찍히는 위치만 조용히 어긋난다.
 */
class KakaoPlaceMappingTest {
    @Test
    fun `x 는 경도이고 y 는 위도다`() {
        // 뒤바꿔도 타입은 맞아 통과한다 — 이 테스트만이 축을 지킨다.
        val places = KakaoKeywordResponse(documents = listOf(doc(x = "127.0276", y = "37.4979"))).toDomain()

        assertEquals(127.0276, places.single().lng)
        assertEquals(37.4979, places.single().lat)
    }

    @Test
    fun `좌표를 숫자로 못 바꾸면 앵커로 쓸 수 없어 버린다`() {
        // 0 으로 접으면 아프리카 앞바다가 인증 기준점이 된다.
        val places = KakaoKeywordResponse(documents = listOf(doc(x = "없음", y = "37.4979"))).toDomain()

        assertTrue(places.isEmpty())
    }

    @Test
    fun `좌표가 아예 없어도 버린다`() {
        val places = KakaoKeywordResponse(documents = listOf(doc(x = null, y = null))).toDomain()

        assertTrue(places.isEmpty())
    }

    @Test
    fun `쓸 수 있는 것만 남기고 나머지는 버린다`() {
        // 하나가 깨졌다고 목록 전체를 버리면 검색이 통째로 실패한 것처럼 보인다.
        val places =
            KakaoKeywordResponse(
                documents = listOf(doc(name = "정상", x = "127.0", y = "37.5"), doc(name = "깨짐", x = null, y = null)),
            ).toDomain()

        assertEquals(listOf("정상"), places.map { it.name })
    }

    @Test
    fun `주소는 도로명을 먼저 쓴다`() {
        val places =
            KakaoKeywordResponse(
                documents = listOf(doc(roadAddress = "테헤란로 1", address = "역삼동 1-1")),
            ).toDomain()

        assertEquals("테헤란로 1", places.single().address)
    }

    @Test
    fun `도로명이 비어 있으면 지번으로 떨어진다`() {
        // 빈 문자열을 그대로 쓰면 주소 없는 카드가 된다.
        val places =
            KakaoKeywordResponse(documents = listOf(doc(roadAddress = "   ", address = "역삼동 1-1"))).toDomain()

        assertEquals("역삼동 1-1", places.single().address)
    }

    @Test
    fun `주소가 둘 다 없으면 지어내지 않는다`() {
        val places =
            KakaoKeywordResponse(documents = listOf(doc(roadAddress = null, address = null))).toDomain()

        assertNull(places.single().address)
    }

    @Test
    fun `결과가 통째로 없으면 빈 목록으로 다룬다`() {
        assertEquals(emptyList(), KakaoKeywordResponse(documents = null).toDomain())
    }

    private fun doc(
        name: String = "헬스장",
        x: String? = "127.0",
        y: String? = "37.5",
        roadAddress: String? = "테헤란로 1",
        address: String? = "역삼동 1-1",
    ) = KakaoPlaceResponse(
        placeName = name,
        x = x,
        y = y,
        addressName = address,
        roadAddressName = roadAddress,
        categoryGroupName = "체육시설",
        categoryName = null,
    )
}
