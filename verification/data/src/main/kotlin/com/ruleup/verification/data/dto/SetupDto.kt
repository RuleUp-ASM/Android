package com.ruleup.verification.data.dto

import com.ruleup.verification.domain.entity.ChallengeSetupResult
import com.ruleup.verification.domain.entity.LocationPin
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
data class LocationBindingDto(
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
    val location: LocationBindingDto? = null,
    @SerialName("targetPackages")
    val targetPackages: List<String>? = null,
)

/** 도메인 앵커/대상앱 → setup 와이어. radiusM 은 UseCase 에서 이미 500~5000 으로 클램프된 값을 반올림한다. */
internal fun buildChallengeSetupRequest(
    anchors: List<LocationPin>,
    targetPackages: List<String>,
): ChallengeSetupRequest =
    ChallengeSetupRequest(
        location =
            anchors
                .takeIf { it.isNotEmpty() }
                ?.let { pins ->
                    LocationBindingDto(
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
