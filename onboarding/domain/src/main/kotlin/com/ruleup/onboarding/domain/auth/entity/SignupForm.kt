package com.ruleup.onboarding.domain.auth.entity

import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.entity.category.InterestLimits
import com.ruleup.domain.entity.user.AgreementConsents
import com.ruleup.domain.entity.user.Gender
import java.time.LocalDate

/** 가입 요청 입력값(POST /auth/signup). */
data class SignupForm(
    val signupToken: String,
    val nickname: String,
    val interestCategories: List<Category>,
    val birthDate: LocalDate,
    val gender: Gender,
    val agreements: AgreementConsents,
    val localImageUri: String? = null,
) {
    /** 가입 요청 입력 검증. */
    init {
        require(interestCategories.size <= InterestLimits.MAX) {
            "관심 분야가 상한을 넘었습니다: ${interestCategories.size}"
        }
    }
}
