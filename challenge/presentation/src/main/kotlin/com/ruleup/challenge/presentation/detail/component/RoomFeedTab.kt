package com.ruleup.challenge.presentation.detail.component

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ruleup.challenge.domain.entity.OwnerType
import com.ruleup.challenge.domain.entity.ThreadItem
import com.ruleup.challenge.domain.entity.ThreadItemType
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailState
import com.ruleup.designsystem.component.StatusChip
import com.ruleup.designsystem.component.StatusChipTone
import com.ruleup.designsystem.component.ruleUpCardSurface
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme

/** 피드 탭. */
@Composable
internal fun RoomFeedTab(
    state: ChallengeDetailState,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // 하단 근처(마지막 3개)에 닿으면 다음 페이지를 미리 받는다.
    val shouldPage by remember(state.threads.size) {
        derivedStateOf {
            val last =
                listState.layoutInfo.visibleItemsInfo
                    .lastOrNull()
                    ?.index ?: return@derivedStateOf false
            last >= listState.layoutInfo.totalItemsCount - 3
        }
    }
    // 이펙트를 콜백 신원 변화로 재시작시키지 않는다
    val loadMore by rememberUpdatedState(onLoadMore)
    LaunchedEffect(listState, state.canLoadMoreThreads) {
        snapshotFlow { shouldPage }
            .collect { if (it && state.canLoadMoreThreads) loadMore() }
    }

    when {
        state.isThreadsLoading && state.threads.isEmpty() ->
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = RuleUpTheme.colors.brand)
            }

        state.threads.isEmpty() && state.threadsError != null ->
            RoomEmptyState(
                modifier = modifier,
                message = state.threadsError,
                actionLabel = "다시 불러오기",
                onAction = onRetry,
            )

        state.threads.isEmpty() ->
            FeedEmptyState(
                modifier = modifier,
                ownerType = state.room?.ownerType,
                isOwner = state.room?.myRole?.isOwner == true,
            )

        else ->
            LazyColumn(
                state = listState,
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // 같은 날짜끼리 묶어 "오늘 / 어제 / 7월 25일" 헤더를 세운다.
                var lastDateKey: String? = null
                state.threads.forEach { item ->
                    val key = feedDateKey(item.at)
                    if (key != lastDateKey) {
                        lastDateKey = key
                        item(key = "date-$key") {
                            Text(
                                text = feedDateHeader(item.at),
                                color = RuleUpTheme.colors.textSecondary,
                                style = RuleUpTheme.typography.smallBold,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                    item(key = "${item.type.value}-${item.id}") {
                        ThreadItemCard(item = item, isMe = item.user.id == state.myUserId)
                    }
                }

                if (state.isThreadsPaging) {
                    item(key = "paging") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                color = RuleUpTheme.colors.brand,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }

                // 이어받기 실패는 기존 목록을 지우지 않는다
                state.threadsError?.takeIf { state.threads.isNotEmpty() }?.let { message ->
                    item(key = "paging-error") {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = message,
                                color = RuleUpTheme.colors.textMuted,
                                style = RuleUpTheme.typography.small,
                            )
                            Text(
                                text = "다시 불러오기",
                                color = RuleUpTheme.colors.brand,
                                style = RuleUpTheme.typography.smallBold,
                                modifier =
                                    Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .singleClickable(onClick = onRetry)
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }
    }
}

/** 빈 피드. */
@Composable
private fun FeedEmptyState(
    ownerType: OwnerType?,
    isOwner: Boolean,
    modifier: Modifier = Modifier,
) {
    when {
        ownerType == OwnerType.BOT ->
            RoomEmptyState(
                modifier = modifier,
                message = "아직 소식이 없어요\n이 방은 방장 자리가 비어 있어요",
            )

        isOwner ->
            RoomEmptyState(
                modifier = modifier,
                message = "아직 소식이 없어요\n멤버들의 인증이 확정되면 여기에 쌓여요",
            )

        else ->
            RoomEmptyState(
                modifier = modifier,
                message = "곧 멤버들의 인증 소식이 올라와요",
            )
    }
}

/** 피드 아이템 카드. */
@Composable
private fun ThreadItemCard(
    item: ThreadItem,
    isMe: Boolean,
) {
    Column(modifier = Modifier.ruleUpCardSurface()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoomAvatar(nickname = item.user.nickname, highlighted = isMe)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isMe) "${item.user.nickname} (나)" else item.user.nickname,
                    color = RuleUpTheme.colors.textPrimary,
                    style = RuleUpTheme.typography.bodyBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = item.subtitle(),
                    color = RuleUpTheme.colors.textSecondary,
                    style = RuleUpTheme.typography.caption,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(10.dp))
            val (label, tone) = item.statusChip()
            StatusChip(text = label, tone = tone)
        }
    }
}

/** 아이템 종류별 부제. */
private fun ThreadItem.subtitle(): String =
    when (type) {
        ThreadItemType.VERIFY_SUCCESS -> {
            val time = feedTimeLabel(at)
            // 연속 일수는 반응을 부르는 신호라 기능 스펙이 성공 이벤트에 노출하도록 정했다.
            val streakLabel = streak?.takeIf { it > 1 }?.let { "연속 ${it}일" }
            listOfNotNull(time.ifBlank { null }, streakLabel).joinToString(" · ").ifBlank { "인증 성공" }
        }

        ThreadItemType.VERIFY_FAIL -> failDateLabel(failDate)
    }

private fun ThreadItem.statusChip(): Pair<String, StatusChipTone> =
    when (type) {
        ThreadItemType.VERIFY_SUCCESS -> "성공" to StatusChipTone.Success
        ThreadItemType.VERIFY_FAIL -> "실패" to StatusChipTone.Danger
    }

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun RoomFeedTabPreview() {
    RuleUpTheme {
        RoomFeedTab(
            state =
                com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailState.initial.copy(
                    isLoading = false,
                ),
            onLoadMore = {
            },
            onRetry = { },
        )
    }
}
