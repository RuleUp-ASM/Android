package com.ruleup.profile.data.repository

import com.ruleup.network.dto.getOrThrow
import com.ruleup.profile.data.api.MyPageApi
import com.ruleup.profile.data.dto.toDomain
import com.ruleup.profile.data.dto.toGroupChallenges
import com.ruleup.profile.domain.entity.ActivityCalendar
import com.ruleup.profile.domain.entity.CalendarDayDetail
import com.ruleup.profile.domain.entity.FriendInvitation
import com.ruleup.profile.domain.entity.GroupChallengeSummary
import com.ruleup.profile.domain.entity.MyHome
import com.ruleup.profile.domain.entity.MyTier
import com.ruleup.profile.domain.entity.StatsReport
import com.ruleup.profile.domain.entity.TierHistory
import com.ruleup.profile.domain.repository.MyPageRepository
import javax.inject.Inject

class MyPageRepositoryImpl
    @Inject
    constructor(
        private val api: MyPageApi,
    ) : MyPageRepository {
        override suspend fun getHome(): MyHome =
            api
                .getHome()
                .getOrThrow()
                .toDomain()

        override suspend fun getMyGroupChallenges(): List<GroupChallengeSummary> =
            api
                .getMyChallenges()
                .getOrThrow()
                .toGroupChallenges()

        override suspend fun getTier(): MyTier =
            api
                .getTier()
                .getOrThrow()
                .toDomain()

        override suspend fun getTierHistory(months: Int): TierHistory =
            api
                .getTierHistory(months)
                .getOrThrow()
                .toDomain()

        override suspend fun getCalendar(month: String): ActivityCalendar =
            api
                .getCalendar(month)
                .getOrThrow()
                .toDomain()

        override suspend fun getCalendarDay(date: String): CalendarDayDetail =
            api
                .getCalendarDay(date)
                .getOrThrow()
                .toDomain()

        override suspend fun getStats(): StatsReport =
            api
                .getStats()
                .getOrThrow()
                .toDomain()

        override suspend fun getInvitation(): FriendInvitation =
            api
                .getInvitation()
                .getOrThrow()
                .toDomain()
    }
