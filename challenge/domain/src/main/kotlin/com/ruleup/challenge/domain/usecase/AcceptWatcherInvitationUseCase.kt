package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.repository.WatcherRepository
import javax.inject.Inject

/**
 * 감시자 수락(명세: POST /watchers/invitations/{token}/accept).
 * 룰업 유저의 인앱 수락이 곧 수신동의 — 채널은 인앱 푸시로 등록되고 감시자가 ACTIVE 가 된다.
 */
class AcceptWatcherInvitationUseCase
    @Inject
    constructor(
        private val watcherRepository: WatcherRepository,
    ) {
        suspend operator fun invoke(token: String) = watcherRepository.acceptInvitation(token)
    }
