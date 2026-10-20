package com.ruleup.android_ruleup.tti

import com.ruleup.domain.device.DeviceInfo
import com.ruleup.domain.time.ServiceDate
import com.ruleup.tti.domain.TtiRecord
import com.ruleup.tti.domain.TtiTimeline
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.format.DateTimeFormatter

/** 시트 한 행. 키 이름은 Apps Script doPost 가 읽는 이름과 같아야 한다(#623). */
@Serializable
internal data class TtiSheetRow(
    val recordedAt: String,
    val pageName: String,
    val viewCreate: Long,
    val backend: Long,
    val viewBinding: Long,
    val bigPartLoading: Long,
    val totalMillis: Long,
    val appVersion: String,
    val buildType: String,
    val deviceModel: String,
    val os: String,
)

/** 네 구간이 다 닫히지 않았으면 null — 빈 칸을 0 으로 채우면 빠른 화면처럼 보인다. */
internal fun TtiRecord.toSheetRow(
    device: DeviceInfo,
    buildType: String,
): TtiSheetRow? {
    val total = totalTimeMillis ?: return null
    val millis = TtiTimeline.entries.associateWith { spans[it]?.durationMillis ?: return null }
    return TtiSheetRow(
        recordedAt = RECORDED_AT_FORMAT.format(Instant.ofEpochMilli(createdAt).atZone(ServiceDate.ZONE)),
        pageName = pageName,
        viewCreate = millis.getValue(TtiTimeline.VIEW_CREATE),
        backend = millis.getValue(TtiTimeline.BACKEND),
        viewBinding = millis.getValue(TtiTimeline.VIEW_BINDING),
        bigPartLoading = millis.getValue(TtiTimeline.BIG_PART_LOADING),
        totalMillis = total,
        appVersion = "${device.versionName} (${device.versionCode})",
        buildType = buildType,
        deviceModel = "${device.manufacturer} ${device.deviceModel}",
        os = "Android ${device.osVersion} (SDK ${device.sdkInt})",
    )
}

private val RECORDED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
