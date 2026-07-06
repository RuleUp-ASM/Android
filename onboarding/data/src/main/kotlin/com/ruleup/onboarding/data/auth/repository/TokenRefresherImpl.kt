package com.ruleup.onboarding.data.auth.repository

import com.ruleup.domain.token.TokenRefresher
import com.ruleup.entity.user.Token
import com.ruleup.network.dto.ApiException
import com.ruleup.network.dto.getOrThrow
import com.ruleup.onboarding.data.auth.api.AuthApi
import com.ruleup.onboarding.data.auth.dto.TokenRefreshRequest
import com.ruleup.onboarding.data.auth.dto.toToken
import retrofit2.HttpException
import javax.inject.Inject

/**
 * [TokenRefresher] 구현. 기존 `POST /auth/refresh`([AuthApi.refreshToken]) 를 재사용한다.
 *
 * 세션 만료(refreshToken 거절)만 `null` 로 좁혀 반환하고, 그 외 오류는 그대로 전파한다.
 * - HTTP 401 또는 `SESSION_EXPIRED` → 세션 만료 → `null`(호출자가 토큰 정리)
 * - 그 밖(네트워크·5xx·응답 파손 등) → 예외 전파 → 세션 유지, 재시도만 포기
 */
class TokenRefresherImpl
    @Inject
    constructor(
        private val api: AuthApi,
    ) : TokenRefresher {
        override suspend fun refresh(refreshToken: String): Token? =
            try {
                api
                    .refreshToken(TokenRefreshRequest(refreshToken = refreshToken))
                    .getOrThrow()
                    .toToken()
            } catch (e: HttpException) {
                if (e.code() == HTTP_UNAUTHORIZED) null else throw e
            } catch (e: ApiException) {
                if (e.code == CODE_SESSION_EXPIRED) null else throw e
            }

        private companion object {
            const val HTTP_UNAUTHORIZED = 401
            const val CODE_SESSION_EXPIRED = "SESSION_EXPIRED"
        }
    }
