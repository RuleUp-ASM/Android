package com.ruleup.support.data.dto

import com.ruleup.support.domain.entity.InquiryDeviceContext
import com.ruleup.support.domain.entity.InquirySubmission
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 문의 접수 요청. */
@Serializable
data class InquiryRequest(
    @SerialName("category")
    val category: String,
    @SerialName("body")
    val body: String,
    // 빈 배열도 보낸다
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
