package com.ruleup.domain.navigation

/**
 * feature 가 외부(다른 feature)에 공개하는 진입점 path 계약.
 *
 * feature 모듈끼리는 서로의 Page 객체에 직접 의존하지 않고 이 상수로 이동한다
 * (예: 챌린지 생성 완료 → [HOME], 홈의 챌린지 생성 버튼 → [CHALLENGE_CREATE]).
 * feature 내부 전용 페이지(profile/nickname 등)는 여기 올리지 않고 각 feature domain 에 남긴다.
 */
object AppEntryRoutes {
    const val HOME = "home"
    const val CHALLENGE_CREATE = "challenge/create"
}
