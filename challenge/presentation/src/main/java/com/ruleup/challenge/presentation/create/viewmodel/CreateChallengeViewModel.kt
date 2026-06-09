package com.ruleup.challenge.presentation.create.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.challenge.domain.ChallengeConfirmPage
import com.ruleup.challenge.domain.entity.Anonymity
import com.ruleup.challenge.domain.entity.ChallengeForm
import com.ruleup.challenge.domain.entity.ParticipationType
import com.ruleup.challenge.domain.entity.Penalty
import com.ruleup.challenge.domain.entity.Reward
import com.ruleup.challenge.domain.entity.SnsShare
import com.ruleup.challenge.domain.usecase.CreateChallengeUseCase
import com.ruleup.challenge.domain.usecase.RecommendChallengeUseCase
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.ui.mvi.MviViewModel
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.launch

/**
 * 챌린지 생성 플로우 공유 ViewModel.
 *
 * 입력(01)·AI 추천 확인(02) 두 페이지가 같은 인스턴스를 공유해 입력값을 누적한다.
 * 추천(명세 3.1)과 생성(명세 3.2) 비동기 분기를 담당하고, 성공 시 직접 페이지를 이동시킨다.
 */
@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class CreateChallengeViewModel
    constructor(
        private val recommendChallengeUseCase: RecommendChallengeUseCase,
        private val createChallengeUseCase: CreateChallengeUseCase,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<CreateChallengeIntent, CreateChallengeState, CreateChallengeReducerEvent, CreateChallengeEffect>(
            CreateChallengeState.initial,
        ) {
        override fun onIntent(intent: CreateChallengeIntent) {
            when (intent) {
                is CreateChallengeIntent.SetTitle -> {
                    dispatch(CreateChallengeReducerEvent.TitleEntered(intent.title))
                }

                is CreateChallengeIntent.SetDescription -> {
                    dispatch(CreateChallengeReducerEvent.DescriptionEntered(intent.description))
                }

                CreateChallengeIntent.Recommend -> {
                    recommend()
                }

                is CreateChallengeIntent.SetCoverImage -> {
                    dispatch(CreateChallengeReducerEvent.CoverImageSelected(intent.uri))
                }

                is CreateChallengeIntent.SetParticipationType -> {
                    dispatch(CreateChallengeReducerEvent.ParticipationTypeSelected(intent.type))
                }

                is CreateChallengeIntent.SetMinMannerTemperature -> {
                    dispatch(CreateChallengeReducerEvent.MinMannerChanged(intent.temperature))
                }

                is CreateChallengeIntent.ToggleRepeatDay -> {
                    dispatch(CreateChallengeReducerEvent.RepeatDayToggled(intent.day))
                }

                is CreateChallengeIntent.SetPeriod -> {
                    dispatch(CreateChallengeReducerEvent.PeriodChanged(intent.startDate, intent.durationDays))
                }

                is CreateChallengeIntent.ToggleVerificationMethod -> {
                    dispatch(CreateChallengeReducerEvent.VerificationToggled(intent.method))
                }

                is CreateChallengeIntent.SetSnsShareEnabled -> {
                    dispatch(CreateChallengeReducerEvent.SnsShareChanged(intent.enabled))
                }

                is CreateChallengeIntent.SetSnsPhone -> {
                    dispatch(CreateChallengeReducerEvent.SnsPhoneEntered(intent.phone))
                }

                is CreateChallengeIntent.SetGroupShare -> {
                    dispatch(CreateChallengeReducerEvent.GroupShareChanged(intent.enabled))
                }

                CreateChallengeIntent.Create -> {
                    create()
                }
            }
        }

        override fun reduce(
            state: CreateChallengeState,
            event: CreateChallengeReducerEvent,
        ): CreateChallengeState =
            when (event) {
                is CreateChallengeReducerEvent.TitleEntered -> {
                    state.copy(title = event.title.take(CreateChallengeState.TITLE_MAX))
                }

                is CreateChallengeReducerEvent.DescriptionEntered -> {
                    state.copy(description = event.description.take(CreateChallengeState.DESCRIPTION_MAX))
                }

                CreateChallengeReducerEvent.Recommending -> {
                    state.copy(isRecommending = true)
                }

                CreateChallengeReducerEvent.RecommendFailed -> {
                    state.copy(isRecommending = false)
                }

                is CreateChallengeReducerEvent.RecommendationReceived -> {
                    val reco = event.recommendation
                    state.copy(
                        isRecommending = false,
                        hasRecommendation = true,
                        title = reco.title.take(CreateChallengeState.TITLE_MAX),
                        description = reco.description ?: state.description,
                        coverImageUri = null,
                        category = reco.category,
                        participationType = reco.participationType,
                        minMannerTemperature =
                            reco.minMannerTemperature
                                ?.toInt()
                                ?.coerceIn(CreateChallengeState.MANNER_MIN, CreateChallengeState.MANNER_MAX)
                                ?: state.minMannerTemperature,
                        repeatDays = reco.repeatDays,
                        startDate = reco.startDate,
                        durationDays = reco.durationDays,
                        verificationMethods = reco.verificationMethods,
                        mannerDeduction = reco.penalty.mannerDeduction,
                        mannerGain = reco.reward.mannerGain,
                        snsShareEnabled = reco.penalty.snsShare.enabled,
                        snsPhone =
                            reco.penalty.snsShare.phone
                                .orEmpty(),
                        groupShare = reco.penalty.groupShare,
                    )
                }

                is CreateChallengeReducerEvent.CoverImageSelected -> {
                    state.copy(coverImageUri = event.uri)
                }

                is CreateChallengeReducerEvent.ParticipationTypeSelected -> {
                    state.copy(participationType = event.type)
                }

                is CreateChallengeReducerEvent.MinMannerChanged -> {
                    state.copy(
                        minMannerTemperature =
                            event.temperature.coerceIn(
                                CreateChallengeState.MANNER_MIN,
                                CreateChallengeState.MANNER_MAX,
                            ),
                    )
                }

                is CreateChallengeReducerEvent.RepeatDayToggled -> {
                    if (event.day in state.repeatDays) {
                        state.copy(repeatDays = state.repeatDays - event.day)
                    } else {
                        state.copy(repeatDays = state.repeatDays + event.day)
                    }
                }

                is CreateChallengeReducerEvent.PeriodChanged -> {
                    state.copy(startDate = event.startDate, durationDays = event.durationDays)
                }

                is CreateChallengeReducerEvent.VerificationToggled -> {
                    if (event.method in state.verificationMethods) {
                        state.copy(verificationMethods = state.verificationMethods - event.method)
                    } else {
                        state.copy(verificationMethods = state.verificationMethods + event.method)
                    }
                }

                is CreateChallengeReducerEvent.SnsShareChanged -> {
                    state.copy(snsShareEnabled = event.enabled)
                }

                is CreateChallengeReducerEvent.SnsPhoneEntered -> {
                    state.copy(snsPhone = event.phone.take(PHONE_MAX))
                }

                is CreateChallengeReducerEvent.GroupShareChanged -> {
                    state.copy(groupShare = event.enabled)
                }

                CreateChallengeReducerEvent.Creating -> {
                    state.copy(isCreating = true)
                }

                CreateChallengeReducerEvent.CreateFailed -> {
                    state.copy(isCreating = false)
                }
            }

        private fun recommend() {
            val state = currentState
            if (state.isRecommending) return
            val title = state.title.trim()
            if (title.length < 2) {
                emitEffect(CreateChallengeEffect.ShowError("챌린지 이름을 2자 이상 입력해주세요"))
                return
            }
            viewModelScope.launch {
                dispatch(CreateChallengeReducerEvent.Recommending)
                runCatching {
                    recommendChallengeUseCase(
                        title = title,
                        description = state.description.trim().ifBlank { null },
                    )
                }.onSuccess { recommendation ->
                    dispatch(CreateChallengeReducerEvent.RecommendationReceived(recommendation))
                    // 입력 화면에서는 확인 페이지로 전진, 확인 화면의 "다시 추천" 은
                    // 동일 키 중복 push 가 무시되므로 그대로 머문다.
                    navigationHelper.navigateTo(ChallengeConfirmPage)
                }.onFailure {
                    dispatch(CreateChallengeReducerEvent.RecommendFailed)
                    emitEffect(CreateChallengeEffect.ShowError(it.message ?: "AI 추천에 실패했어요"))
                }
            }
        }

        private fun create() {
            val state = currentState
            if (state.isCreating) return

            val category =
                state.category ?: run {
                    emitEffect(CreateChallengeEffect.ShowError("카테고리 분류에 실패했어요. 제목을 수정해 다시 추천받아 주세요"))
                    return
                }
            if (state.repeatDays.isEmpty()) {
                emitEffect(CreateChallengeEffect.ShowError("반복 요일을 1개 이상 선택해주세요"))
                return
            }
            if (state.verificationMethods.isEmpty()) {
                emitEffect(CreateChallengeEffect.ShowError("인증 방식을 1개 이상 선택해주세요"))
                return
            }

            val isGroup = state.participationType == ParticipationType.GROUP
            val form =
                ChallengeForm(
                    title = state.title.trim(),
                    description = state.description.trim().ifBlank { null },
                    // TODO: 챌린지 이미지 업로드 API 연동 시 coverImageUri 를 업로드해 URL 로 전달한다.
                    imageUrl = null,
                    category = category,
                    participationType = state.participationType,
                    minMannerTemperature = if (isGroup) state.minMannerTemperature.toDouble() else null,
                    repeatDays = state.repeatDays,
                    durationDays = state.durationDays,
                    startDate = state.startDate,
                    verificationMethods = state.verificationMethods,
                    penalty =
                        Penalty(
                            mannerDeduction = state.mannerDeduction,
                            snsShare =
                                SnsShare(
                                    enabled = state.snsShareEnabled,
                                    phone =
                                        state.snsPhone
                                            .trim()
                                            .ifBlank { null }
                                            .takeIf { state.snsShareEnabled },
                                ),
                            groupShare = state.groupShare,
                        ),
                    reward = Reward(mannerGain = state.mannerGain),
                    anonymity = Anonymity.REAL,
                )

            viewModelScope.launch {
                dispatch(CreateChallengeReducerEvent.Creating)
                runCatching { createChallengeUseCase(form) }
                    .onSuccess {
                        // 홈은 루트 페이지라 백스택이 비워지고 생성 플로우가 정리된다.
                        navigationHelper.navigateByRoute(NavRoute(AppRoutes.HOME))
                    }.onFailure {
                        dispatch(CreateChallengeReducerEvent.CreateFailed)
                        emitEffect(CreateChallengeEffect.ShowError(it.message ?: "챌린지 생성에 실패했어요"))
                    }
            }
        }

        companion object {
            private const val PHONE_MAX = 13
        }
    }
