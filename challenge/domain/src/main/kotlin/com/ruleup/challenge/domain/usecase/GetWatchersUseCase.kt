package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.entity.ChallengeWatchers
import com.ruleup.challenge.domain.repository.WatcherRepository
import javax.inject.Inject

/**
 * 감시자 목록 조회(명세: GET /challenges/{id}/watchers). 챌린지 상세의 감시자 섹션(생성자만)에 노출한다.
 * 한도([ChallengeWatchers.limit], 구독 시 null=무제한)와 수락 대기(INVITED) 포함 전체를 받는다.
 */
class GetWatchersUseCase
    @Inject
    constructor(
        private val watcherRepository: WatcherRepository,
    ) {
        suspend operator fun invoke(challengeId: String): ChallengeWatchers = watcherRepository.getWatchers(challengeId)
    }
