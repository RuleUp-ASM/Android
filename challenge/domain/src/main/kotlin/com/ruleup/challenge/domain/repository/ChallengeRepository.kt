package com.ruleup.challenge.domain.repository

import com.ruleup.challenge.domain.entity.Challenge
import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.challenge.domain.entity.ChallengeForm
import com.ruleup.challenge.domain.entity.ChallengeMembers
import com.ruleup.challenge.domain.entity.ChallengeRecommendation
import com.ruleup.challenge.domain.entity.ChallengeSetupInfo
import com.ruleup.challenge.domain.entity.ChallengeUpdate
import com.ruleup.challenge.domain.entity.DelegationAction
import com.ruleup.challenge.domain.entity.DelegationResolution
import com.ruleup.challenge.domain.entity.DelegationTicket
import com.ruleup.challenge.domain.entity.DeleteResult
import com.ruleup.challenge.domain.entity.JoinResult
import com.ruleup.challenge.domain.entity.LeaveResult
import com.ruleup.challenge.domain.entity.MemberRoleChange
import com.ruleup.challenge.domain.entity.MyChallenge
import com.ruleup.challenge.domain.entity.RoleAction
import com.ruleup.challenge.domain.entity.RoutineRecommendation

interface ChallengeRepository {
    /**
     * 제목/설명으로 LLM 기본값을 추천받는다(명세 3.1). 상태 저장 없음, 구속력 없는 초안.
     */
    suspend fun recommend(
        title: String,
        description: String? = null,
    ): ChallengeRecommendation

    /**
     * 관심사 + 세그먼트 인기도 기반 루틴 템플릿 추천(명세 GET /recommendations/routines).
     * 진행 중 템플릿 제외, 최대 [limit]건(기본 서버값 3). 상태 저장 없음.
     */
    suspend fun recommendRoutines(limit: Int? = null): List<RoutineRecommendation>

    /**
     * 선택한 루틴 템플릿 기반 설정 초안 반환(명세 POST /challenges/recommendation/by-template).
     * LLM 호출 없이 템플릿 카탈로그에서 초안을 구성한다. 응답 스키마는 [recommend] 와 동일하다.
     */
    suspend fun recommendByTemplate(templateId: Long): ChallengeRecommendation

    /** 확정값으로 챌린지를 생성한다(명세 3.2). */
    suspend fun create(form: ChallengeForm): Challenge

    /**
     * 챌린지 대표 이미지를 업로드하고 서버 URL 을 반환한다(명세 3.9).
     * 생성/수정 전에 호출해, 반환된 URL 을 [ChallengeForm.imageUrl] 로 전달한다.
     */
    suspend fun uploadImage(imageUri: String): String

    /** 챌린지 상세 + 참여 자격 조회(명세 3.3). */
    suspend fun getChallenge(challengeId: String): ChallengeDetail

    /**
     * 챌린지 셋업 요구사항 조회(명세: GET /challenges/{id}/setup). 셋업 단계에서 무엇을 바인딩해야 하는지
     * (앵커·대상 앱)와 참고용 requiredPermissions 를 받아, 상세가 필요한 등록만 유도하는 데 쓴다.
     * (제출은 verification 의 POST /setup 이 담당한다.)
     */
    suspend fun getSetupInfo(challengeId: String): ChallengeSetupInfo

    /** 챌린지 수정(시작 전, 생성자만). 변경할 필드만 전달한다(명세 3.4). */
    suspend fun update(
        challengeId: String,
        update: ChallengeUpdate,
    ): Challenge

    /**
     * 챌린지 삭제(생성자만, 명세 DELETE). 참여자(방장 제외) 0명일 때만 가능.
     * 진행 중 + success 이력이 있으면 탈퇴 패널티가 트리거된다([DeleteResult.penaltyApplied]).
     */
    suspend fun delete(challengeId: String): DeleteResult

    /**
     * 챌린지 참여 신청(명세 POST members). 승인 없이 검증 통과 시 즉시 ACTIVE.
     * 자동 인증 챌린지면 [JoinResult.requiredPermissions] 로 가입 직후 권한 요청을 유도한다.
     */
    suspend fun join(challengeId: String): JoinResult

    /**
     * 챌린지 멤버 목록 조회(명세 GET members). 승인제 폐기로 status 필터 없이 확정 멤버만 반환한다.
     */
    suspend fun getMembers(challengeId: String): ChallengeMembers

    /**
     * 내가 참여 중인 챌린지 목록 조회(명세: GET /challenges). 승인제 폐기로 scope 없이 전량 반환한다.
     */
    suspend fun getMyChallenges(): List<MyChallenge>

    /**
     * 챌린지 탈퇴(본인, 명세 DELETE members/me). 본인 success 이력이 있으면 탈퇴 패널티가 트리거된다.
     * OWNER 는 탈퇴 불가(위임 또는 삭제로 안내) — 서버가 403 OWNER_CANNOT_LEAVE 로 분기 사유를 준다.
     */
    suspend fun leaveChallenge(challengeId: String): LeaveResult

    /**
     * 공동 관리자 임명/해제(명세 PATCH members/{userId}/role). 임명·해제는 OWNER, 본인 DEMOTE 는 MANAGER 본인만.
     */
    suspend fun changeMemberRole(
        challengeId: String,
        userId: String,
        action: RoleAction,
    ): MemberRoleChange

    /**
     * 방장 위임 요청 생성(OWNER, 명세 POST delegation). 대상은 MANAGER, 7일 후 자동 만료.
     */
    suspend fun requestDelegation(
        challengeId: String,
        targetUserId: String,
    ): DelegationTicket

    /**
     * 방장 위임 요청 응답(명세 PATCH delegation/{id}). ACCEPT/REJECT 는 대상자, CANCEL 은 요청 OWNER.
     */
    suspend fun respondDelegation(
        challengeId: String,
        delegationId: String,
        action: DelegationAction,
    ): DelegationResolution
}
