package com.ruleup.notification.presentation.center

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.designsystem.component.RuleUpTopBar
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.notification.domain.entity.Notification
import com.ruleup.notification.domain.entity.NotificationGroup
import com.ruleup.notification.presentation.center.viewmodel.NotificationCenterEffect
import com.ruleup.notification.presentation.center.viewmodel.NotificationCenterIntent
import com.ruleup.notification.presentation.center.viewmodel.NotificationCenterState
import com.ruleup.notification.presentation.center.viewmodel.NotificationCenterViewModel
import com.ruleup.ui.helper.LocalMessageHelper
import com.ruleup.ui.helper.LocalNavigationHelper

/**
 * 알림 센터 (Figma 1134:1455).
 *
 * **모든 알림은 푸시 여부와 무관하게 여기 쌓인다** — 푸시를 못 받았거나 야간에 밀린 알림도
 * 반드시 있다. 그래서 이 화면이 비어 보이면 그건 정말 알림이 없는 것이다.
 *
 * Figma 와 다르게 간 곳
 * - **유형 필터 칩을 두지 않는다** — 명세가 "유형 탭 필터는 없다(P2)"로 확정했다
 * - **목록 안 수락/거절 버튼을 두지 않는다** — 딥링크로 해당 화면에 들어가는 것이 계약이다
 * - **「모두 읽음」 버튼을 두지 않는다** — 진입만으로 읽음 처리되고 개별 읽음 API 는 없다
 */
@Composable
fun NotificationCenterScreen(
    modifier: Modifier = Modifier,
    viewModel: NotificationCenterViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val messageHelper = LocalMessageHelper.current
    val navigationHelper = LocalNavigationHelper.current

    LaunchedEffect(Unit) { viewModel.onIntent(NotificationCenterIntent.Load) }
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is NotificationCenterEffect.ShowMessage -> messageHelper.showToast(effect.message)

                is NotificationCenterEffect.OpenDeeplink ->
                    // 딥링크 해석은 :app 이 한다 — feature 가 앱 전체 라우트 표를 알 이유가 없다.
                    navigationHelper.navigateByDeeplink(effect.deeplink)
            }
        }
    }

    NotificationCenterContent(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

/** 상태를 받아 그리기만 한다 — ViewModel 을 직접 꺼내지 않아 상태별 렌더를 그대로 검증할 수 있다. */
@Composable
internal fun NotificationCenterContent(
    state: NotificationCenterState,
    onIntent: (NotificationCenterIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background)
                .statusBarsPadding(),
    ) {
        RuleUpTopBar(title = "알림", onBack = { onIntent(NotificationCenterIntent.Back) })

        when {
            state.isLoading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RuleUpTheme.colors.brand)
                }

            state.errorMessage != null && state.items.isEmpty() ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.errorMessage,
                        color = RuleUpTheme.colors.textSecondary,
                        style = RuleUpTheme.typography.labelMedium,
                    )
                }

            state.items.isEmpty() -> EmptyState()

            else -> NotificationList(state = state, onIntent = onIntent)
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "아직 알림이 없어요",
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.cardTitle,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "판정 결과나 챌린지 소식이 생기면 여기에 쌓여요",
            color = RuleUpTheme.colors.textMuted,
            style = RuleUpTheme.typography.small,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun NotificationList(
    state: NotificationCenterState,
    onIntent: (NotificationCenterIntent) -> Unit,
) {
    val listState = rememberLazyListState()

    if (state.hasMore) {
        val shouldLoadMore by remember(state.items.size) {
            derivedStateOf {
                val last =
                    listState.layoutInfo.visibleItemsInfo
                        .lastOrNull()
                        ?.index ?: 0
                last >= state.items.size - 3
            }
        }
        // onIntent 를 이펙트 안에서 직접 잡으면 재구성 때 옛 람다가 남는다.
        val loadMore by rememberUpdatedState { onIntent(NotificationCenterIntent.LoadMore) }
        LaunchedEffect(listState) {
            snapshotFlow { shouldLoadMore }.collect { if (it) loadMore() }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(state.items, key = { it.id }) { notification ->
            NotificationRow(
                notification = notification,
                unread = notification.id in state.unreadIds,
                onClick = { onIntent(NotificationCenterIntent.Open(notification)) },
            )
        }
        if (state.isLoadingMore) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = RuleUpTheme.colors.brand, modifier = Modifier.size(22.dp))
                }
            }
        }
        state.retentionDays?.let { days ->
            item {
                Text(
                    text = "알림은 ${days / 30}개월 동안 보관돼요",
                    color = RuleUpTheme.colors.textMuted,
                    style = RuleUpTheme.typography.caption,
                    modifier = Modifier.padding(top = 10.dp, bottom = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun NotificationRow(
    notification: Notification,
    unread: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(RuleUpTheme.colors.surface)
                .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(16.dp))
                .singleClickable(onClick = onClick)
                .padding(16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            notification.type?.let { type ->
                Box(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(RuleUpTheme.colors.surfaceVariant)
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = type.group.label,
                        color = RuleUpTheme.colors.textSecondary,
                        style = RuleUpTheme.typography.micro,
                    )
                }
                Spacer(Modifier.height(6.dp))
            }
            Text(
                text = notification.title,
                color = RuleUpTheme.colors.textPrimary,
                style = RuleUpTheme.typography.bodyMedium,
            )
            notification.body?.let {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = it,
                    color = RuleUpTheme.colors.textSecondary,
                    style = RuleUpTheme.typography.small,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = relativeTime(notification.createdAt),
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.caption,
            )
        }
        if (unread) {
            Spacer(Modifier.width(10.dp))
            Box(
                modifier =
                    Modifier
                        .padding(top = 4.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(RuleUpTheme.colors.brand),
            )
        }
    }
}

/** 목록 뱃지 문구. 타입 22종을 다 쓰지 않고 그룹으로 묶는다 — 사용자가 구분해야 할 단위가 그것이다. */
private val NotificationGroup.label: String
    get() =
        when (this) {
            NotificationGroup.ACCOUNT -> "계정"
            NotificationGroup.CHALLENGE -> "챌린지"
            NotificationGroup.MARKETING -> "소식"
            NotificationGroup.REMINDER -> "리마인더"
        }

/**
 * "2026.09.04" — 상대 시각은 쓰지 않는다.
 *
 * `createdAt` 이 **고지가 성립한 시각**이라, "1시간 전"처럼 기준이 흐린 표기보다 날짜를 남기는 편이
 * 나중에 사용자가 언제 통지받았는지 따질 때 쓸모 있다.
 */
private fun relativeTime(iso: String): String {
    val date = iso.substringBefore('T').split('-')
    if (date.size != 3) return iso
    return date.joinToString(".")
}
