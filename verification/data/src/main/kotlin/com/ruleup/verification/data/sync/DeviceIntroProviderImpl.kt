package com.ruleup.verification.data.sync

import com.ruleup.domain.device.DeviceInfoProvider
import com.ruleup.verification.data.signal.common.PermissionSnapshotProvider
import com.ruleup.verification.domain.entity.DeviceIntro
import com.ruleup.verification.domain.entity.DeviceProfile
import com.ruleup.verification.domain.repository.DeviceIntroProvider
import javax.inject.Inject

/** Phase 0 인트로 페이로드 채집. */
class DeviceIntroProviderImpl
    @Inject
    constructor(
        private val deviceInfoProvider: DeviceInfoProvider,
        private val permissionSnapshotProvider: PermissionSnapshotProvider,
    ) : DeviceIntroProvider {
        override suspend fun capture(): DeviceIntro {
            val device = deviceInfoProvider.current()
            val profile =
                DeviceProfile(
                    sdkInt = device.sdkInt,
                    model = device.deviceModel,
                    lowRam = device.lowRam,
                    appVersion = device.versionName,
                )
            return DeviceIntro(profile = profile, permissions = permissionSnapshotProvider.capture())
        }
    }
