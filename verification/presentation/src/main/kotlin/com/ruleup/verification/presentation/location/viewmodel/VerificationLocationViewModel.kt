package com.ruleup.verification.presentation.location.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.verification.domain.usecase.BindLocationUseCase
import com.ruleup.verification.domain.usecase.SearchPlacesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 지도 핀 → 멤버 좌표 지오펜스 바인딩(명세 §5). 확정 시 [BindLocationUseCase] 로 등록(재등록 포함)하고
 * 이전 화면으로 돌아간다. 미설정/권한 등 엣지케이스는 화면(LocationPickerContent)에서 처리한다.
 */
@HiltViewModel
class VerificationLocationViewModel
    @Inject
    constructor(
        private val bindLocationUseCase: BindLocationUseCase,
        private val searchPlacesUseCase: SearchPlacesUseCase,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<VerificationLocationIntent, VerificationLocationState, VerificationLocationReducerEvent, VerificationLocationEffect>(
            VerificationLocationState.initial,
        ) {
        override fun onIntent(intent: VerificationLocationIntent) {
            when (intent) {
                is VerificationLocationIntent.Confirm -> bind(intent)
                is VerificationLocationIntent.Search -> search(intent)
                is VerificationLocationIntent.SelectPlace ->
                    dispatch(
                        VerificationLocationReducerEvent.PlaceSelected(
                            SelectedPlace(
                                lat = intent.place.lat,
                                lng = intent.place.lng,
                                label = intent.place.name,
                            ),
                        ),
                    )
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
                is VerificationLocationReducerEvent.PlaceSelected -> state.copy(selected = event.selected)
            }

        private fun bind(intent: VerificationLocationIntent.Confirm) {
            if (currentState.isBinding) return
            viewModelScope.launch {
                dispatch(VerificationLocationReducerEvent.Binding)
                runCatching {
                    bindLocationUseCase(
                        challengeMemberId = intent.challengeMemberId,
                        lat = intent.lat,
                        lng = intent.lng,
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
            if (currentState.isSearching || intent.query.isBlank()) return
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
                    dispatch(VerificationLocationReducerEvent.SearchLoaded(places))
                    if (places.isEmpty()) emitEffect(VerificationLocationEffect.ShowMessage("검색 결과가 없어요"))
                }.onFailure { error ->
                    dispatch(VerificationLocationReducerEvent.SearchLoaded(emptyList()))
                    emitEffect(VerificationLocationEffect.ShowMessage(error.message ?: "장소 검색에 실패했어요"))
                }
            }
        }
    }
