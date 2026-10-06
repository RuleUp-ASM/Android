package com.ruleup.challenge.presentation.create.component

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ruleup.challenge.domain.entity.ParamKind
import com.ruleup.challenge.domain.entity.ParamSpec
import com.ruleup.challenge.domain.entity.clamp
import com.ruleup.challenge.domain.entity.isInRange
import com.ruleup.challenge.domain.entity.rangeLabel
import com.ruleup.challenge.presentation.common.unitLabel
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import kotlin.math.roundToLong

/** 목표값 편집기. */
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
        if (spec.kind == ParamKind.NUMBER) ParamSlider(spec = spec, onEdit = onEdit)
        // 범위를 벗어난 값은 만들기 버튼이 잠기므로, 왜 잠겼는지 여기서 말한다.
        if (!spec.isInRange) {
            spec.rangeLabel()?.let { range ->
                Text(
                    text = "${range}${spec.unitLabel?.let { " $it" }.orEmpty()} 사이로 입력해 주세요",
                    color = RuleUpTheme.colors.danger,
                    style = RuleUpTheme.typography.caption,
                )
            }
        }
    }
}

/** 숫자 목표값. */
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
        StepButton(plus = false, enabled = current != null && (spec.min?.let { current > it } != false)) {
            current?.let { onEdit(spec.clamp(it - 1).format()) }
        }
        BasicTextField(
            value = spec.value,
            onValueChange = { input ->
                // 숫자 위젯이라도 입력 도중의 빈 문자열은 허용한다
                val digits = input.filter { it.isDigit() || it == '.' }
                if (digits.count { it == '.' } <= 1) onEdit(digits)
            },
            modifier = Modifier.width(56.dp),
            textStyle =
                RuleUpTheme.typography.bodyBold.copy(color = RuleUpTheme.colors.textPrimary, textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            // 버튼과 같은 높이에서 숫자를 가운데에 둔다
            decorationBox = { field ->
                Box(Modifier.height(STEP_BUTTON_SIZE), contentAlignment = Alignment.Center) { field() }
            },
        )
        spec.unitLabel?.let {
            Text(it, color = RuleUpTheme.colors.textMuted, style = RuleUpTheme.typography.caption)
        }
        StepButton(plus = true, enabled = current != null && (spec.max?.let { current < it } != false)) {
            current?.let { onEdit(spec.clamp(it + 1).format()) }
        }
    }
}

/** 숫자 목표값 슬라이더. 양 끝이 정해진 스펙에만 그리고, 1 단위로 끊는다. 정확한 값은 옆 입력칸으로 넣는다. */
@Composable
private fun ParamSlider(
    spec: ParamSpec,
    onEdit: (String) -> Unit,
) {
    val min = spec.min ?: return
    val max = spec.max ?: return
    if (max <= min) return
    Slider(
        value = (spec.value.toDoubleOrNull() ?: min).coerceIn(min, max).toFloat(),
        onValueChange = { onEdit(spec.clamp(it.roundToLong().toDouble()).format()) },
        valueRange = min.toFloat()..max.toFloat(),
        colors =
            SliderDefaults.colors(
                thumbColor = RuleUpTheme.colors.brand,
                activeTrackColor = RuleUpTheme.colors.brand,
                inactiveTrackColor = RuleUpTheme.colors.border,
            ),
    )
}

/** 시각 목표값(`HH:mm`). */
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

// 형식을 모르면 07:00 부터 고르게 한다
private fun String.toHourMinute(): Pair<Int, Int> {
    val parts = split(':').mapNotNull { it.trim().toIntOrNull() }
    return if (parts.size >= 2 && parts[0] in 0..23 && parts[1] in 0..59) parts[0] to parts[1] else 7 to 0
}

/** − / + 버튼. 글자로 그리면 폰트 여백 때문에 원 안에서 위아래가 어긋나 막대로 그린다. */
@Composable
private fun StepButton(
    plus: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    val color = if (enabled) RuleUpTheme.colors.textPrimary else RuleUpTheme.colors.textMuted
    Box(
        modifier =
            modifier
                .size(STEP_BUTTON_SIZE)
                .clip(RuleUpTheme.shapes.pill)
                .background(RuleUpTheme.colors.surface)
                .singleClickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(width = 12.dp, height = 2.dp).clip(RuleUpTheme.shapes.pill).background(color))
        if (plus) Box(Modifier.size(width = 2.dp, height = 12.dp).clip(RuleUpTheme.shapes.pill).background(color))
    }
}

private val STEP_BUTTON_SIZE = 28.dp

/** 표시 라벨. */
private fun ParamSpec.label(): String =
    when (kind) {
        ParamKind.TIME -> "목표 시각"
        ParamKind.NUMBER -> "목표값"
    }

/** 정수면 소수점을 떼고 보낸다 */
private fun Double.format(): String = if (this % 1.0 == 0.0) toLong().toString() else toString()

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun ParamsEditorPreview() {
    RuleUpTheme {
        ParamsEditor(params = emptyList())
    }
}
