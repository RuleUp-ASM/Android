package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.entity.Watcher
import com.ruleup.challenge.domain.repository.WatcherRepository
import javax.inject.Inject

/** 감시자 목록 조회(명세: GET /challenges/{id}/watchers). 챌린지 상세의 감시자 섹션(생성자만)에 노출한다. */
class GetWatchersUseCase
    @Inject
    constructor(
        private val watcherRepository: WatcherRepository,
    ) {
        suspend operator fun invoke(challengeId: String): List<Watcher> = watcherRepository.getWatchers(challengeId)
    }
