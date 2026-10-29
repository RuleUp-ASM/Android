package com.ruleup.verification.data.signal.geofence

// GeofencingClient는 앱 업데이트·강제 종료 시 OS 등록을 제거할 수 있다.
internal fun needsRegistrationRecovery(
    lastRegisteredAt: Long?,
    lastPackageUpdateAt: Long,
    lastUserStopAt: Long?,
    hasPendingIntent: Boolean,
): Boolean =
    lastRegisteredAt != null &&
        (
            lastPackageUpdateAt > lastRegisteredAt ||
                (lastUserStopAt != null && lastUserStopAt > lastRegisteredAt) ||
                !hasPendingIntent
        )
