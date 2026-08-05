package com.ruleup.onboarding.domain.fake

import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.entity.user.Token
import com.ruleup.domain.token.RefreshedSession
import com.ruleup.domain.token.TokenRepository
import com.ruleup.onboarding.domain.auth.model.SignupForm
import com.ruleup.onboarding.domain.auth.repository.AuthRepository
import com.ruleup.onboarding.domain.auth.repository.DeviceIdentityRepository
import com.ruleup.onboarding.domain.entity.AuthSession
import com.ruleup.onboarding.domain.entity.DeviceIdentity
import com.ruleup.onboarding.domain.entity.IntroInfo
import com.ruleup.onboarding.domain.entity.OAuthAuthorization
import com.ruleup.onboarding.domain.entity.OAuthResult
import com.ruleup.onboarding.domain.entity.PermissionSnapshot
import com.ruleup.onboarding.domain.intro.repository.IntroRepository
import com.ruleup.profile.domain.entity.MyProfile
import com.ruleup.profile.domain.entity.Profile
import com.ruleup.profile.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

/** 저장/삭제 호출을 기록하는 토큰 저장소 테스트 더블. */
class FakeTokenRepository(
    private var refreshToken: String? = null,
) : TokenRepository {
    // 실제 구현처럼 refreshToken 유무를 반영해 흘려보낸다. 정적 값으로 두면 세션 종료 전이를
    // 관찰하는 코드를 테스트할 수 없다.
    private val loggedIn = MutableStateFlow(refreshToken != null)

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
        refreshToken = token.refreshToken
        loggedIn.value = true
        saveCount++
    }

    override suspend fun saveTokens(
        token: Token,
        userId: String?,
    ) {
        savedToken = token
        userId?.let { savedUserId = it }
        refreshToken = token.refreshToken
        loggedIn.value = true
        saveCount++
    }

    override suspend fun getAccessToken(): String? = savedToken?.accessToken

    override fun cachedAccessToken(): String? = savedToken?.accessToken

    override suspend fun getRefreshToken(): String? = refreshToken

    override suspend fun getUserId(): String? = savedUserId

    override suspend fun clear() {
        cleared = true
        savedToken = null
        savedUserId = null
        refreshToken = null
        loggedIn.value = false
    }

    override val isLoggedIn: Flow<Boolean> = loggedIn

    override val userId: Flow<String?> get() = flowOf(savedUserId)
}

/** 각 API 의 성공 결과/예외를 주입할 수 있는 인증 저장소 테스트 더블. */
class FakeAuthRepository : AuthRepository {
    var exchangeResult: OAuthResult? = null
    var exchangeError: Throwable? = null
    var exchangedWithDevice: DeviceIdentity? = null

    var refreshResult: RefreshedSession? = null
    var refreshError: Throwable? = null
    var refreshCalledWith: String? = null

    var signupResult: AuthSession? = null
    var signupError: Throwable? = null
    var signedUpForm: SignupForm? = null
    var signedUpWithDevice: DeviceIdentity? = null

    var logoutError: Throwable? = null
    var loggedOutWith: String? = null

    override suspend fun exchangeToken(
        authorization: OAuthAuthorization,
        device: DeviceIdentity,
        permissions: PermissionSnapshot?,
    ): OAuthResult {
        exchangedWithDevice = device
        exchangeError?.let { throw it }
        return exchangeResult!!
    }

    override suspend fun signup(
        form: SignupForm,
        device: DeviceIdentity,
    ): AuthSession {
        signedUpForm = form
        signedUpWithDevice = device
        signupError?.let { throw it }
        return signupResult!!
    }

    override suspend fun refreshToken(refreshToken: String): RefreshedSession {
        refreshCalledWith = refreshToken
        refreshError?.let { throw it }
        return refreshResult!!
    }

    override suspend fun logout(refreshToken: String) {
        loggedOutWith = refreshToken
        logoutError?.let { throw it }
    }
}

/** 고정 식별자를 돌려주는 기기 식별자 테스트 더블. */
class FakeDeviceIdentityRepository(
    private val identity: DeviceIdentity = DeviceIdentity(deviceId = "device-1", installationId = "install-1"),
) : DeviceIdentityRepository {
    override suspend fun current(): DeviceIdentity = identity
}

/** 프로필 업로드·조회를 주입하는 테스트 더블. 나머지는 미사용. */
class FakeProfileRepository : ProfileRepository {
    var uploadResult: String = ""
    var uploadCalledWith: String? = null

    /** 값이 있으면 업로드가 이 예외를 던진다. 실패를 흡수하는지 확인할 때 쓴다. */
    var uploadError: Throwable? = null

    /** [getMyProfile] 이 돌려줄 값. null 이면 [profileError] 를 던진다. */
    var myProfile: MyProfile? = null
    var profileError: Throwable = NotImplementedError()
    var getProfileCallCount = 0

    override suspend fun uploadProfileImage(imageUri: String): String {
        uploadCalledWith = imageUri
        uploadError?.let { throw it }
        return uploadResult
    }

    override suspend fun checkNickname(nickname: String) = throw NotImplementedError()

    override suspend fun getCategories() = throw NotImplementedError()

    override suspend fun getMyProfile(): MyProfile {
        getProfileCallCount++
        return myProfile ?: throw profileError
    }

    override suspend fun getProfile(): Profile = throw NotImplementedError()

    override suspend fun updateProfile(
        nickname: String?,
        interestCategories: List<Category>?,
        profileImageUrl: String?,
    ) = throw NotImplementedError()

    override suspend fun deleteProfileImage() = throw NotImplementedError()
}

/** 인트로 조회 결과/예외를 주입하는 테스트 더블. */
class FakeIntroRepository : IntroRepository {
    var result: IntroInfo? = null
    var error: Throwable? = null

    override suspend fun getIntro(): IntroInfo {
        error?.let { throw it }
        return result!!
    }
}
