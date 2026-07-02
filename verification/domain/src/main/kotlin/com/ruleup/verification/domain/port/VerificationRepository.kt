package com.ruleup.verification.domain.port

import com.ruleup.verification.domain.entity.ChallengeSetupResult
import com.ruleup.verification.domain.entity.DeviceIntro
import com.ruleup.verification.domain.entity.EnvelopeMetadata
import com.ruleup.verification.domain.entity.LocationPin
import com.ruleup.verification.domain.entity.ManualMethod
import com.ruleup.verification.domain.entity.ManualSubmitResult
import com.ruleup.verification.domain.entity.MyLocation
import com.ruleup.verification.domain.entity.Place
import com.ruleup.verification.domain.entity.ProgressFilter
import com.ruleup.verification.domain.entity.ProgressSnapshot
import com.ruleup.verification.domain.entity.SignalBatch
import com.ruleup.verification.domain.entity.SyncPolicy
import com.ruleup.verification.domain.entity.SyncResult
import com.ruleup.verification.domain.entity.VerificationDetail

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

    /** 챌린지 인증 여부 판단(검증 결과 + 실패 사유, 명세 3.3). */
    suspend fun getVerificationDetail(
        challengeId: String,
        logDays: Int = 7,
    ): VerificationDetail

    /**
     * 셋업(앵커·대상앱 바인딩) 제출(명세 setup). 모두 충족 시 [com.ruleup.verification.domain.entity.SetupStatus.READY],
     * 미충족 시 missing[] 과 함께 PENDING_SETUP. 앵커 미입력이면 [anchors] 빈 리스트로 location 을 생략한다.
     * 반경 범위(500~5000m)·개수(최대 10) 위반은 [com.ruleup.verification.domain.entity.InvalidAnchorException] 로 분기한다.
     */
    suspend fun setupChallenge(
        challengeId: String,
        anchors: List<LocationPin>,
        targetPackages: List<String> = emptyList(),
    ): ChallengeSetupResult

    /**
     * 내 인증 장소(앵커) 조회(명세: GET /my-location). 위치 셋업/수정 재진입 시 등록 여부 판별·핀 복원용.
     * 참여(ACTIVE) 멤버만 접근한다. 바인딩된 앵커가 하나도 없으면(GEOFENCE_NOT_CONFIGURED, 미등록)
     * [null] 을 돌려준다. 그 외 실패(401/403 등)는 [com.ruleup.network.dto.ApiException] 로 전파된다.
     */
    suspend fun getMyLocation(challengeId: String): MyLocation?

    /** 수동 인증 제출(명세 3.4·§9). PHOTO 는 [imageUrl] 필수. [asFallback]=true 면 예비 폴백(§9.2). */
    suspend fun submitManual(
        challengeId: String,
        method: ManualMethod,
        targetDate: String? = null,
        imageUrl: String? = null,
        asFallback: Boolean = false,
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
