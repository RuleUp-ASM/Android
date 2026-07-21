package com.ruleup.verification.data.repository

import com.ruleup.network.dto.ApiException
import com.ruleup.network.dto.getOrThrow
import com.ruleup.verification.data.api.KakaoLocalApi
import com.ruleup.verification.data.api.VerificationApi
import com.ruleup.verification.data.dto.ManualSubmitRequest
import com.ruleup.verification.data.dto.ObjectionDecisionRequest
import com.ruleup.verification.data.dto.SubmitObjectionRequest
import com.ruleup.verification.data.dto.UpdateScreenAppsRequest
import com.ruleup.verification.data.dto.buildChallengeSetupRequest
import com.ruleup.verification.data.dto.toDomain
import com.ruleup.verification.data.dto.toDto
import com.ruleup.verification.data.dto.toPlaceOrNull
import com.ruleup.verification.data.dto.toRequest
import com.ruleup.verification.domain.entity.AlreadyVerifiedException
import com.ruleup.verification.domain.entity.ChallengeSetupResult
import com.ruleup.verification.domain.entity.DeviceIntro
import com.ruleup.verification.domain.entity.EnvelopeMetadata
import com.ruleup.verification.domain.entity.FallbackLimitExceededException
import com.ruleup.verification.domain.entity.ImageRequiredException
import com.ruleup.verification.domain.entity.InvalidAnchorException
import com.ruleup.verification.domain.entity.InvalidScreenAppException
import com.ruleup.verification.domain.entity.InvalidSignalPayloadException
import com.ruleup.verification.domain.entity.LocationPin
import com.ruleup.verification.domain.entity.ManualMethod
import com.ruleup.verification.domain.entity.ManualSubmitResult
import com.ruleup.verification.domain.entity.MyLocation
import com.ruleup.verification.domain.entity.MyScreenApps
import com.ruleup.verification.domain.entity.ObjectionDecision
import com.ruleup.verification.domain.entity.ObjectionDecisionResult
import com.ruleup.verification.domain.entity.ObjectionTicket
import com.ruleup.verification.domain.entity.ObjectionType
import com.ruleup.verification.domain.entity.PendingReviews
import com.ruleup.verification.domain.entity.Place
import com.ruleup.verification.domain.entity.ProgressFilter
import com.ruleup.verification.domain.entity.ProgressSnapshot
import com.ruleup.verification.domain.entity.ScreenApp
import com.ruleup.verification.domain.entity.ScreenAppChangeCooldownException
import com.ruleup.verification.domain.entity.ScreenAppsUpdate
import com.ruleup.verification.domain.entity.SignalBatch
import com.ruleup.verification.domain.entity.SyncPolicy
import com.ruleup.verification.domain.entity.SyncResult
import com.ruleup.verification.domain.entity.SyncTooFrequentException
import com.ruleup.verification.domain.entity.VerificationDetail
import com.ruleup.verification.domain.repository.VerificationRepository
import javax.inject.Inject

class VerificationRepositoryImpl
    @Inject
    constructor(
        private val api: VerificationApi,
        private val kakaoLocalApi: KakaoLocalApi,
    ) : VerificationRepository {
        override suspend fun submitIntro(intro: DeviceIntro): SyncPolicy =
            api
                .intro(intro.toRequest())
                .getOrThrow()
                .toDomain()

        override suspend fun sync(
            metadata: EnvelopeMetadata,
            batch: SignalBatch,
        ): SyncResult =
            try {
                api
                    .sync(metadata.toRequest(batch))
                    .getOrThrow()
                    .toDomain()
            } catch (e: ApiException) {
                // 429/400 은 호출자(Worker)가 백오프·폐기로 분기할 수 있도록 도메인 예외로 변환한다(명세 §3.3).
                when (e.code) {
                    CODE_SYNC_TOO_FREQUENT -> throw SyncTooFrequentException()
                    CODE_INVALID_SIGNAL_PAYLOAD -> throw InvalidSignalPayloadException()
                    else -> throw e
                }
            }

        override suspend fun getProgress(filter: ProgressFilter): ProgressSnapshot =
            api
                .getProgress(filter.value)
                .getOrThrow()
                .toDomain()

        override suspend fun getVerificationDetail(
            challengeId: String,
            logDays: Int,
        ): VerificationDetail =
            api
                .getVerification(challengeId, logDays)
                .getOrThrow()
                .toDomain()

        override suspend fun setupChallenge(
            challengeId: String,
            anchors: List<LocationPin>,
            targetPackages: List<String>,
        ): ChallengeSetupResult =
            try {
                api
                    .setup(
                        challengeId = challengeId,
                        request = buildChallengeSetupRequest(anchors, targetPackages),
                    ).getOrThrow()
                    .toDomain()
            } catch (e: ApiException) {
                // 400 앵커 검증 실패는 화면이 입력 수정으로 분기하도록 도메인 예외로 변환(명세 setup).
                // 404/403/401 은 그대로 전파해 호출자가 메시지를 노출한다.
                when (e.code) {
                    CODE_INVALID_ANCHOR -> throw InvalidAnchorException()
                    else -> throw e
                }
            }

        override suspend fun getMyLocation(challengeId: String): MyLocation? =
            try {
                api
                    .getMyLocation(challengeId)
                    .getOrThrow()
                    .toDomain()
            } catch (e: ApiException) {
                // 앵커 미등록(400)은 정상 상태이므로 null 로 내려 호출자가 "등록 안 됨"으로 분기한다.
                // 401/403 등은 그대로 전파해 화면이 메시지를 노출한다.
                if (e.code == CODE_GEOFENCE_NOT_CONFIGURED) null else throw e
            }

        override suspend fun getMyScreenApps(challengeId: String): MyScreenApps? =
            try {
                api
                    .getMyScreenApps(challengeId)
                    .getOrThrow()
                    .toDomain()
            } catch (e: ApiException) {
                // 대상 앱 미설정(400)은 정상 상태이므로 null 로 내려 호출자가 "등록 안 됨"으로 분기한다.
                // 401/403 등은 그대로 전파해 화면이 메시지를 노출한다.
                if (e.code == CODE_SCREENTIME_NOT_CONFIGURED) null else throw e
            }

        override suspend fun updateMyScreenApps(
            challengeId: String,
            apps: List<ScreenApp>,
        ): ScreenAppsUpdate =
            try {
                api
                    .updateMyScreenApps(
                        challengeId = challengeId,
                        request = UpdateScreenAppsRequest(apps.map { it.toDto() }),
                    ).getOrThrow()
                    .toDomain()
            } catch (e: ApiException) {
                // 429 쿨다운·400 형식 위반은 화면이 안내로 분기하도록 도메인 예외로 변환(명세 my-screen-apps).
                // 그 외(403/401 등)는 그대로 전파한다.
                when (e.code) {
                    CODE_SCREENTIME_CHANGE_COOLDOWN -> throw ScreenAppChangeCooldownException()
                    CODE_INVALID_APP -> throw InvalidScreenAppException()
                    else -> throw e
                }
            }

        override suspend fun submitObjection(
            challengeId: String,
            type: ObjectionType,
            targetDate: String,
            content: String,
            imageUrl: String?,
        ): ObjectionTicket =
            api
                .submitObjection(
                    challengeId = challengeId,
                    request =
                        SubmitObjectionRequest(
                            type = type.value,
                            targetDate = targetDate,
                            content = content,
                            imageUrl = imageUrl,
                        ),
                ).getOrThrow()
                .toDomain()

        override suspend fun decideObjection(
            challengeId: String,
            objectionId: String,
            decision: ObjectionDecision,
            reason: String?,
        ): ObjectionDecisionResult =
            api
                .decideObjection(
                    challengeId = challengeId,
                    objectionId = objectionId,
                    request = ObjectionDecisionRequest(decision = decision.value, reason = reason),
                ).getOrThrow()
                .toDomain()

        override suspend fun getPendingReviews(challengeId: String): PendingReviews =
            api
                .getPendingReviews(challengeId)
                .getOrThrow()
                .toDomain()

        override suspend fun submitManual(
            challengeId: String,
            method: ManualMethod,
            targetDate: String?,
            imageUrl: String?,
            asFallback: Boolean,
        ): ManualSubmitResult =
            try {
                api
                    .submitManual(
                        challengeId = challengeId,
                        request =
                            ManualSubmitRequest(
                                method = method.value,
                                targetDate = targetDate,
                                imageUrl = imageUrl,
                                asFallback = asFallback,
                            ),
                    ).getOrThrow()
                    .toDomain()
            } catch (e: ApiException) {
                // 409 중복/한도초과·400 이미지누락은 화면이 안내·분기할 수 있도록 도메인 예외로 변환(명세 §3.4·§6.5·§9.2).
                when (e.code) {
                    CODE_ALREADY_VERIFIED -> throw AlreadyVerifiedException()
                    CODE_IMAGE_REQUIRED -> throw ImageRequiredException()
                    CODE_FALLBACK_LIMIT_EXCEEDED -> throw FallbackLimitExceededException()
                    else -> throw e
                }
            }

        // 카카오 로컬 키워드 검색을 앱에서 직접 호출(명세 §5.2). x=경도, y=위도. radiusM 없으면 전국.
        override suspend fun searchPlaces(
            query: String,
            lat: Double?,
            lng: Double?,
            radiusM: Int?,
        ): List<Place> =
            kakaoLocalApi
                .searchKeyword(
                    query = query,
                    longitude = lng,
                    latitude = lat,
                    radiusM = radiusM,
                ).toDomain()

        // 카카오 로컬 좌표→주소(명세 §5.3). x=경도, y=위도. 결과 없으면 null.
        override suspend fun reverseGeocode(
            lat: Double,
            lng: Double,
        ): Place? =
            kakaoLocalApi
                .coord2Address(longitude = lng, latitude = lat)
                .toPlaceOrNull(lat, lng)

        companion object {
            private const val CODE_SYNC_TOO_FREQUENT = "SYNC_TOO_FREQUENT"
            private const val CODE_INVALID_SIGNAL_PAYLOAD = "INVALID_SIGNAL_PAYLOAD"
            private const val CODE_ALREADY_VERIFIED = "ALREADY_VERIFIED"
            private const val CODE_IMAGE_REQUIRED = "IMAGE_REQUIRED"
            private const val CODE_FALLBACK_LIMIT_EXCEEDED = "FALLBACK_LIMIT_EXCEEDED"
            private const val CODE_INVALID_ANCHOR = "INVALID_ANCHOR"
            private const val CODE_GEOFENCE_NOT_CONFIGURED = "GEOFENCE_NOT_CONFIGURED"
            private const val CODE_SCREENTIME_NOT_CONFIGURED = "SCREENTIME_NOT_CONFIGURED"
            private const val CODE_SCREENTIME_CHANGE_COOLDOWN = "SCREENTIME_CHANGE_COOLDOWN"
            private const val CODE_INVALID_APP = "INVALID_APP"
        }
    }
