package com.ruleup.verification.data.dto

import com.ruleup.verification.domain.entity.MyScreenApps
import com.ruleup.verification.domain.entity.PendingScreenApps
import com.ruleup.verification.domain.entity.ScreenApp
import com.ruleup.verification.domain.entity.ScreenAppsUpdate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// my-screen-apps

@Serializable
data class ScreenAppResponse(
    @SerialName("packageName")
    val packageName: String,
    @SerialName("appName")
    val appName: String,
)

@Serializable
data class PendingScreenAppsResponse(
    @SerialName("apps")
    val apps: List<ScreenAppResponse>? = null,
    @SerialName("effectiveFrom")
    val effectiveFrom: String? = null,
)

@Serializable
data class MyScreenAppsResponse(
    @SerialName("apps")
    val apps: List<ScreenAppResponse>? = null,
    @SerialName("appliedFrom")
    val appliedFrom: String? = null,
    @SerialName("pending")
    val pending: PendingScreenAppsResponse? = null,
)

/** PUT 응답: 접수된 세트 + 익일 적용 시각 + 다음 변경 가능 시각. */
@Serializable
data class UpdateScreenAppsResponse(
    @SerialName("apps")
    val apps: List<ScreenAppResponse>? = null,
    @SerialName("appliedFrom")
    val appliedFrom: String? = null,
    // 저장으로 월 1회를 소진하므로 항상 내려온다(다음 달 1일 00:00 KST)
    @SerialName("nextChangeAvailableAt")
    val nextChangeAvailableAt: String? = null,
)

internal fun ScreenAppResponse.toDomain(): ScreenApp =
    ScreenApp(
        packageName = packageName,
        appName = appName,
    )

internal fun MyScreenAppsResponse.toDomain(): MyScreenApps =
    MyScreenApps(
        apps = apps.orEmpty().map { it.toDomain() },
        appliedFrom = appliedFrom,
        pending =
            pending?.let { p ->
                PendingScreenApps(
                    apps = p.apps.orEmpty().map { it.toDomain() },
                    effectiveFrom = p.effectiveFrom.orEmpty(),
                )
            },
    )

internal fun UpdateScreenAppsResponse.toDomain(): ScreenAppsUpdate =
    ScreenAppsUpdate(
        apps = apps.orEmpty().map { it.toDomain() },
        nextChangeAvailableAt = nextChangeAvailableAt,
        appliedFrom = appliedFrom.orEmpty(),
    )
