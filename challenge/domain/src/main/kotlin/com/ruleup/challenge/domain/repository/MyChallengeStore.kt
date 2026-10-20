package com.ruleup.challenge.domain.repository

import com.ruleup.challenge.domain.entity.MyChallengeSummary

/** 생성/참여한 "내 챌린지"를 세션 동안 보관하는 로컬 스토어(프로세스 싱글톤, 인메모리). */
interface MyChallengeStore {
    /** 최근 추가가 앞에 오는 내 챌린지 요약 스냅샷. */
    fun all(): List<MyChallengeSummary>

    /** 생성/참여한 챌린지를 추가(같은 challengeId 면 최신값으로 갱신). */
    fun add(summary: MyChallengeSummary)

    /** 나간 챌린지를 뺀다. 남겨 두면 서버 목록 조회가 실패했을 때 나간 방이 홈에 되살아난다. */
    fun remove(challengeId: String)
}
