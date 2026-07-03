package com.ruleup.verification.data.dto

import com.ruleup.verification.domain.entity.Place
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 카카오 로컬 키워드 검색 응답(필요 필드만) ----------
@Serializable
data class KakaoKeywordResponse(
    @SerialName("documents")
    val documents: List<KakaoPlaceResponse>? = null,
)

@Serializable
data class KakaoPlaceResponse(
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

// ---------- 카카오 로컬 좌표→주소(coord2address) 응답(필요 필드만) ----------
@Serializable
data class KakaoCoord2AddressResponse(
    @SerialName("documents")
    val documents: List<KakaoAddressResponse>? = null,
)

@Serializable
data class KakaoAddressResponse(
    @SerialName("road_address")
    val roadAddress: KakaoRoadAddressResponse? = null,
    @SerialName("address")
    val address: KakaoLotAddressResponse? = null,
)

@Serializable
data class KakaoRoadAddressResponse(
    @SerialName("address_name")
    val addressName: String? = null,
    @SerialName("building_name")
    val buildingName: String? = null,
)

@Serializable
data class KakaoLotAddressResponse(
    @SerialName("address_name")
    val addressName: String? = null,
)

/**
 * 역지오코딩 1건 → 앵커 [Place]. 좌표는 coord2address 가 돌려주지 않으므로 호출 시점의 탭 좌표를 그대로 유지한다.
 * 건물명이 있으면 이름으로, 없으면 주소를 이름으로 쓴다(이름=주소일 땐 [Place.address] 를 비워 중복 표기 방지).
 */
internal fun KakaoCoord2AddressResponse.toPlaceOrNull(
    lat: Double,
    lng: Double,
): Place? {
    val doc = documents?.firstOrNull() ?: return null
    // 도로명 주소 우선, 없으면 지번 주소.
    val address = doc.roadAddress?.addressName?.ifBlank { null } ?: doc.address?.addressName?.ifBlank { null } ?: return null
    val building = doc.roadAddress?.buildingName?.ifBlank { null }
    return Place(
        name = building ?: address,
        lat = lat,
        lng = lng,
        address = if (building != null) address else null,
        category = null,
    )
}
