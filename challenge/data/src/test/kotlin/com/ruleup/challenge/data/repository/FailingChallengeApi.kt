package com.ruleup.challenge.data.repository

import com.ruleup.challenge.data.api.ChallengeApi
import com.ruleup.challenge.data.dto.ChallengeCategoriesResponse
import com.ruleup.challenge.data.dto.ChallengeDetailResponse
import com.ruleup.challenge.data.dto.ChallengeImageResponse
import com.ruleup.challenge.data.dto.ChallengeMembersResponse
import com.ruleup.challenge.data.dto.ChallengeSettingsResponse
import com.ruleup.challenge.data.dto.ChallengeSetupInfoResponse
import com.ruleup.challenge.data.dto.CreateChallengeRequest
import com.ruleup.challenge.data.dto.CreateChallengeResponse
import com.ruleup.challenge.data.dto.CrossRankingResponse
import com.ruleup.challenge.data.dto.DelegationActionRequest
import com.ruleup.challenge.data.dto.DelegationRequestBody
import com.ruleup.challenge.data.dto.DelegationResolutionResponse
import com.ruleup.challenge.data.dto.DelegationResponse
import com.ruleup.challenge.data.dto.DeleteChallengeResponse
import com.ruleup.challenge.data.dto.DraftRequest
import com.ruleup.challenge.data.dto.DraftResponse
import com.ruleup.challenge.data.dto.ExploreChallengesResponse
import com.ruleup.challenge.data.dto.JoinResponse
import com.ruleup.challenge.data.dto.LeaveChallengeResponse
import com.ruleup.challenge.data.dto.MemberRoleActionRequest
import com.ruleup.challenge.data.dto.MemberRoleResponse
import com.ruleup.challenge.data.dto.MyChallengesResponse
import com.ruleup.challenge.data.dto.OwnerClaimResponse
import com.ruleup.challenge.data.dto.RankingResponse
import com.ruleup.challenge.data.dto.RecommendByTemplateRequest
import com.ruleup.challenge.data.dto.RoomResponse
import com.ruleup.challenge.data.dto.RoutineTemplatesResponse
import com.ruleup.challenge.data.dto.TemplateDraftResponse
import com.ruleup.challenge.data.dto.ThreadsResponse
import com.ruleup.challenge.data.dto.TrendingChallengesResponse
import com.ruleup.challenge.data.dto.UpdateChallengeResponse
import com.ruleup.challenge.data.dto.WatcherInvitationResponse
import com.ruleup.challenge.data.dto.WatchersResponse
import com.ruleup.network.dto.BaseResponse
import com.ruleup.network.dto.EmptyData
import com.ruleup.network.dto.ErrorBody
import kotlinx.serialization.json.JsonObject
import okhttp3.MultipartBody
import retrofit2.http.Part

/**
 * 모든 호출이 같은 서버 에러로 실패하는 [ChallengeApi].
 * 에러 번역만 보는 테스트라 성공 경로는 필요 없다 — 어느 메서드로 들어와도 같은 답을 준다.
 */
class FailingChallengeApi(
    private val error: ErrorBody,
) : ChallengeApi {
    private fun <T> failed(): BaseResponse<T> = BaseResponse(success = false, data = null, error = error)

    override suspend fun getRoutineTemplates(): BaseResponse<RoutineTemplatesResponse> = failed()

    override suspend fun createDraft(request: DraftRequest): BaseResponse<DraftResponse> = failed()

    override suspend fun createDraftFromTemplate(request: RecommendByTemplateRequest): BaseResponse<TemplateDraftResponse> = failed()

    override suspend fun create(
        idempotencyKey: String,
        request: CreateChallengeRequest,
    ): BaseResponse<CreateChallengeResponse> = failed()

    override suspend fun getChallenge(challengeId: String): BaseResponse<ChallengeDetailResponse> = failed()

    override suspend fun getSetup(challengeId: String): BaseResponse<ChallengeSetupInfoResponse> = failed()

    override suspend fun getSettings(challengeId: String): BaseResponse<ChallengeSettingsResponse> = failed()

    override suspend fun update(
        challengeId: String,
        request: JsonObject,
    ): BaseResponse<UpdateChallengeResponse> = failed()

    override suspend fun delete(challengeId: String): BaseResponse<DeleteChallengeResponse> = failed()

    override suspend fun join(challengeId: String): BaseResponse<JoinResponse> = failed()

    override suspend fun getMembers(challengeId: String): BaseResponse<ChallengeMembersResponse> = failed()

    override suspend fun leaveChallenge(challengeId: String): BaseResponse<LeaveChallengeResponse> = failed()

    override suspend fun changeMemberRole(
        challengeId: String,
        userId: String,
        request: MemberRoleActionRequest,
    ): BaseResponse<MemberRoleResponse> = failed()

    override suspend fun claimOwner(challengeId: String): BaseResponse<OwnerClaimResponse> = failed()

    override suspend fun requestDelegation(
        challengeId: String,
        request: DelegationRequestBody,
    ): BaseResponse<DelegationResponse> = failed()

    override suspend fun respondDelegation(
        challengeId: String,
        delegationId: String,
        request: DelegationActionRequest,
    ): BaseResponse<DelegationResolutionResponse> = failed()

    override suspend fun uploadImage(image: MultipartBody.Part): BaseResponse<ChallengeImageResponse> = failed()

    override suspend fun getMyChallenges(): BaseResponse<MyChallengesResponse> = failed()

    override suspend fun getTrending(category: String?): BaseResponse<TrendingChallengesResponse> = failed()

    override suspend fun getCategories(): BaseResponse<ChallengeCategoriesResponse> = failed()

    override suspend fun explore(
        categories: String?,
        verifyType: String?,
        eligibleOnly: Boolean?,
        sort: String?,
        cursor: String?,
        size: Int?,
    ): BaseResponse<ExploreChallengesResponse> = failed()

    override suspend fun clone(challengeId: String): BaseResponse<TemplateDraftResponse> = failed()

    override suspend fun createWatcherInvitation(challengeId: String): BaseResponse<WatcherInvitationResponse> = failed()

    override suspend fun getWatchers(
        challengeId: String,
        status: String?,
    ): BaseResponse<WatchersResponse> = failed()

    override suspend fun removeWatcher(
        challengeId: String,
        watcherId: String,
    ): BaseResponse<EmptyData> = failed()

    override suspend fun getRoom(challengeId: String): BaseResponse<RoomResponse> = failed()

    override suspend fun getThreads(
        challengeId: String,
        cursor: String?,
        size: Int?,
    ): BaseResponse<ThreadsResponse> = failed()

    override suspend fun getRanking(challengeId: String): BaseResponse<RankingResponse> = failed()

    override suspend fun getCrossRanking(
        mode: String,
        challengeId: String?,
        cursor: String?,
        size: Int?,
    ): BaseResponse<CrossRankingResponse> = failed()
}
