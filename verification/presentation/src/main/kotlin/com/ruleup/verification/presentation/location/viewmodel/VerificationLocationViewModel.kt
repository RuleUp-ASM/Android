package com.ruleup.verification.presentation.location.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.verification.domain.usecase.BindLocationUseCase
import com.ruleup.verification.domain.usecase.ReverseGeocodeUseCase
import com.ruleup.verification.domain.usecase.SearchPlacesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 지도 핀 → 멤버 좌표 지오펜스 바인딩(명세 §5). 지도 탭/검색으로 확인 대기 핀을 찍고([pending]),
 * 하단 카드의 [VerificationLocationIntent.Confirm] 시 [BindLocationUseCase] 로 등록(재등록 포함) 후 종료한다.
 * 탭 지점의 이름/주소는 [ReverseGeocodeUseCase] 로 채운다.
 */
@HiltViewModel
class VerificationLocationViewModel
    @Inject
    constructor(
        private val bindLocationUseCase: BindLocationUseCase,
        private val searchPlacesUseCase: SearchPlacesUseCase,
        private val reverseGeocodeUseCase: ReverseGeocodeUseCase,
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
                is VerificationLocationIntent.Confirm -> bind(intent)
                is VerificationLocationIntent.Search -> search(intent)
                is VerificationLocationIntent.ClearSearch -> clearSearch()
                is VerificationLocationIntent.TapMap -> tapMap(intent)
                is VerificationLocationIntent.SelectPlace -> selectPlace(intent)
                is VerificationLocationIntent.CancelSelection -> cancelSelection()
            }
        }

        override fun reduce(
            state: VerificationLocationState,
            event: VerificationLocationReducerEvent,
        ): VerificationLocationState =
            when (event) {
                VerificationLocationReducerEvent.Binding -> state.copy(isBinding = true)
                VerificationLocationReducerEvent.Finished -> state.copy(isBinding = false)
                VerificationLocationReducerEvent.Searching -> state.copy(isSearching = true)
                is VerificationLocationReducerEvent.SearchLoaded -> state.copy(isSearching = false, places = event.places)
                is VerificationLocationReducerEvent.PendingSet ->
                    state.copy(
                        pending = event.pending,
                        isResolving = event.resolving,
                        // 핀을 찍으면 자동완성 목록을 닫는다(지도·카드에 집중).
                        places = if (event.pending != null) emptyList() else state.places,
                    )
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
                    val place = runCatching { reverseGeocodeUseCase(intent.lat, intent.lng) }.getOrNull()
                    dispatch(
                        VerificationLocationReducerEvent.PendingSet(
                            PendingSelection(
                                lat = intent.lat,
                                lng = intent.lng,
                                name = place?.name?.ifBlank { null } ?: FALLBACK_NAME,
                                address = place?.address,
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
                    ),
                    resolving = false,
                ),
            )
        }

        private fun cancelSelection() {
            resolveJob?.cancel()
            dispatch(VerificationLocationReducerEvent.PendingSet(pending = null, resolving = false))
        }

        private fun bind(intent: VerificationLocationIntent.Confirm) {
            val pending = currentState.pending ?: return
            if (currentState.isBinding) return
            viewModelScope.launch {
                dispatch(VerificationLocationReducerEvent.Binding)
                runCatching {
                    bindLocationUseCase(
                        challengeMemberId = intent.challengeMemberId,
                        lat = pending.lat,
                        lng = pending.lng,
                        radiusM = intent.radiusM,
                        dwellMinutes = intent.dwellMinutes,
                    )
                }.onSuccess {
                    dispatch(VerificationLocationReducerEvent.Finished)
                    emitEffect(VerificationLocationEffect.ShowMessage("장소가 등록됐어요"))
                    navigationHelper.navigateToBack()
                }.onFailure { error ->
                    dispatch(VerificationLocationReducerEvent.Finished)
                    emitEffect(VerificationLocationEffect.ShowMessage(error.message ?: "장소 등록에 실패했어요"))
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
                        searchPlacesUseCase(
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
