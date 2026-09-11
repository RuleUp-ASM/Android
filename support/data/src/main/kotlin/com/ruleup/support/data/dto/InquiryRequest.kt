package com.ruleup.support.data.dto

import com.ruleup.support.domain.entity.InquiryDeviceContext
import com.ruleup.support.domain.entity.InquirySubmission
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 문의 접수 요청 (명세 POST /api/v1/inquiries).
 *
 * 자동 첨부 4종은 전부 nullable 이다 — **값이 없으면 없는 대로 보낸다.** 문의를 막을 값이 아니라서
 * 여기서 non-null 로 강제하면 진단 정보 하나를 못 읽었다는 이유로 접수가 끊긴다.
 */
@Serializable
data class InquiryRequest(
    @SerialName("category")
    val category: String,
    @SerialName("body")
    val body: String,
    // 빈 배열도 보낸다 — 기본값을 주면 encodeDefaults=false 때문에 필드가 통째로 빠진다.
    @SerialName("imageUrls")
    val imageUrls: List<String>,
    @SerialName("appVersion")
    val appVersion: String? = null,
    @SerialName("osVersion")
    val osVersion: String? = null,
    @SerialName("deviceModel")
    val deviceModel: String? = null,
    @SerialName("errorLogId")
    val errorLogId: String? = null,
)

internal fun InquirySubmission.toRequest(context: InquiryDeviceContext): InquiryRequest =
    InquiryRequest(
        category = category.value,
        body = body.value,
        imageUrls = imageUrls,
        appVersion = context.appVersion,
        osVersion = context.osVersion,
        deviceModel = context.deviceModel,
        errorLogId = context.errorLogId,
    )
