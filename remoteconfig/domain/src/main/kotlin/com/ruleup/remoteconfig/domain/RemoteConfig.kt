package com.ruleup.remoteconfig.domain

class ConfigId<T : Any> internal constructor(
    val key: String,
    val default: T,
    private val decode: (String) -> T?,
) {
    fun valueOf(value: String): T? = decode(value)
}

enum class WatcherInviteUi {
    LEGACY,
    A,
    C,
}

object RemoteConfigIds {
    val WatcherInviteUi =
        ConfigId("watcher_invite_ui", com.ruleup.remoteconfig.domain.WatcherInviteUi.LEGACY) { value ->
            com.ruleup.remoteconfig.domain.WatcherInviteUi.entries
                .find { it.name == value }
        }

    val all: List<ConfigId<*>> = listOf(WatcherInviteUi)
}

interface RemoteConfig {
    /** 첫 읽기는 현재 프로세스의 값을 고정한다. */
    suspend operator fun <T : Any> get(id: ConfigId<T>): T

    /** 실제 표시를 기록하고 이 디바이스에서 처음 표시한 경우에만 true를 반환한다. */
    suspend fun onShown(id: ConfigId<*>): Boolean

    object Defaults : RemoteConfig {
        override suspend fun <T : Any> get(id: ConfigId<T>): T = id.default

        override suspend fun onShown(id: ConfigId<*>): Boolean = false
    }
}

interface RemoteConfigMetrics {
    suspend fun record(
        id: ConfigId<*>,
        event: String,
        properties: Map<String, String> = emptyMap(),
    )

    object None : RemoteConfigMetrics {
        override suspend fun record(
            id: ConfigId<*>,
            event: String,
            properties: Map<String, String>,
        ) = Unit
    }
}
