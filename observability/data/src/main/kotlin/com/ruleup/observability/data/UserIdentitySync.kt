package com.ruleup.observability.data

import android.annotation.SuppressLint
import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 사용자 식별자를 분석 SDK 상태로 전달한다.
 *
 * **이벤트에 실리는 값이 아니다.** `setUserId` 는 한 번 설정하면 이후 모든 이벤트에 SDK 가 알아서
 * 붙이는 상태라, 관측 파이프라인(`ObsEvent`)을 거칠 이유가 없다. 그래서 도메인 포트도 두지 않는다.
 *
 * Firebase 의존을 `:app` 으로 새게 하지 않으려고 이 모듈에 둔다 — `:app` 은 인증 상태를 관찰해
 * 이 클래스만 호출한다.
 *
 * 넘기는 값은 서버 userId 처럼 **불투명 식별자**여야 한다. 이메일·이름·전화번호를 넣지 않는다.
 */
@Singleton
class UserIdentitySync
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        @SuppressLint("MissingPermission")
        fun setUser(pseudonymousId: String?) {
            FirebaseAnalytics.getInstance(context).setUserId(pseudonymousId)
            // Crashlytics 는 null 을 받지 않는다. 로그아웃은 빈 문자열로 해제한다.
            FirebaseCrashlytics.getInstance().setUserId(pseudonymousId.orEmpty())
        }
    }
