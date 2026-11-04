package com.ruleup.logging.data.sink

import com.amplitude.android.Amplitude
import com.ruleup.logging.domain.BizLog
import com.ruleup.logging.domain.BizLogShooter
import javax.inject.Provider

internal class AmplitudeBizShooter(
    private val amplitude: Provider<Amplitude>,
) : BizLogShooter {
    override suspend fun shoot(log: BizLog) {
        amplitude.get().track(BizEventMapper.eventName(log), BizEventMapper.toProperties(log))
    }
}
