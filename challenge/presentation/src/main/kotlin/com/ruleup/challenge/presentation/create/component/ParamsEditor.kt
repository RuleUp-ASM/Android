package com.ruleup.challenge.presentation.create.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ruleup.challenge.domain.entity.ParamKind
import com.ruleup.challenge.domain.entity.ParamSpec
import com.ruleup.challenge.domain.entity.clamp
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme

/**
 * 목표값 편집기.
 *
 * **루틴별 분기를 하드코딩하지 않는다** — 서버가 준 [ParamSpec.kind]·[ParamSpec.unit]·[ParamSpec.min]·
 * [ParamSpec.max] 로 위젯과 범위를 결정한다. 루틴 테이블이 서버에서 계속 늘어나기 때문에, 여기에 키
 * 이름별 `when` 을 넣으면 새 루틴이 추가될 때마다 앱을 고쳐야 한다.
 */
@Composable
fun ParamsEditor(
    params: List<ParamSpec>,
    modifier: Modifier = Modifier,
    onEdit: (key: String, value: String) -> Unit = { _, _ -> },
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        params.forEach { spec ->
            ParamRow(spec = spec, onEdit = { onEdit(spec.key, it) })
        }
    }
}

@Composable
private fun ParamRow(
    spec: ParamSpec,
    modifier: Modifier = Modifier,
    onEdit: (String) -> Unit = {},
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RuleUpTheme.shapes.small)
                .background(RuleUpTheme.colors.surfaceVariant)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = spec.label(),
            color = RuleUpTheme.colors.textSecondary,
            style = RuleUpTheme.typography.bodyMedium,
        )
        when (spec.kind) {
            ParamKind.NUMBER -> NumberStepper(spec = spec, onEdit = onEdit)
            ParamKind.TIME -> TimeField(spec = spec, onEdit = onEdit)
        }
    }
}

/**
 * 숫자 목표값. 버튼만으로 큰 값을 올리게 두지 않고 **직접 입력도 허용**한다.
 * 범위는 서버가 준 min·max 로 잠근다(없으면 잠그지 않는다).
 */
@Composable
private fun NumberStepper(
    spec: ParamSpec,
    modifier: Modifier = Modifier,
    onEdit: (String) -> Unit = {},
) {
    val current = spec.value.toDoubleOrNull()
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton(text = "−", enabled = current != null) {
            current?.let { onEdit(spec.clamp(it - 1).format()) }
        }
        BasicTextField(
            value = spec.value,
            onValueChange = { input ->
                // 숫자 위젯이라도 입력 도중의 빈 문자열은 허용한다 — 지우자마자 0으로 튀면 편집이 불가능해진다.
                val digits = input.filter { it.isDigit() || it == '.' }
                onEdit(digits)
            },
            modifier = Modifier.size(width = 56.dp, height = 24.dp),
            textStyle =
                RuleUpTheme.typography.bodyBold.copy(color = RuleUpTheme.colors.textPrimary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
        )
        spec.unit?.let {
            Text(it, color = RuleUpTheme.colors.textMuted, style = RuleUpTheme.typography.caption)
        }
        StepButton(text = "+", enabled = current != null) {
            current?.let { onEdit(spec.clamp(it + 1).format()) }
        }
    }
}

/** 시각 목표값(`hh:mm`). 자유 입력으로 두고 형식은 서버가 재검증한다. */
@Composable
private fun TimeField(
    spec: ParamSpec,
    modifier: Modifier = Modifier,
    onEdit: (String) -> Unit = {},
) {
    BasicTextField(
        value = spec.value,
        onValueChange = onEdit,
        modifier = modifier.size(width = 72.dp, height = 24.dp),
        textStyle = RuleUpTheme.typography.bodyBold.copy(color = RuleUpTheme.colors.textPrimary),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
    )
}

@Composable
private fun StepButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    Box(
        modifier =
            modifier
                .size(28.dp)
                .clip(RuleUpTheme.shapes.pill)
                .background(RuleUpTheme.colors.surface)
                .singleClickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (enabled) RuleUpTheme.colors.textPrimary else RuleUpTheme.colors.textMuted,
            style = RuleUpTheme.typography.bodyBold,
        )
    }
}

/** 표시 라벨. 서버가 라벨을 주지 않으므로 key 를 사람이 읽을 형태로 편다(`target_time` → `Target time`). */
private fun ParamSpec.label(): String =
    key
        .replace('_', ' ')
        .replaceFirstChar { it.uppercase() }

/** 정수면 소수점을 떼고 보낸다 — 서버가 `"3"` 을 기대하는데 `"3.0"` 을 보내면 형식 검증에 걸린다. */
private fun Double.format(): String = if (this % 1.0 == 0.0) toLong().toString() else toString()
