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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ruleup.challenge.domain.entity.ChallengeLimits
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import kotlin.math.roundToInt

/** 정원 표기. null 은 무제한이다 — 0명으로 그리면 아무도 못 들어오는 방처럼 보인다. */
internal fun capacityLabel(capacity: Int?): String = capacity?.let { "${it}명" } ?: "무제한"

/**
 * 정원 단계 슬라이더 (Figma 1134:691). 값이 아니라 단계 인덱스로 움직인다 — 5·30·100·300 은 간격이
 * 고르지 않아 값 축으로 두면 앞 단계가 한쪽에 몰린다. 라벨을 탭해도 고른다.
 *
 * [steps] 는 고를 수 있는 단계만 받는다 — 수정 화면은 현재 인원보다 작은 단계를 빼서 넘긴다.
 */
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
        // 고를 단계가 하나뿐이면 움직일 축이 없다(Slider 는 빈 범위를 받지 않는다).
        if (steps.size > 1) {
            Slider(
                value = index.toFloat(),
                onValueChange = { onChange(steps[it.roundToInt()]) },
                valueRange = 0f..steps.lastIndex.toFloat(),
                // 양 끝을 뺀 내부 눈금 수
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
