package com.ruleup.onboarding.data.auth.repository

import com.ruleup.domain.token.RefreshedSession
import com.ruleup.domain.token.TokenRefresher
import com.ruleup.network.dto.ApiException
import com.ruleup.network.dto.getOrThrow
import com.ruleup.onboarding.data.auth.api.AuthApi
import com.ruleup.onboarding.data.auth.dto.TokenRefreshRequest
import com.ruleup.onboarding.data.auth.dto.toRefreshedSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        private val lock = Mutex()

        // 마지막으로 보낸 갱신. 콜드스타트의 자동로그인과 401 Authenticator 가 같은 토큰으로 동시에 오면
        // 서버가 첫 요청에서 토큰을 회전해 두 번째를 401 로 막고, 그쪽이 세션을 지운다.
        private var latest: Pair<String, Deferred<RefreshedSession?>>? = null

        override suspend fun refresh(refreshToken: String): RefreshedSession? {
            val shared =
                lock.withLock {
                    latest?.takeIf { it.first == refreshToken }?.second
                        ?: scope.async { request(refreshToken) }.also { latest = refreshToken to it }
                }
            return try {
                shared.await()
            } catch (e: Exception) {
                // 일시적 실패를 붙잡아 두면 같은 토큰의 재시도가 영영 같은 실패를 받는다.
                lock.withLock { if (latest?.second === shared) latest = null }
                throw e
            }
        }

        private suspend fun request(refreshToken: String): RefreshedSession? =
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
