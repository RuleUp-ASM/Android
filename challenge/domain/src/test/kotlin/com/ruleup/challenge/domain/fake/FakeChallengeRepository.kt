package com.ruleup.challenge.domain.fake

import com.ruleup.challenge.domain.entity.ChallengeDraft
import com.ruleup.challenge.domain.entity.ChallengeMode
import com.ruleup.challenge.domain.entity.ChallengeModeration
import com.ruleup.challenge.domain.entity.ChallengePenalties
import com.ruleup.challenge.domain.entity.ChallengePeriod
import com.ruleup.challenge.domain.entity.ChallengeStatus
import com.ruleup.challenge.domain.entity.ChallengeUpdate
import com.ruleup.challenge.domain.entity.CreateChallengeCommand
import com.ruleup.challenge.domain.entity.CreatedChallenge
import com.ruleup.challenge.domain.entity.DelegationAction
import com.ruleup.challenge.domain.entity.DraftResult
import com.ruleup.challenge.domain.entity.ModerationState
import com.ruleup.challenge.domain.entity.RoleAction
import com.ruleup.challenge.domain.entity.RoutineDescription
import com.ruleup.challenge.domain.entity.VerificationConfig
import com.ruleup.challenge.domain.entity.VerificationMethod
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.challenge.domain.repository.ChallengeRepository
import com.ruleup.challenge.domain.repository.SetupNotifier
import com.ruleup.domain.entity.category.Category

/**
 * 테스트용 [ChallengeRepository]. 검증 대상 메서드만 값을 돌려주고 나머지는 호출되면 실패한다 —
 * 유스케이스가 의도치 않은 호출을 하면 조용히 지나가지 않고 드러나게 하려는 것이다.
 */
class FakeChallengeRepository(
    private val draftResult: DraftResult? = null,
    private val created: CreatedChallenge? = null,
) : ChallengeRepository {
    var lastCommand: CreateChallengeCommand? = null
        private set
    var lastIdempotencyKey: String? = null
        private set

    override suspend fun getRoutineTemplates() = throw NotImplementedError()

    override suspend fun createDraft(description: RoutineDescription): DraftResult = requireNotNull(draftResult)

    override suspend fun createDraftFromTemplate(templateId: Long): DraftResult.Ok = requireNotNull(draftResult) as DraftResult.Ok

    override suspend fun create(
        command: CreateChallengeCommand,
        idempotencyKey: String,
    ): CreatedChallenge {
        lastCommand = command
        lastIdempotencyKey = idempotencyKey
        return requireNotNull(created)
    }

    override suspend fun uploadImage(imageUri: String) = throw NotImplementedError()

    override suspend fun getChallenge(challengeId: String) = throw NotImplementedError()

    override suspend fun getSetupInfo(challengeId: String) = throw NotImplementedError()

    override suspend fun getSettings(challengeId: String) = throw NotImplementedError()

    override suspend fun update(
        challengeId: String,
        update: ChallengeUpdate,
    ) = throw NotImplementedError()

    override suspend fun delete(challengeId: String) = throw NotImplementedError()

    override suspend fun join(challengeId: String) = throw NotImplementedError()

    override suspend fun getMembers(challengeId: String) = throw NotImplementedError()

    override suspend fun getMyChallenges() = throw NotImplementedError()

    override suspend fun leaveChallenge(challengeId: String) = throw NotImplementedError()

    override suspend fun changeMemberRole(
        challengeId: String,
        userId: String,
        action: RoleAction,
    ) = throw NotImplementedError()

    override suspend fun requestDelegation(
        challengeId: String,
        targetUserId: String,
    ) = throw NotImplementedError()

    override suspend fun respondDelegation(
        challengeId: String,
        delegationId: String,
        action: DelegationAction,
    ) = throw NotImplementedError()

    override suspend fun claimOwner(challengeId: String) = throw NotImplementedError()
}

class RecordingSetupNotifier : SetupNotifier {
    data class Call(
        val challengeId: String,
        val title: String,
        val verification: VerificationConfig,
        val personalSetupRequired: Boolean,
    )

    var lastCall: Call? = null
        private set

    override fun notifyAfterCreate(
        challengeId: String,
        title: String,
        verification: VerificationConfig,
        personalSetupRequired: Boolean,
    ) {
        lastCall = Call(challengeId, title, verification, personalSetupRequired)
    }
}

fun verification(
    type: VerificationType = VerificationType.MANUAL,
    method: VerificationMethod = VerificationMethod.SELF_CHECK,
    requiredPermissions: List<String> = emptyList(),
) = VerificationConfig(type = type, method = method, requiredPermissions = requiredPermissions)

fun draft(
    title: String = "매일 아침 6시 기상",
    description: String = "아침형 인간이 되기",
    verification: VerificationConfig = verification(),
) = ChallengeDraft(
    title = title,
    description = description,
    category = Category.WAKE_SLEEP,
    mode = ChallengeMode.SOLO,
    visibility = null,
    rankingVisible = true,
    capacity = 50,
    minTier = null,
    period = ChallengePeriod(start = "2026-08-12", end = "2026-08-26"),
    weeklyCount = 7,
    params = emptyList(),
    verification = verification,
    penalties = ChallengePenalties(score = false, groupShare = false, watcher = false),
)

fun createdChallenge(
    challengeId: String = "c1",
    verification: VerificationConfig = verification(),
    personalSetupRequired: Boolean = false,
) = CreatedChallenge(
    challengeId = challengeId,
    status = ChallengeStatus.UPCOMING,
    moderation =
        ChallengeModeration(
            title = ModerationState.APPROVED,
            description = ModerationState.APPROVED,
            image = ModerationState.NONE,
        ),
    verification = verification,
    personalSetupRequired = personalSetupRequired,
    createdAt = "2026-08-11T10:00:00+09:00",
)

fun command(
    draftId: String = "d1",
    title: String = "매일 아침 6시 기상",
) = CreateChallengeCommand(
    draftId = draftId,
    title = title,
    description = "아침형 인간이 되기",
    category = Category.WAKE_SLEEP,
    mode = ChallengeMode.SOLO,
    visibility = null,
    rankingVisible = true,
    capacity = null,
    minTier = null,
    period = ChallengePeriod(start = "2026-08-12", end = "2026-08-26"),
    weeklyCount = 7,
    params = emptyList(),
    verification = verification(),
    watcherPenalty = false,
    imageUrl = null,
)
