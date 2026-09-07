package com.ruleup.challenge.presentation.mychallenges.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.challenge.domain.entity.MyChallenge
import com.ruleup.challenge.domain.entity.MyChallengeFilter
import com.ruleup.challenge.domain.entity.MyChallengePage
import com.ruleup.challenge.domain.navigation.ChallengeDetailPage
import com.ruleup.challenge.domain.navigation.ChallengeExplorePage
import com.ruleup.challenge.domain.repository.ChallengeRepository
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.notification.domain.repository.NotificationRepository
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.verification.domain.repository.VerificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 내 챌린지 목록 ViewModel (하단 「챌린지」 탭).
 *
 * 완료·이탈 탭은 서버 `filter` 가 둘로 갈려 있어 **두 번 받아 합친다.** 이탈 시각이 없는 완료 건과
 * 섞이므로 정렬 기준은 종료·이탈 시각이 아니라 **기간 종료일 역순**으로 둔다 — 두 목록에 공통으로
 * 있는 값이 그것뿐이다.
 *
 * 달성률과 남은 일수는 목록 응답에 없어 인증 모듈의 진행률(`verifications/progress`)에서 온다.
 * 그 조회가 실패해도 목록은 그대로 뜬다 — 두 값만 비운다.
 */
@HiltViewModel
class MyChallengesViewModel
    @Inject
    constructor(
        private val challengeRepository: ChallengeRepository,
        private val verificationRepository: VerificationRepository,
        private val notificationRepository: NotificationRepository,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<MyChallengesIntent, MyChallengesState, MyChallengesReducerEvent, MyChallengesEffect>(
            MyChallengesState.initial,
        ) {
        override fun onIntent(intent: MyChallengesIntent) {
            when (intent) {
                MyChallengesIntent.Load -> load(force = false)
                MyChallengesIntent.Refresh -> load(force = true)
                is MyChallengesIntent.SelectSegment -> dispatch(MyChallengesReducerEvent.SegmentSelected(intent.segment))
                MyChallengesIntent.LoadMore -> loadMore()

                is MyChallengesIntent.OpenChallenge ->
                    navigationHelper.navigateTo(ChallengeDetailPage(intent.challengeId))

                MyChallengesIntent.OpenExplore, MyChallengesIntent.OpenExploreTab ->
                    navigationHelper.navigateByRoute(ChallengeExplorePage.toRoute())

                MyChallengesIntent.OpenHomeTab -> navigationHelper.navigateByRoute(NavRoute(AppRoutes.HOME))
                MyChallengesIntent.OpenMyTab -> navigationHelper.navigateByRoute(NavRoute(AppRoutes.MY_HOME))
            }
        }

        override fun reduce(
            state: MyChallengesState,
            event: MyChallengesReducerEvent,
        ): MyChallengesState =
            when (event) {
                MyChallengesReducerEvent.Loading -> state.copy(isLoading = true, errorMessage = null)

                is MyChallengesReducerEvent.Loaded ->
                    state.copy(
                        isLoading = false,
                        inProgress = event.inProgress,
                        finished = event.finished,
                        finishedPaging = event.paging,
                        errorMessage = null,
                    )

                is MyChallengesReducerEvent.ProgressLoaded -> state.copy(progress = event.progress)

                is MyChallengesReducerEvent.UnreadLoaded -> state.copy(unread = event.unread)

                is MyChallengesReducerEvent.Failed -> state.copy(isLoading = false, errorMessage = event.message)

                is MyChallengesReducerEvent.LoadingMore -> state.copy(isLoadingMore = event.loading)

                is MyChallengesReducerEvent.MoreLoaded ->
                    state.copy(
                        isLoadingMore = false,
                        finished = (state.finished + event.finished).sortedByDescending { it.period.end },
                        finishedPaging = event.paging,
                    )

                is MyChallengesReducerEvent.SegmentSelected -> state.copy(segment = event.segment)
            }

        private fun load(force: Boolean) {
            // 첫 구독 시에만 스피너 — 복귀 갱신은 보여 주던 목록을 유지한 채 조용히 바꾼다.
            if (!force && !currentState.isLoading) return
            if (currentState.inProgress.isEmpty() && currentState.finished.isEmpty()) {
                dispatch(MyChallengesReducerEvent.Loading)
            }
            viewModelScope.launch {
                runCatching {
                    coroutineScope {
                        val inProgress = async { challengeRepository.getMyChallenges(MyChallengeFilter.IN_PROGRESS) }
                        val completed = async { challengeRepository.getMyChallenges(MyChallengeFilter.COMPLETED) }
                        val left = async { challengeRepository.getMyChallenges(MyChallengeFilter.LEFT) }
                        Triple(inProgress.await(), completed.await(), left.await())
                    }
                }.onSuccess { (inProgress, completed, left) ->
                    dispatch(
                        MyChallengesReducerEvent.Loaded(
                            inProgress = inProgress.challenges,
                            finished = mergeFinished(completed, left),
                            paging = pagingOf(completed, left),
                        ),
                    )
                }.onFailure {
                    if (currentState.inProgress.isEmpty() && currentState.finished.isEmpty()) {
                        dispatch(MyChallengesReducerEvent.Failed(it.message ?: "챌린지 목록을 불러오지 못했어요"))
                    }
                }
            }
            // 달성률만 쓰는 부수 조회라 실패를 삼킨다 — 이것 때문에 목록이 오류 화면이 되면 안 된다.
            viewModelScope.launch {
                runCatching { verificationRepository.getProgress() }
                    .onSuccess { dispatch(MyChallengesReducerEvent.ProgressLoaded(it)) }
            }
            // 미읽음 뱃지도 부수 정보다 — 못 세면 뱃지만 안 붙고 목록은 그대로 뜬다.
            viewModelScope.launch {
                runCatching { notificationRepository.getUnreadSummary() }
                    .onSuccess { dispatch(MyChallengesReducerEvent.UnreadLoaded(it)) }
            }
        }

        private fun loadMore() {
            val paging = currentState.finishedPaging
            if (currentState.isLoadingMore || !paging.hasNext) return
            dispatch(MyChallengesReducerEvent.LoadingMore(true))
            viewModelScope.launch {
                runCatching {
                    coroutineScope {
                        val completed =
                            async {
                                if (paging.completedHasNext) {
                                    challengeRepository.getMyChallenges(MyChallengeFilter.COMPLETED, paging.completedCursor)
                                } else {
                                    null
                                }
                            }
                        val left =
                            async {
                                if (paging.leftHasNext) {
                                    challengeRepository.getMyChallenges(MyChallengeFilter.LEFT, paging.leftCursor)
                                } else {
                                    null
                                }
                            }
                        completed.await() to left.await()
                    }
                }.onSuccess { (completed, left) ->
                    dispatch(
                        MyChallengesReducerEvent.MoreLoaded(
                            finished = completed?.challenges.orEmpty() + left?.challenges.orEmpty(),
                            paging =
                                FinishedPaging(
                                    completedCursor = completed?.nextCursor ?: paging.completedCursor,
                                    completedHasNext = completed?.hasNext ?: false,
                                    leftCursor = left?.nextCursor ?: paging.leftCursor,
                                    leftHasNext = left?.hasNext ?: false,
                                ),
                        ),
                    )
                }.onFailure {
                    dispatch(MyChallengesReducerEvent.LoadingMore(false))
                    emitEffect(MyChallengesEffect.ShowMessage(it.message ?: "더 불러오지 못했어요"))
                }
            }
        }

        private fun mergeFinished(
            completed: MyChallengePage,
            left: MyChallengePage,
        ): List<MyChallenge> = (completed.challenges + left.challenges).sortedByDescending { it.period.end }

        private fun pagingOf(
            completed: MyChallengePage,
            left: MyChallengePage,
        ) = FinishedPaging(
            completedCursor = completed.nextCursor,
            completedHasNext = completed.hasNext,
            leftCursor = left.nextCursor,
            leftHasNext = left.hasNext,
        )
    }
