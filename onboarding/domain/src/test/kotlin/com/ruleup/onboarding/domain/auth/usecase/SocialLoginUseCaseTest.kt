package com.ruleup.onboarding.domain.auth.usecase

import com.ruleup.domain.entity.user.AccountStatus
import com.ruleup.domain.entity.user.LockInfo
import com.ruleup.domain.entity.user.NicknameStatus
import com.ruleup.domain.entity.user.Token
import com.ruleup.onboarding.domain.entity.AuthSession
import com.ruleup.onboarding.domain.entity.LoginOutcome
import com.ruleup.onboarding.domain.entity.OAuthAuthorization
import com.ruleup.onboarding.domain.entity.OAuthProfile
import com.ruleup.onboarding.domain.entity.OAuthProvider
import com.ruleup.onboarding.domain.entity.OAuthResult
import com.ruleup.onboarding.domain.fake.FakeAuthRepository
import com.ruleup.onboarding.domain.fake.FakeDeviceIdentityRepository
import com.ruleup.onboarding.domain.fake.FakeTokenRepository
import com.ruleup.onboarding.domain.fake.testUser
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SocialLoginUseCaseTest {
    private val authorization =
        OAuthAuthorization(
            provider = OAuthProvider.KAKAO,
            code = "code",
            codeVerifier = "verifier",
            redirectUri = "app://oauth",
        )
    private val token = Token("a", "r", "Bearer", 3600)

    @Test
    fun `기존 회원이면 토큰을 저장하고 홈으로 보낸다`() =
        runBlocking {
            val auth = existingUser(testUser())
            val tokens = FakeTokenRepository()

            val result = useCase(auth, tokens)(authorization)

            assertEquals(LoginOutcome.GoHome, result)
            assertEquals(token, tokens.savedToken)
        }

    @Test
    fun `로그인 요청에 기기 식별자를 함께 보낸다`() =
        runBlocking {
            val auth = existingUser(testUser())

            useCase(auth, FakeTokenRepository())(authorization)

            assertEquals("device-1", auth.exchangedWithDevice?.deviceId)
            assertEquals("install-1", auth.exchangedWithDevice?.installationId)
        }

    @Test
    fun `닉네임이 선점된 복원 계정은 세션을 저장하되 닉네임 재설정으로 보낸다`() =
        runBlocking {
            val auth = existingUser(testUser(nickname = "도전왕", nicknameStatus = NicknameStatus.CONFLICT))
            val tokens = FakeTokenRepository()

            val result = useCase(auth, tokens)(authorization)

            // 세션은 저장돼야 한다 — 닉네임 재설정 화면이 인증된 API 를 호출한다.
            assertEquals(token, tokens.savedToken)
            assertEquals(LoginOutcome.ResetNickname("도전왕"), result)
        }

    @Test
    fun `잠금 계정은 열람 전용 홈으로 보낸다`() =
        runBlocking {
            val lock = LockInfo(reason = "신고 누적", unlockAt = "2026-09-01T00:00:00+09:00")
            val auth = existingUser(testUser(accountStatus = AccountStatus.LOCKED, lockInfo = lock))
            val tokens = FakeTokenRepository()

            val result = useCase(auth, tokens)(authorization)

            assertEquals(LoginOutcome.GoHomeReadOnly(lock), result)
            assertEquals(token, tokens.savedToken)
        }

    @Test
    fun `닉네임 충돌은 잠금보다 먼저 판정한다`() =
        runBlocking {
            // 복원 계정은 제재 이력도 함께 복원되므로 둘이 동시에 올 수 있다.
            // 닉네임을 못 바꾸면 방 참여자 목록에 남의 이름이 뜨므로 이쪽이 먼저다.
            val auth =
                existingUser(
                    testUser(
                        nickname = "도전왕",
                        nicknameStatus = NicknameStatus.CONFLICT,
                        accountStatus = AccountStatus.LOCKED,
                    ),
                )

            val result = useCase(auth, FakeTokenRepository())(authorization)

            assertEquals(LoginOutcome.ResetNickname("도전왕"), result)
        }

    @Test
    fun `신규 회원이면 토큰을 저장하지 않고 가입으로 보낸다`() =
        runBlocking {
            val profile = OAuthProfile(email = null, nicknameHint = "도전왕", profileImageUrlHint = null)
            val auth =
                FakeAuthRepository().apply {
                    exchangeResult =
                        OAuthResult.NewUser(
                            signupToken = "signup-token",
                            expiresInSeconds = 300,
                            profile = profile,
                        )
                }
            val tokens = FakeTokenRepository()

            val result = useCase(auth, tokens)(authorization)

            assertEquals(LoginOutcome.GoSignup("signup-token", 300, profile), result)
            assertNull(tokens.savedToken)
        }

    private fun existingUser(user: com.ruleup.domain.entity.user.User) =
        FakeAuthRepository().apply {
            exchangeResult = OAuthResult.ExistingUser(AuthSession(token, user), restored = false)
        }

    private fun useCase(
        auth: FakeAuthRepository,
        tokens: FakeTokenRepository,
    ) = SocialLoginUseCase(auth, FakeDeviceIdentityRepository(), tokens)
}
