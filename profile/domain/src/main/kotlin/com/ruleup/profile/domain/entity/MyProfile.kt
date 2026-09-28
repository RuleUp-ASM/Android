package com.ruleup.profile.domain.entity

import com.ruleup.domain.entity.user.AgreementConsent
import com.ruleup.domain.entity.user.AgreementType
import com.ruleup.domain.entity.user.Gender
import com.ruleup.domain.entity.user.User
import com.ruleup.domain.entity.user.UserIdentity
import java.time.LocalDate

/** 내 프로필 조회(GET /api/v1/users/me). */
data class MyProfile(
    val user: User,
    val profileImageStatus: ImageModerationStatus?,
    val birthDate: LocalDate?,
    val gender: Gender?,
    val agreements: Map<AgreementType, AgreementConsent>,
) : UserIdentity by user

/** 이미지 자동 모더레이션 상태. */
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
