package com.ruleup.observability.data.sink

import android.annotation.SuppressLint
import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.ruleup.observability.domain.event.ObsEvent
import com.ruleup.observability.domain.port.Sink

/**
 * Firebase Analytics 출구.
 *
 * SDK 가 자체 비동기 큐를 가지므로 **여기서 큐를 덧대지 않는다.** `logEvent` 는 즉시 리턴하고
 * 실제 업로드는 나중에 일어난다 — 그래서 업로드 실패는 이 호출로 관측되지 않는다.
 *
 * [FirebaseAnalytics] 는 `google-services.json` + google-services 플러그인으로 초기화된
 * FirebaseApp 이 있어야 동작한다. 초기화 전에 처음 접근하면 예외가 날 수 있으므로 지연 생성한다.
 *
 * `flush()` 는 재정의하지 않는다 — Analytics SDK 에 대응하는 공개 API 가 없다.
 *
 * INTERNET/ACCESS_NETWORK_STATE/WAKE_LOCK 은 firebase-analytics AAR + app 매니페스트에서 병합 제공되므로
 * `MissingPermission` 을 억제한다.
 */
@SuppressLint("MissingPermission")
internal class FirebaseAnalyticsSink(
    context: Context,
) : Sink {
    private val analytics by lazy { FirebaseAnalytics.getInstance(context) }

    override fun emit(event: ObsEvent) {
        analytics.logEvent(
            FirebaseEventMapper.eventName(event.payload),
            FirebaseEventMapper.toBundle(event),
        )
    }
}
