package com.ruleup.challenge.data.repository

import com.ruleup.challenge.data.api.ChallengeApi
import com.ruleup.challenge.data.dto.CreateNoticeRequest
import com.ruleup.challenge.data.dto.PinNoticeRequest
import com.ruleup.challenge.data.dto.UpdateNoticeRequest
import com.ruleup.challenge.data.dto.toDomain
import com.ruleup.challenge.domain.entity.ChallengeRanking
import com.ruleup.challenge.domain.entity.ChallengeRoom
import com.ruleup.challenge.domain.entity.ChallengeThreads
import com.ruleup.challenge.domain.entity.CrossChallengeRanking
import com.ruleup.challenge.domain.entity.NoticeCreateResult
import com.ruleup.challenge.domain.entity.NoticeDetail
import com.ruleup.challenge.domain.entity.NoticePinResult
import com.ruleup.challenge.domain.entity.NoticeSummary
import com.ruleup.challenge.domain.entity.NoticeUpdateResult
import com.ruleup.challenge.domain.entity.RankingMode
import com.ruleup.challenge.domain.entity.ThreadCursorInvalidException
import com.ruleup.challenge.domain.repository.RoomRepository
import com.ruleup.network.dto.ApiException
import com.ruleup.network.dto.getOrThrow
import com.ruleup.network.dto.throwOnError
import javax.inject.Inject

class RoomRepositoryImpl
    @Inject
    constructor(
        private val api: ChallengeApi,
    ) : RoomRepository {
        override suspend fun getRoom(challengeId: String): ChallengeRoom =
            api
                .getRoom(challengeId)
                .getOrThrow()
                .toDomain()

        override suspend fun getThreads(
            challengeId: String,
            cursor: String?,
            size: Int,
        ): ChallengeThreads =
            try {
                api
                    .getThreads(challengeId, cursor = cursor, size = size)
                    .getOrThrow()
                    .toDomain()
            } catch (e: ApiException) {
                // 커서가 만료·변조된 경우다. 화면이 첫 페이지부터 다시 받도록 구분되는 타입으로 올린다.
                if (e.code == CODE_CURSOR_INVALID) throw ThreadCursorInvalidException()
                throw e
            }

        override suspend fun getNotices(challengeId: String): List<NoticeSummary> =
            api
                .getNotices(challengeId)
                .getOrThrow()
                .notices
                .orEmpty()
                .map { it.toDomain() }

        override suspend fun getNotice(
            challengeId: String,
            noticeId: String,
        ): NoticeDetail =
            api
                .getNotice(challengeId, noticeId)
                .getOrThrow()
                .toDomain()

        override suspend fun createNotice(
            challengeId: String,
            title: String,
            content: String,
            pinned: Boolean,
        ): NoticeCreateResult =
            api
                .createNotice(challengeId, CreateNoticeRequest(title = title, content = content, pinned = pinned))
                .getOrThrow()
                .toDomain()

        override suspend fun updateNotice(
            challengeId: String,
            noticeId: String,
            title: String,
            content: String,
            resetRead: Boolean,
        ): NoticeUpdateResult =
            api
                .updateNotice(challengeId, noticeId, UpdateNoticeRequest(title = title, content = content, resetRead = resetRead))
                .getOrThrow()
                .toDomain()

        override suspend fun deleteNotice(
            challengeId: String,
            noticeId: String,
        ) {
            api.deleteNotice(challengeId, noticeId).throwOnError()
        }

        override suspend fun pinNotice(
            challengeId: String,
            noticeId: String,
            pinned: Boolean,
        ): NoticePinResult =
            api
                .pinNotice(challengeId, noticeId, PinNoticeRequest(pinned = pinned))
                .getOrThrow()
                .toDomain()

        override suspend fun getRanking(challengeId: String): ChallengeRanking =
            api
                .getRanking(challengeId)
                .getOrThrow()
                .toDomain()

        override suspend fun getCrossRanking(
            mode: RankingMode,
            challengeId: String?,
            cursor: String?,
            size: Int?,
        ): CrossChallengeRanking =
            api
                .getCrossRanking(
                    mode = mode.value,
                    challengeId = challengeId,
                    cursor = cursor,
                    size = size,
                ).getOrThrow()
                .toDomain()

        companion object {
            private const val CODE_CURSOR_INVALID = "CURSOR_INVALID"
        }
    }
