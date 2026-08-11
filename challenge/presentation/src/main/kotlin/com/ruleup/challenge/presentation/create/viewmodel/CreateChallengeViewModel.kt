package com.ruleup.challenge.presentation.create.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.challenge.domain.entity.ChallengeMode
import com.ruleup.challenge.domain.entity.ChallengeVisibility
import com.ruleup.challenge.domain.entity.CreateChallengeCommand
import com.ruleup.challenge.domain.entity.DraftExpiredException
import com.ruleup.challenge.domain.entity.DraftResult
import com.ruleup.challenge.domain.entity.MyChallengeSummary
import com.ruleup.challenge.domain.entity.RecommendationRateLimitedException
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.challenge.domain.entity.toEntries
import com.ruleup.challenge.domain.navigation.ChallengeConfirmPage
import com.ruleup.challenge.domain.repository.MyChallengeStore
import com.ruleup.challenge.domain.usecase.CreateChallengeUseCase
import com.ruleup.challenge.domain.usecase.CreateDraftFromTemplateUseCase
import com.ruleup.challenge.domain.usecase.CreateDraftUseCase
import com.ruleup.challenge.domain.usecase.GetRoutineTemplatesUseCase
import com.ruleup.challenge.domain.usecase.UploadChallengeImageUseCase
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.UUID
import javax.inject.Inject

/**
 * 챌린지 생성 플로우 공유 ViewModel.
 *
 * 입력 화면과 확인 화면이 같은 인스턴스를 공유한다. 두 진입 경로(추천 칩 · 설명 입력)가 **같은 확인
 * 화면으로 수렴**하므로 초안 수신 처리도 하나로 묶여 있다.
 *
 * 생성 플로우 상태는 프로세스 종료 시 복원하지 않는다 — 초안 재생성 비용이 낮고 부분 복원이 오히려
 * 혼란을 만든다.
 */
@HiltViewModel
class CreateChallengeViewModel
    @Inject
    constructor(
        private val getRoutineTemplatesUseCase: GetRoutineTemplatesUseCase,
        private val createDraftUseCase: CreateDraftUseCase,
        private val createDraftFromTemplateUseCase: CreateDraftFromTemplateUseCase,
        private val createChallengeUseCase: CreateChallengeUseCase,
        private val uploadChallengeImageUseCase: UploadChallengeImageUseCase,
        private val myChallengeStore: MyChallengeStore,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<CreateChallengeIntent, CreateChallengeState, CreateChallengeReducerEvent, CreateChallengeEffect>(
            CreateChallengeState.initial,
        ) {
        override fun onIntent(intent: CreateChallengeIntent) {
            when (intent) {
                CreateChallengeIntent.Load -> loadTemplates()

                CreateChallengeIntent.RetryTemplates -> loadTemplates()

                is CreateChallengeIntent.SetRoutineDescription ->
                    dispatch(CreateChallengeReducerEvent.RoutineDescriptionEntered(intent.description))

                CreateChallengeIntent.SubmitDescription -> submitDescription()

                is CreateChallengeIntent.SelectTemplate -> selectTemplate(intent.templateId)

                is CreateChallengeIntent.SetTitle ->
                    dispatch(CreateChallengeReducerEvent.TitleEntered(intent.title))

                is CreateChallengeIntent.SetDescription ->
                    dispatch(CreateChallengeReducerEvent.DescriptionEntered(intent.description))

                is CreateChallengeIntent.SetCoverImage ->
                    dispatch(CreateChallengeReducerEvent.CoverImageSelected(intent.uri))

                is CreateChallengeIntent.SetMode ->
                    dispatch(CreateChallengeReducerEvent.ModeSelected(intent.mode))

                is CreateChallengeIntent.SetVisibility ->
                    dispatch(CreateChallengeReducerEvent.VisibilitySelected(intent.visibility))

                is CreateChallengeIntent.SetRankingVisible ->
                    dispatch(CreateChallengeReducerEvent.RankingVisibleChanged(intent.visible))

                is CreateChallengeIntent.SetCapacity ->
                    dispatch(CreateChallengeReducerEvent.CapacityChanged(intent.capacity))

                is CreateChallengeIntent.SetMinTier ->
                    dispatch(CreateChallengeReducerEvent.MinTierChanged(intent.tier))

                is CreateChallengeIntent.SetPeriod -> setPeriod(intent.start, intent.end)

                is CreateChallengeIntent.EditParam ->
                    dispatch(CreateChallengeReducerEvent.ParamEdited(intent.key, intent.value))

                is CreateChallengeIntent.SetVerificationType -> setVerificationType(intent.type)

                is CreateChallengeIntent.SetWatcherPenalty ->
                    dispatch(CreateChallengeReducerEvent.WatcherPenaltyChanged(intent.enabled))

                is CreateChallengeIntent.PermissionsResult -> {
                    dispatch(CreateChallengeReducerEvent.PermissionsGranted(intent.granted))
                    // 권한은 생성 이후 단계다 — 결과가 무엇이든 생성은 이미 끝났으므로 홈으로 보낸다.
                    // 미허용은 첫 판정일 전까지 인증 설정에서 다시 받을 수 있다.
                    if (currentState.createdChallengeId != null) goHome()
                }

                CreateChallengeIntent.Create -> create()
            }
        }

        override fun reduce(
            state: CreateChallengeState,
            event: CreateChallengeReducerEvent,
        ): CreateChallengeState =
            when (event) {
                is CreateChallengeReducerEvent.RoutineDescriptionEntered ->
                    state.copy(
                        routineDescription = event.description.take(CreateChallengeState.DESCRIPTION_MAX),
                        // 다시 입력하기 시작하면 지난 폴백 안내는 치운다.
                        fallbackMessage = null,
                    )

                CreateChallengeReducerEvent.TemplatesLoading ->
                    state.copy(isLoadingTemplates = true, templatesFailed = false)

                is CreateChallengeReducerEvent.TemplatesLoaded ->
                    state.copy(
                        templates = event.templates,
                        isLoadingTemplates = false,
                        templatesFailed = false,
                    )

                CreateChallengeReducerEvent.TemplatesFailed ->
                    state.copy(isLoadingTemplates = false, templatesFailed = true)

                CreateChallengeReducerEvent.Drafting ->
                    state.copy(isDrafting = true, fallbackMessage = null)

                CreateChallengeReducerEvent.DraftFailed ->
                    state.copy(isDrafting = false)

                is CreateChallengeReducerEvent.DraftFellBack ->
                    // 입력은 그대로 둔다 — 사용자가 쓴 문장을 지우면 다시 쓰게 만드는 벌이 된다.
                    state.copy(isDrafting = false, fallbackMessage = event.message)

                is CreateChallengeReducerEvent.DraftRateLimited ->
                    state.copy(isDrafting = false, retryAfterSeconds = event.retryAfterSeconds ?: 0)

                CreateChallengeReducerEvent.RateLimitCleared ->
                    state.copy(retryAfterSeconds = null)

                is CreateChallengeReducerEvent.DraftReceived -> {
                    val draft = event.draft.draft
                    state.copy(
                        isDrafting = false,
                        fallbackMessage = null,
                        draftId = event.draft.draftId,
                        original = draft,
                        title = draft.title.take(CreateChallengeState.TITLE_MAX),
                        description = draft.description,
                        category = draft.category,
                        mode = draft.mode,
                        visibility = draft.visibility,
                        rankingVisible = draft.rankingVisible,
                        capacity =
                            draft.capacity.coerceIn(
                                CreateChallengeState.CAPACITY_MIN,
                                CreateChallengeState.CAPACITY_MAX,
                            ),
                        minTier = draft.minTier,
                        // 상한은 초안이 준 기본값(= 생성자 표시 티어)으로 고정한다.
                        ownerTierCap = draft.minTier,
                        period = draft.period,
                        params = draft.params,
                        verification = draft.verification,
                        penalties = draft.penalties,
                        coverImageUri = null,
                        idempotencyKey = event.idempotencyKey,
                        grantedPermissions = emptySet(),
                        permissionRequested = false,
                        createdChallengeId = null,
                    )
                }

                is CreateChallengeReducerEvent.TitleEntered ->
                    state.copy(title = event.title.take(CreateChallengeState.TITLE_MAX))

                is CreateChallengeReducerEvent.DescriptionEntered ->
                    state.copy(description = event.description)

                is CreateChallengeReducerEvent.CoverImageSelected ->
                    state.copy(coverImageUri = event.uri)

                is CreateChallengeReducerEvent.ModeSelected ->
                    // 파생 필드는 서버가 정규화하지만, 화면이 엉뚱한 입력부를 열지 않도록 여기서도 맞춘다.
                    if (event.mode == ChallengeMode.GROUP) {
                        state.copy(
                            mode = event.mode,
                            visibility = state.visibility ?: ChallengeVisibility.PUBLIC,
                            rankingVisible = null,
                        )
                    } else {
                        state.copy(
                            mode = event.mode,
                            visibility = null,
                            rankingVisible = state.rankingVisible ?: true,
                        )
                    }

                is CreateChallengeReducerEvent.VisibilitySelected ->
                    state.copy(visibility = event.visibility)

                is CreateChallengeReducerEvent.RankingVisibleChanged ->
                    state.copy(rankingVisible = event.visible)

                is CreateChallengeReducerEvent.CapacityChanged ->
                    state.copy(
                        capacity =
                            event.capacity.coerceIn(
                                CreateChallengeState.CAPACITY_MIN,
                                CreateChallengeState.CAPACITY_MAX,
                            ),
                    )

                is CreateChallengeReducerEvent.MinTierChanged ->
                    // 상한은 생성자 표시 티어 — 초과하면 서버가 MIN_TIER_EXCEEDS_OWNER 로 막는다.
                    state.copy(
                        minTier =
                            state.ownerTierCap?.let { cap ->
                                if (event.tier.ordinal > cap.ordinal) cap else event.tier
                            } ?: event.tier,
                    )

                is CreateChallengeReducerEvent.PeriodChanged ->
                    state.copy(period = state.period.copy(start = event.start, end = event.end))

                is CreateChallengeReducerEvent.ParamEdited ->
                    state.copy(
                        params = state.params.map { if (it.key == event.key) it.copy(value = event.value) else it },
                    )

                is CreateChallengeReducerEvent.VerificationTypeSelected ->
                    state.copy(
                        verification = state.verification?.copy(type = event.type),
                        // 인증 방식을 바꾸면 서버가 score 패널티를 재계산한다. 표시도 맞춰 둔다.
                        penalties = state.penalties.copy(score = event.type == VerificationType.AUTO),
                    )

                is CreateChallengeReducerEvent.WatcherPenaltyChanged ->
                    state.copy(penalties = state.penalties.copy(watcher = event.enabled))

                is CreateChallengeReducerEvent.PermissionsGranted ->
                    state.copy(
                        grantedPermissions = state.grantedPermissions + event.tokens,
                        permissionRequested = true,
                    )

                CreateChallengeReducerEvent.Creating ->
                    state.copy(isCreating = true)

                CreateChallengeReducerEvent.CreateFailed ->
                    state.copy(isCreating = false)

                is CreateChallengeReducerEvent.Created ->
                    state.copy(isCreating = false, createdChallengeId = event.challengeId)
            }

        private fun loadTemplates() {
            if (currentState.isLoadingTemplates) return
            viewModelScope.launch {
                dispatch(CreateChallengeReducerEvent.TemplatesLoading)
                runCatching { getRoutineTemplatesUseCase() }
                    .onSuccess { dispatch(CreateChallengeReducerEvent.TemplatesLoaded(it)) }
                    // 추천이 실패해도 설명 입력 경로는 살아 있어야 하므로 화면 전체를 에러로 만들지 않는다.
                    .onFailure { dispatch(CreateChallengeReducerEvent.TemplatesFailed) }
            }
        }

        /** 경로 B — 설명 입력. 폴백은 실패가 아니라 재입력 분기다. */
        private fun submitDescription() {
            val state = currentState
            if (!state.canSubmitDescription) return
            viewModelScope.launch {
                dispatch(CreateChallengeReducerEvent.Drafting)
                runCatching { createDraftUseCase(state.routineDescription) }
                    .onSuccess { result ->
                        when (result) {
                            is DraftResult.Ok -> applyDraft(result)
                            is DraftResult.Fallback ->
                                dispatch(CreateChallengeReducerEvent.DraftFellBack(result.message))
                        }
                    }.onFailure { error ->
                        when (error) {
                            is RecommendationRateLimitedException ->
                                dispatch(CreateChallengeReducerEvent.DraftRateLimited(error.retryAfterSeconds))

                            else -> {
                                dispatch(CreateChallengeReducerEvent.DraftFailed)
                                emitEffect(
                                    CreateChallengeEffect.ShowError(error.message ?: "초안을 만들지 못했어요. 다시 시도해 주세요"),
                                )
                            }
                        }
                    }
            }
        }

        /** 경로 A — 추천 칩. LLM 미경유라 폴백·rate limit 이 없다. */
        private fun selectTemplate(templateId: Long) {
            if (currentState.isDrafting) return
            viewModelScope.launch {
                dispatch(CreateChallengeReducerEvent.Drafting)
                runCatching { createDraftFromTemplateUseCase(templateId) }
                    .onSuccess { applyDraft(it) }
                    .onFailure { error ->
                        dispatch(CreateChallengeReducerEvent.DraftFailed)
                        emitEffect(CreateChallengeEffect.ShowError(error.message ?: "루틴 초안을 불러오지 못했어요"))
                    }
            }
        }

        /**
         * 두 경로 공통 — 초안을 편집본에 채우고 확인 화면으로 보낸다.
         * idempotency key 는 **여기서 1회만** 만든다. 생성 재시도는 같은 키를 다시 쓴다.
         */
        private fun applyDraft(draft: DraftResult.Ok) {
            dispatch(
                CreateChallengeReducerEvent.DraftReceived(
                    draft = draft,
                    idempotencyKey = UUID.randomUUID().toString(),
                ),
            )
            navigationHelper.navigateTo(ChallengeConfirmPage)
        }

        private fun setPeriod(
            start: String,
            end: String,
        ) {
            if (!isValidPeriod(start, end)) {
                emitEffect(CreateChallengeEffect.ShowError("종료일은 시작일보다 뒤여야 해요"))
                return
            }
            dispatch(CreateChallengeReducerEvent.PeriodChanged(start, end))
        }

        private fun isValidPeriod(
            start: String,
            end: String,
        ): Boolean =
            try {
                !LocalDate.parse(end).isBefore(LocalDate.parse(start))
            } catch (_: DateTimeParseException) {
                false
            }

        /** AUTO → MANUAL 단방향. 되돌리려는 시도는 화면에서 막고 이유를 알린다. */
        private fun setVerificationType(type: VerificationType) {
            val state = currentState
            if (type == VerificationType.AUTO && !state.canUseAuto) {
                emitEffect(CreateChallengeEffect.ShowError("이 루틴은 자동 인증을 쓸 수 없어요"))
                return
            }
            if (type == VerificationType.AUTO && state.verification?.type == VerificationType.MANUAL) {
                emitEffect(CreateChallengeEffect.ShowError("수동 인증으로 바꾼 뒤에는 되돌릴 수 없어요"))
                return
            }
            dispatch(CreateChallengeReducerEvent.VerificationTypeSelected(type))
        }

        private fun create() {
            val state = currentState
            if (state.isCreating) return

            val draftId = state.draftId ?: return
            val category =
                state.category ?: run {
                    emitEffect(CreateChallengeEffect.ShowError("카테고리를 분류하지 못했어요. 초안을 다시 만들어 주세요"))
                    return
                }
            val verification =
                state.verification ?: run {
                    emitEffect(CreateChallengeEffect.ShowError("인증 방식을 불러오지 못했어요. 초안을 다시 만들어 주세요"))
                    return
                }
            val idempotencyKey = state.idempotencyKey ?: return

            val command =
                CreateChallengeCommand(
                    draftId = draftId,
                    title = state.title.trim(),
                    description = state.description.trim(),
                    category = category,
                    mode = state.mode,
                    visibility = state.visibility.takeIf { state.isGroup },
                    rankingVisible = state.rankingVisible.takeIf { !state.isGroup },
                    capacity = state.capacity.takeIf { state.isGroup },
                    minTier = state.minTier,
                    period = state.period,
                    params = state.params.toEntries(),
                    verification = verification,
                    watcherPenalty = state.penalties.watcher,
                    imageUrl = null,
                )

            val coverImageUri = state.coverImageUri?.takeIf { it.isNotBlank() }
            viewModelScope.launch {
                dispatch(CreateChallengeReducerEvent.Creating)
                runCatching {
                    // 이미지 업로드가 실패해도 생성은 막지 않는다 — 선택 항목이라 기본 이미지로 진행한다.
                    val imageUrl =
                        coverImageUri?.let { uri ->
                            runCatching { uploadChallengeImageUseCase(uri) }
                                .onFailure { emitEffect(CreateChallengeEffect.ShowError("이미지 업로드에 실패해 기본 이미지로 만들어요")) }
                                .getOrNull()
                        }
                    createChallengeUseCase(command.copy(imageUrl = imageUrl), idempotencyKey)
                }.onSuccess { created ->
                    // 진행률 API 반영 전이라도 홈에 즉시 노출되도록 로컬 스토어에 반영한다.
                    myChallengeStore.add(
                        MyChallengeSummary(
                            challengeId = created.challengeId,
                            title = command.title,
                            category = category,
                            mode = state.mode,
                            durationDays = durationDays(state.period.start, state.period.end),
                        ),
                    )
                    dispatch(CreateChallengeReducerEvent.Created(created.challengeId))

                    // 권한은 생성 이후에 받는다 — 생성 전에 받으면 만들지도 않은 방 때문에 권한을 요구하는 꼴이 된다.
                    val missing = created.verification.requiredPermissions - state.grantedPermissions
                    if (created.verification.type == VerificationType.AUTO && missing.isNotEmpty()) {
                        emitEffect(CreateChallengeEffect.RequestPermissions(missing.toList()))
                    } else {
                        goHome()
                    }
                }.onFailure { error ->
                    dispatch(CreateChallengeReducerEvent.CreateFailed)
                    val message =
                        when (error) {
                            is DraftExpiredException -> "초안이 만료됐어요. 처음부터 다시 만들어 주세요"
                            else -> error.message ?: "챌린지 생성에 실패했어요"
                        }
                    emitEffect(CreateChallengeEffect.ShowError(message))
                }
            }
        }

        /** 홈은 루트 페이지라 백스택이 비워지고 생성 플로우가 정리된다. */
        private fun goHome() {
            navigationHelper.navigateByRoute(NavRoute(AppRoutes.HOME))
        }

        private fun durationDays(
            start: String,
            end: String,
        ): Int =
            try {
                (LocalDate.parse(end).toEpochDay() - LocalDate.parse(start).toEpochDay()).toInt().coerceAtLeast(0)
            } catch (_: DateTimeParseException) {
                0
            }
    }
