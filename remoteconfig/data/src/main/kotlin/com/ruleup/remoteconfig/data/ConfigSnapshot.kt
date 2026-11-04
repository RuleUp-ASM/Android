package com.ruleup.remoteconfig.data

import kotlinx.serialization.Serializable

private const val CACHE_LIFETIME_MS = 24 * 60 * 60 * 1_000L

@Serializable
internal data class ConfigSnapshot(
    val fetchedAt: Long,
    val value: String? = null,
    val assignment: ExperimentAssignment? = null,
    val firstAssignment: ExperimentAssignment? = assignment,
) {
    fun isFresh(now: Long): Boolean = now - fetchedAt in 0..CACHE_LIFETIME_MS

    fun preserveAssignment(previous: ConfigSnapshot?): ConfigSnapshot {
        val next = assignment ?: return copy(firstAssignment = previous?.firstAssignment)
        val first = previous?.firstAssignment?.takeIf { it.runId == next.runId } ?: next
        return copy(value = first.value, assignment = first, firstAssignment = first)
    }
}

@Serializable
internal data class ExperimentAssignment(
    val runId: String,
    val variant: String,
    val value: String,
)

@Serializable
internal data class ExperimentEnrollment(
    val assignment: ExperimentAssignment,
    val enrolledAt: Long,
    val shown: Boolean = false,
) {
    fun isWithinConversionWindow(now: Long): Boolean = now - enrolledAt in 0 until 7 * 24 * 60 * 60 * 1_000L
}

internal class ProcessConfig {
    private val candidates = mutableMapOf<String, ConfigSnapshot>()
    private val pinned = mutableMapOf<String, ConfigSnapshot?>()

    @Synchronized
    fun stage(
        key: String,
        snapshot: ConfigSnapshot,
    ) {
        candidates[key] = snapshot
    }

    @Synchronized
    fun pin(
        key: String,
        now: Long,
    ): ConfigSnapshot? {
        if (key !in pinned) pinned[key] = candidates[key]?.takeIf { it.isFresh(now) }
        return pinned[key]
    }

    @Synchronized
    fun applied(key: String): ConfigSnapshot? = pinned[key]
}
