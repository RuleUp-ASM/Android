package com.ruleup.verification.data.dto

import com.ruleup.verification.domain.entity.AnchorSet
import com.ruleup.verification.domain.entity.ChallengeSetupResult
import com.ruleup.verification.domain.entity.LocationPin
import com.ruleup.verification.domain.entity.MyLocation
import com.ruleup.verification.domain.entity.SetupMissing
import com.ruleup.verification.domain.entity.SetupStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- setup 요청 (명세 setup) ----------

/**
 * 앵커 1개. **반경 필드가 없다** — 인증 반경은 서버 설정 단일값이라 요청에 싣지 않고,
 * 응답에서도 앵커별이 아니라 `serverRadiusM` 으로 따로 내려온다(명세 setup · my-location).
 */
@Serializable
data class AnchorDto(
    @SerialName("lat")
    val lat: Double,
    @SerialName("lng")
    val lng: Double,
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
                                AnchorDto(
                                    lat = it.lat,
                                    lng = it.lng,
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
    // 서버 설정 인증 반경(m) — GPS 방 표시·지오펜스 등록용
    @SerialName("serverRadiusM")
    val serverRadiusM: Int? = null,
)

internal fun ChallengeSetupResponse.toDomain(): ChallengeSetupResult =
    ChallengeSetupResult(
        status = SetupStatus.fromValue(setupStatus),
        // 알 수 없는 항목은 버린다(서버가 새 항목을 추가해도 안전).
        missing = missing.orEmpty().mapNotNull { SetupMissing.fromValue(it) },
        serverRadiusM = serverRadiusM?.toFloat(),
    )

// ---------- 앵커 조회 응답 (명세: GET /my-location) ----------

/** setup 요청과 동일한 앵커 표현을 재사용한다. */
internal fun AnchorDto.toDomain(): LocationPin =
    LocationPin(
        lat = lat,
        lng = lng,
        label = label,
    )

@Serializable
data class MyLocationResponse(
    @SerialName("anchors")
    val anchors: List<AnchorDto>? = null,
    @SerialName("appliedFrom")
    val appliedFrom: String? = null,
    @SerialName("serverRadiusM")
    val serverRadiusM: Int? = null,
    // 이번 달 변경 가능 여부. PUT 응답에는 없다 — 저장하면 그 달 1회를 소진하기 때문
    @SerialName("changeAvailable")
    val changeAvailable: Boolean? = null,
    @SerialName("nextChangeAvailableAt")
    val nextChangeAvailableAt: String? = null,
)

internal fun MyLocationResponse.toDomain(): MyLocation =
    MyLocation(
        anchors = anchors.orEmpty().map { it.toDomain() },
        appliedFrom = appliedFrom,
        serverRadiusM = serverRadiusM?.toFloat(),
        // 모르면 못 바꾸는 쪽으로 접는다 — 열어 두면 사용자가 눌렀다 429 를 본다.
        changeAvailable = changeAvailable ?: false,
        nextChangeAvailableAt = nextChangeAvailableAt,
    )

// ---------- 앵커 교체 (명세: PUT /my-location) ----------

/** 앵커 세트 전체 교체 요청. 부분 수정이 아니라 보낸 목록이 곧 새 세트다(최대 3개). */
@Serializable
data class UpdateMyLocationRequest(
    @SerialName("anchors")
    val anchors: List<AnchorDto>,
)

internal fun AnchorSet.toUpdateRequest(): UpdateMyLocationRequest =
    UpdateMyLocationRequest(
        anchors = pins.map { AnchorDto(lat = it.lat, lng = it.lng, label = it.label) },
    )
