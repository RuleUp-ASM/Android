package com.ruleup.challenge.domain.repository

/**
 * 챌린지별 "대상 앱 등록" 상태를 세션 동안 보관하는 로컬 스토어(프로세스 싱글톤, 인메모리).
 *
 * 생성 후 셋업 유도 흐름에서, 상세 화면이 "권한 허용 → 앱 등록 → 참여" 순서의 버튼 모드를 정할 때
 * 앱 등록 완료 여부를 이 스토어로 판별한다(명세: 대상 앱 등록은 로컬 저장으로 판별).
 * (영속이 필요해지면 DataStore 구현으로 교체할 수 있다 — [MyChallengeStore] 와 동일 관례.)
 */
interface TargetAppStore {
    /** 해당 챌린지에 대상 앱이 1개 이상 등록됐는지. */
    fun isRegistered(challengeId: String): Boolean

    /** 등록된 대상 앱 패키지명 목록(없으면 빈 리스트). */
    fun registered(challengeId: String): List<String>

    /** 대상 앱 패키지명 목록을 저장한다(빈 리스트면 미등록으로 되돌린다). */
    fun save(
        challengeId: String,
        packages: List<String>,
    )
}
