package com.ruleup.domain.entity.user

import com.ruleup.domain.entity.category.Category

/**
 * 로그인·가입 응답과 `GET /api/v1/users/me` 가 **같은 스키마로** 싣고 오는 사용자.
 *
 * onboarding(로그인·가입)과 profile(내 프로필 조회) 두 feature 가 쓰므로 core 에 둔다. 한쪽이
 * 다른 쪽 domain 을 참조하면 Gradle 순환이고, 각자 정의하면 서버 스키마가 하나인 이상 곧 어긋난다.
 *
 * @property nickname 본인 화면용. 심사 중이면 입력값, 거부되면 직전 승인본(없으면 임시 닉네임)이다.
 * @property score 티어 내 점수 0~99.
 * @property displayTier 표시·방 입장 판정에 쓰는 티어. 유예 밴드면 [tier] 보다 높을 수 있다.
 * @property lockInfo [accountStatus] 가 [AccountStatus.LOCKED] 일 때만 채워진다.
 */
data class User(
    val id: String,
    val nickname: String,
    val nicknameStatus: NicknameStatus,
    val profileImageUrl: String?,
    val tier: Tier,
    val score: Int,
    val displayTier: Tier,
    val interestCategories: List<Category>,
    val onboardingCompleted: Boolean,
    val accountStatus: AccountStatus,
    val lockInfo: LockInfo?,
)
