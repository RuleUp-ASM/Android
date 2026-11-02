package com.ruleup.onboarding.domain.auth.usecase

import com.ruleup.domain.entity.user.AccountStatus
import com.ruleup.domain.entity.user.Token
import com.ruleup.onboarding.domain.auth.entity.AuthSession
import com.ruleup.onboarding.domain.auth.entity.LoginOutcome
import com.ruleup.onboarding.domain.auth.entity.OAuthResult
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
    private val token = Token("access", "refresh", "Bearer", 3600)

    @Test
    fun `심사 로그인에 성공하면 토큰과 심사 세션을 저장한다`() =
        runBlocking {
            val auth =
                FakeAuthRepository().apply {
                    reviewResult =
                        OAuthResult.ExistingUser(
                            AuthSession(token, testUser()),
                            restored = true,
                        )
                }
            val tokens = FakeTokenRepository()

            val result = ReviewLoginUseCase(auth, tokens)("TEST-CODE")

            assertEquals(LoginOutcome.GoHome(restored = true), result)
            assertEquals("TEST-CODE", auth.reviewCode)
            assertEquals(token, tokens.savedToken)
            assertTrue(tokens.isReviewSession.first())
        }

    @Test
    fun `심사 계정도 정지 상태면 일반 로그인처럼 제한 화면으로 보낸다`() =
        runBlocking {
            val user = testUser(accountStatus = AccountStatus.SUSPENDED)
            val auth = FakeAuthRepository().apply { reviewResult = OAuthResult.ExistingUser(AuthSession(token, user), restored = false) }

            val result = ReviewLoginUseCase(auth, FakeTokenRepository())("TEST-CODE")

            assertEquals(LoginOutcome.Restricted(lockInfo = null), result)
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
