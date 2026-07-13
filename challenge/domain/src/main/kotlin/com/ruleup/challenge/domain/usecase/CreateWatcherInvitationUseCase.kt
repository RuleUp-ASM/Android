package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.entity.WatcherInvitation
import com.ruleup.challenge.domain.repository.WatcherRepository
import javax.inject.Inject

/**
 * 감시자 초대 생성(명세: POST /challenges/{id}/watchers/invitations).
 * 반환된 초대 링크는 생성자 본인이 카카오톡 공유로 전달한다(룰업 직접 발송 금지).
 * 무료 한도 초과면 WatcherLimitExceededException — 화면이 구독 안내로 분기한다.
 */
class CreateWatcherInvitationUseCase
    @Inject
    constructor(
        private val watcherRepository: WatcherRepository,
    ) {
        suspend operator fun invoke(challengeId: String): WatcherInvitation = watcherRepository.createInvitation(challengeId)
    }
