package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.entity.NoticePinResult
import com.ruleup.challenge.domain.repository.RoomRepository
import javax.inject.Inject

/**
 * 공지 고정/해제 (명세: PATCH /challenges/{id}/notices/{noticeId}/pin — 방장 전용).
 * 단일 pin — 새로 고정하면 기존 고정이 자동 해제되며 [NoticePinResult.unpinnedNoticeId] 로 알려준다.
 */
class PinNoticeUseCase
    @Inject
    constructor(
        private val roomRepository: RoomRepository,
    ) {
        suspend operator fun invoke(
            challengeId: String,
            noticeId: String,
            pinned: Boolean,
        ): NoticePinResult = roomRepository.pinNotice(challengeId, noticeId, pinned)
    }
