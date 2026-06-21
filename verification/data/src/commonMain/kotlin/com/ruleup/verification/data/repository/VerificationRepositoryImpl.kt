package com.ruleup.verification.data.repository

import com.ruleup.network.dto.ApiException
import com.ruleup.network.dto.getOrThrow
import com.ruleup.verification.data.api.VerificationApi
import com.ruleup.verification.data.dto.ManualSubmitRequest
import com.ruleup.verification.data.dto.toDomain
import com.ruleup.verification.data.dto.toRequest
import com.ruleup.verification.domain.entity.AlreadyVerifiedException
import com.ruleup.verification.domain.entity.ImageRequiredException
import com.ruleup.verification.domain.entity.InvalidSignalPayloadException
import com.ruleup.verification.domain.entity.ManualMethod
import com.ruleup.verification.domain.entity.ManualSubmitResult
import com.ruleup.verification.domain.entity.ProgressFilter
import com.ruleup.verification.domain.entity.ProgressSnapshot
import com.ruleup.verification.domain.entity.SignalBatch
import com.ruleup.verification.domain.entity.SyncResult
import com.ruleup.verification.domain.entity.SyncTooFrequentException
import com.ruleup.verification.domain.entity.VerificationDetail
import com.ruleup.verification.domain.port.VerificationRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class VerificationRepositoryImpl(
    private val api: VerificationApi,
) : VerificationRepository {
    override suspend fun sync(batch: SignalBatch): SyncResult =
        try {
            api
                .sync(batch.toRequest())
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

    override suspend fun submitManual(
        challengeId: String,
        method: ManualMethod,
        targetDate: String?,
        imageUrl: String?,
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
                        ),
                ).getOrThrow()
                .toDomain()
        } catch (e: ApiException) {
            // 409 중복/400 이미지누락은 화면이 안내·분기할 수 있도록 도메인 예외로 변환한다(명세 §3.4·§6.5).
            when (e.code) {
                CODE_ALREADY_VERIFIED -> throw AlreadyVerifiedException()
                CODE_IMAGE_REQUIRED -> throw ImageRequiredException()
                else -> throw e
            }
        }

    companion object {
        private const val CODE_SYNC_TOO_FREQUENT = "SYNC_TOO_FREQUENT"
        private const val CODE_INVALID_SIGNAL_PAYLOAD = "INVALID_SIGNAL_PAYLOAD"
        private const val CODE_ALREADY_VERIFIED = "ALREADY_VERIFIED"
        private const val CODE_IMAGE_REQUIRED = "IMAGE_REQUIRED"
    }
}
