package com.ruleup.onboarding.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.onboarding.domain.auth.usecase.ValidateBirthDateUseCase
import com.ruleup.onboarding.domain.navigation.OnboardingGenderPage
import com.ruleup.onboarding.domain.observability.OnboardingStep
import com.ruleup.onboarding.presentation.component.OnboardingScaffold
import com.ruleup.onboarding.presentation.onboarding.component.InfoBox
import com.ruleup.onboarding.presentation.onboarding.component.OnboardingFlowPreview
import com.ruleup.onboarding.presentation.onboarding.component.SectionHeader
import com.ruleup.onboarding.presentation.onboarding.viewmodel.OnboardingIntent
import com.ruleup.onboarding.presentation.onboarding.viewmodel.OnboardingState
import com.ruleup.ui.helper.LocalNavigationHelper
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 03 · 생일. **필수**이고 만 14세 미만은 가입할 수 없다(법적 요구사항). 성별은 화면에서
 * 건너뛸 수 있지만 API 필드는 필수라, 안 고르면 논바이너리로 저장된다.
 *
 * 생일이 유효해야 "다음"으로 넘어간다 — 여기서 막지 않으면 약관까지 다 채운 뒤 마지막 제출에서
 * `BIRTHDATE_UNDERAGE` 로 튕긴다.
 */
@Composable
fun BirthDateContent(
    onIntent: (OnboardingIntent) -> Unit,
    modifier: Modifier = Modifier,
    birthDateInput: String = "",
    birthDateError: String? = null,
    birthDateValid: Boolean = false,
) {
    val nav = LocalNavigationHelper.current
    OnboardingScaffold(
        step = OnboardingStep.BIRTH,
        buttonText = "다음",
        modifier = modifier,
        nextEnabled = birthDateValid,
        onNext = { nav.navigateTo(OnboardingGenderPage) },
        onBack = { nav.navigateToBack() },
    ) {
        SectionHeader(
            title = "생일이 언제예요?",
            subtitle = "가입 조건 확인에만 사용해요",
        )

        BirthDateSection(
            digits = birthDateInput,
            error = birthDateError,
            onChange = { onIntent(OnboardingIntent.SetBirthDate(it)) },
        )

        InfoBox(
            background = RuleUpTheme.colors.brandSoft,
            emoji = "ℹ️",
            text = "만 14세 이상만 가입할 수 있어요. 생일은 가입 후 수정할 수 없어요",
            textColor = RuleUpTheme.colors.brandStrong,
        )
    }
}

/**
 * 생일 입력: 달력에서 고르거나 직접 친다.
 *
 * 직접 입력을 남겨둔 건 이미 익숙한 사용자를 막지 않기 위해서다 — 달력만 두면 8자리를 빠르게 치던
 * 사람이 더 느려진다.
 */
@Composable
private fun BirthDateSection(
    digits: String,
    error: String?,
    onChange: (String) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }

    if (showPicker) {
        BirthDatePickerDialog(
            initial = digits,
            onPick = {
                onChange(it)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("생일", color = RuleUpTheme.colors.textPrimary, style = RuleUpTheme.typography.cardTitle)
            Text("YYYY / MM / DD", color = RuleUpTheme.colors.textSecondary, style = RuleUpTheme.typography.small)
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(RuleUpTheme.colors.surface)
                    .border(
                        1.dp,
                        if (error != null) RuleUpTheme.colors.danger else RuleUpTheme.colors.border,
                        RoundedCornerShape(12.dp),
                    ).padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = digits,
                onValueChange = { input -> onChange(input.filter { it.isDigit() }.take(OnboardingState.BIRTH_DATE_LENGTH)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle =
                    RuleUpTheme.typography.numberS.copy(color = RuleUpTheme.colors.textPrimary),
                cursorBrush = SolidColor(RuleUpTheme.colors.brand),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (digits.isEmpty()) {
                        Text("1999 / 03 / 15", color = RuleUpTheme.colors.textSecondary, style = RuleUpTheme.typography.numberS)
                    }
                    inner()
                },
            )
            Text(
                text = "📅",
                modifier = Modifier.singleClickable { showPicker = true },
                style = RuleUpTheme.typography.body,
            )
        }
        if (error != null) {
            Text(error, color = RuleUpTheme.colors.danger, style = RuleUpTheme.typography.small)
        }
    }
}

@Preview
@Composable
private fun BirthDateScreenPreview() {
    OnboardingFlowPreview {
        BirthDateContent(onIntent = {}, birthDateInput = "19990315", birthDateValid = true)
    }
}

/**
 * 생일 달력.
 *
 * **만 14세가 되는 날 이후는 고를 수 없다.** 지금까지는 고른 뒤에 에러 문구로만 알렸는데, 애초에
 * 못 고르게 하는 편이 낫다 — 법적 요건이라 되돌릴 수 없는 조건이기 때문이다. 미래 날짜도 같은
 * 상한에 함께 걸린다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthDatePickerDialog(
    initial: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val zone = remember { ZoneId.systemDefault() }
    // 만 14세 생일이 오늘인 사람까지 허용한다.
    val latestAllowed = remember { LocalDate.now(zone).minusYears(ValidateBirthDateUseCase.MIN_AGE.toLong()) }
    val latestMillis = remember { latestAllowed.atStartOfDay(zone).toInstant().toEpochMilli() }

    val state =
        rememberDatePickerState(
            initialSelectedDateMillis = initial.toEpochMillisOrNull(zone) ?: latestMillis,
            // 달력을 열자마자 고를 수 없는 해가 보이면 혼란스럽다 — 선택 가능한 범위만 보여준다.
            initialDisplayedMonthMillis = initial.toEpochMillisOrNull(zone) ?: latestMillis,
            yearRange = EARLIEST_YEAR..latestAllowed.year,
            selectableDates =
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= latestMillis

                    override fun isSelectableYear(year: Int) = year <= latestAllowed.year
                },
        )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = state.selectedDateMillis != null,
                onClick = { state.selectedDateMillis?.let { onPick(it.toBirthDigits(zone)) } },
            ) { Text("확인") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    ) {
        DatePicker(state = state, title = null)
    }
}

/** `YYYYMMDD` 8자리를 달력이 쓰는 epoch millis 로. 자릿수가 안 맞거나 없는 날짜면 null. */
private fun String.toEpochMillisOrNull(zone: ZoneId): Long? {
    if (length != BIRTH_DIGITS) return null
    val date =
        runCatching {
            LocalDate.of(substring(0, 4).toInt(), substring(4, 6).toInt(), substring(6, 8).toInt())
        }.getOrNull() ?: return null
    return date.atStartOfDay(zone).toInstant().toEpochMilli()
}

/** 달력 선택값을 화면이 쓰는 `YYYYMMDD` 8자리로. */
private fun Long.toBirthDigits(zone: ZoneId): String {
    val date = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()
    return "%04d%02d%02d".format(date.year, date.monthValue, date.dayOfMonth)
}

private const val BIRTH_DIGITS = 8

// 달력 연도 하한. 실제 가입자 범위를 넉넉히 덮으면서 스크롤이 무의미하게 길어지지 않는 값이다.
private const val EARLIEST_YEAR = 1920
