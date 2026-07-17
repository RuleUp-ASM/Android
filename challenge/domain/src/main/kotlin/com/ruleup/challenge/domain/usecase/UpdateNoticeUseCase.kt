package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.entity.NoticeUpdateResult
import com.ruleup.challenge.domain.repository.RoomRepository
import javax.inject.Inject

/**
 * 공지 수정 (명세: PUT /challenges/{id}/notices/{noticeId} — 방장 전용).
 * 기본은 읽음 유지. 규칙 변경 등 재확인이 필요한 수정만 [resetRead]=true 로 전 멤버 미읽음 복귀 + 재발송.
 */
class UpdateNoticeUseCase
    @Inject
    constructor(
        private val roomRepository: RoomRepository,
    ) {
        suspend operator fun invoke(
            challengeId: String,
            noticeId: String,
            title: String,
            content: String,
            resetRead: Boolean,
        ): NoticeUpdateResult = roomRepository.updateNotice(challengeId, noticeId, title, content, resetRead)
    }
