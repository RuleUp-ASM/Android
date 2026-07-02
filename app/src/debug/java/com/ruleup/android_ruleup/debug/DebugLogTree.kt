package com.ruleup.android_ruleup.debug

import timber.log.Timber

/**
 * Logcat 출력(super) + 화면 오버레이 버퍼([DebugLogStore]) 동시 적재용 Timber Tree.
 * 디버그 빌드에서만 심는다([com.ruleup.android_ruleup.App]).
 */
class DebugLogTree : Timber.DebugTree() {
    override fun log(
        priority: Int,
        tag: String?,
        message: String,
        t: Throwable?,
    ) {
        super.log(priority, tag, message, t)
        DebugLogStore.add(DebugLogStore.Entry(priority, tag, message))
    }
}
