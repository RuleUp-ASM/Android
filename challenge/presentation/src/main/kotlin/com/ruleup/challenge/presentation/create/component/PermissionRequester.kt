package com.ruleup.challenge.presentation.create.component

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred

/**
 * AUTO 인증에 필요한 런타임 권한을 OS 다이얼로그로 요청하는 추상화.
 * 서버가 내려준 권한 토큰을 플랫폼 권한으로 매핑·요청하고, 허용된 토큰 집합을 돌려준다.
 */
interface PermissionRequester {
    suspend fun request(tokens: List<String>): Set<String>
}

@Composable
fun rememberPermissionRequester(): PermissionRequester {
    // 프리뷰에서는 no-op(전부 허용 처리).
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

                val result = mutableMapOf<String, Boolean>()
                for (batch in permissionRequestBatches(toRequest)) {
                    // 전경 위치를 받지 못했으면 백그라운드 위치는 OS 가 요청 자체를 거부한다.
                    if (Manifest.permission.ACCESS_BACKGROUND_LOCATION in batch && !foregroundLocationGranted(context)) continue
                    result += holder.await { launcher.launch(batch.toTypedArray()) }
                }
                tokens.forEach { token ->
                    val perm = tokenToPerm[token]
                    if (perm != null && result[perm] == true) granted += token
                }
                return granted
            }
        }
    }
}

/**
 * 요청 묶음. Android 11(API 30)+ 는 백그라운드 위치를 다른 권한과 한 번에 요청하면 다이얼로그 없이 거부하므로
 * 전경 권한을 먼저 묶고 백그라운드 위치는 마지막에 따로 요청한다.
 */
internal fun permissionRequestBatches(permissions: List<String>): List<List<String>> {
    val background = Manifest.permission.ACCESS_BACKGROUND_LOCATION
    val foreground = permissions.filter { it != background }
    return listOfNotNull(
        foreground.takeIf { it.isNotEmpty() },
        listOf(background).takeIf { background in permissions },
    )
}

private fun foregroundLocationGranted(context: Context): Boolean =
    listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

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

/**
 * 런타임 권한만 본다 — 매핑되지 않는 토큰(usage/health 등 특수권한)은 허용으로 간주한다.
 * 상세 화면이 "참여하기" 시 권한 허용 모달을 띄울지 판단하는 데 쓴다.
 */
fun challengePermissionsGranted(
    context: Context,
    tokens: List<String>,
): Boolean =
    tokens.all { token ->
        val perm = androidPermission(token)
        perm == null || ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    }

// TODO(server-contract): 서버 권한 토큰 어휘 확정 시 매핑 보완.
private fun androidPermission(token: String): String? =
    when (token.uppercase()) {
        "LOCATION", "ACCESS_FINE_LOCATION", "GPS", "GEOFENCE" -> Manifest.permission.ACCESS_FINE_LOCATION
        "ACCESS_BACKGROUND_LOCATION", "BACKGROUND_LOCATION" -> Manifest.permission.ACCESS_BACKGROUND_LOCATION
        "CAMERA", "PHOTO" -> Manifest.permission.CAMERA
        // PACKAGE_USAGE_STATS 등 특수 접근(런타임 권한 아님)은 여기서 매핑하지 않는다(별도 설정 화면 필요).
        else -> null
    }

private object NoOpPermissionRequester : PermissionRequester {
    override suspend fun request(tokens: List<String>): Set<String> = tokens.toSet()
}
