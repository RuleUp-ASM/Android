package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.entity.ChallengeRoom
import com.ruleup.challenge.domain.repository.RoomRepository
import javax.inject.Inject

/**
 * 방 홈 일괄 조회 (명세: GET /challenges/{id}/room).
 * 요약·고정 공지·미읽음 수·top3 랭킹·내 오늘 상태를 한 번에 받아 방 홈을 렌더링한다.
 * 비멤버는 403(NOT_A_MEMBER) — 호출 성공 여부가 곧 방 홈 노출 조건이다.
 */
class GetChallengeRoomUseCase
    @Inject
    constructor(
        private val roomRepository: RoomRepository,
    ) {
        suspend operator fun invoke(challengeId: String): ChallengeRoom = roomRepository.getRoom(challengeId)
    }
