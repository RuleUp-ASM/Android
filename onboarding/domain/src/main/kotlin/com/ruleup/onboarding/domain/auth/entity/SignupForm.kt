package com.ruleup.onboarding.domain.auth.entity

import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.entity.user.AgreementConsents
import com.ruleup.domain.entity.user.Gender
import java.time.LocalDate

/**
 * 가입 요청 입력값(POST /auth/signup).
 *
 * [localImageUri] 는 전송 본문에 들어가지 않는다. 프로필 사진은 **가입을 마친 뒤** 발급받은
 * accessToken 으로 별도 API 에 올리므로, 이 값은 그 후속 호출을 위해 폼에 얹혀 갈 뿐이다.
 *
 * @property interestCategories 0~6개. 건너뛰면 빈 리스트.
 * @property gender 필수 필드. UI 에서 건너뛰면 호출부가 [Gender.NON_BINARY] 를 채운다.
 */
data class SignupForm(
    val signupToken: String,
    val nickname: String,
    val interestCategories: List<Category>,
    val birthDate: LocalDate,
    val gender: Gender,
    val agreements: AgreementConsents,
    val localImageUri: String? = null,
)
