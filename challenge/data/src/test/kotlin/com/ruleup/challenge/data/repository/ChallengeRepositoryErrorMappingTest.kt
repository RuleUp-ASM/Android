package com.ruleup.challenge.data.repository

import com.ruleup.challenge.domain.entity.ChallengeNotEditableException
import com.ruleup.challenge.domain.entity.ChallengeNotFoundException
import com.ruleup.challenge.domain.entity.ChallengeVersionConflictException
import com.ruleup.challenge.domain.entity.InvalidWeeklyCountException
import com.ruleup.challenge.domain.entity.JoinBlockReason
import com.ruleup.challenge.domain.entity.JoinBlockedException
import com.ruleup.challenge.domain.entity.ModerationLockedException
import com.ruleup.network.dto.ApiException
import com.ruleup.network.dto.ErrorBody
import com.ruleup.network.image.ImageReader
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** 서버 에러 → 도메인 예외 번역. */
class ChallengeRepositoryErrorMappingTest {
    @Test
    fun `가입 거절은 사유까지 실어 올린다`() =
        runBlocking {
            // 코드 하나에 사유가 여럿 묶여 있어, 사유를 잃으면 화면이 안내를 가를 수 없다.
            val failure =
                assertFailsWith<JoinBlockedException> {
                    repository(error("JOIN_BLOCKED", reason = "FULL")).join("ch1")
                }

            assertEquals(JoinBlockReason.FULL, failure.reason)
        }

    @Test
    fun `재입장 대기는 언제부터 가능한지 함께 올린다`() =
        runBlocking {
            val failure =
                assertFailsWith<JoinBlockedException> {
                    repository(
                        error("JOIN_BLOCKED", reason = "REJOIN_COOLDOWN", rejoinAvailableAt = "2026-09-08T00:00:00Z"),
                    ).join("ch1")
                }

            assertEquals("2026-09-08T00:00:00Z", failure.rejoinAvailableAt)
        }

    @Test
    fun `앱이 모르는 거절 사유는 지어내지 않고 비운다`() =
        runBlocking {
            val failure =
                assertFailsWith<JoinBlockedException> {
                    repository(error("JOIN_BLOCKED", reason = "SOMETHING_NEW")).join("ch1")
                }

            assertNull(failure.reason)
        }

    @Test
    fun `없는 챌린지에 가입하려 하면 없다고 알린다`() =
        runBlocking {
            assertFailsWith<ChallengeNotFoundException> {
                repository(error("CHALLENGE_NOT_FOUND")).join("ch1")
            }
            Unit
        }

    @Test
    fun `가입에서 모르는 오류는 그대로 올려 보낸다`() =
        runBlocking {
            // 삼키면 화면이 "성공했는데 아무 일도 없는" 상태가 된다.
            assertFailsWith<ApiException> { repository(error("SOMETHING_ELSE")).join("ch1") }
            Unit
        }

    @Test
    fun `남이 먼저 고쳤으면 버전 충돌로 구분한다`() =
        runBlocking {
            assertFailsWith<ChallengeVersionConflictException> {
                repository(error("VERSION_CONFLICT")).update("ch1", update())
            }
            Unit
        }

    @Test
    fun `수정할 수 없는 상태는 버전 충돌과 구분한다`() =
        runBlocking {
            // 둘 다 "다시 그려라"로 귀결되지만 사용자에게 할 말이 달라 타입을 나눈다.
            assertFailsWith<ChallengeNotEditableException> {
                repository(error("CHALLENGE_NOT_EDITABLE")).update("ch1", update())
            }
            Unit
        }

    @Test
    fun `검수 잠금은 언제 풀리는지 함께 올린다`() =
        runBlocking {
            val failure =
                assertFailsWith<ModerationLockedException> {
                    repository(error("MODERATION_LOCKED", retryAfterSeconds = 120)).update("ch1", update())
                }

            assertEquals(120, failure.retryAfterSeconds)
        }

    @Test
    fun `주간 횟수 거절은 전용 예외로 올린다`() =
        runBlocking {
            assertFailsWith<InvalidWeeklyCountException> {
                repository(error("INVALID_WEEKLY_COUNT")).update("ch1", update())
            }
            Unit
        }

    @Test
    fun `수정에서 모르는 오류는 그대로 올려 보낸다`() =
        runBlocking {
            assertFailsWith<ApiException> { repository(error("SOMETHING_ELSE")).update("ch1", update()) }
            Unit
        }

    private object NoImageReader : ImageReader {
        override suspend fun read(uri: String) = throw NotImplementedError()
    }

    private fun update() =
        com.ruleup.challenge.domain.entity
            .ChallengeUpdate(version = 1)

    private fun error(
        code: String,
        reason: String? = null,
        retryAfterSeconds: Int? = null,
        rejoinAvailableAt: String? = null,
    ) = ErrorBody(
        code = code,
        message = "서버가 거절했어요",
        retryAfterSeconds = retryAfterSeconds,
        reason = reason,
        rejoinAvailableAt = rejoinAvailableAt,
    )

    private fun repository(error: ErrorBody) =
        ChallengeRepositoryImpl(
            api = FailingChallengeApi(error),
            imageReader = NoImageReader,
        )
}
