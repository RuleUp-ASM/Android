package com.ruleup.verification.domain.port

import com.ruleup.verification.domain.entity.ManualMethod
import com.ruleup.verification.domain.entity.ManualSubmitResult
import com.ruleup.verification.domain.entity.Place
import com.ruleup.verification.domain.entity.ProgressFilter
import com.ruleup.verification.domain.entity.ProgressSnapshot
import com.ruleup.verification.domain.entity.SignalBatch
import com.ruleup.verification.domain.entity.SyncResult
import com.ruleup.verification.domain.entity.VerificationDetail

/**
 * 인증 서버 포트 (명세 §3). 도메인은 본 포트만 알고, data 어댑터가 ktorfit 으로 채운다.
 *
 * 실패는 예외로 전파된다. sync 는 [com.ruleup.verification.domain.entity.SyncTooFrequentException]
 * (429)·[com.ruleup.verification.domain.entity.InvalidSignalPayloadException](400) 로 분기한다.
 */
interface VerificationRepository {
    /** 30분 배치 신호를 전송하고 오늘자 평가 결과를 받는다(명세 3.1). */
    suspend fun sync(batch: SignalBatch): SyncResult

    /** 참여 중인 모든 챌린지 진행률 일괄 조회(명세 3.2). */
    suspend fun getProgress(filter: ProgressFilter = ProgressFilter.ACTIVE): ProgressSnapshot

    /** 챌린지 인증 여부 판단(검증 결과 + 실패 사유, 명세 3.3). */
    suspend fun getVerificationDetail(
        challengeId: String,
        logDays: Int = 7,
    ): VerificationDetail

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
}
