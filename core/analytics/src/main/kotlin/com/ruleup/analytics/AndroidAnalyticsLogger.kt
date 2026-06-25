package com.ruleup.analytics

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Firebase Analytics 기반 [AnalyticsLogger] 구현(Android).
 *
 * [FirebaseAnalytics] 는 google-services.json + google-services 플러그인으로 초기화된 FirebaseApp 이
 * 있어야 동작한다. 해당 설정 전에 처음 주입되면 런타임 예외가 날 수 있으니, 앱에 설정을 추가한 뒤 사용한다.
 */
class AndroidAnalyticsLogger
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : AnalyticsLogger {
        // INTERNET/ACCESS_NETWORK_STATE/WAKE_LOCK 은 firebase-analytics AAR + app 매니페스트에서 병합 제공된다.
        @SuppressLint("MissingPermission")
        private val firebaseAnalytics = FirebaseAnalytics.getInstance(context)

        override fun log(event: AnalyticsEvent) {
            firebaseAnalytics.logEvent(event.name, event.params.toBundle())
        }

        override fun setUserId(id: String?) {
            firebaseAnalytics.setUserId(id)
        }

        override fun setUserProperty(
            key: String,
            value: String,
        ) {
            firebaseAnalytics.setUserProperty(key, value)
        }

        // Firebase 파라미터는 String/Long/Double/Bundle 만 지원하므로 타입별로 매핑한다.
        private fun Map<String, Any>.toBundle(): Bundle {
            val bundle = Bundle()
            forEach { (key, value) ->
                when (value) {
                    is String -> bundle.putString(key, value)
                    is Int -> bundle.putLong(key, value.toLong())
                    is Long -> bundle.putLong(key, value)
                    is Double -> bundle.putDouble(key, value)
                    is Float -> bundle.putDouble(key, value.toDouble())
                    is Boolean -> bundle.putLong(key, if (value) 1L else 0L)
                    else -> bundle.putString(key, value.toString())
                }
            }
            return bundle
        }
    }
