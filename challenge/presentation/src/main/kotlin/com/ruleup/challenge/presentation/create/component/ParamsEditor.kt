package com.ruleup.challenge.presentation.create.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruleup.challenge.domain.entity.ParamKind
import com.ruleup.challenge.domain.entity.ParamSpec
import com.ruleup.challenge.domain.entity.ParamValue
import com.ruleup.challenge.presentation.create.viewmodel.CreateChallengeIntent
import com.ruleup.designsystem.theme.RuleUpTheme
import kotlin.math.roundToInt

/** 목표값 편집기. NUMBER(범위 有)는 슬라이더, TIME·범위 없는 값은 텍스트로 "얼만큼 할지"를 입력받는다. */
@Composable
fun ParamsEditor(
    params: List<ParamSpec>,
    onIntent: (CreateChallengeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(text = "목표값")
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RuleUpTheme.shapes.large)
                    .background(RuleUpTheme.colors.surface)
                    .border(1.dp, RuleUpTheme.colors.border, RuleUpTheme.shapes.large)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            params.forEachIndexed { index, spec ->
                if (index > 0) ParamRowDivider()
                ParamRow(spec = spec, onIntent = onIntent)
            }
        }
    }
}

@Composable
private fun ParamRow(
    spec: ParamSpec,
    onIntent: (CreateChallengeIntent) -> Unit,
) {
    val min = spec.min
    val max = spec.max
    // 범위(min·max)가 있는 숫자 목표값은 슬라이더로, TIME·범위 없는 값은 텍스트 입력으로.
    if (spec.kind == ParamKind.NUMBER && min != null && max != null && max > min) {
        NumberSliderRow(spec = spec, min = min, max = max, onIntent = onIntent)
    } else {
        TextParamRow(spec = spec, onIntent = onIntent)
    }
}

/** min~max 슬라이더로 "얼만큼 할지"를 고른다. 정수 범위면 정수 스텝, 현재값·단위는 상단에 강조 표시. */
@Composable
private fun NumberSliderRow(
    spec: ParamSpec,
    min: Double,
    max: Double,
    onIntent: (CreateChallengeIntent) -> Unit,
) {
    val committed = ((spec.value as? ParamValue.Num)?.value ?: min).coerceIn(min, max)
    val isInt = min % 1.0 == 0.0 && max % 1.0 == 0.0
    val span = (max - min).toInt()
    // 정수 범위가 촘촘하면 각 정수에 스냅(steps=칸수-1), 넓거나 소수면 연속으로 두고 반올림.
    val steps = if (isInt && span in 2..50) span - 1 else 0
    // 드래그 중에는 로컬 값만 갱신하고 손을 뗄 때 한 번만 State 로 커밋한다.
    var dragValue by remember(committed) { mutableStateOf(committed.toFloat()) }
    val shown = if (isInt) dragValue.roundToInt().toDouble() else dragValue.toDouble()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                spec.key,
                color = RuleUpTheme.colors.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    shown.asParamText(),
                    color = RuleUpTheme.colors.brand,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                spec.unit?.let {
                    Text(
                        it,
                        color = RuleUpTheme.colors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }
            }
        }
        Slider(
            value = dragValue,
            onValueChange = { dragValue = it },
            onValueChangeFinished = {
                val snapped = if (isInt) dragValue.roundToInt().toDouble() else dragValue.toDouble()
                onIntent(CreateChallengeIntent.EditParam(spec.key, ParamValue.Num(snapped.coerceIn(min, max))))
            },
            valueRange = min.toFloat()..max.toFloat(),
            steps = steps,
            colors =
                SliderDefaults.colors(
                    thumbColor = RuleUpTheme.colors.brand,
                    activeTrackColor = RuleUpTheme.colors.brand,
                    inactiveTrackColor = RuleUpTheme.colors.surfaceVariant,
                ),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(min.asParamText(), color = RuleUpTheme.colors.textMuted, fontSize = 10.sp)
            Text(max.asParamText(), color = RuleUpTheme.colors.textMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun TextParamRow(
    spec: ParamSpec,
    onIntent: (CreateChallengeIntent) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                spec.key,
                color = RuleUpTheme.colors.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            val min = spec.min
            val max = spec.max
            val rangeHint =
                when {
                    spec.kind == ParamKind.TIME -> "HH:mm"
                    min != null && max != null -> "${min.toInt()}~${max.toInt()}"
                    else -> null
                }
            rangeHint?.let { Text(it, color = RuleUpTheme.colors.textMuted, fontSize = 10.sp) }
        }
        Row(
            modifier =
                Modifier
                    .width(120.dp)
                    .height(40.dp)
                    .clip(RuleUpTheme.shapes.small)
                    .background(RuleUpTheme.colors.background)
                    .border(1.dp, RuleUpTheme.colors.border, RuleUpTheme.shapes.small)
                    .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = spec.value.display(),
                onValueChange = { input -> onIntent(CreateChallengeIntent.EditParam(spec.key, spec.parse(input))) },
                singleLine = true,
                textStyle =
                    TextStyle(
                        color = RuleUpTheme.colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                cursorBrush = SolidColor(RuleUpTheme.colors.brand),
                modifier = Modifier.weight(1f),
            )
            spec.unit?.let { Text(it, color = RuleUpTheme.colors.textSecondary, fontSize = 11.sp) }
        }
    }
}

@Composable
private fun ParamRowDivider() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(RuleUpTheme.colors.border),
    )
}

private fun ParamValue.display(): String =
    when (this) {
        is ParamValue.Num -> if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
        is ParamValue.Text -> value
    }

/** 정수면 소수점 없이, 아니면 그대로 표시. */
private fun Double.asParamText(): String = if (this % 1.0 == 0.0) toInt().toString() else toString()

/** 입력 문자열을 spec 의 kind 에 맞는 ParamValue 로 변환(NUMBER 는 min/max clamp). */
private fun ParamSpec.parse(input: String): ParamValue =
    when (kind) {
        ParamKind.NUMBER -> {
            val number = input.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0
            val clamped = number.coerceIn(min ?: number, max ?: number)
            ParamValue.Num(clamped)
        }
        ParamKind.TIME -> ParamValue.Text(input)
    }
