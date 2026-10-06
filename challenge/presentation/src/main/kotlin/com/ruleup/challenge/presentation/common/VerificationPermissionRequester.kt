package com.ruleup.challenge.presentation.common

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.ruleup.challenge.presentation.create.component.rememberPermissionRequester
import com.ruleup.ui.helper.LocalMessageHelper
import com.ruleup.ui.permission.healthConnectAvailable
import com.ruleup.ui.permission.healthRequestPermissions
import com.ruleup.ui.permission.rememberHealthPermissionLauncher
import com.ruleup.verification.domain.entity.PermissionRequestKind
import com.ruleup.verification.domain.entity.PermissionSnapshot
import kotlinx.coroutines.launch

@Composable
internal fun rememberVerificationPermissionRequester(onResult: () -> Unit): (List<String>) -> Unit {
    val context = LocalContext.current
    val messages = LocalMessageHelper.current
    val currentOnResult by rememberUpdatedState(onResult)
    val scope = rememberCoroutineScope()
    val runtimeRequester = rememberPermissionRequester()
    val settingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { currentOnResult() }
    val healthLauncher = rememberHealthPermissionLauncher { currentOnResult() }
    return { tokens ->
        val kind = tokens.firstOrNull()?.let(PermissionSnapshot::requestKindOf)
        val batch = tokens.filter { PermissionSnapshot.requestKindOf(it) == kind }
        when (kind) {
            PermissionRequestKind.RUNTIME ->
                scope.launch {
                    runtimeRequester.request(batch)
                    currentOnResult()
                }
            PermissionRequestKind.USAGE_ACCESS_SETTINGS -> settingsLauncher.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            PermissionRequestKind.HEALTH_CONNECT -> {
                if (healthConnectAvailable(context)) {
                    healthLauncher.launch(healthRequestPermissions(context))
                } else {
                    messages.showToast("헬스 커넥트를 설치하거나 업데이트한 뒤 다시 시도해 주세요")
                }
            }
            null -> currentOnResult()
        }
    }
}
