package com.ruleup.onboarding.domain.fake

import com.ruleup.analytics.domain.AnalyticsEvent
import com.ruleup.analytics.domain.AnalyticsLogger
import com.ruleup.domain.token.TokenRepository
import com.ruleup.entity.user.Agreement
import com.ruleup.entity.user.AuthSession
import com.ruleup.entity.user.InterestCategory
import com.ruleup.entity.user.Token
import com.ruleup.onboarding.domain.auth.repository.AuthRepository
import com.ruleup.onboarding.domain.entity.AppVersionGate
import com.ruleup.onboarding.domain.entity.OAuthAuthorization
import com.ruleup.onboarding.domain.entity.OAuthResult
import com.ruleup.onboarding.domain.intro.repository.IntroRepository
import com.ruleup.onboarding.domain.profile.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** 저장/삭제 호출을 기록하는 토큰 저장소 테스트 더블. */
class FakeTokenRepository(
    private var refreshToken: String? = null,
) : TokenRepository {
    var savedToken: Token? = null
    var saveCount = 0
    var cleared = false

    override suspend fun saveTokens(token: Token) {
        savedToken = token
        saveCount++
    }

    override suspend fun getAccessToken(): String? = savedToken?.accessToken

    override fun cachedAccessToken(): String? = savedToken?.accessToken

    override suspend fun getRefreshToken(): String? = refreshToken

    override suspend fun clear() {
        cleared = true
        savedToken = null
        refreshToken = null
    }

    override val isLoggedIn: Flow<Boolean> = flowOf(false)
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

/** 프로필 이미지 업로드만 사용하는 테스트 더블. 나머지는 미사용. */
class FakeProfileRepository : ProfileRepository {
    var uploadResult: String = ""
    var uploadCalledWith: String? = null

    override suspend fun uploadProfileImage(imageUri: String): String {
        uploadCalledWith = imageUri
        return uploadResult
    }

    override suspend fun checkNickname(nickname: String) = throw NotImplementedError()

    override suspend fun getCategories() = throw NotImplementedError()

    override suspend fun getProfile() = throw NotImplementedError()

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

/** 로깅된 이벤트를 기록하는 애널리틱스 테스트 더블. */
class FakeAnalyticsLogger : AnalyticsLogger {
    val events = mutableListOf<AnalyticsEvent>()

    override fun log(event: AnalyticsEvent) {
        events += event
    }

    override fun setUserId(id: String?) = Unit

    override fun setUserProperty(
        key: String,
        value: String,
    ) = Unit
}
