package com.ruleup.onboarding.domain.auth.usecase

import com.ruleup.domain.entity.user.Token
import com.ruleup.onboarding.domain.auth.entity.AuthSession
import com.ruleup.onboarding.domain.fake.FakeAuthRepository
import com.ruleup.onboarding.domain.fake.FakeTokenRepository
import com.ruleup.onboarding.domain.fake.testUser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReviewLoginUseCaseTest {
    @Test
    fun `심사 로그인에 성공하면 토큰과 심사 세션을 저장한다`() =
        runBlocking {
            val session = AuthSession(Token("access", "refresh", "Bearer", 3600), testUser())
            val auth = FakeAuthRepository().apply { reviewResult = session }
            val tokens = FakeTokenRepository()

            val result = ReviewLoginUseCase(auth, tokens)("TEST-CODE")

            assertEquals(session, result)
            assertEquals("TEST-CODE", auth.reviewCode)
            assertEquals(session.token, tokens.savedToken)
            assertTrue(tokens.isReviewSession.first())
        }

    @Test
    fun `심사 로그인 실패는 세션을 만들지 않는다`() =
        runBlocking {
            val auth = FakeAuthRepository().apply { reviewError = IllegalStateException("failed") }
            val tokens = FakeTokenRepository()

            assertFailsWith<IllegalStateException> { ReviewLoginUseCase(auth, tokens)("wrong") }

            assertEquals(0, tokens.saveCount)
            assertFalse(tokens.isReviewSession.first())
        }
}
