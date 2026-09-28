package com.ruleup.tti.data

import android.os.SystemClock
import com.ruleup.tti.domain.TtiClock
import javax.inject.Inject

/** 안드로이드 시계. */
internal class AndroidTtiClock
    @Inject
    constructor() : TtiClock {
        override fun elapsedMillis(): Long = SystemClock.elapsedRealtime()

        override fun wallTimeMillis(): Long = System.currentTimeMillis()
    }
