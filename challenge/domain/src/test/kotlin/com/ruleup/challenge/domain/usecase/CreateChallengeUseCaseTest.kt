package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.entity.VerificationMethod
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.challenge.domain.fake.FakeChallengeRepository
import com.ruleup.challenge.domain.fake.RecordingSetupNotifier
import com.ruleup.challenge.domain.fake.command
import com.ruleup.challenge.domain.fake.createdChallenge
import com.ruleup.challenge.domain.fake.verification
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CreateChallengeUseCaseTest {
    @Test
    fun `생성 성공 시 생성된 챌린지를 반환한다`() =
        runBlocking {
            val created = createdChallenge(challengeId = "c1")
            val repo = FakeChallengeRepository(created = created)

            val result = CreateChallengeUseCase(repo, RecordingSetupNotifier())(command(), "key-1")

            assertEquals(created, result)
        }

    @Test
    fun `호출자가 준 idempotency key 를 그대로 전달한다`() =
        runBlocking {
            // 키를 유스케이스가 새로 만들면 재시도마다 값이 바뀌어 중복 생성 방지가 무의미해진다.
            val repo = FakeChallengeRepository(created = createdChallenge())

            CreateChallengeUseCase(repo, RecordingSetupNotifier())(command(), "key-fixed")

            assertEquals("key-fixed", repo.lastIdempotencyKey)
        }

    @Test
    fun `자동 인증 챌린지는 필요한 권한과 함께 셋업 알림을 요청한다`() =
        runBlocking {
            val created =
                createdChallenge(
                    challengeId = "c1",
                    verification =
                        verification(
                            type = VerificationType.AUTO,
                            method = VerificationMethod.WAKE,
                            requiredPermissions = listOf("PACKAGE_USAGE_STATS"),
                        ),
                )
            val notifier = RecordingSetupNotifier()

            CreateChallengeUseCase(FakeChallengeRepository(created = created), notifier)(
                command(title = "달리기"),
                "key-1",
            )

            val call = notifier.lastCall!!
            assertEquals("c1", call.challengeId)
            // 생성 응답에 제목이 없으므로 방금 보낸 요청값을 쓴다.
            assertEquals("달리기", call.title)
            assertTrue(call.isAuto)
            assertEquals(listOf("PACKAGE_USAGE_STATS"), call.requiredPermissions)
        }

    @Test
    fun `수동 인증 챌린지는 isAuto false 와 빈 권한으로 셋업 알림을 요청한다`() =
        runBlocking {
            val notifier = RecordingSetupNotifier()

            CreateChallengeUseCase(FakeChallengeRepository(created = createdChallenge()), notifier)(
                command(),
                "key-1",
            )

            val call = notifier.lastCall!!
            assertFalse(call.isAuto)
            assertTrue(call.requiredPermissions.isEmpty())
        }
}
