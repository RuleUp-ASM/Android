package com.ruleup.onboarding.domain.auth.usecase

import com.ruleup.domain.entity.user.User
import com.ruleup.domain.token.TokenRepository
import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.api.w
import com.ruleup.onboarding.domain.auth.entity.SignupForm
import com.ruleup.onboarding.domain.auth.repository.AuthRepository
import com.ruleup.onboarding.domain.auth.repository.DeviceIdentityRepository
import com.ruleup.profile.domain.repository.ProfileRepository
import javax.inject.Inject

private const val TAG = "Signup"

/**
 * 가입 완료. **가입 → 토큰 저장 → 사진 업로드 순서가 계약이다** — 사진 등록이 accessToken 을 요구하는
 * 별도 API 라 가입보다 뒤여야 한다.
 *
 * **업로드 실패는 삼킨다.** 여기서 던지면 계정은 만들어졌는데 화면만 실패로 보여, 사용자가 처음부터
 * 다시 하려다 `INVALID_SIGNUP_TOKEN` 을 만난다.
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
