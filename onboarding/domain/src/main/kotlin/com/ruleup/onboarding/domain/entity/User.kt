package com.ruleup.onboarding.domain.entity

import com.ruleup.domain.entity.category.Category
import com.ruleup.profile.domain.entity.NicknameStatus

/**
 * 로그인·가입 응답이 싣고 오는 사용자. 화면에 보이는 계정 정보는
 * [com.ruleup.profile.domain.entity.Profile] 쪽이다.
 *
 * `GET /api/v1/users/me` 도 같은 스키마를 쓴다. 그 조회 경로가 붙는 시점에 두 feature 가 이 타입을
 * 공유하게 되므로, 그때 core 로 올린다(#186).
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
