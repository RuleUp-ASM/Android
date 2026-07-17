package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.entity.NoticeDetail
import com.ruleup.challenge.domain.repository.RoomRepository
import javax.inject.Inject

/**
 * 공지 상세 조회 (명세: GET /challenges/{id}/notices/{noticeId}).
 * 조회가 곧 읽음 처리(서버 upsert·멱등) — 호출부는 목록의 isRead·미읽음 수를 로컬 갱신한다.
 */
class GetNoticeDetailUseCase
    @Inject
    constructor(
        private val roomRepository: RoomRepository,
    ) {
        suspend operator fun invoke(
            challengeId: String,
            noticeId: String,
        ): NoticeDetail = roomRepository.getNotice(challengeId, noticeId)
    }
