package com.ruleup.logging.data.sink

import android.annotation.SuppressLint
import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.ruleup.logging.domain.BizLog
import com.ruleup.logging.domain.BizLogShooter

/**
 * Firebase Analytics 출구. SDK 자체 큐가 비동기로 올리므로 업로드 실패는 [shoot] 으로 관측되지 않는다.
 *
 * INTERNET/ACCESS_NETWORK_STATE/WAKE_LOCK 은 firebase-analytics AAR + app 매니페스트에서 병합되므로
 * `MissingPermission` 을 억제한다.
 */
@SuppressLint("MissingPermission")
internal class FirebaseBizShooter(
    context: Context,
) : BizLogShooter {
    // google-services 플러그인이 FirebaseApp 을 초기화하기 전에 접근하면 예외가 난다.
    private val analytics by lazy { FirebaseAnalytics.getInstance(context) }

    override suspend fun shoot(log: BizLog) {
        // 사용자 식별자는 SDK 상태로 따로 전달된다(UserIdentitySync) — 이벤트에 다시 싣지 않는다.
        analytics.logEvent(BizEventMapper.eventName(log), BizEventMapper.toBundle(log))
    }
}
