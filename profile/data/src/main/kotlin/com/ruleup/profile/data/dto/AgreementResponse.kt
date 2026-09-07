package com.ruleup.profile.data.dto

import com.ruleup.domain.entity.user.AgreementType
import com.ruleup.network.dto.ApiException
import com.ruleup.profile.domain.entity.AgreementRevokeForbiddenException
import com.ruleup.profile.domain.entity.AgreementState
import com.ruleup.profile.domain.entity.AgreementStatus
import com.ruleup.profile.domain.entity.AgreementSubmission
import com.ruleup.profile.domain.entity.AgreementVersionMismatchException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 동의 현황 (GET · POST /users/me/agreements) ----------
@Serializable
data class AgreementItemResponse(
    // TOS / PRIVACY / LOCATION / MARKETING / EVENT / LOCATION_INFO / HEALTH_INFO
    @SerialName("type")
    val type: String? = null,
    @SerialName("required")
    val required: Boolean? = null,
    @SerialName("agreed")
    val agreed: Boolean? = null,
    @SerialName("version")
    val version: String? = null,
    @SerialName("agreedAt")
    val agreedAt: String? = null,
)

@Serializable
data class AgreementStatusResponse(
    @SerialName("agreements")
    val agreements: List<AgreementItemResponse>? = null,
    @SerialName("reconsentRequired")
    val reconsentRequired: List<String>? = null,
)

internal fun AgreementStatusResponse.toDomain(): AgreementStatus =
    AgreementStatus(
        // 모르는 항목은 버린다 — 이름도 설명도 없는 토글을 화면에 세울 수 없다.
        agreements = agreements.orEmpty().mapNotNull { it.toDomain() },
        reconsentRequired = reconsentRequired.orEmpty().mapNotNull(AgreementType::fromApiType),
    )

internal fun AgreementItemResponse.toDomain(): AgreementState? {
    val type = AgreementType.fromApiType(type) ?: return null
    return AgreementState(
        type = type,
        // 필수 여부를 모르면 서버가 아니라 항목 정의를 믿는다 — 필수를 선택으로 보이면 철회 버튼이 열린다.
        required = required ?: type.required,
        agreed = agreed ?: false,
        version = version,
        agreedAt = agreedAt,
    )
}

@Serializable
data class AgreementSubmitItemRequest(
    @SerialName("type")
    val type: String,
    @SerialName("agreed")
    val agreed: Boolean,
    @SerialName("version")
    val version: String,
)

@Serializable
data class AgreementSubmitRequest(
    @SerialName("agreements")
    val agreements: List<AgreementSubmitItemRequest>,
)

/** 동의 API 로 보낼 수 없는 항목(폐기된 야간 알림)은 요청에서 뺀다 — 서버가 400 으로 막는다. */
internal fun List<AgreementSubmission>.toRequest(): AgreementSubmitRequest =
    AgreementSubmitRequest(
        agreements =
            mapNotNull { submission ->
                val type = submission.type.apiType ?: return@mapNotNull null
                AgreementSubmitItemRequest(type = type, agreed = submission.agreed, version = submission.version)
            },
    )

/**
 * 동의 제출 실패를 화면이 분기할 수 있는 타입으로 옮긴다.
 *
 * 필수 약관 철회는 "탈퇴 안내"로, 버전 불일치는 "다시 불러오기"로 갈린다 — 둘을 같은 토스트로
 * 접으면 사용자가 무엇을 해야 할지 알 수 없다.
 */
internal fun ApiException.toAgreementFailure(): Throwable =
    when (code) {
        "AGREEMENT_REVOKE_FORBIDDEN" -> AgreementRevokeForbiddenException()
        "AGREEMENT_VERSION_MISMATCH" -> AgreementVersionMismatchException()
        else -> this
    }
