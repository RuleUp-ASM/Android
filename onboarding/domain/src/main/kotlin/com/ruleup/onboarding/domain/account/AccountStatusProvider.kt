package com.ruleup.onboarding.domain.account

import com.ruleup.domain.entity.user.AccountStatus

/**
 * 지금 계정이 어떤 상태인지 알려주는 포트.
 *
 * 스플래시가 진입을 판정할 때 필요한데, 상태의 원본은 마이페이지 소관(`GET /users/me/sanctions`)이다.
 * onboarding 이 profile 의 저장소를 직접 물면 진입 경로가 남의 모듈 사정에 묶이므로 계약만 둔다 —
 * 구현은 `:app` 이 꽂는다.
 *
 * **모르면 [AccountStatus.ACTIVE] 로 답한다.** 조회 실패로 정상 사용자를 잠그면 앱을 통째로 잃는다.
 */
fun interface AccountStatusProvider {
    suspend fun current(): AccountStatus
}
