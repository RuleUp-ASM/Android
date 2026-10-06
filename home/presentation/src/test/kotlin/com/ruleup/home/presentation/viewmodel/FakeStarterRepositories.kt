package com.ruleup.home.presentation.viewmodel

import com.ruleup.challenge.domain.entity.ChallengeCategoryCount
import com.ruleup.challenge.domain.entity.DraftResult
import com.ruleup.challenge.domain.entity.ExploreFilter
import com.ruleup.challenge.domain.entity.ExploreResult
import com.ruleup.challenge.domain.entity.ExploreSort
import com.ruleup.challenge.domain.entity.TrendingChallenge
import com.ruleup.challenge.domain.entity.TrendingSnapshot
import com.ruleup.challenge.domain.repository.ExploreRepository
import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.entity.user.User
import com.ruleup.profile.domain.entity.CategoryCatalog
import com.ruleup.profile.domain.entity.MemberProfile
import com.ruleup.profile.domain.entity.MyProfile
import com.ruleup.profile.domain.entity.NicknameCheck
import com.ruleup.profile.domain.entity.Profile
import com.ruleup.profile.domain.repository.ProfileRepository

/** 홈은 실시간 인기만 읽는다. */
internal class FakeExploreRepository(
    private val trending: () -> List<TrendingChallenge> = { emptyList() },
) : ExploreRepository {
    var trendingCalls = 0
        private set

    override suspend fun getTrending(category: Category?): TrendingSnapshot {
        trendingCalls++
        return TrendingSnapshot(calculatedAt = null, items = trending())
    }

    override suspend fun getCategories(): List<ChallengeCategoryCount> = error("홈에서 쓰지 않는다")

    override suspend fun explore(
        filter: ExploreFilter,
        sort: ExploreSort,
        cursor: String?,
        size: Int?,
    ): ExploreResult = error("홈에서 쓰지 않는다")

    override suspend fun clone(challengeId: String): DraftResult.Ok = error("홈에서 쓰지 않는다")
}

/** 홈은 관심 분야만 읽는다. */
internal class FakeProfileRepository(
    private val interests: () -> List<Category> = { emptyList() },
) : ProfileRepository {
    override suspend fun getProfile(): Profile =
        Profile(
            user = User(id = "me", nickname = "지수", profileImageUrl = null),
            email = null,
            nicknameChangedAt = null,
            nicknameChangeableAfter = null,
            mannerTemperature = 36.5,
            interestCategories = interests(),
            createdAt = "2026-09-01T00:00:00Z",
        )

    override suspend fun getMyProfile(): MyProfile = error("홈에서 쓰지 않는다")

    override suspend fun checkNickname(nickname: String): NicknameCheck = error("홈에서 쓰지 않는다")

    override suspend fun getCategories(): CategoryCatalog = error("홈에서 쓰지 않는다")

    override suspend fun updateProfile(
        nickname: String?,
        interestCategories: List<Category>?,
    ): Profile = error("홈에서 쓰지 않는다")

    override suspend fun uploadProfileImage(imageUri: String): String = error("홈에서 쓰지 않는다")

    override suspend fun deleteProfileImage() = error("홈에서 쓰지 않는다")

    override suspend fun getMemberProfile(userId: String): MemberProfile = error("홈에서 쓰지 않는다")
}
