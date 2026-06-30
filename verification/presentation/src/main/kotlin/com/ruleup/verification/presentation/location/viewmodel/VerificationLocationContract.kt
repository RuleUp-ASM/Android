package com.ruleup.verification.presentation.location.viewmodel

import com.ruleup.ui.mvi.MviEffect
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState
import com.ruleup.verification.domain.entity.LocationPin
import com.ruleup.verification.domain.entity.Place
import com.ruleup.verification.domain.entity.SetupMissing

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

    /** 하단 카드 [이 위치 추가] → 확인 대기 핀을 앵커 목록에 담는다(최대 10, 명세 setup). */
    data class AddAnchor(
        val radiusM: Float,
    ) : VerificationLocationIntent

    /** 추가된 앵커 1개 제거. */
    data class RemoveAnchor(
        val index: Int,
    ) : VerificationLocationIntent

    /** [제출] → 누적 앵커를 setup 으로 송신(명세 setup). READY 면 첫 앵커를 OS 지오펜스로 등록 후 종료. */
    data class Submit(
        val challengeId: String,
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
    // setup 제출 진행 중(서버 송신 + READY 시 OS 등록).
    val isSubmitting: Boolean = false,
    val isSearching: Boolean = false,
    // 탭 직후 역지오코딩 진행 중(핀은 보이지만 주소·추가 버튼은 대기).
    val isResolving: Boolean = false,
    val places: List<Place> = emptyList(),
    val pending: PendingSelection? = null,
    // 제출 전까지 누적한 앵커(최대 10, 명세 setup). label = 핀 이름, radiusM = 화면 기본값.
    val anchors: List<LocationPin> = emptyList(),
    // 직전 제출이 PENDING_SETUP 으로 떨어졌을 때의 미충족 항목(안내용).
    val missing: List<SetupMissing> = emptyList(),
) : UiState {
    companion object {
        val initial = VerificationLocationState()
    }
}

sealed interface VerificationLocationReducerEvent : ReducerEvent {
    data object Submitting : VerificationLocationReducerEvent

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

    /** 확인 대기 핀을 앵커로 추가(추가 후 pending 해제). */
    data class AnchorAdded(
        val anchor: LocationPin,
    ) : VerificationLocationReducerEvent

    /** 앵커 1개 제거. */
    data class AnchorRemoved(
        val index: Int,
    ) : VerificationLocationReducerEvent

    /** PENDING_SETUP 미충족 항목 갱신(제출 성공 시 비움). */
    data class MissingUpdated(
        val missing: List<SetupMissing>,
    ) : VerificationLocationReducerEvent
}

sealed interface VerificationLocationEffect : MviEffect {
    data class ShowMessage(
        val message: String,
    ) : VerificationLocationEffect
}
