package com.ruleup.remoteconfig.data

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.amplitude.android.Amplitude
import com.amplitude.experiment.Experiment
import com.amplitude.experiment.ExperimentConfig
import com.amplitude.experiment.ExperimentUser
import com.amplitude.experiment.Variant
import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.api.w
import com.ruleup.remoteconfig.domain.ConfigId
import com.ruleup.remoteconfig.domain.RemoteConfig
import com.ruleup.remoteconfig.domain.RemoteConfigIds
import com.ruleup.remoteconfig.domain.RemoteConfigMetrics
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

data class RemoteConfigDeployment(
    val analyticsKey: String,
    val experimentKey: String,
) {
    val enabled: Boolean get() = analyticsKey.isNotBlank() && experimentKey.isNotBlank()
}

@Singleton
class AmplitudeRemoteConfig
    @Inject
    constructor(
        private val application: Application,
        private val deployment: RemoteConfigDeployment,
        private val analytics: Provider<Amplitude>,
        @param:RemoteConfigStore private val store: DataStore<Preferences>,
        private val observability: Observability,
    ) : RemoteConfig,
        RemoteConfigMetrics {
        private val process = ProcessConfig()
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val refreshLock = Mutex()
        private val cacheLoaded = CompletableDeferred<Unit>()
        private val metricsLock = Mutex()
        private val unavailableReported = mutableSetOf<String>()

        @Volatile
        private var storagePrefix: String? = null

        suspend fun refresh() {
            if (!deployment.enabled) return
            refreshLock.withLock {
                try {
                    refreshFromDeployment()
                } finally {
                    cacheLoaded.complete(Unit)
                }
            }
        }

        private suspend fun refreshFromDeployment() {
            safely("설정 갱신 실패") {
                val amplitude = analytics.get()
                if (!amplitude.isBuilt.await()) return@safely
                val deviceId = amplitude.getDeviceId()?.takeIf { it.isNotBlank() } ?: return@safely
                val namespace = storageNamespace(deviceId)
                storagePrefix = namespace
                val preferences = store.data.first()
                val cached =
                    RemoteConfigIds.all.associateWith { id ->
                        decode<ConfigSnapshot>(preferences[cacheKey(namespace, id)])
                    }
                cached.forEach { (id, snapshot) -> snapshot?.let { process.stage(id.key, it) } }
                cacheLoaded.complete(Unit)

                val client =
                    Experiment.initialize(
                        application,
                        deployment.experimentKey,
                        ExperimentConfig
                            .builder()
                            .automaticExposureTracking(false)
                            .retryFetchOnFailure(false)
                            .fetchTimeoutMillis(3_000)
                            .build(),
                    )
                val variants =
                    runInterruptible(Dispatchers.IO) {
                        client.fetch(ExperimentUser.builder().deviceId(deviceId).build()).get().all()
                    }
                val now = System.currentTimeMillis()
                val snapshots =
                    cached.mapValues { (id, previous) ->
                        val received = parse(id, variants[id.key], now)
                        val accepted = received.preserveAssignment(previous)
                        if (received.assignment != accepted.assignment) {
                            observability.w(TAG) { "실험 실행 중 배정 변경을 무시함: ${id.key}" }
                        }
                        accepted
                    }
                store.edit { data ->
                    snapshots.forEach { (id, snapshot) -> data[cacheKey(namespace, id)] = Json.encodeToString(snapshot) }
                }
                snapshots.forEach { (id, snapshot) -> process.stage(id.key, snapshot) }
            }
        }

        override suspend fun <T : Any> get(id: ConfigId<T>): T {
            if (!deployment.enabled) return id.default
            withTimeoutOrNull(LOCAL_CACHE_WAIT_MS) { cacheLoaded.await() }
            val snapshot = process.pin(id.key, System.currentTimeMillis())
            if (snapshot?.assignment != null) {
                scope.launch {
                    metricsLock.withLock { safely("실험 배정 기록 실패") { enroll(id, snapshot.assignment) } }
                }
            } else if (snapshot == null) {
                scope.launch {
                    metricsLock.withLock {
                        if (unavailableReported.add(id.key)) {
                            safely("설정 미확정 기록 실패") {
                                analytics.get().track("remote_config_unavailable", mapOf("flag_key" to id.key))
                            }
                        }
                    }
                }
            }
            return snapshot?.value?.let(id::valueOf) ?: id.default
        }

        override suspend fun onShown(id: ConfigId<*>): Boolean =
            metricsLock.withLock {
                safely("실험 노출 기록 실패") {
                    val snapshot = process.applied(id.key) ?: return@safely false
                    val value = snapshot.value ?: return@safely false
                    val namespace = storagePrefix ?: return@safely false
                    val assignment = snapshot.assignment
                    if (assignment == null) {
                        val seen = stringPreferencesKey("${namespace}_${id.key}_shown_fixed")
                        var first = false
                        store.edit { data ->
                            first = data[seen] != value
                            if (first) data[seen] = value
                        }
                        return@safely first
                    }
                    val enrollment = enroll(id, assignment) ?: return@safely false
                    if (enrollment.shown) return@safely false
                    store.edit { it[enrollmentKey(namespace, id)] = Json.encodeToString(enrollment.copy(shown = true)) }
                    analytics.get().track("\$exposure", properties(id, assignment))
                    true
                } ?: false
            }

        override suspend fun record(
            id: ConfigId<*>,
            event: String,
            properties: Map<String, String>,
        ) {
            if (!deployment.enabled) return
            withTimeoutOrNull(LOCAL_CACHE_WAIT_MS) { cacheLoaded.await() }
            metricsLock.withLock {
                safely("실험 행동 기록 실패") {
                    val namespace = storagePrefix ?: return@safely
                    val assignment = process.applied(id.key)?.assignment
                    val enrollment =
                        if (assignment != null) {
                            enroll(id, assignment)
                        } else {
                            decode<ExperimentEnrollment>(store.data.first()[enrollmentKey(namespace, id)])
                        }
                    val attributes =
                        enrollment
                            ?.takeIf { it.isWithinConversionWindow(System.currentTimeMillis()) }
                            ?.let { properties(id, it.assignment) }
                            ?: mapOf("flag_key" to id.key)
                    analytics.get().track(event, properties + attributes)
                }
            }
        }

        private suspend fun enroll(
            id: ConfigId<*>,
            assignment: ExperimentAssignment,
        ): ExperimentEnrollment? {
            val namespace = storagePrefix ?: return null
            val key = enrollmentKey(namespace, id)
            val prior = decode<ExperimentEnrollment>(store.data.first()[key])
            if (prior?.assignment?.runId == assignment.runId) return prior
            val enrollment = ExperimentEnrollment(assignment, System.currentTimeMillis())
            store.edit { it[key] = Json.encodeToString(enrollment) }
            analytics.get().track("remote_config_experiment_enrolled", properties(id, assignment))
            return enrollment
        }

        private fun properties(
            id: ConfigId<*>,
            assignment: ExperimentAssignment,
        ): Map<String, Any> =
            mapOf(
                "flag_key" to id.key,
                "experiment_key" to assignment.runId,
                "variant" to assignment.variant,
            )

        private fun storageNamespace(deviceId: String): String {
            val input = "${deployment.analyticsKey}:${deployment.experimentKey}:$deviceId"
            val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            return "remote_config_" + digest.joinToString("") { "%02x".format(it) }
        }

        private suspend fun <T> safely(
            message: String,
            block: suspend () -> T,
        ): T? =
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                observability.w(TAG) { "$message (${failure.javaClass.simpleName})" }
                null
            }

        private companion object {
            const val TAG = "RemoteConfig"
            const val LOCAL_CACHE_WAIT_MS = 300L

            fun cacheKey(
                namespace: String,
                id: ConfigId<*>,
            ) = stringPreferencesKey("${namespace}_${id.key}_cache")

            fun enrollmentKey(
                namespace: String,
                id: ConfigId<*>,
            ) = stringPreferencesKey("${namespace}_${id.key}_enrolled")

            inline fun <reified T> decode(value: String?): T? = value?.let { runCatching { Json.decodeFromString<T>(it) }.getOrNull() }

            fun parse(
                id: ConfigId<*>,
                variant: Variant?,
                now: Long,
            ): ConfigSnapshot {
                val payload = variant?.payload as? JSONObject ?: return ConfigSnapshot(now)
                val value = payload.optString("value").takeIf { id.valueOf(it) != null } ?: return ConfigSnapshot(now)
                return when (payload.optString("mode")) {
                    "fixed" -> ConfigSnapshot(now, value)
                    "experiment" -> {
                        if (id.valueOf(value) == id.default) return ConfigSnapshot(now)
                        val run = variant.expKey?.takeIf { it.isNotBlank() } ?: return ConfigSnapshot(now)
                        val key = (variant.key ?: variant.value)?.takeIf { it.isNotBlank() } ?: return ConfigSnapshot(now)
                        ConfigSnapshot(now, value, ExperimentAssignment(run, key, value))
                    }
                    else -> ConfigSnapshot(now)
                }
            }
        }
    }
