package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.entity.WatcherInvitationInfo
import com.ruleup.challenge.domain.repository.WatcherRepository
import javax.inject.Inject

/**
 * 초대 링크 진입 시 토큰 검증 + 초대 정보 조회(명세: GET /watchers/invitations/{token}).
 * 만료/이미 수락/30일 차단은 state 로 내려와 수락 화면이 안내 문구로 분기한다.
 */
class GetWatcherInvitationUseCase
    @Inject
    constructor(
        private val watcherRepository: WatcherRepository,
    ) {
        suspend operator fun invoke(token: String): WatcherInvitationInfo = watcherRepository.getInvitation(token)
    }
