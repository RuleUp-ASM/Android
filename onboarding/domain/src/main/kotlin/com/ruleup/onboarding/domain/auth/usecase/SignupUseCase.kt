package com.ruleup.onboarding.domain.auth.usecase

import com.ruleup.domain.device.DeviceIdentityRepository
import com.ruleup.domain.entity.user.User
import com.ruleup.domain.token.TokenRepository
import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.api.w
import com.ruleup.onboarding.domain.auth.entity.SignupForm
import com.ruleup.onboarding.domain.auth.repository.AuthRepository
import com.ruleup.profile.domain.repository.ProfileRepository
import javax.inject.Inject

private const val TAG = "Signup"

/** 가입 완료. */
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
