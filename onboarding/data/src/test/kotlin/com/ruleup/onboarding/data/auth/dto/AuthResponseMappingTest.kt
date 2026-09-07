package com.ruleup.onboarding.data.auth.dto

import com.ruleup.domain.entity.user.Tier
import com.ruleup.network.dto.ApiException
import com.ruleup.onboarding.domain.auth.entity.OAuthResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * 소셜 로그인 응답 매핑. **신규와 기존을 잘못 가르면** 기존 사용자가 가입을 다시 하거나 신규
 * 사용자가 빈 홈에 떨어진다 — 로그인 직후 갈림길이라 되돌릴 방법이 없다.
 *
 * 사용자 정보는 `id`·`nickname` 만 필수다. 나머지를 안전한 기본으로 떨어뜨려야 서버가 enum 을
 * 넓힐 때 구버전 앱이 **로그인부터** 막히지 않는다.
 */
class AuthResponseMappingTest {
    @Test
    fun `신규 여부를 안 주면 가입 토큰 유무로 판단한다`() {
        // 서버 배포본에 따라 isNewUser 가 빠질 수 있다 — 그때도 갈림길은 정확해야 한다.
        val result = response(isNewUser = null, signupToken = "signup-token").toOAuthResult()

        assertTrue(result is OAuthResult.NewUser)
    }

    @Test
    fun `가입 토큰이 없으면 기존 사용자로 본다`() {
        val result = response(isNewUser = null, signupToken = null, user = user()).toOAuthResult()

        assertTrue(result is OAuthResult.ExistingUser)
    }

    @Test
    fun `신규인데 가입 토큰이 없으면 조용히 넘기지 않고 실패로 알린다`() {
        // 토큰 없이 가입 화면에 보내면 마지막 제출에서 튕기고, 그때는 되돌아올 수 없다.
        assertFailsWith<ApiException> {
            response(isNewUser = true, signupToken = null).toOAuthResult()
        }
    }

    @Test
    fun `표시 티어가 없으면 실제 티어로 떨어뜨린다`() {
        // 방 입장 판정에 쓰이므로 부풀리면 못 들어갈 방에 들어가려다 튕긴다.
        val result = response(user = user(tier = "SILVER", displayTier = null)).toOAuthResult()

        assertEquals(Tier.SILVER, (result as OAuthResult.ExistingUser).session.user.displayTier)
    }

    @Test
    fun `모르는 티어는 최하위로 떨어뜨린다`() {
        // 서버 enum 확장이 방 입장 판정을 부풀리면 안 된다.
        val result = response(user = user(tier = "PLATINUM", displayTier = null)).toOAuthResult()

        assertEquals(Tier.BRONZE, (result as OAuthResult.ExistingUser).session.user.displayTier)
    }

    @Test
    fun `사용자 식별자가 없으면 로그인을 성공으로 다루지 않는다`() {
        assertFailsWith<ApiException> {
            response(user = user(id = null)).toOAuthResult()
        }
    }

    @Test
    fun `온보딩 완료 여부를 안 주면 마친 것으로 본다`() {
        // 안 마친 것으로 접으면 기존 사용자가 로그인마다 온보딩으로 되돌아간다.
        val result = response(user = user(onboardingCompleted = null)).toOAuthResult()

        assertTrue((result as OAuthResult.ExistingUser).session.user.onboardingCompleted)
    }

    private fun response(
        isNewUser: Boolean? = false,
        signupToken: String? = null,
        user: UserResponse? = user(),
    ) = SocialLoginAuthResponse(
        isNewUser = isNewUser,
        restored = false,
        accessToken = "at",
        refreshToken = "rt",
        tokenType = "Bearer",
        expiresIn = 3600,
        user = user,
        signupToken = signupToken,
        signupTokenExpiresIn = signupToken?.let { 300 },
        oauthProfile = OAuthProfileResponse(email = null, nicknameHint = "도전왕", profileImageUrlHint = null),
    )

    private fun user(
        id: String? = "u1",
        tier: String? = "BRONZE",
        displayTier: String? = null,
        onboardingCompleted: Boolean? = true,
    ) = UserResponse(
        id = id,
        nickname = "도전왕",
        tier = tier,
        displayTier = displayTier,
        onboardingCompleted = onboardingCompleted,
    )
}
