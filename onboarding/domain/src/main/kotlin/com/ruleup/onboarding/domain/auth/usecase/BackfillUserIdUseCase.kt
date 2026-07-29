package com.ruleup.onboarding.domain.auth.usecase

import com.ruleup.domain.profile.ProfileRepository
import com.ruleup.domain.token.TokenRepository
import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.api.w
import javax.inject.Inject

private const val TAG = "SessionBootstrap"

/**
 * userId 가 비어 있는 세션을 프로필 조회로 메운다.
 *
 * 토큰 갱신 응답(`TokenRefreshResponse`)에는 userId 가 없다. 그래서 userId 저장 이전에 만들어진
 * 세션은 자동 로그인으로 복구돼도 계속 비어 있고, 사용자 귀속이 필요한 쪽(관측 식별자 등)이
 * 빈 값을 본다. **앱 업데이트로도 해소되지 않는다** — DataStore 파일이 그대로 남기 때문이다.
 *
 * 계약:
 * - **이미 있으면 조회하지 않는다.** 매 실행의 비용이 아니라 한 번의 복구여야 한다.
 * - **던지지 않는다.** 호출부(`SessionBootstrap`)는 이미 인증 판정을 방출한 뒤라, 여기서 던지면
 *   부팅 흐름과 무관한 실패가 스코프로 새어 나간다. 실패는 기록만 하고 다음 실행이 재시도한다.
 */
class BackfillUserIdUseCase
    @Inject
    constructor(
        private val tokenRepository: TokenRepository,
        private val profileRepository: ProfileRepository,
        private val observability: Observability,
    ) {
        suspend operator fun invoke() {
            if (tokenRepository.getUserId() != null) return
            runCatching { profileRepository.getProfile() }
                .onSuccess { tokenRepository.saveUserId(it.id) }
                .onFailure { observability.w(TAG, it) { "userId 백필 실패 — 다음 실행에서 재시도" } }
        }
    }
