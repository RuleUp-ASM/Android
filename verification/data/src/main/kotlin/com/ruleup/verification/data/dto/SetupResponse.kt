package com.ruleup.verification.data.dto

import com.ruleup.verification.domain.entity.ChallengeSetupResult
import com.ruleup.verification.domain.entity.LocationPin
import com.ruleup.verification.domain.entity.MyLocation
import com.ruleup.verification.domain.entity.SetupMissing
import com.ruleup.verification.domain.entity.SetupStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnchorResponse(
    @SerialName("lat")
    val lat: Double,
    @SerialName("lng")
    val lng: Double,
    @SerialName("label")
    val label: String? = null,
)

// setup 응답

@Serializable
data class ChallengeSetupResponse(
    @SerialName("setupStatus")
    val setupStatus: String? = null,
    @SerialName("missing")
    val missing: List<String>? = null,
    // 서버 설정 인증 반경(m)
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

// 앵커 조회 응답

internal fun AnchorResponse.toDomain(): LocationPin =
    LocationPin(
        lat = lat,
        lng = lng,
        label = label,
    )

@Serializable
data class MyLocationResponse(
    @SerialName("anchors")
    val anchors: List<AnchorResponse>? = null,
    @SerialName("appliedFrom")
    val appliedFrom: String? = null,
    @SerialName("serverRadiusM")
    val serverRadiusM: Int? = null,
    // 이번 달 변경 가능 여부.
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
        // 모르면 못 바꾸는 쪽으로 접는다
        changeAvailable = changeAvailable ?: false,
        nextChangeAvailableAt = nextChangeAvailableAt,
    )
