package com.ruleup.android_ruleup.debug

import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.ruleup.ui.helper.rememberSingleClick
import com.ruleup.verification.domain.port.SyncScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import timber.log.Timber

/**
 * 디버그 빌드 전용 "지금 수집·동기화" 트리거. 30분 주기를 기다리지 않고 expedited catch-up 으로
 * 실제 [com.ruleup.verification.data.sync.VerificationSyncWorker] 를 즉시 한 번 돌린다.
 *
 * 결과는 Logcat + 화면 우측 상단 [DebugLogOverlay] 에 'VerifySync' 태그로 뜬다:
 * 스코프(타깃 수) → 타입별 수집 건수 → 서버 결과(갱신/무시) 또는 전송 생략 사유.
 */
@Composable
fun DebugSyncButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    FilledTonalButton(
        onClick =
            rememberSingleClick {
                val scheduler =
                    EntryPointAccessors
                        .fromApplication(context.applicationContext, DebugToolsEntryPoint::class.java)
                        .syncScheduler()
                Timber.tag("VerifySync").i("▶ 디버그 수집·동기화 트리거 (catch-up enqueue)")
                scheduler.enqueueCatchUp()
            },
        modifier = modifier,
    ) {
        Text("수집·동기화")
    }
}

/** [SyncScheduler] 를 컴포저블(비-Hilt)에서 꺼내기 위한 Hilt 진입점(디버그 트리거 전용). */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface DebugToolsEntryPoint {
    fun syncScheduler(): SyncScheduler
}
