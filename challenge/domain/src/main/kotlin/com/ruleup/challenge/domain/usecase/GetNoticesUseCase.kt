package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.entity.NoticeSummary
import com.ruleup.challenge.domain.repository.RoomRepository
import javax.inject.Inject

/**
 * 공지 목록 조회 (명세: GET /challenges/{id}/notices).
 * 정렬(고정 우선 → 최신순)·건수(최근 10건)는 서버 고정 — 클라 파라미터 없음.
 */
class GetNoticesUseCase
    @Inject
    constructor(
        private val roomRepository: RoomRepository,
    ) {
        suspend operator fun invoke(challengeId: String): List<NoticeSummary> = roomRepository.getNotices(challengeId)
    }
