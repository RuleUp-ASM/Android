package com.ruleup.verification.data.repository

import com.ruleup.network.dto.ApiException
import com.ruleup.network.dto.getOrThrow
import com.ruleup.network.dto.throwOnError
import com.ruleup.verification.data.api.KakaoLocalApi
import com.ruleup.verification.data.api.VerificationApi
import com.ruleup.verification.data.dto.ManualSubmitRequest
import com.ruleup.verification.data.dto.SubmitAppealRequest
import com.ruleup.verification.data.dto.UpdateScreenAppsRequest
import com.ruleup.verification.data.dto.buildChallengeSetupRequest
import com.ruleup.verification.data.dto.toDomain
import com.ruleup.verification.data.dto.toDto
import com.ruleup.verification.data.dto.toPlaceOrNull
import com.ruleup.verification.data.dto.toRequest
import com.ruleup.verification.data.dto.toUpdateRequest
import com.ruleup.verification.domain.entity.AlreadyVerifiedException
import com.ruleup.verification.domain.entity.AnchorSet
import com.ruleup.verification.domain.entity.AppealReceipt
import com.ruleup.verification.domain.entity.CancelWindowClosedException
import com.ruleup.verification.domain.entity.ChallengeSetupResult
import com.ruleup.verification.domain.entity.DeviceIntro
import com.ruleup.verification.domain.entity.EnvelopeMetadata
import com.ruleup.verification.domain.entity.FallbackLimitExceededException
import com.ruleup.verification.domain.entity.ImageRequiredException
import com.ruleup.verification.domain.entity.InvalidAnchorException
import com.ruleup.verification.domain.entity.InvalidScreenAppException
import com.ruleup.verification.domain.entity.InvalidSignalPayloadException
import com.ruleup.verification.domain.entity.LocationLockedInWindowException
import com.ruleup.verification.domain.entity.ManualMethod
import com.ruleup.verification.domain.entity.ManualSubmitResult
import com.ruleup.verification.domain.entity.MyLocation
import com.ruleup.verification.domain.entity.MyScreenApps
import com.ruleup.verification.domain.entity.Place
import com.ruleup.verification.domain.entity.ProgressFilter
import com.ruleup.verification.domain.entity.ProgressSnapshot
import com.ruleup.verification.domain.entity.ScreenAppChangeCooldownException
import com.ruleup.verification.domain.entity.ScreenAppSet
import com.ruleup.verification.domain.entity.ScreenAppsUpdate
import com.ruleup.verification.domain.entity.SettingChangeLimitException
import com.ruleup.verification.domain.entity.SignalBatch
import com.ruleup.verification.domain.entity.SyncPayloadTooLargeException
import com.ruleup.verification.domain.entity.SyncPolicy
import com.ruleup.verification.domain.entity.SyncResult
import com.ruleup.verification.domain.entity.SyncTooFrequentException
import com.ruleup.verification.domain.entity.TodayResult
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
                // 429/413/400 은 호출자가 백오프·분할·폐기로 분기할 수 있도록 도메인 예외로 변환한다(명세 sync).
                when (e.code) {
                    CODE_SYNC_TOO_FREQUENT -> throw SyncTooFrequentException()
                    CODE_INVALID_SIGNAL_PAYLOAD -> throw InvalidSignalPayloadException()
                    CODE_SYNC_PAYLOAD_TOO_LARGE -> throw SyncPayloadTooLargeException()
                    else -> throw e
                }
            }

        override suspend fun getProgress(filter: ProgressFilter): ProgressSnapshot =
            api
                .getProgress(filter.value)
                .getOrThrow()
                .toDomain()

        override suspend fun getTodayResult(challengeId: String): TodayResult =
            api
                .getTodayResult(challengeId)
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
            anchors: AnchorSet,
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

        override suspend fun updateMyLocation(
            challengeId: String,
            anchors: AnchorSet,
        ): MyLocation =
            try {
                api
                    .updateMyLocation(challengeId, anchors.toUpdateRequest())
                    .getOrThrow()
                    .toDomain()
            } catch (e: ApiException) {
                // 화면이 문구를 갈라야 하는 실패만 도메인 어휘로 올린다(명세 my-location PUT).
                // 개수 초과는 AnchorSet 이 이미 막지만 서버가 되돌려주면 같은 인라인 안내로 흐른다.
                when (e.code) {
                    CODE_LOCATION_LOCKED_IN_WINDOW -> throw LocationLockedInWindowException()
                    CODE_SETTING_CHANGE_LIMIT -> throw SettingChangeLimitException()
                    CODE_INVALID_ANCHOR, CODE_ANCHOR_LIMIT_EXCEEDED -> throw InvalidAnchorException()
                    else -> throw e
                }
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
            apps: ScreenAppSet,
        ): ScreenAppsUpdate =
            try {
                api
                    .updateMyScreenApps(
                        challengeId = challengeId,
                        request = UpdateScreenAppsRequest(apps.apps.map { it.toDto() }),
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

        override suspend fun submitAppeal(
            verificationId: String,
            reason: String,
            imageUrl: String?,
        ): AppealReceipt =
            api
                .submitAppeal(
                    verificationId = verificationId,
                    request = SubmitAppealRequest(reason = reason, imageUrl = imageUrl),
                ).getOrThrow()
                .toDomain()

        override suspend fun acknowledgeResult(verificationId: String) {
            // 멱등이라 결과 플래그를 도메인으로 올리지 않는다 — 성공했으면 확인된 것이다.
            api.acknowledgeResult(verificationId).throwOnError()
        }

        override suspend fun cancelManual(verificationId: String) {
            try {
                api.cancelManual(verificationId).throwOnError()
            } catch (e: ApiException) {
                // 기한 경과만 화면이 갈라 안내한다. 자동 판정 건 취소(NOT_MANUAL_VERIFICATION)는
                // 화면이 그 버튼을 두지 않는 것이 전제라 그대로 전파한다.
                when (e.code) {
                    CODE_CANCEL_WINDOW_CLOSED -> throw CancelWindowClosedException()
                    else -> throw e
                }
            }
        }

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
            private const val CODE_SYNC_PAYLOAD_TOO_LARGE = "SYNC_PAYLOAD_TOO_LARGE"
            private const val CODE_ALREADY_VERIFIED = "ALREADY_VERIFIED"
            private const val CODE_IMAGE_REQUIRED = "IMAGE_REQUIRED"
            private const val CODE_FALLBACK_LIMIT_EXCEEDED = "FALLBACK_LIMIT_EXCEEDED"
            private const val CODE_INVALID_ANCHOR = "INVALID_ANCHOR"
            private const val CODE_ANCHOR_LIMIT_EXCEEDED = "ANCHOR_LIMIT_EXCEEDED"
            private const val CODE_LOCATION_LOCKED_IN_WINDOW = "LOCATION_LOCKED_IN_WINDOW"
            private const val CODE_SETTING_CHANGE_LIMIT = "SETTING_CHANGE_LIMIT"
            private const val CODE_CANCEL_WINDOW_CLOSED = "CANCEL_WINDOW_CLOSED"
            private const val CODE_GEOFENCE_NOT_CONFIGURED = "GEOFENCE_NOT_CONFIGURED"
            private const val CODE_SCREENTIME_NOT_CONFIGURED = "SCREENTIME_NOT_CONFIGURED"
            private const val CODE_SCREENTIME_CHANGE_COOLDOWN = "SCREENTIME_CHANGE_COOLDOWN"
            private const val CODE_INVALID_APP = "INVALID_APP"
        }
    }
