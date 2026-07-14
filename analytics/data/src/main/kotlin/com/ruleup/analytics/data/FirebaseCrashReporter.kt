package com.ruleup.analytics.data

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.ruleup.analytics.domain.CrashReporter
import javax.inject.Inject

/**
 * Firebase Crashlytics 기반 [CrashReporter] 구현(Android).
 *
 * 처리된 예외를 non-fatal 로 기록한다. [FirebaseCrashlytics] 는 google-services.json + crashlytics 플러그인으로
 * 초기화된 FirebaseApp 이 있어야 동작한다.
 */
class FirebaseCrashReporter
    @Inject
    constructor() : CrashReporter {
        private val crashlytics by lazy { FirebaseCrashlytics.getInstance() }

        override fun recordException(
            throwable: Throwable,
            keys: Map<String, String>,
        ) {
            keys.forEach { (key, value) -> crashlytics.setCustomKey(key, value) }
            crashlytics.recordException(throwable)
        }
    }
