package com.ruleup.verification.data.signal

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.ruleup.verification.data.db.LocationSampleDao
import com.ruleup.verification.data.db.LocationSampleEntity
import com.ruleup.verification.domain.entity.SignalScope
import com.ruleup.verification.domain.port.SignalCollector
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * sync 시점 OS 신호 수집(명세 §2.3). 보조 측위 단발(isMock 포함) + UsageStats 델타 + 움직임/수면(HEALTH·SLEEP).
 * 지오펜스 전이는 리시버가 이미 적재한다. 연속 측위(requestLocationUpdates)는 쓰지 않는다(배터리, 명세 §2.3).
 *
 * 측위/UsageStats 호출은 모두 런타임 권한 가드 뒤에서 일어나므로 MissingPermission lint 를 클래스 단위로 억제한다.
 */
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
            // SCREEN_TIME + WAKE 증분 수집(Phase 3). 대상 패키지는 스코프에서, WAKE 는 패키지 무관.
            usageEventCollector.collect(scope.targetPackages)
            // 움직임·수면(명세 §8). connect-client minSdk 26 + java.time → API 26 미만에선 호출 자체를 막는다
            // (이 가드가 HealthConnectCollector.capture 의 java.time 코드가 구 기기에서 실행되지 않게 보장).
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                (scope.healthTargets.isNotEmpty() || scope.sleepRequested)
            ) {
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
