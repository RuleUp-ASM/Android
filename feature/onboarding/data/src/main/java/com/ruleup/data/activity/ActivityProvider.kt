package com.ruleup.data.activity

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 현재 화면에 떠 있는 Activity 를 약한 참조로 보관한다.
 *
 * Activity 가 필요한 SDK 호출(예: 카카오 OAuth)에서 [current] 로 꺼내 쓰되,
 * ViewModel·도메인 시그니처에는 Activity 가 노출되지 않도록 한다.
 *
 * [Application.ActivityLifecycleCallbacks] 를 직접 구현하므로
 * `registerActivityLifecycleCallbacks(activityProvider)` 한 줄로 추적이 시작된다.
 */
@Singleton
class ActivityProvider
    @Inject
    constructor() : Application.ActivityLifecycleCallbacks {
        private var activityRef: WeakReference<Activity>? = null

        /** 현재 resumed 상태의 Activity. 없으면 null. */
        val current: Activity?
            get() = activityRef?.get()

        override fun onActivityResumed(activity: Activity) {
            activityRef = WeakReference(activity)
        }

        override fun onActivityDestroyed(activity: Activity) {
            if (activityRef?.get() === activity) {
                activityRef = null
            }
        }

        override fun onActivityCreated(
            activity: Activity,
            savedInstanceState: Bundle?,
        ) = Unit

        override fun onActivityStarted(activity: Activity) = Unit

        override fun onActivityPaused(activity: Activity) = Unit

        override fun onActivityStopped(activity: Activity) = Unit

        override fun onActivitySaveInstanceState(
            activity: Activity,
            outState: Bundle,
        ) = Unit
    }
