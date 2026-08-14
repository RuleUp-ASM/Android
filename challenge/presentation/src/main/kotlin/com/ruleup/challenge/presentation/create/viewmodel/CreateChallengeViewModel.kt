package com.ruleup.challenge.presentation.create.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.challenge.domain.entity.ChallengeMode
import com.ruleup.challenge.domain.entity.ChallengeVisibility
import com.ruleup.challenge.domain.entity.CreateChallengeCommand
import com.ruleup.challenge.domain.entity.DraftExpiredException
import com.ruleup.challenge.domain.entity.DraftResult
import com.ruleup.challenge.domain.entity.MyChallengeSummary
import com.ruleup.challenge.domain.entity.RecommendationRateLimitedException
import com.ruleup.challenge.domain.entity.RoutineDescription
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.challenge.domain.entity.toEntries
import com.ruleup.challenge.domain.navigation.ChallengeConfirmPage
import com.ruleup.challenge.domain.observability.ChallengeEvents
import com.ruleup.challenge.domain.observability.CreateEntry
import com.ruleup.challenge.domain.observability.CreatePath
import com.ruleup.challenge.domain.observability.DraftField
import com.ruleup.challenge.domain.repository.ChallengeRepository
import com.ruleup.challenge.domain.repository.MyChallengeStore
import com.ruleup.challenge.domain.usecase.CreateChallengeUseCase
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.event.Channel
import com.ruleup.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
        private val createChallengeUseCase: CreateChallengeUseCase,
        private val challengeRepository: ChallengeRepository,
        private val myChallengeStore: MyChallengeStore,
        private val navigationHelper: NavigationHelper,
        private val observability: Observability,
    ) : MviViewModel<CreateChallengeIntent, CreateChallengeState, CreateChallengeReducerEvent, CreateChallengeEffect>(
            CreateChallengeState.initial,
        ) {
        override fun onIntent(intent: CreateChallengeIntent) {
            when (intent) {
                CreateChallengeIntent.Load -> {
                    // 생성 전환율의 분모. 프로그래매틱 재진입에서 중복 전송되지 않게 1회로 잠근다.
                    if (!createStartLogged) {
                        createStartLogged = true
                        // TODO(entry): 진입점(홈·목록 빈 상태·탐색 빈 결과) 구분은 라우트 인자 확정 후 채운다.
                        observability.log(Channel.BUSINESS) { ChallengeEvents.createStart(CreateEntry.UNKNOWN) }
                    }
                    loadTemplates()
                }

                CreateChallengeIntent.RetryTemplates -> loadTemplates()

                is CreateChallengeIntent.SetRoutineDescription ->
                    dispatch(CreateChallengeReducerEvent.RoutineDescriptionEntered(intent.description))

                CreateChallengeIntent.SubmitDescription -> submitDescription()

                CreateChallengeIntent.CancelDrafting -> cancelDrafting()

                CreateChallengeIntent.DismissFallback ->
                    dispatch(CreateChallengeReducerEvent.FallbackDismissed)

                is CreateChallengeIntent.SelectTemplate -> selectTemplate(intent.templateId)

                is CreateChallengeIntent.SetTitle ->
                    dispatch(CreateChallengeReducerEvent.TitleEntered(intent.title))

                is CreateChallengeIntent.SetDescription ->
                    dispatch(CreateChallengeReducerEvent.DescriptionEntered(intent.description))

                is CreateChallengeIntent.SetCoverImage -> {
                    logDraftEdit(DraftField.IMAGE)
                    dispatch(CreateChallengeReducerEvent.CoverImageSelected(intent.uri))
                }

                is CreateChallengeIntent.SetMode -> {
                    if (intent.mode != currentState.original?.mode) logDraftEdit(DraftField.MODE)
                    dispatch(CreateChallengeReducerEvent.ModeSelected(intent.mode))
                }

                is CreateChallengeIntent.SetVisibility -> {
                    if (intent.visibility != currentState.original?.visibility) logDraftEdit(DraftField.VISIBILITY)
                    dispatch(CreateChallengeReducerEvent.VisibilitySelected(intent.visibility))
                }

                is CreateChallengeIntent.SetRankingVisible -> {
                    if (intent.visible != currentState.original?.rankingVisible) logDraftEdit(DraftField.RANKING_VISIBLE)
                    dispatch(CreateChallengeReducerEvent.RankingVisibleChanged(intent.visible))
                }

                is CreateChallengeIntent.SetCapacity -> {
                    if (intent.capacity != currentState.original?.capacity) logDraftEdit(DraftField.CAPACITY)
                    dispatch(CreateChallengeReducerEvent.CapacityChanged(intent.capacity))
                }

                is CreateChallengeIntent.SetMinTier -> {
                    if (intent.tier != currentState.original?.minTier) logDraftEdit(DraftField.MIN_TIER)
                    dispatch(CreateChallengeReducerEvent.MinTierChanged(intent.tier))
                }

                is CreateChallengeIntent.SetPeriod -> {
                    val origin = currentState.original?.period
                    if (intent.start != origin?.start || intent.end != origin.end) logDraftEdit(DraftField.PERIOD)
                    setPeriod(intent.start, intent.end)
                }

                is CreateChallengeIntent.SetWeeklyCount -> {
                    if (intent.count != currentState.original?.weeklyCount) logDraftEdit(DraftField.WEEKLY_COUNT)
                    dispatch(CreateChallengeReducerEvent.WeeklyCountChanged(intent.count))
                }

                is CreateChallengeIntent.EditParam -> {
                    val origin =
                        currentState.original
                            ?.params
                            ?.firstOrNull { it.key == intent.key }
                            ?.value
                    if (intent.value != origin) logDraftEdit(DraftField.PARAMS)
                    dispatch(CreateChallengeReducerEvent.ParamEdited(intent.key, intent.value))
                }

                is CreateChallengeIntent.SetVerificationType -> setVerificationType(intent.type)

                is CreateChallengeIntent.SetWatcherPenalty -> {
                    if (intent.enabled != currentState.original?.penalties?.watcher) logDraftEdit(DraftField.PENALTIES)
                    dispatch(CreateChallengeReducerEvent.WatcherPenaltyChanged(intent.enabled))
                }

                is CreateChallengeIntent.PermissionsResult -> {
                    dispatch(CreateChallengeReducerEvent.PermissionsGranted(intent.granted))
                    // 권한은 생성 이후 단계다 — 결과가 무엇이든 생성은 이미 끝났으므로 홈으로 보낸다.
                    // 미허용은 첫 판정일 전까지 인증 설정에서 다시 받을 수 있다.
                    if (currentState.createdChallengeId != null) goHome()
                }

                is CreateChallengeIntent.ConfirmTextEdit -> confirmTextEdit(intent.field)

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
                        routineDescription = event.description.take(RoutineDescription.MAX_LENGTH),
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

                CreateChallengeReducerEvent.RateLimitTicked ->
                    state.copy(retryAfterSeconds = (state.retryAfterSeconds ?: 0).minus(1).coerceAtLeast(0))

                CreateChallengeReducerEvent.RateLimitCleared ->
                    state.copy(retryAfterSeconds = null)

                CreateChallengeReducerEvent.FallbackDismissed ->
                    state.copy(fallbackMessage = null)

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
                        weeklyCount = draft.weeklyCount,
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

                is CreateChallengeReducerEvent.WeeklyCountChanged ->
                    // 범위 밖 값은 서버가 400 INVALID_WEEKLY_COUNT 로 막는다. 여기서 먼저 잘라 보낸다.
                    state.copy(
                        weeklyCount =
                            event.count.coerceIn(
                                CreateChallengeState.WEEKLY_COUNT_MIN,
                                CreateChallengeState.WEEKLY_COUNT_MAX,
                            ),
                    )

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
                runCatching { challengeRepository.getRoutineTemplates() }
                    .onSuccess { dispatch(CreateChallengeReducerEvent.TemplatesLoaded(it)) }
                    // 추천이 실패해도 설명 입력 경로는 살아 있어야 하므로 화면 전체를 에러로 만들지 않는다.
                    .onFailure { dispatch(CreateChallengeReducerEvent.TemplatesFailed) }
            }
        }

        /** 경로 B — 설명 입력. 폴백은 실패가 아니라 재입력 분기다. */
        private fun submitDescription() {
            val state = currentState
            if (!state.canSubmitDescription) return
            observability.log(Channel.BUSINESS) { ChallengeEvents.createPathSelect(CreatePath.PROMPT) }
            draftJob =
                viewModelScope.launch {
                    dispatch(CreateChallengeReducerEvent.Drafting)
                    runCatching { challengeRepository.createDraft(RoutineDescription.of(state.routineDescription)) }
                        .onSuccess { result ->
                            when (result) {
                                is DraftResult.Ok -> applyDraft(result)
                                is DraftResult.Fallback ->
                                    dispatch(CreateChallengeReducerEvent.DraftFellBack(result.message))
                            }
                        }.onFailure { error ->
                            when (error) {
                                is RecommendationRateLimitedException -> {
                                    dispatch(CreateChallengeReducerEvent.DraftRateLimited(error.retryAfterSeconds))
                                    startRateLimitCountdown()
                                }

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

        /** 초안 생성은 최대 10초까지 걸릴 수 있어 화면을 잠근다 — 대신 뒤로가기로 빠져나갈 수 있게 둔다. */
        private fun cancelDrafting() {
            if (!currentState.isDrafting) return
            draftJob?.cancel()
            dispatch(CreateChallengeReducerEvent.DraftFailed)
        }

        /**
         * 남은 제한 시간을 1초씩 깎는다. **끝나도 자동으로 재요청하지 않는다** — 버튼만 다시 열어준다.
         * 자동 재시도는 남은 rate limit 을 소진시켜 사용자를 더 오래 막는다.
         */
        private fun startRateLimitCountdown() {
            countdownJob?.cancel()
            countdownJob =
                viewModelScope.launch {
                    while (true) {
                        val remaining = currentState.retryAfterSeconds ?: break
                        if (remaining <= 0) {
                            dispatch(CreateChallengeReducerEvent.RateLimitCleared)
                            break
                        }
                        delay(COUNTDOWN_TICK_MS)
                        dispatch(CreateChallengeReducerEvent.RateLimitTicked)
                    }
                }
        }

        /** 경로 A — 추천 칩. LLM 미경유라 폴백·rate limit 이 없다. */
        private fun selectTemplate(templateId: Long) {
            if (currentState.isDrafting) return
            observability.log(Channel.BUSINESS) { ChallengeEvents.createPathSelect(CreatePath.TEMPLATE) }
            viewModelScope.launch {
                dispatch(CreateChallengeReducerEvent.Drafting)
                runCatching { challengeRepository.createDraftFromTemplate(templateId) }
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
            editedFields.clear()
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

        /**
         * 인증 방식 선택.
         *
         * **확인 화면에서는 되돌릴 수 있다.** 아직 방이 만들어지지 않았고 초안은 메모리에만 있어서,
         * 초안이 AUTO 로 온 루틴이라면 다시 AUTO 를 보내도 서버가 받는다. 계약의 "역방향 불가"는
         * *자동 인증을 지원하지 않는 루틴에 AUTO 를 요청하는 것*을 막는 규칙이고
         * (`ROUTINE_AUTO_NOT_SUPPORTED`), 단방향 잠금은 **생성 이후 수정 화면**의 규칙이다(FE 스펙 4-7).
         *
         * 여기서 잠가두면 잠깐 눌러본 사용자가 초안을 처음부터 다시 만들어야 한다.
         */
        private fun setVerificationType(type: VerificationType) {
            val state = currentState
            if (type == VerificationType.AUTO && !state.canUseAuto) {
                emitEffect(CreateChallengeEffect.ShowError("이 루틴은 자동 인증을 쓸 수 없어요"))
                return
            }
            if (type != state.original?.verification?.type) {
                logDraftEdit(
                    DraftField.VERIFICATION,
                    autoToManual = type == VerificationType.MANUAL && state.canUseAuto,
                )
            }
            dispatch(CreateChallengeReducerEvent.VerificationTypeSelected(type))
        }

        /** 포커스가 빠진 시점에 원본과 비교한다. 되돌려 원문과 같아졌으면 보내지 않는다. */
        private fun confirmTextEdit(field: TextEditField) {
            val state = currentState
            val origin = state.original ?: return
            when (field) {
                TextEditField.TITLE -> if (state.title != origin.title) logDraftEdit(DraftField.TITLE)
                TextEditField.DESCRIPTION ->
                    if (state.description != origin.description) logDraftEdit(DraftField.DESCRIPTION)
            }
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
                    weeklyCount = state.weeklyCount,
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
                            runCatching { challengeRepository.uploadImage(uri) }
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

        private var countdownJob: Job? = null
        private var draftJob: Job? = null
        private var createStartLogged = false

        // 초안 수정률은 **필드별 1회**로 센다 — 타이핑마다 보내면 수정률이 타이핑 양에 좌우된다.
        private val editedFields = mutableSetOf<DraftField>()

        /** 원본과 달라진 항목을 필드당 한 번만 기록한다. 되돌려도 취소하지 않는다(이미 만진 것은 사실이다). */
        private fun logDraftEdit(
            field: DraftField,
            autoToManual: Boolean? = null,
        ) {
            if (!editedFields.add(field)) return
            observability.log(Channel.BUSINESS) { ChallengeEvents.draftEdit(field, autoToManual) }
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

        private companion object {
            const val COUNTDOWN_TICK_MS = 1_000L
        }
    }
