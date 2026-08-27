package com.ruleup.observability.data.sink

import android.annotation.SuppressLint
import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.ruleup.observability.domain.event.ObsEvent
import com.ruleup.observability.domain.port.Sink

/**
 * Firebase Analytics 출구. SDK 자체 큐가 비동기로 올리므로 업로드 실패는 [emit] 으로 관측되지 않고,
 * 대응하는 공개 API 가 없어 `flush()` 도 재정의하지 않는다.
 *
 * INTERNET/ACCESS_NETWORK_STATE/WAKE_LOCK 은 firebase-analytics AAR + app 매니페스트에서 병합되므로
 * `MissingPermission` 을 억제한다.
 */
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
