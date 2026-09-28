package com.ruleup.verification.data.dto

import com.ruleup.verification.domain.entity.ScreenApp
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScreenAppRequest(
    @SerialName("packageName")
    val packageName: String,
    @SerialName("appName")
    val appName: String,
)

/** PUT 요청 본문: `{ apps: [{ packageName, appName }] }` (1~10개). */
@Serializable
data class UpdateScreenAppsRequest(
    @SerialName("apps")
    val apps: List<ScreenAppRequest>,
)

internal fun ScreenApp.toRequest(): ScreenAppRequest =
    ScreenAppRequest(
        packageName = packageName,
        appName = appName,
    )
