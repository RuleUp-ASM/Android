package com.ruleup.domain.auth.usecase

import com.ruleup.domain.auth.model.SignupForm
import com.ruleup.domain.profile.ProfileRepository
import com.ruleup.domain.token.TokenRepository
import com.ruleup.entity.user.User
import javax.inject.Inject

/**
 * 신규 가입 완료 유스케이스.
 *
 * 1) 로컬 프로필 이미지가 있으면 업로드(4.10)해 URL 을 확보한다.
 * 2) signup(4.3) 으로 가입을 완료하고 앱 토큰을 수령한다.
 * 3) 발급된 토큰을 영속화하고 가입된 [User] 를 반환한다.
 *
 * 실패는 예외([com.ruleup.network.dto.ApiException] 등)로 전파된다.
 */
class SignupUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val profileRepository: ProfileRepository,
        private val tokenRepository: TokenRepository,
    ) {
        suspend operator fun invoke(form: SignupForm): User {
            val profileImageUrl =
                form.localImageUri
                    ?.takeIf { it.isNotBlank() }
                    ?.let { profileRepository.uploadProfileImage(it) }

            val session =
                authRepository.signup(
                    signupToken = form.signupToken,
                    nickname = form.nickname,
                    interestCategories = form.interestCategories,
                    profileImageUrl = profileImageUrl,
                    agreements = form.agreements,
                )

            tokenRepository.saveTokens(session.token)
            return session.user
        }
    }
