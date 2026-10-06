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
import com.ruleup.onboarding.domain.logging.OnboardingStep
import com.ruleup.onboarding.domain.navigation.OnboardingGenderPage
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
import java.time.ZoneOffset

/** 03 · 생일. */
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

/** 생일 입력: 달력에서 고르거나 직접 친다. */
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

/** 생일 달력. */
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
    val latestMillis = remember { latestAllowed.toPickerMillis() }

    val state =
        rememberDatePickerState(
            initialSelectedDateMillis = initial.birthDigitsToPickerMillis() ?: latestMillis,
            // 달력을 열자마자 고를 수 없는 해가 보이면 혼란스럽다
            initialDisplayedMonthMillis = initial.birthDigitsToPickerMillis() ?: latestMillis,
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
                onClick = { state.selectedDateMillis?.let { onPick(it.pickerMillisToBirthDigits()) } },
            ) { Text("확인") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    ) {
        DatePicker(state = state, title = null)
    }
}

// Material3 DatePicker 는 날짜를 UTC 자정 millis 로 주고받는다. 기기 시간대 자정을 넘기면 UTC 보다 빠른 곳(KST)에서 하루 전이 선택된다.

/** `YYYYMMDD` 8자리를 달력이 쓰는 UTC 자정 millis 로. */
internal fun String.birthDigitsToPickerMillis(): Long? {
    if (length != BIRTH_DIGITS) return null
    val date =
        runCatching {
            LocalDate.of(substring(0, 4).toInt(), substring(4, 6).toInt(), substring(6, 8).toInt())
        }.getOrNull() ?: return null
    return date.toPickerMillis()
}

/** 달력 선택값(UTC 자정 millis)을 화면이 쓰는 `YYYYMMDD` 8자리로. */
internal fun Long.pickerMillisToBirthDigits(): String {
    val date = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
    return "%04d%02d%02d".format(date.year, date.monthValue, date.dayOfMonth)
}

private fun LocalDate.toPickerMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private const val BIRTH_DIGITS = 8

// 달력 연도 하한.
private const val EARLIEST_YEAR = 1920
