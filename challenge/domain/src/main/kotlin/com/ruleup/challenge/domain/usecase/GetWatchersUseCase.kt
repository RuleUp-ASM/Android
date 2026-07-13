package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.entity.ChallengeWatchers
import com.ruleup.challenge.domain.repository.WatcherRepository
import javax.inject.Inject

/**
 * 내 감시자 목록 조회(명세: GET /challenges/{id}/watchers). 챌린지 상세의 감시자 섹션(참여자 본인)에 노출한다.
 * 미참여면 서버가 거부하므로 호출 성공 여부가 곧 섹션 노출 조건이 된다.
 * 한도([ChallengeWatchers.limit], 구독 시 null=무제한)와 수락 대기(INVITED) 포함 전체를 받는다.
 */
class GetWatchersUseCase
    @Inject
    constructor(
        private val watcherRepository: WatcherRepository,
    ) {
        suspend operator fun invoke(challengeId: String): ChallengeWatchers = watcherRepository.getWatchers(challengeId)
    }
