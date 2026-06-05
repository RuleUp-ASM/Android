package com.ruleup.domain.challenge

import com.ruleup.entity.challenge.Challenge
import com.ruleup.entity.challenge.ChallengeDetail
import com.ruleup.entity.challenge.ChallengeForm
import com.ruleup.entity.challenge.ChallengeMembers
import com.ruleup.entity.challenge.ChallengeRecommendation
import com.ruleup.entity.challenge.ChallengeUpdate
import com.ruleup.entity.challenge.MemberAction
import com.ruleup.entity.challenge.MemberStatus
import com.ruleup.entity.challenge.MemberStatusFilter

interface ChallengeRepository {
    /**
     * 제목/설명으로 LLM 기본값을 추천받는다(명세 3.1). 상태 저장 없음, 구속력 없는 초안.
     */
    suspend fun recommend(
        title: String,
        description: String? = null,
    ): ChallengeRecommendation

    /** 확정값으로 챌린지를 생성한다(명세 3.2). */
    suspend fun create(form: ChallengeForm): Challenge

    /** 챌린지 상세 + 참여 자격 조회(명세 3.3). */
    suspend fun getChallenge(challengeId: String): ChallengeDetail

    /** 챌린지 수정(시작 전, 생성자만). 변경할 필드만 전달한다(명세 3.4). */
    suspend fun update(
        challengeId: String,
        update: ChallengeUpdate,
    ): Challenge

    /** 챌린지 소프트 삭제(생성자만, 명세 3.5). */
    suspend fun delete(challengeId: String)

    /**
     * 챌린지 참여 신청(명세 3.6). 그룹/기준 미충족은 PENDING, 솔로/기준 미설정은 ACTIVE.
     */
    suspend fun join(challengeId: String): MemberStatus

    /** 참여 승인/거절(운영자, 명세 3.7). */
    suspend fun decideMember(
        challengeId: String,
        userId: String,
        action: MemberAction,
    ): MemberStatus

    /**
     * 챌린지 멤버 목록 조회(명세 3.8). 기본 ACTIVE, PENDING/ALL 은 OWNER만.
     */
    suspend fun getMembers(
        challengeId: String,
        status: MemberStatusFilter = MemberStatusFilter.ACTIVE,
    ): ChallengeMembers
}
