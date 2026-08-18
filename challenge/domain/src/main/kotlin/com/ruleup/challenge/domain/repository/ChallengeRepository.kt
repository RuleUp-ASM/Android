package com.ruleup.challenge.domain.repository

import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.challenge.domain.entity.ChallengeMembers
import com.ruleup.challenge.domain.entity.ChallengeSettings
import com.ruleup.challenge.domain.entity.ChallengeSetupInfo
import com.ruleup.challenge.domain.entity.ChallengeUpdate
import com.ruleup.challenge.domain.entity.ChallengeUpdateResult
import com.ruleup.challenge.domain.entity.CreateChallengeCommand
import com.ruleup.challenge.domain.entity.CreatedChallenge
import com.ruleup.challenge.domain.entity.DelegationAction
import com.ruleup.challenge.domain.entity.DelegationResolution
import com.ruleup.challenge.domain.entity.DelegationTicket
import com.ruleup.challenge.domain.entity.DeleteResult
import com.ruleup.challenge.domain.entity.DraftResult
import com.ruleup.challenge.domain.entity.JoinResult
import com.ruleup.challenge.domain.entity.LeaveResult
import com.ruleup.challenge.domain.entity.MemberRoleChange
import com.ruleup.challenge.domain.entity.MyChallenge
import com.ruleup.challenge.domain.entity.OwnerClaimResult
import com.ruleup.challenge.domain.entity.RoleAction
import com.ruleup.challenge.domain.entity.RoutineDescription
import com.ruleup.challenge.domain.entity.RoutineTemplate

interface ChallengeRepository {
    /**
     * 생성 화면에 항상 떠 있는 추천 루틴(명세 GET /challenges/recommendations).
     * 서버가 **항상 3개**를 보장하므로 개수 파라미터가 없다.
     */
    suspend fun getRoutineTemplates(): List<RoutineTemplate>

    /**
     * 루틴 설명으로 초안을 만든다(명세 POST /challenges/draft — 경로 B).
     *
     * 루틴으로 보이지 않거나 불순한 내용이면 [DraftResult.Fallback] 이 돌아온다 — **HTTP 200 이고 에러가
     * 아니다.** 호출자는 재입력 화면에 머물며 안내만 띄운다.
     *
     * 1분 10회 제한이 있어 초과 시 [com.ruleup.challenge.domain.entity.RecommendationRateLimitedException]
     * 이 던져진다. 자동 재시도는 금지 — 남은 rate limit 을 소진시킨다.
     */
    suspend fun createDraft(description: RoutineDescription): DraftResult

    /**
     * 추천 루틴 탭으로 초안을 만든다(명세 POST /challenges/recommendation/by-template — 경로 A).
     * LLM 을 거치지 않아 대기가 없고 폴백도 없다.
     */
    suspend fun createDraftFromTemplate(templateId: Long): DraftResult.Ok

    /**
     * 확인 화면에서 확정한 값으로 챌린지를 생성한다(명세 POST /challenges).
     *
     * [idempotencyKey] 는 확인 화면 진입 시 1회 생성해 재시도까지 계속 쓴다 — 네트워크 타임아웃 후
     * 재시도가 두 번째 방을 만들지 않게 하기 위해서다. 같은 키에 다른 본문을 보내면 서버가 409 로 막는다.
     */
    suspend fun create(
        command: CreateChallengeCommand,
        idempotencyKey: String,
    ): CreatedChallenge

    /**
     * 챌린지 대표 이미지를 업로드하고 서버 URL 을 반환한다(명세 POST /challenges/image).
     * 반환된 URL 만 생성·수정에 쓸 수 있다 — 서버가 발급 주체를 검증한다.
     */
    suspend fun uploadImage(imageUri: String): String

    /** 챌린지 공개 상세 조회(명세 GET /challenges/{id}). */
    suspend fun getChallenge(challengeId: String): ChallengeDetail

    /**
     * 챌린지 셋업 요구사항 조회(명세: GET /challenges/{id}/setup). 셋업 단계에서 무엇을 바인딩해야 하는지
     * (앵커·대상 앱)와 참고용 requiredPermissions 를 받아, 상세가 필요한 등록만 유도하는 데 쓴다.
     * (제출은 verification 의 POST /setup 이 담당한다.)
     */
    suspend fun getSetupInfo(challengeId: String): ChallengeSetupInfo

    /**
     * 방장 전용 설정 조회(명세 GET /challenges/{id}/settings). 수정 화면 진입 시 현재 설정 전체와
     * `editableFields`·`version` 을 받아 폼을 잠근다.
     */
    suspend fun getSettings(challengeId: String): ChallengeSettings

    /**
     * 챌린지 수정(방장, 명세 PATCH /challenges/{id}).
     *
     * [ChallengeUpdate.version] 이 서버와 다르거나 그 사이 수정 가능 범위가 바뀌었으면
     * [com.ruleup.challenge.domain.entity.ChallengeVersionConflictException] 이 던져진다 —
     * settings 를 재조회해 다시 그린 뒤 재시도한다.
     */
    suspend fun update(
        challengeId: String,
        update: ChallengeUpdate,
    ): ChallengeUpdateResult

    /**
     * 챌린지 삭제(생성자만, 명세 DELETE). 참여자(방장 제외) 0명일 때만 가능.
     * 진행 중 + success 이력이 있으면 탈퇴 패널티가 트리거된다([DeleteResult.penaltyApplied]).
     */
    suspend fun delete(challengeId: String): DeleteResult

    /**
     * 챌린지 가입(명세 POST members). 승인 없이 게이트만 통과하면 즉시 멤버가 된다.
     *
     * **자동 인증 방은 호출 전에 공개 상세의 `verification.requiredPermissions` 를 확보해야 한다** —
     * 서버는 권한을 게이트로 검사하지 않으며, 가입 후 권한 거부를 탈퇴로 롤백하는 경로는 폐기됐다.
     * 게이트에 막히면 [com.ruleup.challenge.domain.entity.JoinBlockedException] 이 던져진다.
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

    /**
     * 봇방장 방에서 손들고 방장 되기(명세 POST owner/claim). **선착순**이라 경합에서 밀리면
     * [com.ruleup.challenge.domain.entity.OwnerAlreadyExistsException] 이 올라온다.
     */
    suspend fun claimOwner(challengeId: String): OwnerClaimResult
}
