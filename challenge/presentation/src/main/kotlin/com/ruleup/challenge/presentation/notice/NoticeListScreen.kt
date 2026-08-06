package com.ruleup.challenge.presentation.notice

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.challenge.domain.entity.NoticeSummary
import com.ruleup.challenge.presentation.notice.viewmodel.NoticeListIntent
import com.ruleup.challenge.presentation.notice.viewmodel.NoticeListViewModel
import com.ruleup.designsystem.component.RuleUpTopBar
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme

/**
 * 공지 목록 화면. 방 홈의 "공지 전체 보기"로 진입한다.
 * 정렬(고정 우선 → 최신순)·건수(최근 10건)는 서버 고정. 항목 탭 = 상세 진입 = 읽음 처리.
 * 시안 부재 — 기존 디자인 시스템 토큰으로 구성 (#110).
 */
@Composable
fun NoticeListScreen(
    challengeId: String,
    canManage: Boolean,
    modifier: Modifier = Modifier,
    viewModel: NoticeListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(challengeId) {
        viewModel.onIntent(NoticeListIntent.Load(challengeId, canManage))
    }
    // 상세(읽음)·작성 화면에서 돌아오면 목록을 갱신한다.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onIntent(NoticeListIntent.Refresh)
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background)
                .statusBarsPadding(),
    ) {
        NoticeListTopBar(
            canManage = state.canManage,
            onBack = { viewModel.onIntent(NoticeListIntent.Back) },
            onCreate = { viewModel.onIntent(NoticeListIntent.CreateNotice) },
        )

        when {
            state.isLoading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RuleUpTheme.colors.brand)
                }

            state.errorMessage != null ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.errorMessage.orEmpty(),
                        color = RuleUpTheme.colors.textSecondary,
                        style = RuleUpTheme.typography.labelMedium,
                    )
                }

            state.notices.isEmpty() ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // 장식용 글리프라 타입 스케일(최대 22)에 넣으면 확 줄어든다. 그리는 크기로 잡는다.
                        Text(text = "📭", fontSize = 34.sp)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "아직 공지가 없어요",
                            color = RuleUpTheme.colors.textSecondary,
                            style = RuleUpTheme.typography.labelMedium,
                        )
                    }
                }

            else ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.notices, key = { it.noticeId }) { notice ->
                        NoticeCard(
                            notice = notice,
                            onClick = { viewModel.onIntent(NoticeListIntent.OpenNotice(notice.noticeId)) },
                        )
                    }
                }
        }
    }
}

@Composable
private fun NoticeListTopBar(
    canManage: Boolean,
    onBack: () -> Unit,
    onCreate: () -> Unit,
) {
    RuleUpTopBar(
        title = "공지",
        onBack = onBack,
    ) {
        Spacer(Modifier.weight(1f))
        if (canManage) {
            Box(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(RuleUpTheme.colors.brandSoft)
                        .singleClickable(onClick = onCreate)
                        .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Text(
                    text = "+ 작성",
                    color = RuleUpTheme.colors.brand,
                    style = RuleUpTheme.typography.bodyBold,
                )
            }
            Spacer(Modifier.width(8.dp))
        }
    }
}

@Composable
private fun NoticeCard(
    notice: NoticeSummary,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(RuleUpTheme.colors.surface)
                .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(14.dp))
                .singleClickable(onClick = onClick)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (notice.pinned) {
                Text(text = "📌", style = RuleUpTheme.typography.small)
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = notice.title,
                color = RuleUpTheme.colors.textPrimary,
                style = if (notice.isRead) RuleUpTheme.typography.labelMedium else RuleUpTheme.typography.cardTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (!notice.isRead) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier =
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(RuleUpTheme.colors.danger),
                )
            }
        }
        if (notice.preview.isNotBlank()) {
            Text(
                text = notice.preview,
                color = RuleUpTheme.colors.textSecondary,
                style = RuleUpTheme.typography.small,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = noticeDateLabel(notice.createdAt),
            color = RuleUpTheme.colors.textMuted,
            style = RuleUpTheme.typography.caption,
        )
    }
}
