package com.ruleup.observability.data.sink

import android.annotation.SuppressLint
import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.ruleup.observability.domain.event.ObsEvent
import com.ruleup.observability.domain.port.Sink

/** Firebase Analytics 출구. */
@SuppressLint("MissingPermission")
internal class FirebaseAnalyticsSink(
    context: Context,
) : Sink {
    // google-services 플러그인이 FirebaseApp 을 초기화하기 전에 접근하면 예외가 난다.
    private val analytics by lazy { FirebaseAnalytics.getInstance(context) }

    override fun emit(event: ObsEvent) {
        analytics.logEvent(
            FirebaseEventMapper.eventName(event.payload),
            FirebaseEventMapper.toBundle(event),
        )
    }
}
