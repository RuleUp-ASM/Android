package com.ruleup.challenge.domain.repository

import com.ruleup.challenge.domain.entity.ChallengeRanking
import com.ruleup.challenge.domain.entity.ChallengeRoom
import com.ruleup.challenge.domain.entity.NoticeCreateResult
import com.ruleup.challenge.domain.entity.NoticeDetail
import com.ruleup.challenge.domain.entity.NoticePinResult
import com.ruleup.challenge.domain.entity.NoticeSummary
import com.ruleup.challenge.domain.entity.NoticeUpdateResult

/**
 * 챌린지 방 내부 (방 홈·공지·랭킹) 조회/쓰기.
 * 전 API가 ACTIVE 멤버 전용(403 NOT_A_MEMBER), 공지 쓰기는 방장 전용(403 NOT_CHALLENGE_OWNER)이다.
 */
interface RoomRepository {
    suspend fun getRoom(challengeId: String): ChallengeRoom

    suspend fun getNotices(challengeId: String): List<NoticeSummary>

    /** 상세 조회 = 서버가 읽음 upsert (멱등). */
    suspend fun getNotice(
        challengeId: String,
        noticeId: String,
    ): NoticeDetail

    suspend fun createNotice(
        challengeId: String,
        title: String,
        content: String,
        pinned: Boolean,
    ): NoticeCreateResult

    suspend fun updateNotice(
        challengeId: String,
        noticeId: String,
        title: String,
        content: String,
        resetRead: Boolean,
    ): NoticeUpdateResult

    suspend fun deleteNotice(
        challengeId: String,
        noticeId: String,
    )

    suspend fun pinNotice(
        challengeId: String,
        noticeId: String,
        pinned: Boolean,
    ): NoticePinResult

    suspend fun getRanking(challengeId: String): ChallengeRanking
}
