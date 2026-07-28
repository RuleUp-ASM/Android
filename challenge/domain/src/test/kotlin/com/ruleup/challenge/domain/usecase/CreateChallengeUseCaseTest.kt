package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.entity.Anonymity
import com.ruleup.challenge.domain.entity.Challenge
import com.ruleup.challenge.domain.entity.ChallengeForm
import com.ruleup.challenge.domain.entity.ChallengeStatus
import com.ruleup.challenge.domain.entity.ChallengeUpdate
import com.ruleup.challenge.domain.entity.ModerationStatus
import com.ruleup.challenge.domain.entity.ParamValue
import com.ruleup.challenge.domain.entity.ParticipationType
import com.ruleup.challenge.domain.entity.Penalty
import com.ruleup.challenge.domain.entity.Reward
import com.ruleup.challenge.domain.entity.SelectedMethod
import com.ruleup.challenge.domain.entity.SnsShare
import com.ruleup.challenge.domain.entity.VerificationConfig
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.challenge.domain.entity.WearableRequirement
import com.ruleup.challenge.domain.repository.ChallengeRepository
import com.ruleup.challenge.domain.repository.SetupNotifier
import com.ruleup.entity.user.InterestCategory
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CreateChallengeUseCaseTest {
    @Test
    fun `생성 성공 시 생성된 챌린지를 반환한다`() =
        runBlocking {
            val challenge = challenge(challengeId = "c1", durationDays = 21, verification = null)
            val repo = FakeChallengeRepository(challenge)
            val notifier = RecordingSetupNotifier()

            val result = CreateChallengeUseCase(repo, notifier)(form())

            assertEquals(challenge, result)
        }

    @Test
    fun `자동 인증 챌린지는 필요한 권한과 함께 셋업 알림을 요청한다`() =
        runBlocking {
            val verification =
                VerificationConfig(
                    selectedMethod = SelectedMethod.AUTO,
                    verificationType = VerificationType.PHONE,
                    signalSource = null,
                    wearableRequirement = WearableRequirement.NONE,
                    requiredPermissions = listOf("ACTIVITY_RECOGNITION"),
                    externalService = null,
                )
            val challenge = challenge(challengeId = "c1", title = "달리기", verification = verification)
            val notifier = RecordingSetupNotifier()

            CreateChallengeUseCase(FakeChallengeRepository(challenge), notifier)(form())

            val call = notifier.lastCall!!
            assertEquals("c1", call.challengeId)
            assertEquals("달리기", call.title)
            assertTrue(call.isAuto)
            assertEquals(listOf("ACTIVITY_RECOGNITION"), call.requiredPermissions)
        }

    @Test
    fun `수동 인증 챌린지는 isAuto false 와 빈 권한으로 셋업 알림을 요청한다`() =
        runBlocking {
            val challenge = challenge(challengeId = "c2", verification = null)
            val notifier = RecordingSetupNotifier()

            CreateChallengeUseCase(FakeChallengeRepository(challenge), notifier)(form())

            val call = notifier.lastCall!!
            assertFalse(call.isAuto)
            assertTrue(call.requiredPermissions.isEmpty())
        }

    private fun challenge(
        challengeId: String,
        title: String = "챌린지",
        durationDays: Int = 7,
        verification: VerificationConfig?,
    ) = Challenge(
        challengeId = challengeId,
        status = ChallengeStatus.UPCOMING,
        title = title,
        description = null,
        imageUrl = null,
        category = InterestCategory.EXERCISE,
        participationType = ParticipationType.SOLO,
        maxParticipants = 1,
        minMannerTemperature = null,
        repeatDays = emptyList(),
        durationDays = durationDays,
        startDate = "2026-07-06",
        endDate = "2026-07-13",
        templateId = null,
        moderationStatus = ModerationStatus.NONE,
        verification = verification,
        params = emptyMap(),
        penalty = Penalty(mannerDeduction = 1.0, snsShare = SnsShare(enabled = false, phone = null), groupShare = false),
        reward = Reward(mannerGain = 1.0),
    )

    private fun form() =
        ChallengeForm(
            title = "챌린지",
            description = null,
            imageUrl = null,
            category = InterestCategory.EXERCISE,
            participationType = ParticipationType.SOLO,
            maxParticipants = 1,
            minMannerTemperature = null,
            repeatDays = emptyList(),
            durationDays = 7,
            startDate = "2026-07-06",
            templateId = null,
            selectedMethod = SelectedMethod.MANUAL,
            params = emptyMap<String, ParamValue>(),
            grantedPermissions = emptyList(),
            penalty = Penalty(mannerDeduction = 1.0, snsShare = SnsShare(enabled = false, phone = null), groupShare = false),
            reward = Reward(mannerGain = 1.0),
            anonymity = Anonymity.REAL,
        )

    private class FakeChallengeRepository(
        private val created: Challenge,
    ) : ChallengeRepository {
        override suspend fun create(form: ChallengeForm): Challenge = created

        override suspend fun recommend(
            title: String,
            description: String?,
        ) = throw NotImplementedError()

        override suspend fun recommendRoutines(limit: Int?) = throw NotImplementedError()

        override suspend fun recommendByTemplate(templateId: Long) = throw NotImplementedError()

        override suspend fun uploadImage(imageUri: String) = throw NotImplementedError()

        override suspend fun getChallenge(challengeId: String) = throw NotImplementedError()

        override suspend fun getSetupInfo(challengeId: String) = throw NotImplementedError()

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
            action: com.ruleup.challenge.domain.entity.RoleAction,
        ) = throw NotImplementedError()

        override suspend fun requestDelegation(
            challengeId: String,
            targetUserId: String,
        ) = throw NotImplementedError()

        override suspend fun respondDelegation(
            challengeId: String,
            delegationId: String,
            action: com.ruleup.challenge.domain.entity.DelegationAction,
        ) = throw NotImplementedError()
    }

    private class RecordingSetupNotifier : SetupNotifier {
        data class Call(
            val challengeId: String,
            val title: String,
            val requiredPermissions: List<String>,
            val isAuto: Boolean,
        )

        var lastCall: Call? = null

        override fun notifyAfterCreate(
            challengeId: String,
            title: String,
            requiredPermissions: List<String>,
            isAuto: Boolean,
        ) {
            lastCall = Call(challengeId, title, requiredPermissions, isAuto)
        }
    }
}
