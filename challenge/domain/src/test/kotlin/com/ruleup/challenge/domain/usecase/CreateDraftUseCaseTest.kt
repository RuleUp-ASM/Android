package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.entity.DraftResult
import com.ruleup.challenge.domain.fake.FakeChallengeRepository
import com.ruleup.challenge.domain.fake.draft
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class CreateDraftUseCaseTest {
    @Test
    fun `초안을 받으면 draftId 와 함께 Ok 를 돌려준다`() =
        runBlocking {
            val expected = DraftResult.Ok(draftId = "d1", draft = draft())
            val repo = FakeChallengeRepository(draftResult = expected)

            val result = CreateDraftUseCase(repo)("아침 6시에 일어나고 싶어요")

            assertEquals(expected, assertIs<DraftResult.Ok>(result))
        }

    @Test
    fun `폴백은 예외가 아니라 Fallback 결과로 돌아온다`() =
        runBlocking {
            // 서버가 200 으로 내려주는 정상 분기다. 예외로 바꾸면 화면이 실패 UI 로 빠진다.
            val repo = FakeChallengeRepository(draftResult = DraftResult.Fallback("더 구체적으로 적어주세요"))

            val result = CreateDraftUseCase(repo)("음")

            assertEquals("더 구체적으로 적어주세요", assertIs<DraftResult.Fallback>(result).message)
        }

    @Test
    fun `빈 설명은 서버까지 보내지 않는다`() {
        val repo = FakeChallengeRepository(draftResult = DraftResult.Fallback("unused"))

        assertFailsWith<IllegalArgumentException> {
            runBlocking { CreateDraftUseCase(repo)("   ") }
        }
    }

    @Test
    fun `200자를 넘는 설명은 서버까지 보내지 않는다`() {
        val repo = FakeChallengeRepository(draftResult = DraftResult.Fallback("unused"))

        assertFailsWith<IllegalArgumentException> {
            runBlocking { CreateDraftUseCase(repo)("가".repeat(CreateDraftUseCase.MAX_DESCRIPTION_LENGTH + 1)) }
        }
    }
}
