package com.ruleup.challenge.presentation.create.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.challenge.domain.entity.Anonymity
import com.ruleup.challenge.domain.entity.ChallengeForm
import com.ruleup.challenge.domain.entity.ChallengePermissionRequiredException
import com.ruleup.challenge.domain.entity.ChallengeRecommendation
import com.ruleup.challenge.domain.entity.MyChallengeSummary
import com.ruleup.challenge.domain.entity.ParticipationType
import com.ruleup.challenge.domain.entity.Penalty
import com.ruleup.challenge.domain.entity.RecommendationRateLimitedException
import com.ruleup.challenge.domain.entity.Reward
import com.ruleup.challenge.domain.entity.SelectedMethod
import com.ruleup.challenge.domain.entity.SnsShare
import com.ruleup.challenge.domain.navigation.ChallengeConfirmPage
import com.ruleup.challenge.domain.repository.MyChallengeStore
import com.ruleup.challenge.domain.usecase.CreateChallengeUseCase
import com.ruleup.challenge.domain.usecase.RecommendChallengeByTemplateUseCase
import com.ruleup.challenge.domain.usecase.RecommendChallengeUseCase
import com.ruleup.challenge.domain.usecase.UploadChallengeImageUseCase
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 챌린지 생성 플로우 공유 ViewModel.
 *
 * 입력(01)·AI 추천 확인(02) 두 페이지가 같은 인스턴스를 공유해 입력값을 누적한다.
 * 추천(명세 3.1)과 생성(명세 3.2) 비동기 분기를 담당하고, 성공 시 직접 페이지를 이동시킨다.
 */
@HiltViewModel
class CreateChallengeViewModel
    @Inject
    constructor(
        private val recommendChallengeUseCase: RecommendChallengeUseCase,
        private val recommendChallengeByTemplateUseCase: RecommendChallengeByTemplateUseCase,
        private val createChallengeUseCase: CreateChallengeUseCase,
        private val uploadChallengeImageUseCase: UploadChallengeImageUseCase,
        private val myChallengeStore: MyChallengeStore,
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

                is CreateChallengeIntent.RecommendByTemplate -> {
                    recommendByTemplate(intent.templateId)
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

                is CreateChallengeIntent.SetMaxParticipants -> {
                    dispatch(CreateChallengeReducerEvent.MaxParticipantsChanged(intent.count))
                }

                is CreateChallengeIntent.ToggleRepeatDay -> {
                    dispatch(CreateChallengeReducerEvent.RepeatDayToggled(intent.day))
                }

                is CreateChallengeIntent.SetPeriod -> {
                    dispatch(CreateChallengeReducerEvent.PeriodChanged(intent.startDate, intent.durationDays))
                }

                is CreateChallengeIntent.SelectMethod -> {
                    dispatch(CreateChallengeReducerEvent.MethodSelected(intent.method))
                }

                is CreateChallengeIntent.EditParam -> {
                    dispatch(CreateChallengeReducerEvent.ParamEdited(intent.key, intent.value))
                }

                is CreateChallengeIntent.PermissionsResult -> {
                    // 화면이 OS 다이얼로그로 받은 허용 토큰을 누적하고 생성을 재시도한다.
                    dispatch(CreateChallengeReducerEvent.PermissionsGranted(intent.granted))
                    create()
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
                        matched = reco.matched,
                        templateId = reco.templateId,
                        title = reco.title.take(CreateChallengeState.TITLE_MAX),
                        description = reco.description ?: state.description,
                        coverImageUri = null,
                        category = reco.category,
                        participationType = reco.participationType,
                        // 기준 온도 상한 = 생성자 현재 온도(없으면 MANNER_MAX).
                        maxMannerTemperature =
                            reco.maxMannerTemperature
                                ?.toInt()
                                ?.coerceIn(CreateChallengeState.MANNER_MIN, CreateChallengeState.MANNER_MAX)
                                ?: CreateChallengeState.MANNER_MAX,
                        minMannerTemperature =
                            reco.minMannerTemperature
                                ?.toInt()
                                ?.coerceIn(CreateChallengeState.MANNER_MIN, CreateChallengeState.MANNER_MAX)
                                ?: state.minMannerTemperature,
                        maxParticipants =
                            reco.maxParticipants
                                ?.coerceIn(
                                    CreateChallengeState.GROUP_PARTICIPANTS_MIN,
                                    CreateChallengeState.GROUP_PARTICIPANTS_MAX,
                                )
                                ?: state.maxParticipants,
                        repeatDays = reco.repeatDays,
                        startDate = reco.startDate,
                        durationDays = reco.durationDays,
                        options = reco.options,
                        selectedMethod = reco.recommendedMethod,
                        params = reco.params,
                        rationale = reco.rationale,
                        // 새 추천을 받으면 권한 누적/요청 이력은 초기화한다.
                        grantedPermissions = emptySet(),
                        permissionRequested = false,
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
                        // 상한은 생성자 온도(maxMannerTemperature) — 초과 값은 서버가 거부(MIN_TEMP_EXCEEDS_OWNER).
                        minMannerTemperature =
                            event.temperature.coerceIn(
                                CreateChallengeState.MANNER_MIN,
                                state.maxMannerTemperature,
                            ),
                    )
                }

                is CreateChallengeReducerEvent.MaxParticipantsChanged -> {
                    state.copy(
                        maxParticipants =
                            event.count.coerceIn(
                                CreateChallengeState.GROUP_PARTICIPANTS_MIN,
                                CreateChallengeState.GROUP_PARTICIPANTS_MAX,
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

                is CreateChallengeReducerEvent.MethodSelected -> {
                    // 인증 방식을 바꾸면 권한 요청 이력을 초기화한다.
                    state.copy(
                        selectedMethod = event.method,
                        permissionRequested = false,
                    )
                }

                is CreateChallengeReducerEvent.ParamEdited -> {
                    state.copy(
                        params =
                            state.params.map {
                                if (it.key == event.key) it.copy(value = event.value) else it
                            },
                    )
                }

                is CreateChallengeReducerEvent.PermissionsGranted -> {
                    state.copy(
                        grantedPermissions = state.grantedPermissions + event.tokens,
                        permissionRequested = true,
                    )
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
                    applyRecommendation(recommendation)
                }.onFailure { error ->
                    dispatch(CreateChallengeReducerEvent.RecommendFailed)
                    val message =
                        when (error) {
                            is RecommendationRateLimitedException -> {
                                val minutes = error.retryAfterSeconds?.let { (it + 59) / 60 }
                                if (minutes != null) {
                                    "추천 요청이 많아요. ${minutes}분 후에 다시 시도해 주세요"
                                } else {
                                    "추천 요청이 많아요. 잠시 후 다시 시도해 주세요"
                                }
                            }

                            else -> error.message ?: "AI 추천에 실패했어요"
                        }
                    emitEffect(CreateChallengeEffect.ShowError(message))
                }
            }
        }

        /**
         * 탐색 "추천 루틴" 선택 → 템플릿 초안 생성 후 확인 화면으로. by-template 은 LLM 미호출이라
         * fallback·rate-limit 이 없다(matched 항상 true). 실패(TEMPLATE_NOT_FOUND 등)는 안내한다.
         */
        private fun recommendByTemplate(templateId: Long) {
            if (currentState.isRecommending) return
            viewModelScope.launch {
                dispatch(CreateChallengeReducerEvent.Recommending)
                runCatching { recommendChallengeByTemplateUseCase(templateId) }
                    .onSuccess { recommendation -> applyRecommendation(recommendation) }
                    .onFailure { error ->
                        dispatch(CreateChallengeReducerEvent.RecommendFailed)
                        emitEffect(CreateChallengeEffect.ShowError(error.message ?: "루틴 초안을 불러오지 못했어요"))
                    }
            }
        }

        /** 추천/템플릿 초안 수신 공통 처리: fallback 이면 머무르고, 아니면 확인 화면으로 전진. */
        private fun applyRecommendation(recommendation: ChallengeRecommendation) {
            if (recommendation.fallback) {
                // Step 1·2 차단(200) — 확인 화면으로 넘어가지 않고 최초 생성 화면에 머문다(명세).
                dispatch(CreateChallengeReducerEvent.RecommendFailed)
                emitEffect(CreateChallengeEffect.ShowError("추천을 만들지 못했어요. 제목·설명을 다시 확인해 주세요"))
            } else {
                dispatch(CreateChallengeReducerEvent.RecommendationReceived(recommendation))
                // 입력 화면에서는 확인 페이지로 전진, 확인 화면의 "다시 추천" 은
                // 동일 키 중복 push 가 무시되므로 그대로 머문다.
                navigationHelper.navigateTo(ChallengeConfirmPage)
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

            // AUTO 인증이면 필요한 권한을 먼저 확보한다. 부족하면 OS 권한 요청 후 재시도(PermissionsResult).
            if (state.selectedMethod == SelectedMethod.AUTO) {
                val missing = state.selectedOption?.requiredPermissions.orEmpty() - state.grantedPermissions
                if (missing.isNotEmpty()) {
                    if (state.permissionRequested) {
                        emitEffect(
                            CreateChallengeEffect.ShowError("자동 인증에 필요한 권한이 거부됐어요. 권한을 허용하거나 수동 인증을 선택해주세요"),
                        )
                    } else {
                        emitEffect(CreateChallengeEffect.RequestPermissions(missing.toList()))
                    }
                    return
                }
            }

            val isGroup = state.participationType == ParticipationType.GROUP
            val form =
                ChallengeForm(
                    title = state.title.trim(),
                    description = state.description.trim().ifBlank { null },
                    // 로컬 커버 이미지는 생성 직전 업로드(3.9)해 URL 로 치환한다. 아래 launch 참고.
                    imageUrl = null,
                    category = category,
                    participationType = state.participationType,
                    // GROUP 필수, SOLO 는 1 고정(명세 MAX_PARTICIPANTS_REQUIRED 예방).
                    maxParticipants = if (isGroup) state.maxParticipants else 1,
                    // 그룹이라도 최저값(제한 없음)이면 null 을 보낸다. 서버는 기준을 생성자 본인 온도 이하로만
                    // 허용하므로, 최저값을 실제 온도로 보내면(예: 37 > 신규 유저 36.5) 항상 400 이 된다.
                    minMannerTemperature =
                        if (isGroup && state.minMannerTemperature > CreateChallengeState.MANNER_MIN) {
                            state.minMannerTemperature.toDouble()
                        } else {
                            null
                        },
                    repeatDays = state.repeatDays,
                    durationDays = state.durationDays,
                    startDate = state.startDate,
                    templateId = state.templateId,
                    selectedMethod = state.selectedMethod,
                    params = state.params.associate { it.key to it.value },
                    grantedPermissions = state.grantedPermissions.toList(),
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

            val coverImageUri = state.coverImageUri?.takeIf { it.isNotBlank() }
            viewModelScope.launch {
                dispatch(CreateChallengeReducerEvent.Creating)
                runCatching {
                    // 커버 이미지가 있으면 먼저 업로드(3.9)해 URL 을 확보한 뒤 생성한다.
                    val imageUrl = coverImageUri?.let { uploadChallengeImageUseCase(it) }
                    createChallengeUseCase(form.copy(imageUrl = imageUrl))
                }.onSuccess { challenge ->
                    // 생성 응답은 슬림(title·category 생략 가능)하므로 요약은 폼/상태 값으로 채운다.
                    // 진행률 API 반영 전이라도 홈에 즉시 노출되도록 로컬 스토어에 반영한다.
                    myChallengeStore.add(
                        MyChallengeSummary(
                            challengeId = challenge.challengeId,
                            title = state.title.trim(),
                            category = category,
                            participationType = state.participationType,
                            durationDays = state.durationDays,
                        ),
                    )
                    // 홈은 루트 페이지라 백스택이 비워지고 생성 플로우가 정리된다.
                    navigationHelper.navigateByRoute(NavRoute(AppRoutes.HOME))
                }.onFailure { error ->
                    dispatch(CreateChallengeReducerEvent.CreateFailed)
                    when (error) {
                        // 사전 권한 확보를 거쳐도 서버가 부족하다고 바운스하면(거부 등) 1회만 재요청, 이후엔 안내.
                        is ChallengePermissionRequiredException -> {
                            val tokens = state.selectedOption?.requiredPermissions.orEmpty() - state.grantedPermissions
                            if (!state.permissionRequested && tokens.isNotEmpty()) {
                                emitEffect(CreateChallengeEffect.RequestPermissions(tokens.toList()))
                            } else {
                                emitEffect(
                                    CreateChallengeEffect.ShowError("자동 인증 권한이 필요해요. 권한을 허용하거나 수동 인증을 선택해주세요"),
                                )
                            }
                        }

                        else -> emitEffect(CreateChallengeEffect.ShowError(error.message ?: "챌린지 생성에 실패했어요"))
                    }
                }
            }
        }

        companion object {
            private const val PHONE_MAX = 13
        }
    }
