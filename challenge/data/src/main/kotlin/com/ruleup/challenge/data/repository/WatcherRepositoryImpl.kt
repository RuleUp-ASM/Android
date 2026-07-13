package com.ruleup.challenge.data.repository

import com.ruleup.challenge.data.api.ChallengeApi
import com.ruleup.challenge.data.dto.toDomain
import com.ruleup.challenge.domain.entity.Watcher
import com.ruleup.challenge.domain.entity.WatcherInvitation
import com.ruleup.challenge.domain.entity.WatcherInvitationInfo
import com.ruleup.challenge.domain.entity.WatcherLimitExceededException
import com.ruleup.challenge.domain.repository.WatcherRepository
import com.ruleup.network.dto.ApiException
import com.ruleup.network.dto.getOrThrow
import com.ruleup.network.dto.throwOnError
import javax.inject.Inject

class WatcherRepositoryImpl
    @Inject
    constructor(
        private val api: ChallengeApi,
    ) : WatcherRepository {
        override suspend fun createInvitation(challengeId: String): WatcherInvitation =
            try {
                api
                    .createWatcherInvitation(challengeId)
                    .getOrThrow()
                    .toDomain()
            } catch (e: ApiException) {
                // 무료 한도(챌린지당 3명) 초과는 화면이 구독 안내로 분기할 수 있게 도메인 예외로 변환한다.
                if (e.code == "WATCHER_LIMIT_EXCEEDED") {
                    throw WatcherLimitExceededException()
                }
                throw e
            }

        override suspend fun getWatchers(challengeId: String): List<Watcher> =
            api
                .getWatchers(challengeId)
                .getOrThrow()
                .toDomain()

        override suspend fun removeWatcher(
            challengeId: String,
            watcherId: String,
        ) {
            api.removeWatcher(challengeId, watcherId).throwOnError()
        }

        override suspend fun getInvitation(token: String): WatcherInvitationInfo =
            api
                .getWatcherInvitation(token)
                .getOrThrow()
                .toDomain()

        override suspend fun acceptInvitation(token: String) {
            api.acceptWatcherInvitation(token).throwOnError()
        }
    }
