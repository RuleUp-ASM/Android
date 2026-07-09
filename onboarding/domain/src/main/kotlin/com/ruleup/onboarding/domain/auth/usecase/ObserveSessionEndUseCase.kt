package com.ruleup.onboarding.domain.auth.usecase

import com.ruleup.domain.token.TokenRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 세션 종료(로그인→로그아웃 전이)를 관찰한다. 토큰 만료(Authenticator 의 정리)나 명시적 로그아웃으로
 * [TokenRepository.isLoggedIn] 이 로그인→로그아웃으로 바뀔 때만 방출한다.
 *
 * 최초 방출(앱 시작 시점의 로그인 여부)은 [drop] 으로 건너뛰어, Splash 의 초기 라우팅과 충돌하지 않게 한다.
 */
class ObserveSessionEndUseCase
    @Inject
    constructor(
        private val tokenRepository: TokenRepository,
    ) {
        operator fun invoke(): Flow<Unit> =
            tokenRepository.isLoggedIn
                .distinctUntilChanged()
                .drop(1)
                .filter { loggedIn -> !loggedIn }
                .map { }
    }
