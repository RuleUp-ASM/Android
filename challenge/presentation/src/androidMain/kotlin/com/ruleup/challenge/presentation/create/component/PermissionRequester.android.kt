package com.ruleup.challenge.presentation.create.component

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred

@Composable
actual fun rememberPermissionRequester(): PermissionRequester {
    // 프리뷰에는 Activity/Registry 가 없어 런처 등록 불가 → no-op(전부 허용 처리).
    if (LocalInspectionMode.current) return NoOpPermissionRequester

    val context = LocalContext.current
    val holder = remember { AndroidPermissionHolder() }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            holder.complete(result)
        }

    return remember(context) {
        object : PermissionRequester {
            override suspend fun request(tokens: List<String>): Set<String> {
                // 토큰 → android 권한. 매핑 안 되는 토큰은 요청 불가 → 낙관적 허용으로 통과.
                val tokenToPerm = tokens.associateWith { androidPermission(it) }
                val granted =
                    tokens
                        .filter { token ->
                            val perm = tokenToPerm[token]
                            perm == null ||
                                ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
                        }.toMutableSet()

                val toRequest =
                    tokens
                        .mapNotNull { tokenToPerm[it] }
                        .filter { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }
                        .distinct()
                if (toRequest.isEmpty()) return granted

                val result = holder.await { launcher.launch(toRequest.toTypedArray()) }
                tokens.forEach { token ->
                    val perm = tokenToPerm[token]
                    if (perm != null && result[perm] == true) granted += token
                }
                return granted
            }
        }
    }
}

/** 콜백 기반 RequestMultiplePermissions 결과를 suspend 로 브리지한다. */
private class AndroidPermissionHolder {
    private var deferred: CompletableDeferred<Map<String, Boolean>>? = null

    suspend fun await(launch: () -> Unit): Map<String, Boolean> {
        val d = CompletableDeferred<Map<String, Boolean>>()
        deferred = d
        launch()
        return d.await()
    }

    fun complete(result: Map<String, Boolean>) {
        deferred?.complete(result)
        deferred = null
    }
}

// TODO(server-contract): 서버 권한 토큰 어휘 확정 시 매핑 보완.
private fun androidPermission(token: String): String? =
    when (token.uppercase()) {
        "LOCATION", "ACCESS_FINE_LOCATION", "GPS", "GEOFENCE" -> Manifest.permission.ACCESS_FINE_LOCATION
        "ACTIVITY_RECOGNITION", "PHYSICAL_ACTIVITY" -> Manifest.permission.ACTIVITY_RECOGNITION
        "CAMERA", "PHOTO" -> Manifest.permission.CAMERA
        else -> null
    }

private object NoOpPermissionRequester : PermissionRequester {
    override suspend fun request(tokens: List<String>): Set<String> = tokens.toSet()
}
