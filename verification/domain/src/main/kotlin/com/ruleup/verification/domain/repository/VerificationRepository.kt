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

/** 인증 서버 포트. */
interface VerificationRepository {
    /** Phase 0 인트로. */
    suspend fun submitIntro(intro: DeviceIntro): SyncPolicy

    /** 30분 배치 신호 + envelope 메타데이터([metadata])를 한 번에 전송하고 오늘자 평가 결과를 받는다. */
    suspend fun sync(
        metadata: EnvelopeMetadata,
        batch: SignalBatch,
    ): SyncResult

    /** 참여 중인 모든 챌린지 진행률 일괄 조회. */
    suspend fun getProgress(filter: ProgressFilter = ProgressFilter.ACTIVE): ProgressSnapshot

    /** 오늘 인증 결과. */
    suspend fun getTodayResult(challengeId: String): TodayResult

    /** 셋업(앵커·대상앱 바인딩) 제출. */
    suspend fun setupChallenge(
        challengeId: String,
        anchors: AnchorSet,
        targetPackages: List<String> = emptyList(),
    ): ChallengeSetupResult

    /** 내 인증 장소(앵커) 조회. */
    suspend fun getMyLocation(challengeId: String): MyLocation?

    /** 내 인증 장소(앵커) 교체. */
    suspend fun updateMyLocation(
        challengeId: String,
        anchors: AnchorSet,
    ): MyLocation

    /** 내 스크린타임 대상 앱 조회. */
    suspend fun getMyScreenApps(challengeId: String): MyScreenApps?

    /** 스크린타임 대상 앱 세트 교체. */
    suspend fun updateMyScreenApps(
        challengeId: String,
        apps: ScreenAppSet,
    ): ScreenAppsUpdate

    /** 인증 이의 제기. */
    suspend fun submitAppeal(
        verificationId: String,
        reason: String,
        imageUrl: String? = null,
    ): AppealReceipt

    /** 판정 결과를 봤다고 알린다. */
    suspend fun acknowledgeResult(verificationId: String)

    /** 수동 인증 취소. */
    suspend fun cancelManual(verificationId: String)

    /** 이의 증빙 사진 업로드. */
    suspend fun uploadAppealImage(imageUri: String): String

    /** 내가 낸 이의 이력. */
    suspend fun getMyAppeals(): List<AppealHistoryItem>

    /** 수동 인증 제출. */
    suspend fun submitManual(
        challengeId: String,
        targetDate: String? = null,
        note: String? = null,
    ): ManualSubmitResult

    /** 장소 검색. */
    suspend fun searchPlaces(
        query: String,
        lat: Double? = null,
        lng: Double? = null,
        radiusM: Int? = null,
    ): List<Place>

    /** 좌표 → 주소 역지오코딩. */
    suspend fun reverseGeocode(
        lat: Double,
        lng: Double,
    ): Place?
}
