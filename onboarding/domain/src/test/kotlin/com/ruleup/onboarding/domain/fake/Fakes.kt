package com.ruleup.onboarding.domain.fake

import com.ruleup.domain.profile.ProfileRepository
import com.ruleup.domain.token.TokenRepository
import com.ruleup.entity.user.Agreement
import com.ruleup.entity.user.AuthSession
import com.ruleup.entity.user.InterestCategory
import com.ruleup.entity.user.Profile
import com.ruleup.entity.user.Token
import com.ruleup.onboarding.domain.auth.repository.AuthRepository
import com.ruleup.onboarding.domain.entity.AppVersionGate
import com.ruleup.onboarding.domain.entity.OAuthAuthorization
import com.ruleup.onboarding.domain.entity.OAuthResult
import com.ruleup.onboarding.domain.intro.repository.IntroRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** 저장/삭제 호출을 기록하는 토큰 저장소 테스트 더블. */
class FakeTokenRepository(
    private var refreshToken: String? = null,
) : TokenRepository {
    var savedToken: Token? = null
    var savedUserId: String? = null
    var saveCount = 0
    var cleared = false

    override suspend fun saveSession(
        token: Token,
        userId: String,
    ) {
        savedToken = token
        savedUserId = userId
        saveCount++
    }

    override suspend fun saveTokens(token: Token) {
        savedToken = token
        saveCount++
    }

    override suspend fun getAccessToken(): String? = savedToken?.accessToken

    override fun cachedAccessToken(): String? = savedToken?.accessToken

    override suspend fun getRefreshToken(): String? = refreshToken

    override suspend fun saveUserId(userId: String) {
        savedUserId = userId
    }

    override suspend fun getUserId(): String? = savedUserId

    override suspend fun clear() {
        cleared = true
        savedToken = null
        savedUserId = null
        refreshToken = null
    }

    override val isLoggedIn: Flow<Boolean> = flowOf(false)

    override val userId: Flow<String?> get() = flowOf(savedUserId)
}

/** 각 API 의 성공 결과/예외를 주입할 수 있는 인증 저장소 테스트 더블. */
class FakeAuthRepository : AuthRepository {
    var exchangeResult: OAuthResult? = null
    var exchangeError: Throwable? = null

    var refreshResult: Token? = null
    var refreshError: Throwable? = null
    var refreshCalledWith: String? = null

    var signupResult: AuthSession? = null
    var signupError: Throwable? = null

    var logoutError: Throwable? = null
    var loggedOutWith: String? = null

    override suspend fun exchangeToken(authorization: OAuthAuthorization): OAuthResult {
        exchangeError?.let { throw it }
        return exchangeResult!!
    }

    override suspend fun signup(
        signupToken: String,
        nickname: String,
        interestCategories: List<InterestCategory>,
        profileImageUrl: String?,
        agreements: Agreement,
    ): AuthSession {
        signupError?.let { throw it }
        return signupResult!!
    }

    override suspend fun refreshToken(refreshToken: String): Token {
        refreshCalledWith = refreshToken
        refreshError?.let { throw it }
        return refreshResult!!
    }

    override suspend fun logout(refreshToken: String) {
        loggedOutWith = refreshToken
        logoutError?.let { throw it }
    }
}

/** 프로필 업로드·조회를 주입하는 테스트 더블. 나머지는 미사용. */
class FakeProfileRepository : ProfileRepository {
    var uploadResult: String = ""
    var uploadCalledWith: String? = null
    var onboardingInfoCalledWith: Pair<String?, String?>? = null

    /** [getProfile] 이 돌려줄 값. null 이면 [profileError] 를 던진다. */
    var profile: Profile? = null
    var profileError: Throwable = NotImplementedError()
    var getProfileCallCount = 0

    override suspend fun uploadProfileImage(imageUri: String): String {
        uploadCalledWith = imageUri
        return uploadResult
    }

    override suspend fun updateOnboardingInfo(
        birthDate: String?,
        gender: String?,
    ) {
        onboardingInfoCalledWith = birthDate to gender
    }

    override suspend fun checkNickname(nickname: String) = throw NotImplementedError()

    override suspend fun getCategories() = throw NotImplementedError()

    override suspend fun getProfile(): Profile {
        getProfileCallCount++
        return profile ?: throw profileError
    }

    override suspend fun updateProfile(
        nickname: String?,
        interestCategories: List<InterestCategory>?,
        profileImageUrl: String?,
    ) = throw NotImplementedError()

    override suspend fun deleteProfileImage() = throw NotImplementedError()
}

/** 버전 게이트 조회 결과/예외를 주입하는 테스트 더블. */
class FakeIntroRepository : IntroRepository {
    var result: AppVersionGate? = null
    var error: Throwable? = null

    override suspend fun getVersionGate(): AppVersionGate {
        error?.let { throw it }
        return result!!
    }
}
