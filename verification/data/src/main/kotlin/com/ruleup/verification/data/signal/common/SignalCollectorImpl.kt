package com.ruleup.verification.data.signal.common

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.ruleup.verification.data.db.geofence.LocationSampleDao
import com.ruleup.verification.data.db.geofence.LocationSampleEntity
import com.ruleup.verification.data.signal.geofence.hasFineLocation
import com.ruleup.verification.data.signal.geofence.isMockCompat
import com.ruleup.verification.data.signal.health.HealthConnectCollector
import com.ruleup.verification.data.signal.usage.UsageEventCollector
import com.ruleup.verification.domain.entity.SignalScope
import com.ruleup.verification.domain.repository.SignalCollector
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/** sync 시점 OS 신호 수집. */
@SuppressLint("MissingPermission")
class SignalCollectorImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val locationSampleDao: LocationSampleDao,
        private val usageEventCollector: UsageEventCollector,
        private val healthConnectCollector: HealthConnectCollector,
    ) : SignalCollector {
        private val fused by lazy {
            LocationServices.getFusedLocationProviderClient(context.applicationContext)
        }

        override suspend fun capture(scope: SignalScope) {
            captureLocation()
            // 대상 앱은 스코프에서 거르고 WAKE(화면·잠금해제)는 패키지 무관하게 전부 담는다.
            usageEventCollector.collect(scope.targetPackages)
            // 움직임·수면.
            if (scope.healthTargets.isNotEmpty() || scope.sleepRequested) {
                healthConnectCollector.capture(scope.healthTargets, scope.sleepRequested)
            }
        }

        private suspend fun captureLocation() {
            if (!context.hasFineLocation()) return
            val location =
                try {
                    fused
                        .getCurrentLocation(
                            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                            CancellationTokenSource().token,
                        ).await()
                } catch (e: SecurityException) {
                    null
                }
            if (location != null) {
                locationSampleDao.insert(
                    LocationSampleEntity(
                        lat = location.latitude,
                        lng = location.longitude,
                        accuracy = location.accuracy,
                        isMock = location.isMockCompat(),
                        occurredAt = location.time,
                    ),
                )
            }
        }
    }
