package com.ruleup.onboarding.domain.auth.usecase

import com.ruleup.domain.entity.user.User
import com.ruleup.domain.token.TokenRepository
import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.api.w
import com.ruleup.onboarding.domain.auth.model.SignupForm
import com.ruleup.onboarding.domain.auth.repository.AuthRepository
import com.ruleup.onboarding.domain.auth.repository.DeviceIdentityRepository
import com.ruleup.profile.domain.repository.ProfileRepository
import javax.inject.Inject

private const val TAG = "Signup"

/**
 * 가입 완료.
 *
 * 1. `POST /auth/signup` 으로 가입하고 앱 토큰을 받는다.
 * 2. 토큰을 저장한다 — 이후 요청에 Authorization 헤더가 자동으로 붙는다.
 * 3. 고른 사진이 있으면 **인증된 상태로** 업로드한다.
 *
 * 3번이 2번 뒤인 게 계약의 핵심이다. 예전엔 가입 **전에** 올려 URL 을 가입 요청에 실었는데, 지금은
 * 사진 등록이 accessToken 을 요구하는 별도 API 라 순서가 뒤집혔다.
 *
 * **업로드 실패는 삼킨다.** 사진은 선택 항목이고 가입은 이미 끝난 상태다 — 여기서 던지면 계정이
 * 만들어졌는데도 화면은 실패로 보여, 사용자가 처음부터 다시 하려다 `INVALID_SIGNUP_TOKEN` 을 만난다.
 */
class SignupUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val deviceIdentityRepository: DeviceIdentityRepository,
        private val profileRepository: ProfileRepository,
        private val tokenRepository: TokenRepository,
        private val observability: Observability,
    ) {
        suspend operator fun invoke(form: SignupForm): User {
            val device = deviceIdentityRepository.current()
            val session = authRepository.signup(form, device)

            tokenRepository.saveSession(session.token, session.user.id)

            val uploadedUrl =
                form.localImageUri
                    ?.takeIf { it.isNotBlank() }
                    ?.let { uri ->
                        runCatching { profileRepository.uploadProfileImage(uri) }
                            .onFailure { observability.w(TAG, it) { "프로필 사진 등록 실패 — 기본 이미지로 진행" } }
                            .getOrNull()
                    }

            return uploadedUrl?.let { session.user.copy(profileImageUrl = it) } ?: session.user
        }
    }
