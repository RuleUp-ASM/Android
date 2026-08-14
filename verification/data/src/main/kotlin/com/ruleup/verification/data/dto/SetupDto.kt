package com.ruleup.verification.data.dto

import com.ruleup.verification.domain.entity.AnchorSet
import com.ruleup.verification.domain.entity.ChallengeSetupResult
import com.ruleup.verification.domain.entity.LocationPin
import com.ruleup.verification.domain.entity.MyLocation
import com.ruleup.verification.domain.entity.SetupAnchors
import com.ruleup.verification.domain.entity.SetupMissing
import com.ruleup.verification.domain.entity.SetupStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- setup 요청 (명세 setup) ----------

@Serializable
data class AnchorDto(
    @SerialName("lat")
    val lat: Double,
    @SerialName("lng")
    val lng: Double,
    @SerialName("radiusM")
    val radiusM: Int,
    @SerialName("label")
    val label: String? = null,
)

/** 장소 바인딩(GPS_PRESENCE). 현재 앱은 지도 수동 입력만 지원하므로 fillMode 는 MANUAL 고정. */
@Serializable
data class LocationBindingRequest(
    @SerialName("fillMode")
    val fillMode: String = FILL_MODE_MANUAL,
    @SerialName("anchors")
    val anchors: List<AnchorDto>,
) {
    companion object {
        const val FILL_MODE_MANUAL = "MANUAL"
    }
}

/**
 * 셋업 제출 본문(명세 setup). 앵커가 없으면 location 은 null(GPS 바인딩 없음),
 * targetPackages 가 비면 null 로 생략한다(explicitNulls=false).
 */
@Serializable
data class ChallengeSetupRequest(
    @SerialName("location")
    val location: LocationBindingRequest? = null,
    @SerialName("targetPackages")
    val targetPackages: List<String>? = null,
)

/** 도메인 앵커/대상앱 → setup 와이어. radiusM 은 [LocationPin] 이 이미 범위를 보장하므로 반올림만 한다. */
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
                                AnchorDto(
                                    lat = it.lat,
                                    lng = it.lng,
                                    radiusM = it.radiusM.toInt(),
                                    label = it.label,
                                )
                            },
                    )
                },
        targetPackages = targetPackages.takeIf { it.isNotEmpty() },
    )

// ---------- setup 응답 (명세 setup) ----------

@Serializable
data class ChallengeSetupResponse(
    @SerialName("setupStatus")
    val setupStatus: String? = null,
    @SerialName("missing")
    val missing: List<String>? = null,
)

internal fun ChallengeSetupResponse.toDomain(): ChallengeSetupResult =
    ChallengeSetupResult(
        status = SetupStatus.fromValue(setupStatus),
        // 알 수 없는 항목은 버린다(서버가 새 항목을 추가해도 안전).
        missing = missing.orEmpty().mapNotNull { SetupMissing.fromValue(it) },
    )

// ---------- 앵커 조회 응답 (명세: GET /my-location) ----------

/**
 * setup 요청과 동일한 앵커 표현을 재사용한다. radiusM 은 도메인에서 Float 로 승격.
 *
 * [LocationPin] 은 반경 범위를 생성 시점에 강제한다. 서버가 계약을 벗어난 값을 주더라도 조회가
 * 깨지면 안 되므로 경계에서 흡수한다.
 */
internal fun AnchorDto.toDomain(): LocationPin =
    LocationPin(
        lat = lat,
        lng = lng,
        radiusM = radiusM.toFloat().coerceIn(SetupAnchors.MIN_RADIUS_M, SetupAnchors.MAX_RADIUS_M),
        label = label,
    )

@Serializable
data class MyLocationResponse(
    @SerialName("anchors")
    val anchors: List<AnchorDto>? = null,
    @SerialName("appliedFrom")
    val appliedFrom: String? = null,
)

internal fun MyLocationResponse.toDomain(): MyLocation =
    MyLocation(
        anchors = anchors.orEmpty().map { it.toDomain() },
        appliedFrom = appliedFrom,
    )
