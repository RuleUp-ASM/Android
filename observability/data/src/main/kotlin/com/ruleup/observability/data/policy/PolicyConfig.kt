package com.ruleup.observability.data.policy

import com.ruleup.observability.domain.event.Channel
import com.ruleup.observability.domain.model.Severity
import com.ruleup.observability.domain.port.Policy

/**
 * 게이트 설정 스냅샷. [Policy.isEnabled] 구현이 판단 근거로 삼고, 온디바이스 인스펙터가
 * *"이 로그가 왜 안 찍히지"* 에 답할 때 조회한다.
 */
@ConsistentCopyVisibility
data class PolicyConfig private constructor(
    /** 채널별 최소 심각도. **채널마다 독립이다.** */
    val channelFloors: Map<Channel, Severity>,
    /** 진단 채널의 태그별 floor 오버라이드. 해당 태그에 한해 [channelFloors] 를 대체한다. */
    val tagOverrides: Map<String, Severity>,
    /** 심각도와 무관하게 통째로 꺼진 채널. */
    val disabledChannels: Set<Channel>,
    /**
     * 구현별 진단 정보(예: `"dedupWindowMillis" to "3000"`). 특정 구현을 타입이 전제하지 않도록
     * 자유 형식으로 두고, 인스펙터는 그대로 출력만 한다.
     */
    val extras: Map<String, String> = emptyMap(),
) {
    /** [channel] 의 floor 를 [floor] 로 바꾼 새 스냅샷. */
    fun withChannelFloor(
        channel: Channel,
        floor: Severity,
    ): PolicyConfig = PolicyConfig(channelFloors + (channel to floor), tagOverrides, disabledChannels, extras)

    /** [tag] 오버라이드를 설정하거나([floor] 가 null 이면) 해제한 새 스냅샷. */
    fun withTagOverride(
        tag: String,
        floor: Severity?,
    ): PolicyConfig =
        PolicyConfig(
            channelFloors,
            if (floor == null) tagOverrides - tag else tagOverrides + (tag to floor),
            disabledChannels,
            extras,
        )

    /** [channel] 을 통째로 켜거나 끈 새 스냅샷. */
    fun withChannel(
        channel: Channel,
        enabled: Boolean,
    ): PolicyConfig =
        PolicyConfig(
            channelFloors,
            tagOverrides,
            if (enabled) disabledChannels - channel else disabledChannels + channel,
            extras,
        )

    /** 정책 구현별 진단 정보를 갱신한 새 스냅샷. */
    fun withExtra(
        key: String,
        value: String,
    ): PolicyConfig = PolicyConfig(channelFloors, tagOverrides, disabledChannels, extras + (key to value))

    fun summary(): String =
        buildString {
            append(
                Channel.entries.joinToString(" ") { channel ->
                    val floor = channelFloors[channel] ?: Severity.VERBOSE
                    "${channel.name.take(4)}=${if (channel in disabledChannels) "OFF" else floor.name}"
                },
            )
            if (tagOverrides.isNotEmpty()) append(" +${tagOverrides.size} tag overrides")
            extras.forEach { (key, value) -> append(" $key=$value") }
        }

    companion object {
        fun of(
            channelFloors: Map<Channel, Severity>,
            tagOverrides: Map<String, Severity> = emptyMap(),
            disabledChannels: Set<Channel> = emptySet(),
            extras: Map<String, String> = emptyMap(),
        ) = PolicyConfig(
            channelFloors.toMap(),
            tagOverrides.toMap(),
            disabledChannels.toSet(),
            extras.toMap(),
        )
    }
}
