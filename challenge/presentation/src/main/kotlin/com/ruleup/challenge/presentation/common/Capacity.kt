package com.ruleup.challenge.presentation.common

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ruleup.challenge.domain.entity.ChallengeLimits
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import kotlin.math.roundToInt

/** 정원 표기. */
internal fun capacityLabel(capacity: Int?): String = capacity?.let { "${it}명" } ?: "무제한"

/** 정원 단계 슬라이더 */
@Composable
internal fun CapacitySlider(
    capacity: Int?,
    onChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    steps: List<Int?> = ChallengeLimits.CREATE_CAPACITY_STEPS,
) {
    val index = steps.indexOf(capacity).coerceAtLeast(0)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .border(1.dp, RuleUpTheme.colors.border, RuleUpTheme.shapes.medium)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("정원", color = RuleUpTheme.colors.textPrimary, style = RuleUpTheme.typography.bodyMedium)
            Text(capacityLabel(capacity), color = RuleUpTheme.colors.brand, style = RuleUpTheme.typography.numberS)
        }
        if (steps.size > 1) {
            Slider(
                value = index.toFloat(),
                onValueChange = { onChange(steps[it.roundToInt()]) },
                valueRange = 0f..steps.lastIndex.toFloat(),
                steps = steps.size - 2,
                colors =
                    SliderDefaults.colors(
                        thumbColor = RuleUpTheme.colors.brand,
                        activeTrackColor = RuleUpTheme.colors.brand,
                        inactiveTrackColor = RuleUpTheme.colors.border,
                        activeTickColor = RuleUpTheme.colors.brandSoft,
                        inactiveTickColor = RuleUpTheme.colors.borderStrong,
                    ),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            steps.forEach { step ->
                val selected = step == capacity
                Text(
                    text = step?.toString() ?: "무제한",
                    modifier = Modifier.singleClickable { onChange(step) },
                    color = if (selected) RuleUpTheme.colors.brand else RuleUpTheme.colors.textMuted,
                    style = if (selected) RuleUpTheme.typography.smallBold else RuleUpTheme.typography.smallMedium,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun CapacitySliderPreview() {
    RuleUpTheme {
        var capacity by remember { mutableStateOf(ChallengeLimits.CREATE_CAPACITY_STEPS[1]) }
        CapacitySlider(
            capacity = capacity,
            onChange = { capacity = it },
            modifier = Modifier.padding(16.dp),
        )
    }
}
