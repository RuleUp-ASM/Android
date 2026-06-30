package com.ruleup.verification.data.sync

import android.util.Base64
import com.ruleup.verification.data.db.ProgressCacheDao
import com.ruleup.verification.data.settings.VerificationSettingsStore
import com.ruleup.verification.data.signal.NetworkStateProvider
import com.ruleup.verification.data.signal.PermissionSnapshotProvider
import com.ruleup.verification.domain.entity.EnvelopeMetadata
import com.ruleup.verification.domain.entity.GapReason
import com.ruleup.verification.domain.entity.PermissionSnapshot
import com.ruleup.verification.domain.entity.PermissionState
import com.ruleup.verification.domain.entity.SignalGap
import com.ruleup.verification.domain.entity.SignalScope
import com.ruleup.verification.domain.port.EnvelopeMetadataProvider
import javax.inject.Inject

/**
 * envelope 메타데이터 채집기(전송 스펙 §0.1). 디바이스 시계(부팅 세션·monotonic), 권한 스냅샷,
 * VPN, Play Integrity, 진단 heartbeat, 활성 챌린지 id, 권한 부재 기반 gap 을 한 번에 모은다.
 *
 * 권한 부재 gap: 스코프에 든 신호인데 그 권한이 DENIED 면 PERMISSION_MISSING 을 계산형 gap 으로 넣어,
 * 서버가 신호 부재를 "권한 없음"으로 구분하게 한다(§0.5). 수집기가 적재한 버퍼형 gap 은 유스케이스가 합친다.
 */
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
    ) : EnvelopeMetadataProvider {
        override suspend fun capture(scope: SignalScope): EnvelopeMetadata {
            val clock = bootSessionProvider.currentClock()
            val permissions = permissionSnapshotProvider.capture()
            val network = networkStateProvider.current()
            val nonce = integrityNonce(clock.bootSessionId, clock.deviceTimeMillis)
            val integrity = integrityTokenProvider.snapshot(nonce)
            val diagnostics = diagnosticsProvider.snapshot()
            val activeChallengeIds = runCatching { progressCacheDao.allChallengeIds() }.getOrDefault(emptyList())
            val windowFrom = settings.lastSuccessfulFlushAt() ?: (clock.deviceTimeMillis - DEFAULT_WINDOW_MS)

            return EnvelopeMetadata(
                clock = clock,
                activeChallengeIds = activeChallengeIds,
                permissions = permissions,
                network = network,
                integrity = integrity,
                diagnostics = diagnostics,
                gaps = permissionGaps(scope, permissions, windowFrom, clock.deviceTimeMillis),
            )
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
                    (perms.location == PermissionState.DENIED || perms.backgroundLocation == PermissionState.DENIED)
                ) {
                    gap(SIGNAL_GEOFENCE)
                }
                if (scope.targetPackages.isNotEmpty() && perms.usageStats == PermissionState.DENIED) {
                    gap(SIGNAL_SCREEN_TIME)
                }
                if (scope.healthTargets.isNotEmpty() &&
                    perms.healthDistance == PermissionState.DENIED &&
                    perms.healthSteps == PermissionState.DENIED
                ) {
                    gap(SIGNAL_HEALTH)
                }
                if (scope.sleepRequested && perms.healthSleep == PermissionState.DENIED) {
                    gap(SIGNAL_SLEEP)
                }
            }

        // 클라 nonce(bootSession + 시각). 정식 운영은 서버 발급 nonce 바인딩 필요(§6.5, Phase 2).
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
