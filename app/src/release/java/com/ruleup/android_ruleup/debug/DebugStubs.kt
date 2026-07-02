package com.ruleup.android_ruleup.debug

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import timber.log.Timber

/*
 * 릴리스 빌드용 no-op 스텁.
 *
 * 실제 디버그 도구(로그 오버레이·sync 트리거·jank 측정·로그 트리)는 src/debug 에만 있어 릴리스 APK 에
 * 포함되지 않는다. main 코드가 BuildConfig.DEBUG 가드 아래에서 이 심볼들을 참조하므로, 릴리스 컴파일이
 * 통과하도록 동일 시그니처의 빈 구현만 제공한다(런타임엔 호출되지 않음).
 */

/** 릴리스에선 아무 것도 기록하지 않는 Timber 트리(실제로는 plant 되지 않는다). */
class DebugLogTree : Timber.Tree() {
    override fun log(
        priority: Int,
        tag: String?,
        message: String,
        t: Throwable?,
    ) = Unit
}

@Composable
fun DebugLogOverlay(modifier: Modifier = Modifier) = Unit

@Composable
fun DebugSyncButton(modifier: Modifier = Modifier) = Unit

@Composable
fun TrackJankScreen(screen: String) = Unit
