package com.ruleup.verification.presentation.location.viewmodel

import com.ruleup.ui.mvi.MviEffect
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState
import com.ruleup.verification.domain.entity.LocationPin
import com.ruleup.verification.domain.entity.Place
import com.ruleup.verification.domain.entity.SetupMissing

sealed interface VerificationLocationIntent : MviIntent {
    /**
     * 화면 진입 → 앵커 조회(명세: GET /my-location)로 등록 여부 확인. 이미 등록돼 있으면 재등록하지 않고
     * 종료하고, 미등록일 때만 지도 등록 UI 를 노출한다.
     */
    data class Init(
        val challengeId: String,
    ) : VerificationLocationIntent

    /** 지도 탭 → 역지오코딩 후 확인 대기 핀(명세 §5.3). 핀은 즉시 뜨고 주소는 뒤따라 채워진다. */
    data class TapMap(
        val lat: Double,
        val lng: Double,
    ) : VerificationLocationIntent

    /** 검색 결과 선택 → 그 좌표를 확인 대기 핀으로(이름·주소 채움). */
    data class SelectPlace(
        val place: Place,
    ) : VerificationLocationIntent

    /** 하단 카드 [이 위치 추가] → 확인 대기 핀을 앵커 목록에 담는다(최대 3, 인증 정책 §1.1). */
    data object AddAnchor : VerificationLocationIntent

    data class RemoveAnchor(
        val index: Int,
    ) : VerificationLocationIntent

    /**
     * [제출] → 누적 앵커를 저장한다. **최초 등록이면 `POST /setup`, 이미 등록됐으면 `PUT /my-location`**
     * — 같은 화면이지만 다른 계약이다. 최초 설정은 월 변경 횟수를 소진하지 않고, 교체는 소진한다.
     */
    data class Submit(
        val challengeId: String,
        val dwellMinutes: Int,
        val targetPackages: List<String>,
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

    /** 검색 필의 뒤로가기 → 화면 종료. */
    data object Back : VerificationLocationIntent
}

/** 확정 대기 중인 선택 지점(탭/검색으로 찍은 핀). 하단 확인 시트에 표시된다. */
data class PendingSelection(
    val lat: Double,
    val lng: Double,
    val name: String,
    val address: String?,
    // 카카오 로컬 카테고리(검색 결과만, 예: "스포츠,레저 > 헬스장"). 시트의 카테고리 칩에 마지막 단계만 표시.
    val category: String? = null,
)

data class VerificationLocationState(
    // 진입 시 앵커 조회로 등록 여부 확인 중(완료 전에는 지도 대신 로딩 표시).
    val isChecking: Boolean = true,
    // setup 제출 진행 중(서버 송신 + READY 시 OS 등록).
    val isSubmitting: Boolean = false,
    val isSearching: Boolean = false,
    // 탭 직후 역지오코딩 진행 중(핀은 보이지만 주소·추가 버튼은 대기).
    val isResolving: Boolean = false,
    val places: List<Place> = emptyList(),
    val pending: PendingSelection? = null,
    // 제출 전까지 누적한 앵커(최대 3, 인증 정책 §1.1). label = 핀 이름.
    val anchors: List<LocationPin> = emptyList(),
    // 이미 등록된 앵커를 편집 중인가. true 면 저장이 PUT(교체)이고 월 1회를 소진한다.
    val isEditing: Boolean = false,
    // 서버가 정한 인증 반경(m). 지도 원·목록 문구가 이 값을 쓴다 — 화면이 임의 값을 그리면
    // 실제 판정 범위와 어긋난다. 아직 못 받았으면 null.
    val serverRadiusM: Float? = null,
    // 이번 달 앵커 변경이 가능한지(월 1회). 편집 진입일 때만 의미가 있다.
    val changeAvailable: Boolean = true,
    // 다음 변경 가능 시각(ISO). 소진했을 때만 채워진다.
    val nextChangeAvailableAt: String? = null,
    // 직전 제출이 PENDING_SETUP 으로 떨어졌을 때의 미충족 항목(안내용).
    val missing: List<SetupMissing> = emptyList(),
) : UiState {
    companion object {
        val initial = VerificationLocationState()
    }
}

sealed interface VerificationLocationReducerEvent : ReducerEvent {
    /** 앵커 조회 완료(미등록 확인) → 로딩 해제하고 지도 등록 UI 노출. */
    data object CheckingDone : VerificationLocationReducerEvent

    /** 등록된 앵커를 불러와 편집 모드로 연다(재진입·변경 동선). */
    data class ExistingLoaded(
        val anchors: List<LocationPin>,
        val serverRadiusM: Float?,
        val changeAvailable: Boolean,
        val nextChangeAvailableAt: String?,
    ) : VerificationLocationReducerEvent

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
