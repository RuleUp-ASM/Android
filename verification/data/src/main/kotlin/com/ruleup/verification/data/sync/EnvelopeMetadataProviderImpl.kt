package com.ruleup.verification.data.sync

import android.util.Base64
import com.ruleup.domain.device.DeviceIdentityRepository
import com.ruleup.verification.data.db.common.ProgressCacheDao
import com.ruleup.verification.data.settings.VerificationSettingsStore
import com.ruleup.verification.data.signal.common.NetworkStateProvider
import com.ruleup.verification.data.signal.common.PermissionSnapshotProvider
import com.ruleup.verification.domain.entity.CoverageWindow
import com.ruleup.verification.domain.entity.EnvelopeMetadata
import com.ruleup.verification.domain.entity.GapReason
import com.ruleup.verification.domain.entity.PermissionSnapshot
import com.ruleup.verification.domain.entity.SignalGap
import com.ruleup.verification.domain.entity.SignalScope
import com.ruleup.verification.domain.repository.EnvelopeMetadataProvider
import javax.inject.Inject

/** envelope 메타데이터 채집기. */
class EnvelopeMetadataProviderImpl
    @Inject
    constructor(
        private val bootSessionProvider: BootSessionProvider,
        private val permissionSnapshotProvider: PermissionSnapshotProvider,
        private val networkStateProvider: NetworkStateProvider,
        private val integrityTokenProvider: IntegrityTokenProvider,
        private val diagnosticsProvider: DiagnosticsProvider,
        private val progressCacheDao: ProgressCacheDao,
        private val settings: VerificationSettingsStore,
        private val deviceIdentityRepository: DeviceIdentityRepository,
    ) : EnvelopeMetadataProvider {
        override suspend fun capture(scope: SignalScope): EnvelopeMetadata {
            val clock = bootSessionProvider.currentClock()
            val permissions = permissionSnapshotProvider.capture()
            val network = networkStateProvider.current()
            val nonce = integrityNonce(clock.bootSessionId, clock.deviceTimeMillis)
            val integrity = integrityTokenProvider.snapshot(nonce)
            val diagnostics = diagnosticsProvider.snapshot()
            val activeChallengeIds =
                scope.activeChallengeIds?.toList() ?: runCatching { progressCacheDao.allChallengeIds() }.getOrDefault(emptyList())
            val windowFrom = settings.lastSuccessfulFlushAt() ?: (clock.deviceTimeMillis - DEFAULT_WINDOW_MS)
            // 첫 전송은 기본 창만큼만 선언한다.
            val coveredFrom =
                (settings.lastCoveredUntil() ?: (clock.deviceTimeMillis - DEFAULT_WINDOW_MS))
                    .coerceAtMost(clock.deviceTimeMillis)

            return EnvelopeMetadata(
                clock = clock,
                deviceId = deviceIdentityRepository.current().deviceId,
                activeChallengeIds = activeChallengeIds,
                permissions = permissions,
                network = network,
                integrity = integrity,
                diagnostics = diagnostics,
                gaps = permissionGaps(scope, permissions, windowFrom, clock.deviceTimeMillis),
                coverage = CoverageWindow(from = coveredFrom, until = clock.deviceTimeMillis),
            )
        }

        override suspend fun markCovered(until: Long) {
            settings.setLastCoveredUntil(until)
        }

        /** 스코프에 든 신호의 권한이 빠졌으면 PERMISSION_MISSING(recoverable=true) gap 으로 보고. */
        private fun permissionGaps(
            scope: SignalScope,
            perms: PermissionSnapshot,
            from: Long,
            to: Long,
        ): List<SignalGap> =
            buildList {
                fun gap(type: String) = add(SignalGap(type, GapReason.PERMISSION_MISSING, from, to, recoverable = true))

                val geofenceActive = scope.activeRequestIds.isNotEmpty()
                if (geofenceActive &&
                    (perms.location.isDenied || perms.backgroundLocation.isDenied)
                ) {
                    gap(SIGNAL_GEOFENCE)
                }
                if (scope.targetPackages.isNotEmpty() && perms.usageStats.isDenied) {
                    gap(SIGNAL_SCREEN_TIME)
                }
                if (scope.healthTargets.isNotEmpty() &&
                    perms.healthDistance.isDenied &&
                    perms.healthSteps.isDenied
                ) {
                    gap(SIGNAL_HEALTH)
                }
                if (scope.sleepRequested && perms.healthSleep.isDenied) {
                    gap(SIGNAL_SLEEP)
                }
            }

        // 클라 nonce(bootSession + 시각).
        private fun integrityNonce(
            bootSessionId: String,
            deviceTimeMillis: Long,
        ): String = Base64.encodeToString("$bootSessionId:$deviceTimeMillis".toByteArray(), Base64.NO_WRAP or Base64.URL_SAFE)

        private companion object {
            const val DEFAULT_WINDOW_MS = 30L * 60 * 1000
            const val SIGNAL_GEOFENCE = "GEOFENCE"
            const val SIGNAL_SCREEN_TIME = "SCREEN_TIME"
            const val SIGNAL_HEALTH = "HEALTH"
            const val SIGNAL_SLEEP = "SLEEP"
        }
    }
