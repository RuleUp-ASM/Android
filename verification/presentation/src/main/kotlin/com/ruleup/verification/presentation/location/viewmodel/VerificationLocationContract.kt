package com.ruleup.verification.presentation.location.viewmodel

import com.ruleup.ui.mvi.MviEffect
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState
import com.ruleup.verification.domain.entity.Place

sealed interface VerificationLocationIntent : MviIntent {
    /** 지도 탭 → 역지오코딩 후 확인 대기 핀(명세 §5.3). 핀은 즉시 뜨고 주소는 뒤따라 채워진다. */
    data class TapMap(
        val lat: Double,
        val lng: Double,
    ) : VerificationLocationIntent

    /** 검색 결과 선택 → 그 좌표를 확인 대기 핀으로(이름·주소 채움). */
    data class SelectPlace(
        val place: Place,
    ) : VerificationLocationIntent

    /** 하단 카드 [이 위치 선택] → 멤버 좌표 지오펜스 등록 후 종료(명세 §5.3 결과 {lat,lng,radiusM}). */
    data class Confirm(
        val challengeMemberId: String,
        val radiusM: Float,
        val dwellMinutes: Int,
    ) : VerificationLocationIntent

    /** 하단 카드 [취소] → 핀/카드 제거. */
    data object CancelSelection : VerificationLocationIntent

    /** 장소 검색(명세 §5.2·§11.7). 키워드(+중심·반경)로 앵커 후보를 받아온다. */
    data class Search(
        val query: String,
        val lat: Double? = null,
        val lng: Double? = null,
        val radiusM: Int? = null,
    ) : VerificationLocationIntent

    /** 입력칸이 비면 자동완성 목록을 닫는다. */
    data object ClearSearch : VerificationLocationIntent
}

/** 확정 대기 중인 선택 지점(탭/검색으로 찍은 핀). 하단 확인 카드에 표시된다. */
data class PendingSelection(
    val lat: Double,
    val lng: Double,
    val name: String,
    val address: String?,
)

data class VerificationLocationState(
    val isBinding: Boolean = false,
    val isSearching: Boolean = false,
    // 탭 직후 역지오코딩 진행 중(핀은 보이지만 주소·확정 버튼은 대기).
    val isResolving: Boolean = false,
    val places: List<Place> = emptyList(),
    val pending: PendingSelection? = null,
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

    /** 확인 대기 핀 설정/해제. [resolving]=true 면 주소를 채우는 중(탭 직후). */
    data class PendingSet(
        val pending: PendingSelection?,
        val resolving: Boolean,
    ) : VerificationLocationReducerEvent
}

sealed interface VerificationLocationEffect : MviEffect {
    data class ShowMessage(
        val message: String,
    ) : VerificationLocationEffect
}
