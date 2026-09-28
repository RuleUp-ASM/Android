package com.ruleup.domain.entity.user

/** 사용자 식별 정보. */
interface UserIdentity {
    val id: String
    val nickname: String
    val profileImageUrl: String?
}
