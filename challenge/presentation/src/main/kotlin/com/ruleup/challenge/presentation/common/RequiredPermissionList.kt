package com.ruleup.challenge.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.verification.domain.entity.PermissionSnapshot

/**
 * 이 챌린지가 쓰는 권한 목록(서버가 준 requiredPermissions).
 * 권한마다 무엇에 쓰는지 한 줄, [snapshot] 이 있으면 지금 허용됐는지도 붙인다.
 */
@Composable
internal fun RequiredPermissionList(
    tokens: List<String>,
    snapshot: PermissionSnapshot?,
    modifier: Modifier = Modifier,
) {
    // 같은 권한을 다른 이름으로 겹쳐 보낼 수 있어 설명 기준으로 한 줄씩만 둔다
    val rows = tokens.distinctBy(::permissionDescription)
    if (rows.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = "필요한 권한", color = RuleUpTheme.colors.textMuted, style = RuleUpTheme.typography.smallBold)
        rows.forEach { token ->
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = "· ${permissionDescription(token)}",
                    color = RuleUpTheme.colors.textSlate,
                    style = RuleUpTheme.typography.small,
                    modifier = Modifier.weight(1f),
                )
                snapshot?.isGranted(token)?.let { granted ->
                    Text(
                        text = if (granted) "허용됨" else "꺼짐",
                        color = if (granted) RuleUpTheme.colors.success else RuleUpTheme.colors.danger,
                        style = RuleUpTheme.typography.smallBold,
                    )
                }
            }
        }
    }
}
