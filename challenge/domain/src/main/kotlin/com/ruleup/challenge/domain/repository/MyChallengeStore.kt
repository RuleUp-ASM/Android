package com.ruleup.challenge.domain.repository

import com.ruleup.challenge.domain.entity.MyChallengeSummary

/**
 * 생성/참여한 "내 챌린지"를 세션 동안 보관하는 로컬 스토어(프로세스 싱글톤, 인메모리).
 *
 * 홈이 서버 진행률(verification/progress)과 병합해, 방금 만든 챌린지가 진행률 API 반영 전이라도
 * 즉시 보이게 한다.
 * (영속이 필요해지면 DataStore 구현으로 교체할 수 있다.)
 */
interface MyChallengeStore {
    /** 최근 추가가 앞에 오는 내 챌린지 요약 스냅샷. */
    fun all(): List<MyChallengeSummary>

    /** 생성/참여한 챌린지를 추가(같은 challengeId 면 최신값으로 갱신). */
    fun add(summary: MyChallengeSummary)
}
