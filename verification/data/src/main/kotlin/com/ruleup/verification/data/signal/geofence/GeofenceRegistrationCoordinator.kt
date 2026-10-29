package com.ruleup.verification.data.signal.geofence

import com.ruleup.verification.domain.entity.GeofenceTarget
import java.nio.ByteBuffer
import java.security.MessageDigest

internal class GeofenceRegistrationCoordinator(
    private val readRegistered: suspend () -> Map<String, String>,
    private val writeRegistered: suspend (Map<String, String>) -> Unit,
    private val add: suspend (List<GeofenceTarget>) -> Boolean,
    private val remove: suspend (List<String>) -> Boolean,
) {
    suspend fun reconcile(
        targets: List<GeofenceTarget>,
        canRegister: Boolean = true,
    ) {
        val registered = readRegistered().toMutableMap()
        val fingerprints = targets.associate { it.requestId to it.fingerprint() }
        val removed = registered.keys - fingerprints.keys
        if (removed.isNotEmpty() && remove(removed.toList())) registered.keys.removeAll(removed)
        val changed = targets.filter { registered[it.requestId] != fingerprints[it.requestId] }
        if (canRegister && changed.isNotEmpty() && add(changed)) {
            changed.forEach { registered[it.requestId] = fingerprints.getValue(it.requestId) }
        }
        writeRegistered(registered)
    }
}

private fun GeofenceTarget.fingerprint(): String {
    val data =
        ByteBuffer
            .allocate(24)
            .putDouble(lat)
            .putDouble(lng)
            .putFloat(radiusM)
            .putInt(dwellMinutes)
            .array()
    return MessageDigest.getInstance("SHA-256").digest(data).joinToString("") { "%02x".format(it) }
}
