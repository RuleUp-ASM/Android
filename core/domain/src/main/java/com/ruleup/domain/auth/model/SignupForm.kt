package com.ruleup.domain.auth.model

import com.ruleup.entity.user.Agreement
import com.ruleup.entity.user.InterestCategory

/**
 * 신규 가입 입력값 (명세 4.3).
 *
 * [localImageUri] 는 아직 업로드되지 않은 로컬 이미지 URI 다. 값이 있으면
 * 가입 전에 업로드(4.10)해 받은 URL 로 가입한다. null 이면 기본 프로필로 가입.
 */
data class SignupForm(
    val signupToken: String,
    val nickname: String,
    val interestCategories: List<InterestCategory>,
    val agreements: Agreement,
    val localImageUri: String? = null,
)
