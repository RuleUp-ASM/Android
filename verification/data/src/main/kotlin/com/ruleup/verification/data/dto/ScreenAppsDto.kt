package com.ruleup.verification.data.dto

import com.ruleup.verification.domain.entity.MyScreenApps
import com.ruleup.verification.domain.entity.PendingScreenApps
import com.ruleup.verification.domain.entity.ScreenApp
import com.ruleup.verification.domain.entity.ScreenAppsUpdate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- my-screen-apps (명세: GET·PUT /my-screen-apps) ----------

@Serializable
data class ScreenAppDto(
    @SerialName("packageName")
    val packageName: String,
    @SerialName("appName")
    val appName: String,
)

@Serializable
data class PendingScreenAppsDto(
    @SerialName("apps")
    val apps: List<ScreenAppDto>? = null,
    @SerialName("effectiveFrom")
    val effectiveFrom: String? = null,
)

@Serializable
data class MyScreenAppsResponse(
    @SerialName("apps")
    val apps: List<ScreenAppDto>? = null,
    @SerialName("appliedFrom")
    val appliedFrom: String? = null,
    @SerialName("pending")
    val pending: PendingScreenAppsDto? = null,
)

/** PUT 요청 본문(명세): `{ apps: [{ packageName, appName }] }` (1~10개). */
@Serializable
data class UpdateScreenAppsRequest(
    @SerialName("apps")
    val apps: List<ScreenAppDto>,
)

/** PUT 응답(명세): 접수된 세트 + 익일 적용 시각. */
@Serializable
data class UpdateScreenAppsResponse(
    @SerialName("apps")
    val apps: List<ScreenAppDto>? = null,
    @SerialName("appliedFrom")
    val appliedFrom: String? = null,
)

internal fun ScreenAppDto.toDomain(): ScreenApp =
    ScreenApp(
        packageName = packageName,
        appName = appName,
    )

internal fun ScreenApp.toDto(): ScreenAppDto =
    ScreenAppDto(
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
        appliedFrom = appliedFrom.orEmpty(),
    )
