package com.ruleup.ui.time

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.ruleup.domain.time.ServiceDate
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.ZonedDateTime

/** 자정과 화면 복귀 때 갱신되는 서비스 날짜. */
@Composable
fun rememberServiceDate(): java.time.LocalDate {
    var today by remember { mutableStateOf(ServiceDate.today()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { today = ServiceDate.today() }
    LaunchedEffect(today) {
        val now = ZonedDateTime.now(ServiceDate.ZONE)
        delay(Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay(ServiceDate.ZONE)).toMillis().coerceAtLeast(1L))
        today = ServiceDate.today()
    }
    return today
}
