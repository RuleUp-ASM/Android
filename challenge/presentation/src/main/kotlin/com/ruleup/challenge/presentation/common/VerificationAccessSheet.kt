package com.ruleup.challenge.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ruleup.designsystem.component.RuleUpPrimaryButton
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.domain.entity.user.AgreementType
import com.ruleup.verification.domain.entity.PermissionRequestKind
import com.ruleup.verification.domain.entity.PermissionSnapshot
import com.ruleup.verification.domain.entity.VerificationAccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VerificationAccessSheet(
    access: VerificationAccess,
    isSubmitting: Boolean,
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
) {
    val submitting by rememberUpdatedState(isSubmitting)
    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { it != SheetValue.Hidden || !submitting },
        )
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        containerColor = RuleUpTheme.colors.surface,
    ) {
        VerificationAccessContent(access, isSubmitting, onContinue, onDismiss)
    }
}

@Composable
internal fun VerificationAccessContent(
    access: VerificationAccess,
    isSubmitting: Boolean,
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("자동 인증 설정", color = RuleUpTheme.colors.textPrimary, style = RuleUpTheme.typography.cardTitle)
        Text("자동 인증에 필요한 정보와 접근 권한을 확인해 주세요.", style = RuleUpTheme.typography.body)
        if (AgreementType.LOCATION_INFO in access.missingConsents) {
            Text("위치정보 수집·이용 동의\n룰업이 등록한 장소에서의 인증을 위해 위치정보를 수집하고 이용해요.", style = RuleUpTheme.typography.body)
        }
        if (AgreementType.HEALTH_INFO in access.missingConsents) {
            Text("건강정보 수집·이용 동의\n룰업이 걸음·거리·수면 인증을 위해 필요한 건강 기록을 수집하고 이용해요.", style = RuleUpTheme.typography.body)
        }
        if (access.missingConsents.isNotEmpty()) {
            Text("아래 버튼을 누르면 위 정보의 수집·이용에 동의해요. 동의는 마이 › 계정 · 약관에서 철회할 수 있어요.", style = RuleUpTheme.typography.small)
        }
        access.missingPermissions.distinct().forEach { token ->
            Text(permissionDescription(token), color = RuleUpTheme.colors.textSecondary, style = RuleUpTheme.typography.body)
        }
        if (access.missingPermissions.isNotEmpty()) {
            Text("기기 권한은 이어지는 시스템 화면에서 허용해 주세요.", style = RuleUpTheme.typography.small)
        }
        val nextKind = access.missingPermissions.firstOrNull()?.let(PermissionSnapshot::requestKindOf)
        val buttonText =
            when {
                isSubmitting -> "확인 중…"
                access.missingConsents.isNotEmpty() -> "동의하고 계속하기"
                nextKind == PermissionRequestKind.USAGE_ACCESS_SETTINGS -> "사용 정보 접근 설정 열기"
                nextKind == PermissionRequestKind.HEALTH_CONNECT -> "헬스 커넥트 권한 허용"
                else -> "권한 허용하기"
            }
        RuleUpPrimaryButton(text = buttonText, enabled = !isSubmitting, onClick = onContinue, modifier = Modifier.fillMaxWidth())
        Text(
            text = "다음에 할게요",
            color = RuleUpTheme.colors.textMuted,
            style = RuleUpTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().singleClickable { if (!isSubmitting) onDismiss() }.padding(vertical = 8.dp),
        )
    }
}

private fun permissionDescription(token: String): String =
    when (PermissionSnapshot.normalizeToken(token)) {
        "LOCATION", "ACCESS_FINE_LOCATION", "GPS", "GEOFENCE" -> "위치 접근 · 등록한 장소에서 인증해요"
        "ACCESS_BACKGROUND_LOCATION", "BACKGROUND_LOCATION" -> "백그라운드 위치 · 앱을 열지 않아도 인증해요"
        "PACKAGE_USAGE_STATS", "USAGE_STATS", "SCREEN_TIME" -> "사용 기록 접근 · 선택한 앱의 사용 시간을 확인해요"
        "READ_STEPS", "HEALTH_STEPS", "HEALTH" -> "건강 데이터 접근 · 걸음 수를 확인해요"
        "READ_DISTANCE", "HEALTH_DISTANCE" -> "건강 데이터 접근 · 이동 거리를 확인해요"
        "READ_SLEEP", "HEALTH_SLEEP", "SLEEP" -> "건강 데이터 접근 · 수면 기록을 확인해요"
        "READ_HEALTH_DATA_IN_BACKGROUND", "HEALTH_BACKGROUND" -> "백그라운드 건강 데이터 · 앱을 열지 않아도 기록을 확인해요"
        "POST_NOTIFICATIONS", "NOTIFICATION" -> "알림 · 인증 결과와 권한 복구 안내를 보내요"
        "CAMERA", "PHOTO" -> "카메라 접근"
        else -> token
    }

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun VerificationAccessSheetPreview() {
    RuleUpTheme {
        VerificationAccessSheet(
            access =
                com.ruleup.verification.domain.entity.VerificationAccess(
                    permissions =
                        com.ruleup.verification.domain.entity.PermissionSnapshot(
                            location = com.ruleup.verification.domain.entity.PermissionState.DENIED,
                            backgroundLocation = com.ruleup.verification.domain.entity.PermissionState.DENIED,
                            usageStats = com.ruleup.verification.domain.entity.PermissionState.DENIED,
                            postNotifications = com.ruleup.verification.domain.entity.PermissionState.DENIED,
                            healthDistance = com.ruleup.verification.domain.entity.PermissionState.DENIED,
                            healthSteps = com.ruleup.verification.domain.entity.PermissionState.DENIED,
                            healthSleep = com.ruleup.verification.domain.entity.PermissionState.DENIED,
                            healthBackground = com.ruleup.verification.domain.entity.PermissionState.DENIED,
                        ),
                    missingPermissions = listOf("LOCATION"),
                    missingConsents = listOf(com.ruleup.domain.entity.user.AgreementType.LOCATION_INFO),
                ),
            isSubmitting = false,
            onContinue = {
            },
            onDismiss = { },
        )
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun VerificationAccessContentPreview() {
    RuleUpTheme {
        VerificationAccessContent(
            access =
                com.ruleup.verification.domain.entity.VerificationAccess(
                    permissions =
                        com.ruleup.verification.domain.entity.PermissionSnapshot(
                            location = com.ruleup.verification.domain.entity.PermissionState.DENIED,
                            backgroundLocation = com.ruleup.verification.domain.entity.PermissionState.DENIED,
                            usageStats = com.ruleup.verification.domain.entity.PermissionState.DENIED,
                            postNotifications = com.ruleup.verification.domain.entity.PermissionState.DENIED,
                            healthDistance = com.ruleup.verification.domain.entity.PermissionState.DENIED,
                            healthSteps = com.ruleup.verification.domain.entity.PermissionState.DENIED,
                            healthSleep = com.ruleup.verification.domain.entity.PermissionState.DENIED,
                            healthBackground = com.ruleup.verification.domain.entity.PermissionState.DENIED,
                        ),
                    missingPermissions = listOf("LOCATION"),
                    missingConsents = listOf(com.ruleup.domain.entity.user.AgreementType.LOCATION_INFO),
                ),
            isSubmitting = false,
            onContinue = {
            },
            onDismiss = { },
        )
    }
}
