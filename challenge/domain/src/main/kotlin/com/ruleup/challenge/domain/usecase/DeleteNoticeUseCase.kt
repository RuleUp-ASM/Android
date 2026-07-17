package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.repository.RoomRepository
import javax.inject.Inject

/** 공지 삭제 (명세: DELETE /challenges/{id}/notices/{noticeId} — 방장 전용, 서버는 소프트 삭제). */
class DeleteNoticeUseCase
    @Inject
    constructor(
        private val roomRepository: RoomRepository,
    ) {
        suspend operator fun invoke(
            challengeId: String,
            noticeId: String,
        ) = roomRepository.deleteNotice(challengeId, noticeId)
    }
