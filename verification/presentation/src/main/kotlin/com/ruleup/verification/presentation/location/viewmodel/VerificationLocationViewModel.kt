package com.ruleup.verification.presentation.location.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.verification.domain.entity.AnchorSet
import com.ruleup.verification.domain.entity.LocationPin
import com.ruleup.verification.domain.entity.SetupAnchors
import com.ruleup.verification.domain.entity.SetupMissing
import com.ruleup.verification.domain.repository.VerificationRepository
import com.ruleup.verification.domain.usecase.BindLocationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 지도 핀 → 셋업(앵커 바인딩) 제출(명세 setup). 지도 탭/검색으로 확인 대기 핀을 찍고([pending]),
 * 하단 카드의 [VerificationLocationIntent.AddAnchor] 로 최대 3개까지 누적한 뒤
 * [VerificationLocationIntent.Submit] 시 setup 으로 송신한다. READY 면 앵커 전체를
 * [BindLocationUseCase] 로 OS 지오펜스 등록 후 종료한다. 탭 지점의 이름/주소는 역지오코딩으로 채운다.
 */
@HiltViewModel
class VerificationLocationViewModel
    @Inject
    constructor(
        private val verificationRepository: VerificationRepository,
        private val bindLocationUseCase: BindLocationUseCase,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<VerificationLocationIntent, VerificationLocationState, VerificationLocationReducerEvent, VerificationLocationEffect>(
            VerificationLocationState.initial,
        ) {
        // 자동완성 디바운스용: 새 키워드가 오면 진행 중 검색을 취소해 응답 역전(stale)을 막는다.
        private var searchJob: Job? = null

        // 탭 역지오코딩용: 새 탭이 오면 진행 중 조회를 취소해 늦은 응답이 핀을 덮어쓰지 않게 한다.
        private var resolveJob: Job? = null

        override fun onIntent(intent: VerificationLocationIntent) {
            when (intent) {
                is VerificationLocationIntent.Init -> init(intent)
                is VerificationLocationIntent.AddAnchor -> addAnchor()
                is VerificationLocationIntent.RemoveAnchor -> dispatch(VerificationLocationReducerEvent.AnchorRemoved(intent.index))
                is VerificationLocationIntent.Submit -> submit(intent)
                is VerificationLocationIntent.Search -> search(intent)
                is VerificationLocationIntent.ClearSearch -> clearSearch()
                is VerificationLocationIntent.TapMap -> tapMap(intent)
                is VerificationLocationIntent.SelectPlace -> selectPlace(intent)
                is VerificationLocationIntent.CancelSelection -> cancelSelection()
                is VerificationLocationIntent.Back -> navigationHelper.navigateToBack()
            }
        }

        override fun reduce(
            state: VerificationLocationState,
            event: VerificationLocationReducerEvent,
        ): VerificationLocationState =
            when (event) {
                VerificationLocationReducerEvent.CheckingDone -> state.copy(isChecking = false)
                VerificationLocationReducerEvent.Submitting -> state.copy(isSubmitting = true, missing = emptyList())
                VerificationLocationReducerEvent.Finished -> state.copy(isSubmitting = false)
                VerificationLocationReducerEvent.Searching -> state.copy(isSearching = true)
                is VerificationLocationReducerEvent.SearchLoaded -> state.copy(isSearching = false, places = event.places)
                is VerificationLocationReducerEvent.PendingSet ->
                    state.copy(
                        pending = event.pending,
                        isResolving = event.resolving,
                        // 핀을 찍으면 자동완성 목록을 닫는다(지도·카드에 집중).
                        places = if (event.pending != null) emptyList() else state.places,
                    )
                // 앵커를 담으면 확인 핀은 비워 다음 입력을 받는다.
                is VerificationLocationReducerEvent.AnchorAdded ->
                    state.copy(anchors = state.anchors + event.anchor, pending = null, isResolving = false)
                is VerificationLocationReducerEvent.AnchorRemoved ->
                    state.copy(anchors = state.anchors.filterIndexed { i, _ -> i != event.index })
                is VerificationLocationReducerEvent.MissingUpdated -> state.copy(missing = event.missing)
            }

        // 진입 게이트: 앵커 조회로 등록 여부 확인. 이미 등록돼 있으면 재등록하지 않고 종료(뒤로가기),
        // 미등록(null)일 때만 지도 등록 UI 를 연다. 조회 자체 실패는 등록을 막지 않도록 미등록처럼 진행한다.
        private fun init(intent: VerificationLocationIntent.Init) {
            viewModelScope.launch {
                val existing = runCatching { verificationRepository.getMyLocation(intent.challengeId) }.getOrNull()
                if (existing != null) {
                    emitEffect(VerificationLocationEffect.ShowMessage("이미 인증 장소가 등록돼 있어요"))
                    navigationHelper.navigateToBack()
                } else {
                    dispatch(VerificationLocationReducerEvent.CheckingDone)
                }
            }
        }

        // 지도 탭: 핀은 즉시 표시(주소는 뒤따라). 결과가 없으면 좌표만으로 진행한다.
        private fun tapMap(intent: VerificationLocationIntent.TapMap) {
            resolveJob?.cancel()
            dispatch(
                VerificationLocationReducerEvent.PendingSet(
                    PendingSelection(lat = intent.lat, lng = intent.lng, name = PLACEHOLDER_NAME, address = null),
                    resolving = true,
                ),
            )
            resolveJob =
                viewModelScope.launch {
                    val place = runCatching { verificationRepository.reverseGeocode(intent.lat, intent.lng) }.getOrNull()
                    dispatch(
                        VerificationLocationReducerEvent.PendingSet(
                            PendingSelection(
                                lat = intent.lat,
                                lng = intent.lng,
                                name = place?.name?.ifBlank { null } ?: FALLBACK_NAME,
                                address = place?.address,
                                category = place?.category,
                            ),
                            resolving = false,
                        ),
                    )
                }
        }

        // 검색 결과 선택: 진행 중 검색·역지오코딩을 끊고 그 좌표를 확인 대기 핀으로.
        private fun selectPlace(intent: VerificationLocationIntent.SelectPlace) {
            searchJob?.cancel()
            resolveJob?.cancel()
            val place = intent.place
            dispatch(
                VerificationLocationReducerEvent.PendingSet(
                    PendingSelection(
                        lat = place.lat,
                        lng = place.lng,
                        name = place.name.ifBlank { FALLBACK_NAME },
                        address = place.address,
                        category = place.category,
                    ),
                    resolving = false,
                ),
            )
        }

        private fun cancelSelection() {
            resolveJob?.cancel()
            dispatch(VerificationLocationReducerEvent.PendingSet(pending = null, resolving = false))
        }

        // 확인 핀을 앵커 목록에 담는다(최대 3). label 은 핀 이름 — 반경은 앵커의 속성이 아니다.
        private fun addAnchor() {
            val pending = currentState.pending ?: return
            if (currentState.anchors.size >= SetupAnchors.MAX_COUNT) {
                emitEffect(VerificationLocationEffect.ShowMessage("앵커는 최대 ${SetupAnchors.MAX_COUNT}개까지 추가할 수 있어요"))
                return
            }
            dispatch(
                VerificationLocationReducerEvent.AnchorAdded(
                    LocationPin(
                        lat = pending.lat,
                        lng = pending.lng,
                        label = pending.name,
                        address = pending.address,
                    ),
                ),
            )
        }

        // 누적 앵커를 setup 으로 송신. READY 면 앵커 전체를 OS 지오펜스로 등록 후 종료, PENDING_SETUP 이면 미충족 안내.
        private fun submit(intent: VerificationLocationIntent.Submit) {
            if (currentState.isSubmitting) return
            val anchors = currentState.anchors
            if (anchors.isEmpty()) {
                emitEffect(VerificationLocationEffect.ShowMessage("앵커를 1개 이상 추가해 주세요"))
                return
            }
            viewModelScope.launch {
                dispatch(VerificationLocationReducerEvent.Submitting)
                runCatching {
                    verificationRepository.setupChallenge(
                        challengeId = intent.challengeId,
                        anchors = AnchorSet.of(anchors),
                        targetPackages = intent.targetPackages,
                    )
                }.onSuccess { result ->
                    dispatch(VerificationLocationReducerEvent.Finished)
                    if (result.isReady) {
                        // 앵커 전체를 OS 지오펜스로 등록. 등록 실패는 gap 으로 보고되고 다음 reconcile 이 재시도.
                        // 반경은 서버가 정한 값을 그대로 쓴다 — 응답에 없을 때만 폴백.
                        runCatching {
                            bindLocationUseCase(
                                challengeId = intent.challengeId,
                                anchors = anchors,
                                radiusM = result.serverRadiusM ?: SetupAnchors.DEFAULT_RADIUS_M,
                                dwellMinutes = intent.dwellMinutes,
                            )
                        }
                        emitEffect(VerificationLocationEffect.ShowMessage("장소가 등록됐어요"))
                        navigationHelper.navigateToBack()
                    } else {
                        dispatch(VerificationLocationReducerEvent.MissingUpdated(result.missing))
                        emitEffect(VerificationLocationEffect.ShowMessage(missingMessage(result.missing)))
                    }
                }.onFailure { error ->
                    dispatch(VerificationLocationReducerEvent.Finished)
                    emitEffect(VerificationLocationEffect.ShowMessage(error.message ?: "셋업 제출에 실패했어요"))
                }
            }
        }

        // PENDING_SETUP 미충족 항목 → 안내 문구. 알 수 없으면 일반 안내.
        private fun missingMessage(missing: List<SetupMissing>): String {
            if (missing.isEmpty()) return "셋업이 아직 완료되지 않았어요"
            return missing.joinToString("\n") { item ->
                when (item) {
                    SetupMissing.ANCHORS_REQUIRED -> "앵커를 추가해 주세요"
                    SetupMissing.TARGET_PACKAGES_REQUIRED -> "대상 앱을 설정해 주세요"
                }
            }
        }

        private fun search(intent: VerificationLocationIntent.Search) {
            if (intent.query.isBlank()) return
            // 직전 검색을 취소하고 최신 키워드만 살린다(디바운스 + 응답 역전 방지).
            searchJob?.cancel()
            searchJob =
                viewModelScope.launch {
                    dispatch(VerificationLocationReducerEvent.Searching)
                    runCatching {
                        verificationRepository.searchPlaces(
                            query = intent.query,
                            lat = intent.lat,
                            lng = intent.lng,
                            radiusM = intent.radiusM,
                        )
                    }.onSuccess { places ->
                        // 자동완성이라 빈 결과 토스트는 띄우지 않는다(타이핑 중 방해).
                        dispatch(VerificationLocationReducerEvent.SearchLoaded(places))
                    }.onFailure { error ->
                        dispatch(VerificationLocationReducerEvent.SearchLoaded(emptyList()))
                        emitEffect(VerificationLocationEffect.ShowMessage(error.message ?: "장소 검색에 실패했어요"))
                    }
                }
        }

        private fun clearSearch() {
            searchJob?.cancel()
            dispatch(VerificationLocationReducerEvent.SearchLoaded(emptyList()))
        }

        companion object {
            // 탭 직후 주소를 채우기 전 임시 이름, 역지오코딩 실패 시 최종 이름.
            private const val PLACEHOLDER_NAME = "위치 확인 중…"
            private const val FALLBACK_NAME = "선택한 위치"
        }
    }
