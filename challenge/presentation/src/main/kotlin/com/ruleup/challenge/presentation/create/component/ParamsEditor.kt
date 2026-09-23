package com.ruleup.challenge.presentation.create.component

import android.app.TimePickerDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ruleup.challenge.domain.entity.ParamKind
import com.ruleup.challenge.domain.entity.ParamSpec
import com.ruleup.challenge.domain.entity.clamp
import com.ruleup.challenge.domain.entity.isInRange
import com.ruleup.challenge.domain.entity.rangeLabel
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
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RuleUpTheme.shapes.small)
                .background(RuleUpTheme.colors.surfaceVariant)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
        // 범위를 벗어난 값은 만들기 버튼이 잠기므로, 왜 잠겼는지 여기서 말한다. 안내가 없으면
        // 사용자는 버튼이 고장난 것으로 읽는다.
        if (!spec.isInRange) {
            spec.rangeLabel()?.let { range ->
                Text(
                    text = "${range}${spec.unit?.let { " $it" }.orEmpty()} 사이로 입력해 주세요",
                    color = RuleUpTheme.colors.danger,
                    style = RuleUpTheme.typography.caption,
                )
            }
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

/** 시각 목표값(`HH:mm`). 숫자 키패드로 받으면 콜론을 칠 수 없어 형식이 깨진다 — 시각 선택기로 받는다. */
@Composable
private fun TimeField(
    spec: ParamSpec,
    modifier: Modifier = Modifier,
    onEdit: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val (hour, minute) = spec.value.toHourMinute()
    Text(
        text = spec.value,
        color = RuleUpTheme.colors.textPrimary,
        style = RuleUpTheme.typography.bodyBold,
        modifier =
            modifier.singleClickable {
                TimePickerDialog(
                    context,
                    { _, h, m -> onEdit("%02d:%02d".format(h, m)) },
                    hour,
                    minute,
                    true,
                ).show()
            },
    )
}

// 형식을 모르면 07:00 부터 고르게 한다 — 00:00 을 기본으로 두면 한밤중 루틴으로 저장되기 쉽다.
private fun String.toHourMinute(): Pair<Int, Int> {
    val parts = split(':').mapNotNull { it.trim().toIntOrNull() }
    return if (parts.size >= 2 && parts[0] in 0..23 && parts[1] in 0..59) parts[0] to parts[1] else 7 to 0
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

/** 표시 라벨. 서버가 라벨을 주지 않아 key 를 펴면 영어('Target time')가 보인다 — 위젯 종류로 한국어 라벨을 붙인다. */
private fun ParamSpec.label(): String =
    when (kind) {
        ParamKind.TIME -> "목표 시각"
        ParamKind.NUMBER -> "목표값"
    }

/** 정수면 소수점을 떼고 보낸다 — 서버가 `"3"` 을 기대하는데 `"3.0"` 을 보내면 형식 검증에 걸린다. */
private fun Double.format(): String = if (this % 1.0 == 0.0) toLong().toString() else toString()
