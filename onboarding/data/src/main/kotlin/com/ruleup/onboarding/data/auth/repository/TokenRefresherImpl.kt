package com.ruleup.onboarding.data.auth.repository

import com.ruleup.domain.token.RefreshedSession
import com.ruleup.domain.token.TokenRefresher
import com.ruleup.network.dto.ApiException
import com.ruleup.network.dto.getOrThrow
import com.ruleup.onboarding.data.auth.api.AuthApi
import com.ruleup.onboarding.data.auth.dto.TokenRefreshRequest
import com.ruleup.onboarding.data.auth.dto.toRefreshedSession
import retrofit2.HttpException
import javax.inject.Inject

/**
 * [TokenRefresher] 구현. HTTP 401·`SESSION_EXPIRED` 만 세션 만료로 보고 `null`(호출자가 토큰 정리),
 * 그 밖(네트워크·5xx·응답 파손)은 전파해 세션을 유지한 채 재시도만 포기한다.
 */
class TokenRefresherImpl
    @Inject
    constructor(
        private val api: AuthApi,
    ) : TokenRefresher {
        override suspend fun refresh(refreshToken: String): RefreshedSession? =
            try {
                api
                    .refreshToken(TokenRefreshRequest(refreshToken = refreshToken))
                    .getOrThrow()
                    .toRefreshedSession()
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
