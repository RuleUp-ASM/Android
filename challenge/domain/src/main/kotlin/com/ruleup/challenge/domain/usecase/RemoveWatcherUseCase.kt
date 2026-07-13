package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.repository.WatcherRepository
import javax.inject.Inject

/** 감시자 해제(명세: DELETE /challenges/{id}/watchers/{watcherId}). 서버가 REVOKED 처리 + 연락처 파기. */
class RemoveWatcherUseCase
    @Inject
    constructor(
        private val watcherRepository: WatcherRepository,
    ) {
        suspend operator fun invoke(
            challengeId: String,
            watcherId: String,
        ) = watcherRepository.removeWatcher(challengeId, watcherId)
    }
