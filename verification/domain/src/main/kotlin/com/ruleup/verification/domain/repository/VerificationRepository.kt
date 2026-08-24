package com.ruleup.verification.domain.repository

import com.ruleup.verification.domain.entity.AnchorSet
import com.ruleup.verification.domain.entity.AppealHistoryItem
import com.ruleup.verification.domain.entity.AppealReceipt
import com.ruleup.verification.domain.entity.ChallengeSetupResult
import com.ruleup.verification.domain.entity.DeviceIntro
import com.ruleup.verification.domain.entity.EnvelopeMetadata
import com.ruleup.verification.domain.entity.ManualSubmitResult
import com.ruleup.verification.domain.entity.MyLocation
import com.ruleup.verification.domain.entity.MyScreenApps
import com.ruleup.verification.domain.entity.Place
import com.ruleup.verification.domain.entity.ProgressFilter
import com.ruleup.verification.domain.entity.ProgressSnapshot
import com.ruleup.verification.domain.entity.ScreenAppSet
import com.ruleup.verification.domain.entity.ScreenAppsUpdate
import com.ruleup.verification.domain.entity.SignalBatch
import com.ruleup.verification.domain.entity.SyncPolicy
import com.ruleup.verification.domain.entity.SyncResult
import com.ruleup.verification.domain.entity.TodayResult

/**
 * 인증 서버 포트 (명세 §3). 도메인은 본 포트만 알고, data 어댑터가 Retrofit 으로 채운다.
 *
 * 실패는 예외로 전파된다. sync 는 [com.ruleup.verification.domain.entity.SyncTooFrequentException]
 * (429)·[com.ruleup.verification.domain.entity.InvalidSignalPayloadException](400) 로 분기한다.
 */
interface VerificationRepository {
    /**
     * Phase 0 인트로(전송 스펙 §0.3). 정적 프로필 + 최초 권한 스냅샷을 보내고 서버 정책을 받는다.
     * 로그인 직후 1회 호출.
     */
    suspend fun submitIntro(intro: DeviceIntro): SyncPolicy

    /**
     * 30분 배치 신호 + envelope 메타데이터([metadata])를 한 번에 전송하고 오늘자 평가 결과를 받는다
     * (전송 스펙 §0.1). data 어댑터가 둘을 합쳐 §0.1 envelope 로 직렬화한다.
     */
    suspend fun sync(
        metadata: EnvelopeMetadata,
        batch: SignalBatch,
    ): SyncResult

    /** 참여 중인 모든 챌린지 진행률 일괄 조회(명세 3.2). */
    suspend fun getProgress(filter: ProgressFilter = ProgressFilter.ACTIVE): ProgressSnapshot

    /**
     * 오늘 인증 결과(명세 GET verifications/today). 방 상세의 "오늘 내 인증" 카드와 판정 결과
     * 모달이 같은 응답을 쓴다. 연속 일수와 이의 신청 가능 기한도 여기서 온다 — 잔여 횟수는 없다
     * (한도 폐기).
     */
    suspend fun getTodayResult(challengeId: String): TodayResult

    /**
     * 셋업(앵커·대상앱 바인딩) 제출(명세 setup). 모두 충족 시 [com.ruleup.verification.domain.entity.SetupStatus.READY],
     * 미충족 시 missing[] 과 함께 PENDING_SETUP. 앵커 미입력이면 [anchors] 를 비워 location 을 생략한다.
     * 앵커 개수(최대 [com.ruleup.verification.domain.entity.SetupAnchors.MAX_COUNT]개)는 [AnchorSet] 이
     * 생성 시점에 보장한다. 반경은 서버 설정 단일값이라 요청에 싣지 않는다.
     */
    suspend fun setupChallenge(
        challengeId: String,
        anchors: AnchorSet,
        targetPackages: List<String> = emptyList(),
    ): ChallengeSetupResult

    /**
     * 내 인증 장소(앵커) 조회(명세: GET /my-location). 위치 셋업/수정 재진입 시 등록 여부 판별·핀 복원용.
     * 참여(ACTIVE) 멤버만 접근한다. 바인딩된 앵커가 하나도 없으면(GEOFENCE_NOT_CONFIGURED, 미등록)
     * [null] 을 돌려준다. 그 외 실패(401/403 등)는 [com.ruleup.network.dto.ApiException] 로 전파된다.
     */
    suspend fun getMyLocation(challengeId: String): MyLocation?

    /**
     * 내 인증 장소(앵커) 교체(명세: PUT /my-location). 보낸 목록으로 세트 전체를 갈아끼운다(부분 수정 아님).
     *
     * 성공하면 **즉시 적용**되고 그 달의 변경 1회를 소진한다. 첫 설정([setupChallenge])은 소진하지 않는다.
     * 실패는 화면이 문구를 갈라야 하므로 도메인 예외로 온다 —
     * [com.ruleup.verification.domain.entity.LocationLockedInWindowException](409, 익일 재시도),
     * [com.ruleup.verification.domain.entity.SettingChangeLimitException](429, 이번 달 소진),
     * [com.ruleup.verification.domain.entity.InvalidAnchorException](400, 좌표·개수).
     *
     * 언제부터 다시 바꿀 수 있는지는 [getMyLocation] 의 `nextChangeAvailableAt` 에서 읽는다.
     */
    suspend fun updateMyLocation(
        challengeId: String,
        anchors: AnchorSet,
    ): MyLocation

    /**
     * 내 스크린타임 대상 앱 조회(명세: GET /my-screen-apps). 앱 셋업/수정 재진입 시 복원용.
     * 참여(ACTIVE) 멤버만 접근한다. 대상 앱이 하나도 없으면(SCREENTIME_NOT_CONFIGURED, 미설정)
     * [null] 을 돌려준다. 그 외 실패(401/403 등)는 [com.ruleup.network.dto.ApiException] 로 전파된다.
     */
    suspend fun getMyScreenApps(challengeId: String): MyScreenApps?

    /**
     * 스크린타임 대상 앱 세트 교체(명세: PUT /my-screen-apps). 항상 익일 00:00 부터 적용된다.
     * 쿨다운은 [com.ruleup.verification.domain.entity.ScreenAppChangeCooldownException](429) 로 분기한다.
     * 중복·개수(1~10)는 [ScreenAppSet] 이 생성 시점에 보장하고, 서버가 되돌려주는 형식 위반은
     * [com.ruleup.verification.domain.entity.InvalidScreenAppException](400) 이다.
     */
    suspend fun updateMyScreenApps(
        challengeId: String,
        apps: ScreenAppSet,
    ): ScreenAppsUpdate

    /**
     * 인증 이의 제기(명세 POST /verifications/{verificationId}/appeals).
     *
     * **판정이 없다** — 형식 요건(사유 [AppealPolicy.MIN_REASON_LENGTH]자 이상)만 맞으면 즉시 인용된다.
     * 요건 미달·기한 경과·비실패 건은 접수 자체가 되지 않고 [com.ruleup.network.dto.ApiException]
     * 으로 전파된다(`INVALID_REASON` / `APPEAL_WINDOW_CLOSED` / `NOT_FAILED`).
     */
    suspend fun submitAppeal(
        verificationId: String,
        reason: String,
        imageUrl: String? = null,
    ): AppealReceipt

    /**
     * 판정 결과를 봤다고 알린다(명세: POST /verifications/{verificationId}/ack).
     *
     * 호출하면 이후 `today` 응답에서 `unacknowledgedResult` 가 사라져 모달이 다시 뜨지 않는다.
     * **멱등이라 중복 호출이 안전하다** — 실패해도 모달은 닫고 다음 진입에 다시 띄우면 되므로
     * 호출부가 사용자에게 알릴 실패가 아니다(프론트엔드 테크스펙 4-6).
     */
    suspend fun acknowledgeResult(verificationId: String)

    /**
     * 수동 인증 취소(명세: DELETE /verifications/{verificationId}). 당일(KST) 안에서만 가능하다.
     *
     * 기한이 지나면 [com.ruleup.verification.domain.entity.CancelWindowClosedException](409) 이고,
     * 자동 판정 건에 대고 부르면 `NOT_MANUAL_VERIFICATION`(409) 이 그대로 전파된다 — 화면이
     * 자동 방에 취소 버튼을 두지 않는 것이 전제라 도메인 어휘로 올리지 않는다.
     */
    suspend fun cancelManual(verificationId: String)

    /**
     * 이의 증빙 사진 업로드(명세: POST /appeals/images). 반환된 URL 을 [submitAppeal] 의 `imageUrl` 로 넣는다.
     *
     * 사진은 **선택 항목이고 진위 확인에 쓰이지 않는다**(이의는 판정하지 않는다). 그래서 업로드 실패는
     * 도메인 어휘로 올리지 않는다 — 형식·크기 오류 어느 쪽이든 화면이 할 일은 "사진 없이 제출 가능"
     * 안내 하나로 같다.
     *
     * [imageUri] 는 플랫폼 이미지 소스(content URI 등) 문자열이고, 바이트로 읽는 것은 data 어댑터 몫이다.
     */
    suspend fun uploadAppealImage(imageUri: String): String

    /**
     * 내가 낸 이의 이력(명세: GET /users/me/appeals). 최신순.
     *
     * 접수된 건은 즉시 인용되므로 계류·기각이 없고, 형식 미달은 접수 자체가 안 되어 이력에 남지 않는다.
     * 식별자가 없는 행은 버린다 — 한 행 때문에 현황 화면 전체가 죽지 않게 한다.
     */
    suspend fun getMyAppeals(): List<AppealHistoryItem>

    /**
     * 수동 인증 제출(명세: POST /challenges/{id}/verifications). **수동 방에서만 쓴다** —
     * 자동 방의 실패 구제는 이의 제기가 담당하고, 자동 방에 대고 부르면 `NOT_MANUAL_CHALLENGE`(409)
     * 가 그대로 전파된다(화면이 그 버튼을 두지 않는 것이 전제다).
     *
     * [targetDate] 를 비우면 서버가 오늘로 잡는다. 오늘이 아닌 날짜는
     * [com.ruleup.verification.domain.entity.InvalidTargetDateException](400) 이고,
     * 같은 날 두 번째 제출은 [com.ruleup.verification.domain.entity.AlreadyVerifiedException](409) 이다.
     *
     * [note] 는 기록용 메모라 서버가 검증하지 않는다.
     */
    suspend fun submitManual(
        challengeId: String,
        targetDate: String? = null,
        note: String? = null,
    ): ManualSubmitResult

    /**
     * 장소 검색(명세 §5.2·§11.7, Naver Local 프록시). 셋업·수정 시 앵커 채우기용.
     * MANUAL=키워드만, NEARBY_BRAND=키워드+중심좌표+반경. 평가 시엔 호출하지 않는다.
     */
    suspend fun searchPlaces(
        query: String,
        lat: Double? = null,
        lng: Double? = null,
        radiusM: Int? = null,
    ): List<Place>

    /**
     * 좌표 → 주소 역지오코딩(명세 §5.3, 카카오 로컬 coord2address). 지도를 탭한 지점의 이름/주소를 채운다.
     * 결과가 없으면 null(바다·해외 등) — 호출자는 좌표만으로 선택을 진행할 수 있다.
     */
    suspend fun reverseGeocode(
        lat: Double,
        lng: Double,
    ): Place?
}
