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
    fun `셋업 알림에 인증 스냅샷을 그대로 넘긴다`() =
        runBlocking {
            // 무엇을 등록해야 하는지는 method 가 결정한다 — 알림이 장소·앱을 가르려면 이 값이 필요하다.
            val snapshot =
                verification(
                    type = VerificationType.AUTO,
                    method = VerificationMethod.GPS_PRESENCE,
                    requiredPermissions = listOf("ACCESS_FINE_LOCATION"),
                )
            val created = createdChallenge(challengeId = "c1", verification = snapshot, personalSetupRequired = true)
            val notifier = RecordingSetupNotifier()

            CreateChallengeUseCase(FakeChallengeRepository(created = created), notifier)(
                command(title = "달리기"),
                "key-1",
            )

            val call = notifier.lastCall!!
            assertEquals("c1", call.challengeId)
            // 생성 응답에 제목이 없으므로 방금 보낸 요청값을 쓴다.
            assertEquals("달리기", call.title)
            assertEquals(snapshot, call.verification)
            assertTrue(call.personalSetupRequired)
        }

    @Test
    fun `서버가 개인 설정 불필요라고 하면 그대로 전달한다`() =
        runBlocking {
            // 알림을 띄울지 말지는 구현이 정하지만, 서버 판단이 유실되면 정할 수가 없다.
            val notifier = RecordingSetupNotifier()

            CreateChallengeUseCase(FakeChallengeRepository(created = createdChallenge()), notifier)(
                command(),
                "key-1",
            )

            assertFalse(notifier.lastCall!!.personalSetupRequired)
        }
}
