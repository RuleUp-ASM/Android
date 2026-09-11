package com.ruleup.support.data.repository

import com.ruleup.network.dto.ApiException
import com.ruleup.network.dto.getOrThrow
import com.ruleup.network.dto.requireField
import com.ruleup.network.image.ImageReader
import com.ruleup.support.data.api.InquiryApi
import com.ruleup.support.data.dto.toDomain
import com.ruleup.support.data.dto.toInquiryFailure
import com.ruleup.support.data.dto.toRequest
import com.ruleup.support.domain.entity.InquiryDetail
import com.ruleup.support.domain.entity.InquiryException
import com.ruleup.support.domain.entity.InquiryFailure
import com.ruleup.support.domain.entity.InquiryReceipt
import com.ruleup.support.domain.entity.InquirySubmission
import com.ruleup.support.domain.entity.InquirySummary
import com.ruleup.support.domain.repository.DeviceContextProvider
import com.ruleup.support.domain.repository.InquiryRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject

class InquiryRepositoryImpl
    @Inject
    constructor(
        private val api: InquiryApi,
        private val deviceContextProvider: DeviceContextProvider,
        private val imageReader: ImageReader,
    ) : InquiryRepository {
        override suspend fun submit(submission: InquirySubmission): InquiryReceipt =
            translating {
                // 진단 정보 채집이 실패해도 접수는 나가야 한다 — provider 가 빈 값을 채워 돌려준다.
                val context = deviceContextProvider.capture()
                api
                    .submit(submission.toRequest(context))
                    .getOrThrow()
                    .toDomain()
            }

        override suspend fun getInquiries(): List<InquirySummary> =
            translating {
                api.getInquiries().getOrThrow().toDomain()
            }

        override suspend fun getInquiry(inquiryId: String): InquiryDetail =
            translating {
                api.getInquiry(inquiryId).getOrThrow().toDomain()
            }

        override suspend fun uploadImage(imageUri: String): String =
            translating {
                val image = imageReader.read(imageUri)
                val part =
                    MultipartBody.Part.createFormData(
                        name = "image",
                        filename = "inquiry_image",
                        body = image.bytes.toRequestBody(image.mimeType.toMediaType()),
                    )
                api
                    .uploadImage(part)
                    .getOrThrow()
                    .imageUrl
                    .requireField("imageUrl")
            }

        /**
         * 모든 실패를 [InquiryException] 하나로 모은다. 화면이 `ApiException` 코드 문자열을 읽지
         * 않게 하려는 것이고, [IOException] 을 따로 잡는 이유는 "다시 시도"를 권할 수 있는
         * 실패인지가 거기서 갈리기 때문이다 — 서버가 거절한 것과 아예 닿지 못한 것은 다르다.
         *
         * 서버 문구를 그대로 싣는다. 접수 상한·길이 초과는 서버가 사용자 문장으로 내려주고,
         * 여기서 다시 쓰면 같은 규칙이 두 곳에 살아 한쪽만 고쳐진다.
         */
        private inline fun <T> translating(block: () -> T): T =
            try {
                block()
            } catch (e: ApiException) {
                throw InquiryException(e.toInquiryFailure(), e.message.orEmpty(), e)
            } catch (e: IOException) {
                throw InquiryException(InquiryFailure.NETWORK, "지금은 연결이 불안정해요. 잠시 후 다시 시도해 주세요.", e)
            }
    }
