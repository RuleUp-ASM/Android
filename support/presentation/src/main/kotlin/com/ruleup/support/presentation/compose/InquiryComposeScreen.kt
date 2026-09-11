package com.ruleup.support.presentation.compose

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.ruleup.designsystem.component.RuleUpPrimaryButton
import com.ruleup.designsystem.component.RuleUpTopBar
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.support.domain.entity.InquiryLimits
import com.ruleup.support.presentation.compose.viewmodel.InquiryAttachment
import com.ruleup.support.presentation.compose.viewmodel.InquiryComposeIntent
import com.ruleup.support.presentation.compose.viewmodel.InquiryComposeState
import com.ruleup.support.presentation.compose.viewmodel.InquiryComposeViewModel

/**
 * 문의하기 · 작성 (Figma `1417:117`).
 *
 * Figma 와 다르게 간 곳
 * - **"답변은 알림함으로 알려드려요"를 뺐다.** 답변을 알림함으로 알리지 않기로 했다(2026-09-11).
 *   그 자리에 자동 첨부 고지를 둔다 — 앱 버전·기기 정보는 토글 없이 함께 전송되므로(명세) 보내기
 *   전에 알려야 한다.
 */
@Composable
fun InquiryComposeScreen(
    modifier: Modifier = Modifier,
    viewModel: InquiryComposeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    InquiryComposeContent(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

@Composable
internal fun InquiryComposeContent(
    state: InquiryComposeState,
    onIntent: (InquiryComposeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RuleUpTheme.colors
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding()
                .imePadding(),
    ) {
        RuleUpTopBar(title = "문의하기", onBack = { onIntent(InquiryComposeIntent.Back) })

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
        ) {
            CategoryRow(
                label = state.category.title,
                onClick = { onIntent(InquiryComposeIntent.ChangeCategory) },
            )
            Spacer(Modifier.height(18.dp))

            RequiredLabel(text = "내용")
            Spacer(Modifier.height(8.dp))
            BodyField(state = state, onIntent = onIntent)

            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = "사진", color = colors.textPrimary, style = RuleUpTheme.typography.section)
                Spacer(Modifier.size(6.dp))
                Text(
                    text = "(선택 · 최대 ${InquiryLimits.IMAGE_MAX_COUNT}장)",
                    color = colors.textMuted,
                    style = RuleUpTheme.typography.caption,
                )
            }
            Spacer(Modifier.height(8.dp))
            AttachmentRow(state = state, onIntent = onIntent)

            Spacer(Modifier.height(14.dp))
            Text(
                text = "보내주신 내용과 함께 앱 버전 · 기기 정보가 전달돼요",
                color = colors.textMuted,
                style = RuleUpTheme.typography.caption,
            )

            state.errorMessage?.let {
                Spacer(Modifier.height(10.dp))
                Text(text = it, color = colors.danger, style = RuleUpTheme.typography.small)
            }
            Spacer(Modifier.height(24.dp))
        }

        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp).navigationBarsPadding()) {
            RuleUpPrimaryButton(
                text = if (state.submitting) "접수 중이에요" else "접수하기",
                enabled = state.canSubmit,
                onClick = { onIntent(InquiryComposeIntent.Submit) },
            )
        }
    }

    state.receiptId?.let { inquiryId ->
        InquiryReceiptSheet(
            inquiryId = inquiryId,
            onConfirm = { onIntent(InquiryComposeIntent.ConfirmReceipt) },
        )
    }
}

@Composable
private fun CategoryRow(
    label: String,
    onClick: () -> Unit,
) {
    val colors = RuleUpTheme.colors
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RuleUpTheme.shapes.medium)
                .background(colors.surface)
                .singleClickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "분류",
            color = colors.textSecondary,
            style = RuleUpTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier =
                Modifier
                    .clip(RuleUpTheme.shapes.small)
                    .background(colors.brandSoft)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Text(text = label, color = colors.brand, style = RuleUpTheme.typography.captionMedium)
        }
        Spacer(Modifier.size(8.dp))
        Text(text = "›", color = colors.textMuted, style = RuleUpTheme.typography.body)
    }
}

@Composable
private fun RequiredLabel(text: String) {
    val colors = RuleUpTheme.colors
    Row {
        Text(text = text, color = colors.textPrimary, style = RuleUpTheme.typography.section)
        Text(text = " *", color = colors.danger, style = RuleUpTheme.typography.section)
    }
}

@Composable
private fun BodyField(
    state: InquiryComposeState,
    onIntent: (InquiryComposeIntent) -> Unit,
) {
    val colors = RuleUpTheme.colors
    val overLimit = state.bodyLength > InquiryLimits.BODY_MAX_LENGTH

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RuleUpTheme.shapes.medium)
                .background(colors.surface)
                .border(1.dp, if (overLimit) colors.danger else colors.border, RuleUpTheme.shapes.medium)
                .padding(14.dp),
    ) {
        Box {
            if (state.body.isEmpty()) {
                Text(
                    text = "어떤 상황이었는지 적어 주세요. 날짜와 챌린지 이름이 있으면 더 빨리 확인할 수 있어요.",
                    color = colors.textMuted,
                    style = RuleUpTheme.typography.small,
                )
            }
            BasicTextField(
                value = state.body,
                onValueChange = { onIntent(InquiryComposeIntent.BodyChanged(it)) },
                textStyle = RuleUpTheme.typography.small.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.brand),
                modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${InquiryLimits.BODY_MIN_LENGTH}자 이상 · ${InquiryLimits.BODY_MAX_LENGTH}자 이하",
                color = colors.textMuted,
                style = RuleUpTheme.typography.caption,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${state.bodyLength} / ${InquiryLimits.BODY_MAX_LENGTH}",
                color = if (overLimit) colors.danger else colors.textMuted,
                style = RuleUpTheme.typography.caption,
            )
        }
    }
}

@Composable
private fun AttachmentRow(
    state: InquiryComposeState,
    onIntent: (InquiryComposeIntent) -> Unit,
) {
    val pickImage = rememberImagePicker { onIntent(InquiryComposeIntent.ImagePicked(it)) }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        state.attachments.forEach { attachment ->
            AttachmentThumb(
                attachment = attachment,
                onRemove = { onIntent(InquiryComposeIntent.ImageRemoved(attachment.uri)) },
            )
        }
        if (state.canAddImage) {
            AddAttachmentBox(count = state.attachments.size, onClick = pickImage)
        }
    }
}

@Composable
private fun AttachmentThumb(
    attachment: InquiryAttachment,
    onRemove: () -> Unit,
) {
    val colors = RuleUpTheme.colors
    Box(
        modifier =
            Modifier
                .size(64.dp)
                .clip(RuleUpTheme.shapes.small)
                .background(colors.surfaceVariant)
                .singleClickable(onClick = onRemove),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = attachment.uri,
            contentDescription = "첨부한 사진. 누르면 뺍니다",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        when {
            attachment.uploading ->
                CircularProgressIndicator(color = colors.brand, modifier = Modifier.size(20.dp))

            // 실패한 장은 남겨 두고 표시만 바꾼다 — 조용히 지우면 사용자는 올라간 줄 안다.
            attachment.failed ->
                Box(
                    modifier = Modifier.fillMaxSize().background(colors.dangerContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "실패", color = colors.danger, style = RuleUpTheme.typography.caption)
                }
        }
    }
}

@Composable
private fun AddAttachmentBox(
    count: Int,
    onClick: () -> Unit,
) {
    val colors = RuleUpTheme.colors
    Column(
        modifier =
            Modifier
                .size(64.dp)
                .clip(RuleUpTheme.shapes.small)
                .border(1.dp, colors.border, RuleUpTheme.shapes.small)
                .singleClickable(onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "+", color = colors.textMuted, style = RuleUpTheme.typography.section)
        Text(
            text = "$count/${InquiryLimits.IMAGE_MAX_COUNT}",
            color = colors.textMuted,
            style = RuleUpTheme.typography.caption,
        )
    }
}

/**
 * 갤러리에서 사진 한 장을 고른다. 카메라 촬영은 두지 않았다 — 문의 첨부는 대부분 앱 화면 캡처라
 * 카메라를 띄우면 한 번 더 고르게 만드는 단계만 는다.
 */
@Composable
private fun rememberImagePicker(onPick: (String) -> Unit): () -> Unit {
    if (LocalInspectionMode.current) return {}
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) onPick(uri.toString())
        }
    return { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}
