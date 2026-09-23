package com.ruleup.onboarding.domain.account

import com.ruleup.domain.entity.user.AccountRestriction

/**
 * 지금 계정에 어떤 제한이 걸려 있는지 알려주는 포트.
 *
 * 스플래시와 로그인이 진입을 판정할 때 필요한데, 원본은 마이페이지 소관(`GET /users/me/sanctions`)이다.
 * onboarding 이 profile 의 저장소를 직접 물면 진입 경로가 남의 모듈 사정에 묶이므로 계약만 둔다 —
 * 구현은 `:app` 이 꽂는다.
 *
 * **계정 상태가 아니라 제한을 돌려준다.** 정지 계정의 상태값은 종류와 무관하게 `SUSPENDED` 하나라,
 * 상태만 받으면 기능 정지와 전체 잠금을 가를 수 없다.
 *
 * **모르면 [AccountRestriction.None] 으로 답한다.** 조회 실패로 정상 사용자를 잠그면 앱을 통째로 잃는다.
 */
fun interface AccountRestrictionProvider {
    suspend fun current(): AccountRestriction
}
