package com.ruleup.verification.data.dto

import com.ruleup.verification.domain.entity.AnchorSet
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// setup 요청

/** 앵커 1개. */
@Serializable
data class AnchorRequest(
    @SerialName("lat")
    val lat: Double,
    @SerialName("lng")
    val lng: Double,
    @SerialName("label")
    val label: String? = null,
)

/** 장소 바인딩(GPS_PRESENCE). */
@Serializable
data class LocationBindingRequest(
    @SerialName("fillMode")
    val fillMode: String = FILL_MODE_MANUAL,
    @SerialName("anchors")
    val anchors: List<AnchorRequest>,
) {
    companion object {
        const val FILL_MODE_MANUAL = "MANUAL"
    }
}

/** 셋업 제출 본문. */
@Serializable
data class ChallengeSetupRequest(
    @SerialName("location")
    val location: LocationBindingRequest? = null,
    @SerialName("targetPackages")
    val targetPackages: List<String>? = null,
)

/** 도메인 앵커/대상앱 → setup 와이어. */
internal fun buildChallengeSetupRequest(
    anchors: AnchorSet,
    targetPackages: List<String>,
): ChallengeSetupRequest =
    ChallengeSetupRequest(
        location =
            anchors
                .pins
                .takeIf { it.isNotEmpty() }
                ?.let { pins ->
                    LocationBindingRequest(
                        anchors =
                            pins.map {
                                AnchorRequest(
                                    lat = it.lat,
                                    lng = it.lng,
                                    label = it.label,
                                )
                            },
                    )
                },
        targetPackages = targetPackages.takeIf { it.isNotEmpty() },
    )

// 앵커 교체

/** 앵커 세트 전체 교체 요청. */
@Serializable
data class UpdateMyLocationRequest(
    @SerialName("anchors")
    val anchors: List<AnchorRequest>,
)

internal fun AnchorSet.toUpdateRequest(): UpdateMyLocationRequest =
    UpdateMyLocationRequest(
        anchors = pins.map { AnchorRequest(lat = it.lat, lng = it.lng, label = it.label) },
    )
