package com.ruleup.domain.model

import com.ruleup.core.model.InterestCategory

data class SignupForm(
    val signupToken: String,
    val nickname: String,
    val interestCategories: List<InterestCategory>,
    val profileImageUrl: String?,
    val agreements: Agreements,
)
