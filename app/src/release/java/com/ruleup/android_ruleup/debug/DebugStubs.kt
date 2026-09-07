package com.ruleup.android_ruleup.debug

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/*
 * 릴리스 빌드용 no-op 스텁. 실제 구현은 src/debug 에만 있는데 main 이 BuildConfig.DEBUG 가드
 * 아래에서 이 심볼을 참조하므로, 릴리스 컴파일이 통과하도록 같은 시그니처만 둔다.
 */

@Composable
fun DebugLogOverlay(modifier: Modifier = Modifier) = Unit

@Composable
fun DebugSyncButton(modifier: Modifier = Modifier) = Unit
