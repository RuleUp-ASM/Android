package com.ruleup.challenge.domain.repository

import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.challenge.domain.entity.ChallengeInvitation
import com.ruleup.challenge.domain.entity.ChallengeInvitationPreview
import com.ruleup.challenge.domain.entity.ChallengeMembers
import com.ruleup.challenge.domain.entity.ChallengeSettings
import com.ruleup.challenge.domain.entity.ChallengeSetupInfo
import com.ruleup.challenge.domain.entity.ChallengeUpdate
import com.ruleup.challenge.domain.entity.ChallengeUpdateResult
import com.ruleup.challenge.domain.entity.CreateChallengeCommand
import com.ruleup.challenge.domain.entity.CreatedChallenge
import com.ruleup.challenge.domain.entity.DraftResult
import com.ruleup.challenge.domain.entity.JoinResult
import com.ruleup.challenge.domain.entity.LeaveResult
import com.ruleup.challenge.domain.entity.MyChallengeFilter
import com.ruleup.challenge.domain.entity.MyChallengePage
import com.ruleup.challenge.domain.entity.RoutineDescription
import com.ruleup.challenge.domain.entity.RoutineTemplate

interface ChallengeRepository {
    /** 생성 화면에 항상 떠 있는 추천 루틴. */
    suspend fun getRoutineTemplates(): List<RoutineTemplate>

    /** 루틴 설명으로 초안을 만든다. */
    suspend fun createDraft(description: RoutineDescription): DraftResult

    /** 추천 루틴 탭으로 초안을 만든다. */
    suspend fun createDraftFromTemplate(templateId: Long): DraftResult.Ok

    /** 확인 화면에서 확정한 값으로 챌린지를 생성한다. */
    suspend fun create(
        command: CreateChallengeCommand,
        idempotencyKey: String,
    ): CreatedChallenge

    /** 챌린지 대표 이미지를 업로드하고 서버 URL 을 반환한다. */
    suspend fun uploadImage(imageUri: String): String

    /** 챌린지 공개 상세 조회. */
    suspend fun getChallenge(challengeId: String): ChallengeDetail

    /** 챌린지 셋업 요구사항 조회. */
    suspend fun getSetupInfo(challengeId: String): ChallengeSetupInfo

    /** 방장 전용 설정 조회. */
    suspend fun getSettings(challengeId: String): ChallengeSettings

    /** 챌린지 수정. */
    suspend fun update(
        challengeId: String,
        update: ChallengeUpdate,
    ): ChallengeUpdateResult

    /** 챌린지 가입. */
    suspend fun join(challengeId: String): JoinResult

    /** 멤버 초대 링크 발급. */
    suspend fun createInvitation(challengeId: String): ChallengeInvitation

    /** 초대 링크 미리보기. */
    suspend fun getInvitation(token: String): ChallengeInvitationPreview

    /** 초대 수락 가입. */
    suspend fun acceptInvitation(token: String): JoinResult

    /** 챌린지 멤버 목록 조회. */
    suspend fun getMembers(challengeId: String): ChallengeMembers

    /** 내 챌린지 목록 조회. */
    suspend fun getMyChallenges(
        filter: MyChallengeFilter = MyChallengeFilter.IN_PROGRESS,
        cursor: String? = null,
        size: Int? = null,
    ): MyChallengePage

    /** 챌린지 탈퇴. */
    suspend fun leaveChallenge(challengeId: String): LeaveResult
}
