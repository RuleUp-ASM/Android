package com.ruleup.observability.data.policy

import com.ruleup.observability.domain.event.Channel
import com.ruleup.observability.domain.model.Severity
import com.ruleup.observability.domain.model.atLeast
import com.ruleup.observability.domain.port.Policy
import java.util.concurrent.atomic.AtomicReference

/** 게이트 구현. 설정을 **불변 스냅샷 하나로 들고 참조만 교체**해 읽기 경로에 잠금이 없다. */
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

    /** 현재 스냅샷. 인스펙터가 *"이 로그가 왜 안 찍히지"* 에 답할 때 조회한다. */
    fun config(): PolicyConfig = snapshot.get()

    /** 설정을 갱신한다. 에셋 로딩 완료·원격 컨피그 수신·QA 토글이 호출한다. */
    fun update(transform: (PolicyConfig) -> PolicyConfig) {
        snapshot.updateAndGet(transform)
    }
}
