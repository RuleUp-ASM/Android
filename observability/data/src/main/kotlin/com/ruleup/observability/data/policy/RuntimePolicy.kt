package com.ruleup.observability.data.policy

import com.ruleup.observability.domain.event.Channel
import com.ruleup.observability.domain.model.Severity
import com.ruleup.observability.domain.model.atLeast
import com.ruleup.observability.domain.port.Policy
import java.util.concurrent.atomic.AtomicReference

/** 게이트 구현. */
class RuntimePolicy(
    initial: PolicyConfig,
) : Policy {
    private val snapshot = AtomicReference(initial)

    override fun isEnabled(
        channel: Channel,
        severity: Severity,
        tag: String?,
    ): Boolean {
        val config = snapshot.get()
        if (channel in config.disabledChannels) return false
        val floor =
            tag?.let { config.tagOverrides[it] }
                ?: config.channelFloors[channel]
                ?: Severity.VERBOSE
        return severity atLeast floor
    }

    /** 현재 스냅샷. */
    fun config(): PolicyConfig = snapshot.get()

    /** 설정을 갱신한다. */
    fun update(transform: (PolicyConfig) -> PolicyConfig) {
        snapshot.updateAndGet(transform)
    }
}
