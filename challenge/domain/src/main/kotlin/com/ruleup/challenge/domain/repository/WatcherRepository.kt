package com.ruleup.challenge.domain.repository

import com.ruleup.challenge.domain.entity.ChallengeWatchers
import com.ruleup.challenge.domain.entity.WatcherAcceptance
import com.ruleup.challenge.domain.entity.WatcherInvitation
import com.ruleup.challenge.domain.entity.Watching

/** 루틴 실패 패널티 */
interface WatcherRepository {
    /** 감시자 초대 생성. */
    suspend fun createInvitation(challengeId: String): WatcherInvitation

    /** 내 감시자 목록 조회. */
    suspend fun getWatchers(challengeId: String): ChallengeWatchers

    /** 내가 감시자로 등록된 관계 목록. */
    suspend fun getWatching(): List<Watching>

    /** 초대 수락. */
    suspend fun acceptInvitation(token: String): WatcherAcceptance
}
