package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.entity.NoticeCreateResult
import com.ruleup.challenge.domain.repository.RoomRepository
import javax.inject.Inject

/**
 * 공지 작성 (명세: POST /challenges/{id}/notices — 방장 전용).
 * 저장 후 서버가 ACTIVE 멤버(작성자 제외)에게 푸시 fan-out. pinned=true 면 기존 고정 자동 해제(단일 pin).
 * 길이 검증(제목 1~100자·본문 1~2,000자)은 화면이 NoticePolicy 로 선차단하고 서버가 재검증한다.
 */
class CreateNoticeUseCase
    @Inject
    constructor(
        private val roomRepository: RoomRepository,
    ) {
        suspend operator fun invoke(
            challengeId: String,
            title: String,
            content: String,
            pinned: Boolean,
        ): NoticeCreateResult = roomRepository.createNotice(challengeId, title, content, pinned)
    }
