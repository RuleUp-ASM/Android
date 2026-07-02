package com.ruleup.challenge.domain

import com.ruleup.challenge.domain.entity.Challenge
import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.challenge.domain.entity.ChallengeForm
import com.ruleup.challenge.domain.entity.ChallengeMembers
import com.ruleup.challenge.domain.entity.ChallengeRecommendation
import com.ruleup.challenge.domain.entity.ChallengeUpdate
import com.ruleup.challenge.domain.entity.MemberAction
import com.ruleup.challenge.domain.entity.MemberStatus
import com.ruleup.challenge.domain.entity.MemberStatusFilter
import com.ruleup.challenge.domain.entity.MyChallenge
import com.ruleup.challenge.domain.entity.MyChallengeScope

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

    /**
     * 챌린지 대표 이미지를 업로드하고 서버 URL 을 반환한다(명세 3.9).
     * 생성/수정 전에 호출해, 반환된 URL 을 [ChallengeForm.imageUrl] 로 전달한다.
     */
    suspend fun uploadImage(imageUri: String): String

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

    /**
     * 내가 참여 중인 챌린지 목록 조회(명세: GET /challenges). 페이지네이션 없이 전량 반환하며,
     * 항목마다 내 멤버십 상태(memberStatus)를 포함한다. 기본 [MyChallengeScope.ACTIVE],
     * [MyChallengeScope.ALL] 은 승인 대기(PENDING) 멤버십까지 포함한다.
     */
    suspend fun getMyChallenges(scope: MyChallengeScope = MyChallengeScope.ACTIVE): List<MyChallenge>
}
