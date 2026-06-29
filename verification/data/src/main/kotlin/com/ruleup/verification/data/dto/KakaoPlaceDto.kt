package com.ruleup.verification.data.dto

import com.ruleup.verification.domain.entity.Place
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 카카오 로컬 키워드 검색 응답(필요 필드만) ----------
@Serializable
data class KakaoKeywordResponse(
    @SerialName("documents")
    val documents: List<KakaoPlaceDto>? = null,
)

@Serializable
data class KakaoPlaceDto(
    @SerialName("place_name")
    val placeName: String? = null,
    // 카카오는 x=경도, y=위도 를 "문자열" 로 준다.
    @SerialName("x")
    val x: String? = null,
    @SerialName("y")
    val y: String? = null,
    @SerialName("address_name")
    val addressName: String? = null,
    @SerialName("road_address_name")
    val roadAddressName: String? = null,
    @SerialName("category_group_name")
    val categoryGroupName: String? = null,
    @SerialName("category_name")
    val categoryName: String? = null,
)

internal fun KakaoKeywordResponse.toDomain(): List<Place> =
    documents.orEmpty().mapNotNull { dto ->
        // 좌표가 없거나 숫자로 못 바꾸면 앵커로 쓸 수 없으니 버린다.
        val lng = dto.x?.toDoubleOrNull() ?: return@mapNotNull null
        val lat = dto.y?.toDoubleOrNull() ?: return@mapNotNull null
        Place(
            name = dto.placeName.orEmpty(),
            lat = lat,
            lng = lng,
            // 도로명 주소 우선, 없으면 지번 주소.
            address = dto.roadAddressName?.ifBlank { null } ?: dto.addressName?.ifBlank { null },
            category = dto.categoryGroupName?.ifBlank { null } ?: dto.categoryName?.ifBlank { null },
        )
    }
