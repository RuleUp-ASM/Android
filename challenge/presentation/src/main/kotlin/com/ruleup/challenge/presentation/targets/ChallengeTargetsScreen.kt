package com.ruleup.challenge.presentation.targets

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.challenge.presentation.targets.viewmodel.ChallengeTargetsEffect
import com.ruleup.challenge.presentation.targets.viewmodel.ChallengeTargetsIntent
import com.ruleup.challenge.presentation.targets.viewmodel.ChallengeTargetsViewModel
import com.ruleup.ui.component.PrimaryGradientButton
import com.ruleup.ui.helper.LocalMessageHelper
import com.ruleup.ui.helper.singleClickable
import com.ruleup.ui.theme.RuleUpTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 대상 앱 1개(표시 라벨 + 패키지명). */
private data class AppEntry(
    val packageName: String,
    val label: String,
)

/**
 * 대상 앱 등록 화면. 기기에 설치된 실행 가능한 앱을 나열하고, 사용자가 고른 앱을 저장한다(로컬).
 * 상세 화면의 "앱 등록하기" 로 진입하며, 저장하면 등록 완료로 표시되어 상세 버튼이 "참여하기" 로 넘어간다.
 */
@Composable
fun ChallengeTargetsScreen(
    challengeId: String,
    modifier: Modifier = Modifier,
    viewModel: ChallengeTargetsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val messageHelper = LocalMessageHelper.current

    var apps by remember { mutableStateOf<List<AppEntry>?>(null) }
    // Map state 로 두어 항목 토글 시 해당 키를 읽는 행만 리컴포지션되게 한다(List 는 전체 무효화).
    val selected = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ChallengeTargetsEffect.ShowMessage -> messageHelper.showToast(effect.message)
            }
        }
    }

    // 설치된 실행 가능한 앱 목록 조회(메인 스레드 밖에서). 시스템/자기 자신 제외.
    LaunchedEffect(Unit) {
        apps =
            withContext(Dispatchers.IO) {
                val pm = context.packageManager
                val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
                pm
                    .queryIntentActivities(intent, 0)
                    .asSequence()
                    .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
                    .filter { (pkg, _) -> pkg != context.packageName }
                    .distinctBy { it.first }
                    .map { (pkg, label) -> AppEntry(packageName = pkg, label = label) }
                    .sortedBy { it.label.lowercase() }
                    .toList()
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
            TargetsTopBar(onBack = { viewModel.onIntent(ChallengeTargetsIntent.Back) })

            Text(
                text = "인증에 사용할 앱을 선택해주세요",
                color = RuleUpTheme.colors.textSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )

            val loaded = apps
            when {
                loaded == null ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = RuleUpTheme.colors.brand)
                    }

                else ->
                    LazyColumn(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                        contentPadding =
                            androidx.compose.foundation.layout
                                .PaddingValues(bottom = 120.dp),
                    ) {
                        items(loaded, key = { it.packageName }) { app ->
                            AppRow(
                                label = app.label,
                                checked = selected[app.packageName] == true,
                                onToggle = {
                                    if (selected[app.packageName] == true) {
                                        selected.remove(app.packageName)
                                    } else {
                                        selected[app.packageName] = true
                                    }
                                },
                            )
                        }
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
            val selectedPackages = selected.filterValues { it }.keys.toList()
            PrimaryGradientButton(
                text = if (state.isSaving) "등록 중…" else "등록 완료 (${selectedPackages.size})",
                onClick = {
                    viewModel.onIntent(
                        ChallengeTargetsIntent.Save(
                            challengeId = challengeId,
                            packages = selectedPackages,
                        ),
                    )
                },
            )
        }
    }
}

@Composable
private fun TargetsTopBar(onBack: () -> Unit) {
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
            text = "대상 앱 등록",
            color = RuleUpTheme.colors.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun AppRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .singleClickable(onClick = onToggle)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Text(
            text = label,
            color = RuleUpTheme.colors.textPrimary,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}
