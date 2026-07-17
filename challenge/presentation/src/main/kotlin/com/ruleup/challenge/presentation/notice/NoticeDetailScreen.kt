package com.ruleup.challenge.presentation.notice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.challenge.domain.entity.NoticeDetail
import com.ruleup.challenge.presentation.notice.viewmodel.NoticeDetailEffect
import com.ruleup.challenge.presentation.notice.viewmodel.NoticeDetailIntent
import com.ruleup.challenge.presentation.notice.viewmodel.NoticeDetailViewModel
import com.ruleup.ui.helper.LocalMessageHelper
import com.ruleup.ui.helper.singleClickable
import com.ruleup.ui.theme.RuleUpTheme

/**
 * 공지 상세 화면. 진입(조회)만으로 서버가 읽음 처리한다 — 별도 읽음 버튼 없음.
 * 방장(canManage)에게만 고정/수정/삭제 메뉴를 노출한다. 시안 부재 — 디자인 시스템 토큰 구성 (#110).
 */
@Composable
fun NoticeDetailScreen(
    challengeId: String,
    noticeId: String,
    canManage: Boolean,
    modifier: Modifier = Modifier,
    viewModel: NoticeDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val messageHelper = LocalMessageHelper.current

    LaunchedEffect(noticeId) {
        viewModel.onIntent(NoticeDetailIntent.Load(challengeId, noticeId, canManage))
    }
    // 수정 화면에서 돌아오면 내용을 갱신한다.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onIntent(NoticeDetailIntent.Refresh)
    }
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is NoticeDetailEffect.ShowMessage -> messageHelper.showToast(effect.message)
            }
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background)
                .statusBarsPadding(),
    ) {
        NoticeDetailTopBar(
            canManage = state.canManage,
            pinned = state.detail?.pinned == true,
            onBack = { viewModel.onIntent(NoticeDetailIntent.Back) },
            onTogglePin = { viewModel.onIntent(NoticeDetailIntent.TogglePin) },
            onEdit = { viewModel.onIntent(NoticeDetailIntent.Edit) },
            onDelete = { viewModel.onIntent(NoticeDetailIntent.SetDeleteDialog(true)) },
        )

        when {
            state.isLoading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RuleUpTheme.colors.brand)
                }

            state.detail == null ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.errorMessage ?: "공지를 불러오지 못했어요",
                        color = RuleUpTheme.colors.textSecondary,
                        fontSize = 14.sp,
                    )
                }

            else -> NoticeDetailBody(detail = state.detail!!)
        }
    }

    if (state.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onIntent(NoticeDetailIntent.SetDeleteDialog(false)) },
            containerColor = RuleUpTheme.colors.surface,
            title = {
                Text(
                    text = "공지를 삭제할까요?",
                    color = RuleUpTheme.colors.textPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = "삭제한 공지는 멤버에게 더 이상 보이지 않아요.",
                    color = RuleUpTheme.colors.textSecondary,
                    fontSize = 13.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.onIntent(NoticeDetailIntent.ConfirmDelete) }) {
                    Text(text = "삭제", color = RuleUpTheme.colors.danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onIntent(NoticeDetailIntent.SetDeleteDialog(false)) }) {
                    Text(text = "취소", color = RuleUpTheme.colors.textSecondary)
                }
            },
        )
    }
}

@Composable
private fun NoticeDetailTopBar(
    canManage: Boolean,
    pinned: Boolean,
    onBack: () -> Unit,
    onTogglePin: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .singleClickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(com.ruleup.ui.R.drawable.ic_arrow_back),
                contentDescription = "뒤로",
                tint = RuleUpTheme.colors.textPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = "공지",
            color = RuleUpTheme.colors.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.weight(1f))
        if (canManage) {
            Box {
                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .singleClickable(onClick = { menuExpanded = true }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "⋮",
                        color = RuleUpTheme.colors.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    containerColor = RuleUpTheme.colors.surface,
                ) {
                    DropdownMenuItem(
                        text = { Text(if (pinned) "고정 해제" else "고정") },
                        onClick = {
                            menuExpanded = false
                            onTogglePin()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("수정") },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("삭제", color = RuleUpTheme.colors.danger) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun NoticeDetailBody(detail: NoticeDetail) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 40.dp),
    ) {
        if (detail.pinned) {
            Box(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(RuleUpTheme.colors.brandSoft)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "📌 고정됨",
                    color = RuleUpTheme.colors.brand,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(10.dp))
        }
        Text(
            text = detail.title,
            color = RuleUpTheme.colors.textPrimary,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = detail.authorNickname,
                color = RuleUpTheme.colors.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = noticeDateLabel(detail.createdAt),
                color = RuleUpTheme.colors.textMuted,
                fontSize = 12.sp,
            )
            if (detail.updatedAt != null) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "(수정됨)",
                    color = RuleUpTheme.colors.textMuted,
                    fontSize = 11.sp,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = RuleUpTheme.colors.border)
        Spacer(Modifier.height(16.dp))
        Text(
            text = detail.content,
            color = RuleUpTheme.colors.textPrimary,
            fontSize = 14.sp,
            lineHeight = 22.sp,
        )
    }
}
