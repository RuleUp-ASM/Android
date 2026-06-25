package com.ruleup.verification.data.dto

import com.ruleup.verification.domain.entity.Place
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 11.7 장소 검색 (Naver Local 프록시) ----------
@Serializable
data class PlaceDto(
    @SerialName("name")
    val name: String? = null,
    @SerialName("lat")
    val lat: Double? = null,
    @SerialName("lng")
    val lng: Double? = null,
    @SerialName("address")
    val address: String? = null,
    @SerialName("category")
    val category: String? = null,
)

@Serializable
data class PlaceSearchResponse(
    @SerialName("places")
    val places: List<PlaceDto>? = null,
)

internal fun PlaceSearchResponse.toDomain(): List<Place> =
    places.orEmpty().mapNotNull { dto ->
        // 좌표가 없으면 앵커로 쓸 수 없으니 버린다.
        val lat = dto.lat ?: return@mapNotNull null
        val lng = dto.lng ?: return@mapNotNull null
        Place(
            name = dto.name.orEmpty(),
            lat = lat,
            lng = lng,
            address = dto.address,
            category = dto.category,
        )
    }
