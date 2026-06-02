package com.ruleup.domain.auth.model

import com.ruleup.core.model.InterestCategory

data class SignupForm(
    val signupToken: String,
    val nickname: String,
    val interestCategories: List<InterestCategory>,
    /** 화면에서 고른 로컬 이미지 Uri 문자열. 업로드 후 서버 URL 로 치환된다. */
    val localImageUri: String?,
    val agreements: Agreements,
)
