package com.ruleup.observability.data

import android.annotation.SuppressLint
import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** 사용자 식별자를 분석 SDK 상태로 전달한다. */
@Singleton
class UserIdentitySync
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        @SuppressLint("MissingPermission")
        fun setUser(pseudonymousId: String?) {
            FirebaseAnalytics.getInstance(context).setUserId(pseudonymousId)
            // Crashlytics 는 null 을 받지 않는다.
            FirebaseCrashlytics.getInstance().setUserId(pseudonymousId.orEmpty())
        }
    }
