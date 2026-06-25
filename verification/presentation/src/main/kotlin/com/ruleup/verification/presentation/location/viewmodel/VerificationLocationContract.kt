package com.ruleup.verification.presentation.location.viewmodel

import com.ruleup.ui.mvi.MviEffect
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState
import com.ruleup.verification.domain.entity.Place

sealed interface VerificationLocationIntent : MviIntent {
    /** 지도 핀 확정(명세 §5.3 결과 {lat,lng,radiusM,label}). */
    data class Confirm(
        val challengeMemberId: String,
        val lat: Double,
        val lng: Double,
        val radiusM: Float,
        val dwellMinutes: Int,
        // 표시·서버 전송용(로컬 지오펜스에는 미사용).
        val label: String,
    ) : VerificationLocationIntent

    /** 장소 검색(명세 §5.2·§11.7). 키워드(+중심·반경)로 앵커 후보를 받아온다. */
    data class Search(
        val query: String,
        val lat: Double? = null,
        val lng: Double? = null,
        val radiusM: Int? = null,
    ) : VerificationLocationIntent

    /** 검색 결과 선택 → 지도 중심을 그 좌표로 이동(핀 채우기). */
    data class SelectPlace(
        val place: Place,
    ) : VerificationLocationIntent
}

/** 검색에서 고른 앵커 후보(지도 중심 이동용). */
data class SelectedPlace(
    val lat: Double,
    val lng: Double,
    val label: String,
)

data class VerificationLocationState(
    val isBinding: Boolean = false,
    val isSearching: Boolean = false,
    val places: List<Place> = emptyList(),
    val selected: SelectedPlace? = null,
) : UiState {
    companion object {
        val initial = VerificationLocationState()
    }
}

sealed interface VerificationLocationReducerEvent : ReducerEvent {
    data object Binding : VerificationLocationReducerEvent

    data object Finished : VerificationLocationReducerEvent

    data object Searching : VerificationLocationReducerEvent

    data class SearchLoaded(
        val places: List<Place>,
    ) : VerificationLocationReducerEvent

    data class PlaceSelected(
        val selected: SelectedPlace,
    ) : VerificationLocationReducerEvent
}

sealed interface VerificationLocationEffect : MviEffect {
    data class ShowMessage(
        val message: String,
    ) : VerificationLocationEffect
}
