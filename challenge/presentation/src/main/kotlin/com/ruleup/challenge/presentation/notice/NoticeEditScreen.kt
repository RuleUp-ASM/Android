package com.ruleup.challenge.presentation.notice

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.challenge.domain.entity.NoticePolicy
import com.ruleup.challenge.presentation.notice.viewmodel.NoticeEditEffect
import com.ruleup.challenge.presentation.notice.viewmodel.NoticeEditIntent
import com.ruleup.challenge.presentation.notice.viewmodel.NoticeEditViewModel
import com.ruleup.designsystem.component.RuleUpPrimaryButton
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.ui.helper.LocalMessageHelper

/**
 * 공지 작성/수정 화면 (방장 전용 진입). noticeId 유무로 모드가 갈린다.
 * - 작성: "고정으로 등록" 옵션 (기존 고정은 서버가 자동 해제 — 단일 pin)
 * - 수정: "멤버 재확인 받기" 옵션 (읽음 초기화 + 재알림, 기본 꺼짐 = 읽음 유지)
 * 시안 부재 — 디자인 시스템 토큰 구성 (#110).
 */
@Composable
fun NoticeEditScreen(
    challengeId: String,
    noticeId: String?,
    modifier: Modifier = Modifier,
    viewModel: NoticeEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val messageHelper = LocalMessageHelper.current

    LaunchedEffect(challengeId, noticeId) {
        viewModel.onIntent(NoticeEditIntent.Load(challengeId, noticeId))
    }
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is NoticeEditEffect.ShowMessage -> messageHelper.showToast(effect.message)
            }
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
        ) {
            NoticeEditTopBar(
                title = if (state.isEditMode) "공지 수정" else "공지 작성",
                onBack = { viewModel.onIntent(NoticeEditIntent.Back) },
            )

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RuleUpTheme.colors.brand)
                }
                return@Column
            }

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                EditFieldCard(label = "제목", counter = "${state.title.length}/${NoticePolicy.TITLE_MAX_LENGTH}") {
                    BasicTextField(
                        value = state.title,
                        onValueChange = { viewModel.onIntent(NoticeEditIntent.ChangeTitle(it)) },
                        singleLine = true,
                        textStyle =
                            TextStyle(
                                color = RuleUpTheme.colors.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        cursorBrush = SolidColor(RuleUpTheme.colors.brand),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (state.title.isEmpty()) {
                                Text(
                                    text = "예) 이번 주 인증 시간 변경 안내",
                                    color = RuleUpTheme.colors.textMuted,
                                    fontSize = 15.sp,
                                )
                            }
                            inner()
                        },
                    )
                }

                EditFieldCard(label = "내용", counter = "${state.content.length}/${NoticePolicy.CONTENT_MAX_LENGTH}") {
                    BasicTextField(
                        value = state.content,
                        onValueChange = { viewModel.onIntent(NoticeEditIntent.ChangeContent(it)) },
                        textStyle =
                            TextStyle(
                                color = RuleUpTheme.colors.textPrimary,
                                fontSize = 14.sp,
                                lineHeight = 22.sp,
                            ),
                        cursorBrush = SolidColor(RuleUpTheme.colors.brand),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 180.dp),
                        decorationBox = { inner ->
                            if (state.content.isEmpty()) {
                                Text(
                                    text = "멤버에게 전할 내용을 적어주세요",
                                    color = RuleUpTheme.colors.textMuted,
                                    fontSize = 14.sp,
                                )
                            }
                            inner()
                        },
                    )
                }

                if (state.isEditMode) {
                    OptionRow(
                        title = "멤버 재확인 받기",
                        description = "모든 멤버가 미읽음으로 돌아가고 알림을 다시 보내요",
                        checked = state.resetRead,
                        onCheckedChange = { viewModel.onIntent(NoticeEditIntent.ToggleResetRead(it)) },
                    )
                } else {
                    OptionRow(
                        title = "고정으로 등록",
                        description = "방 홈 상단에 노출돼요. 기존 고정 공지는 자동 해제돼요",
                        checked = state.pinned,
                        onCheckedChange = { viewModel.onIntent(NoticeEditIntent.TogglePinned(it)) },
                    )
                }
            }
        }

        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(RuleUpTheme.colors.surface)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            RuleUpPrimaryButton(
                text =
                    when {
                        state.isSaving -> "저장 중…"
                        state.isEditMode -> "수정하기"
                        else -> "등록하기"
                    },
                onClick = { viewModel.onIntent(NoticeEditIntent.Save) },
            )
        }
    }
}

@Composable
private fun NoticeEditTopBar(
    title: String,
    onBack: () -> Unit,
) {
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
                painter = painterResource(com.ruleup.designsystem.R.drawable.ic_arrow_back),
                contentDescription = "뒤로",
                tint = RuleUpTheme.colors.textPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = title,
            color = RuleUpTheme.colors.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun EditFieldCard(
    label: String,
    counter: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(RuleUpTheme.colors.surface)
                .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(14.dp))
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                color = RuleUpTheme.colors.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = counter,
                color = RuleUpTheme.colors.textMuted,
                fontSize = 11.sp,
            )
        }
        content()
    }
}

@Composable
private fun OptionRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(RuleUpTheme.colors.surface)
                .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(14.dp))
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = RuleUpTheme.colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                color = RuleUpTheme.colors.textSecondary,
                fontSize = 12.sp,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors =
                SwitchDefaults.colors(
                    checkedTrackColor = RuleUpTheme.colors.brand,
                ),
        )
    }
}
