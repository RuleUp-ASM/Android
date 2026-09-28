package com.ruleup.verification.domain.test

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
import com.ruleup.verification.domain.repository.VerificationRepository

/** 테스트용 [VerificationRepository]. */
class FakeVerificationRepository(
    private val progress: (() -> ProgressSnapshot)? = null,
    private val todayResult: ((String) -> TodayResult)? = null,
    private val myAppeals: (() -> List<AppealHistoryItem>)? = null,
    private val myLocation: ((String) -> MyLocation?)? = null,
    private val myScreenApps: ((String) -> MyScreenApps?)? = null,
    private val places: ((String) -> List<Place>)? = null,
    private val submitIntro: ((DeviceIntro) -> SyncPolicy)? = null,
    private val updateScreenApps: ((String, ScreenAppSet) -> ScreenAppsUpdate)? = null,
    private val submitManual: ((String, String?, String?) -> ManualSubmitResult)? = null,
    private val cancelManual: ((String) -> Unit)? = null,
) : VerificationRepository {
    /** 어떤 메서드가 몇 번 불렸는지. */
    val calls = mutableListOf<String>()

    private fun <T> answer(
        name: String,
        provider: (() -> T)?,
    ): T {
        calls += name
        return requireNotNull(provider) { "$name 을(를) 준비하지 않았다" }()
    }

    override suspend fun getProgress(filter: ProgressFilter): ProgressSnapshot = answer("getProgress", progress)

    override suspend fun getTodayResult(challengeId: String): TodayResult =
        answer("getTodayResult") { requireNotNull(todayResult)(challengeId) }

    override suspend fun getMyAppeals(): List<AppealHistoryItem> = answer("getMyAppeals", myAppeals)

    override suspend fun getMyLocation(challengeId: String): MyLocation? =
        answer("getMyLocation") { requireNotNull(myLocation)(challengeId) }

    override suspend fun getMyScreenApps(challengeId: String): MyScreenApps? =
        answer("getMyScreenApps") { requireNotNull(myScreenApps)(challengeId) }

    override suspend fun searchPlaces(
        query: String,
        lat: Double?,
        lng: Double?,
        radiusM: Int?,
    ): List<Place> = answer("searchPlaces") { requireNotNull(places)(query) }

    override suspend fun submitIntro(intro: DeviceIntro): SyncPolicy = answer("submitIntro") { requireNotNull(submitIntro)(intro) }

    override suspend fun sync(
        metadata: EnvelopeMetadata,
        batch: SignalBatch,
    ): SyncResult = throw NotImplementedError()

    override suspend fun setupChallenge(
        challengeId: String,
        anchors: AnchorSet,
        targetPackages: List<String>,
    ): ChallengeSetupResult = throw NotImplementedError()

    override suspend fun updateMyLocation(
        challengeId: String,
        anchors: AnchorSet,
    ): MyLocation = throw NotImplementedError()

    override suspend fun updateMyScreenApps(
        challengeId: String,
        apps: ScreenAppSet,
    ): ScreenAppsUpdate = answer("updateMyScreenApps") { requireNotNull(updateScreenApps)(challengeId, apps) }

    override suspend fun submitAppeal(
        verificationId: String,
        reason: String,
        imageUrl: String?,
    ): AppealReceipt = throw NotImplementedError()

    override suspend fun acknowledgeResult(verificationId: String): Unit = throw NotImplementedError()

    override suspend fun cancelManual(verificationId: String): Unit =
        answer("cancelManual") { requireNotNull(cancelManual)(verificationId) }

    override suspend fun uploadAppealImage(imageUri: String): String = throw NotImplementedError()

    override suspend fun submitManual(
        challengeId: String,
        targetDate: String?,
        note: String?,
    ): ManualSubmitResult = answer("submitManual") { requireNotNull(submitManual)(challengeId, targetDate, note) }

    override suspend fun reverseGeocode(
        lat: Double,
        lng: Double,
    ): Place? = throw NotImplementedError()
}
