package com.ruleup.profile.domain.entity

import com.ruleup.domain.entity.user.AgreementConsent
import com.ruleup.domain.entity.user.AgreementType
import com.ruleup.domain.entity.user.Gender
import com.ruleup.domain.entity.user.User
import java.time.LocalDate

/**
 * 내 프로필 조회(GET /api/v1/users/me). [user] 는 로그인 응답과 같은 스키마고, 본인만 보는 항목이 더 붙는다.
 * @property birthDate 가입 후 수정 불가.
 * @property agreements 6종 전부. 선택 약관 토글과 약관 개정 시 재동의 판정에 쓴다.
 */
data class MyProfile(
    val user: User,
    val profileImageStatus: ImageModerationStatus?,
    val birthDate: LocalDate?,
    val gender: Gender?,
    val agreements: Map<AgreementType, AgreementConsent>,
)

/** 이미지 자동 모더레이션 상태. 미등록이면 null 이다. */
enum class ImageModerationStatus(
    val value: String,
) {
    PENDING("PENDING"),
    APPROVED("APPROVED"),
    REJECTED("REJECTED"),
    ;

    companion object {
        fun fromValue(value: String?): ImageModerationStatus? = entries.find { it.value == value }
    }
}
