package com.ruleup.logging.data.sink

import android.annotation.SuppressLint
import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.ruleup.logging.domain.BizLog
import com.ruleup.logging.domain.BizLogShooter

/** Firebase Analytics 출구. */
@SuppressLint("MissingPermission")
internal class FirebaseBizShooter(
    context: Context,
) : BizLogShooter {
    // google-services 플러그인이 FirebaseApp 을 초기화하기 전에 접근하면 예외가 난다.
    private val analytics by lazy { FirebaseAnalytics.getInstance(context) }

    override suspend fun shoot(log: BizLog) {
        // 사용자 식별자는 SDK 상태로 따로 전달된다(UserIdentitySync)
        analytics.logEvent(BizEventMapper.eventName(log), BizEventMapper.toBundle(log))
    }
}
