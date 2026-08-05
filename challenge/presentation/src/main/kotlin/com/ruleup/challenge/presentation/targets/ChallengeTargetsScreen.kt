package com.ruleup.challenge.presentation.targets

import android.annotation.SuppressLint
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.challenge.presentation.targets.viewmodel.ChallengeTargetsEffect
import com.ruleup.challenge.presentation.targets.viewmodel.ChallengeTargetsIntent
import com.ruleup.challenge.presentation.targets.viewmodel.ChallengeTargetsViewModel
import com.ruleup.designsystem.component.RuleUpPrimaryButton
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.ui.helper.LocalMessageHelper
import com.ruleup.verification.domain.entity.ScreenApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 대상 앱 1개(표시 라벨 + 패키지명 + 아이콘·카테고리·주간 사용 시간). */
private data class AppEntry(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?,
    // ApplicationInfo.category 의 현지화 제목(예: "소셜"). 미선언(UNDEFINED)이면 null → "전체"에만 노출.
    val category: String?,
    val weeklyUsageMs: Long,
)

/**
 * 대상 앱 등록 화면(피그마 "01 · 인증 셋업 UX 시안" ③, Android 앱 선택기 참고). 설치된 실행 가능한
 * 앱을 아이콘·주간 사용 시간과 함께 나열하고, 검색·카테고리 필터 칩으로 좁혀 고른 앱을 저장한다(로컬).
 * 상세 화면의 "앱 등록하기" 로 진입하며, 진입 플로우가 권한 → 앱 등록 순으로 게이팅되어 사용정보 접근
 * 권한이 이미 허용된 상태다(중도 회수 등 실패 시 사용 시간만 숨긴다).
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
    val selected = remember { mutableStateMapOf<String, Boolean>() }
    var query by remember { mutableStateOf("") }
    // 카테고리 필터(현지화 제목). null = "전체".
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ChallengeTargetsEffect.ShowMessage -> messageHelper.showToast(effect.message)
            }
        }
    }

    // 설치된 실행 가능한 앱 목록 + 카테고리 + 최근 1주 사용 시간 조회(메인 스레드 밖에서).
    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) { loadApps(context) }
    }

    // 진입 시 서버에 바인딩된 대상 앱 조회 → 복원되면 선택 상태를 시드한다.
    LaunchedEffect(Unit) {
        viewModel.onIntent(ChallengeTargetsIntent.Load(challengeId))
    }
    LaunchedEffect(state.restoredPackages) {
        state.restoredPackages.forEach { selected[it] = true }
    }

    val loaded = apps
    val categories =
        remember(loaded) {
            loaded
                .orEmpty()
                .mapNotNull { it.category }
                .distinct()
                .sorted()
        }
    val filtered =
        remember(loaded, query, selectedCategory) {
            loaded.orEmpty().filter { app ->
                (query.isBlank() || app.label.contains(query.trim(), ignoreCase = true)) &&
                    (selectedCategory == null || app.category == selectedCategory)
            }
        }
    val selectedApps = loaded.orEmpty().filter { selected[it.packageName] == true }

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
            TargetsTopBar(
                selectedCount = selectedApps.size,
                onBack = { viewModel.onIntent(ChallengeTargetsIntent.Back) },
            )

            Text(
                text = "선택한 앱의 사용 시간을 측정해 자동으로 인증해요",
                color = RuleUpTheme.colors.textSecondary,
                style = RuleUpTheme.typography.body,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            SearchField(
                query = query,
                onQueryChange = { query = it },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )

            if (categories.isNotEmpty()) {
                CategoryChips(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onSelect = { selectedCategory = it },
                )
            }

            if (selectedApps.isNotEmpty()) {
                SelectedAppChips(
                    apps = selectedApps,
                    onRemove = { selected.remove(it.packageName) },
                    modifier = Modifier.padding(top = 10.dp),
                )
            }

            when {
                loaded == null ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = RuleUpTheme.colors.brand)
                    }

                filtered.isEmpty() ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "조건에 맞는 앱이 없어요",
                            color = RuleUpTheme.colors.textSecondary,
                            style = RuleUpTheme.typography.labelMedium,
                        )
                    }

                else ->
                    LazyColumn(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(filtered, key = { it.packageName }) { app ->
                            AppRow(
                                app = app,
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
            RuleUpPrimaryButton(
                text = if (state.isSaving) "등록 중…" else "등록 완료 (${selectedApps.size})",
                onClick = {
                    viewModel.onIntent(
                        ChallengeTargetsIntent.Save(
                            challengeId = challengeId,
                            apps = selectedApps.map { ScreenApp(packageName = it.packageName, appName = it.label) },
                        ),
                    )
                },
            )
        }
    }
}

/**
 * 설치된 실행 가능한 앱 + 카테고리(현지화 제목) + 최근 1주 사용 시간을 모은다. 시스템/자기 자신 제외.
 * queryAndAggregateUsageStats(PACKAGE_USAGE_STATS)는 상세 CTA 의 권한 게이팅 뒤에서만 도달하므로
 * lint 를 억제하고, 중도 회수 등 실패 시 0(캡션 숨김)으로 둔다.
 */
@SuppressLint("MissingPermission")
private fun loadApps(context: Context): List<AppEntry> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
    val now = System.currentTimeMillis()
    val usage =
        runCatching {
            val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            manager.queryAndAggregateUsageStats(now - WEEK_MS, now)
        }.getOrNull().orEmpty()
    return pm
        .queryIntentActivities(intent, 0)
        .asSequence()
        .filter { it.activityInfo.packageName != context.packageName }
        .distinctBy { it.activityInfo.packageName }
        .map { resolveInfo ->
            val appInfo = resolveInfo.activityInfo.applicationInfo
            val packageName = resolveInfo.activityInfo.packageName
            AppEntry(
                packageName = packageName,
                label = resolveInfo.loadLabel(pm).toString(),
                icon = runCatching { resolveInfo.loadIcon(pm)?.toBitmap(ICON_PX, ICON_PX)?.asImageBitmap() }.getOrNull(),
                category =
                    appInfo.category
                        .takeIf { it != ApplicationInfo.CATEGORY_UNDEFINED }
                        ?.let { ApplicationInfo.getCategoryTitle(context, it)?.toString() },
                weeklyUsageMs = usage[packageName]?.totalTimeInForeground ?: 0L,
            )
        }.sortedBy { it.label.lowercase() }
        .toList()
}

/** "주 n시간 n분" 표기. 1분 미만이면 null(캡션 숨김). */
private fun weeklyUsageLabel(ms: Long): String? {
    val totalMinutes = ms / 60_000
    if (totalMinutes <= 0) return null
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "주 ${hours}시간 ${minutes}분" else "주 ${minutes}분"
}

@Composable
private fun TargetsTopBar(
    selectedCount: Int,
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
            text = "대상 앱 선택",
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.section,
            modifier = Modifier.weight(1f),
        )
        if (selectedCount > 0) {
            Text(
                text = "${selectedCount}개 선택",
                color = RuleUpTheme.colors.brandStrong,
                style = RuleUpTheme.typography.smallBold,
                modifier =
                    Modifier
                        .padding(end = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(RuleUpTheme.colors.brandSoft)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

/** 앱 이름 검색 필드(시안 ③). */
@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier =
            modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(RuleUpTheme.colors.surfaceVariant)
                .padding(horizontal = 14.dp),
    ) {
        Icon(
            painter = painterResource(com.ruleup.designsystem.R.drawable.ic_search),
            contentDescription = null,
            tint = RuleUpTheme.colors.textMuted,
            modifier = Modifier.size(20.dp),
        )
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = "앱 이름 검색",
                    color = RuleUpTheme.colors.textMuted,
                    style = RuleUpTheme.typography.labelMedium,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = RuleUpTheme.typography.labelMedium.copy(color = RuleUpTheme.colors.textPrimary),
                cursorBrush = SolidColor(RuleUpTheme.colors.brand),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Icon(
                painter = painterResource(com.ruleup.designsystem.R.drawable.ic_close),
                contentDescription = "지우기",
                tint = RuleUpTheme.colors.textMuted,
                modifier =
                    Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .singleClickable { onQueryChange("") },
            )
        }
    }
}

/** 카테고리 필터 칩 행. "전체" + 설치 앱에 존재하는 카테고리만. 미선언(UNDEFINED) 앱은 "전체"에만 보인다. */
@Composable
private fun CategoryChips(
    categories: List<String>,
    selectedCategory: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        item(key = "전체") {
            CategoryChip(
                label = "전체",
                isSelected = selectedCategory == null,
                onClick = { onSelect(null) },
            )
        }
        items(categories, key = { it }) { category ->
            CategoryChip(
                label = category,
                isSelected = selectedCategory == category,
                onClick = { onSelect(category) },
            )
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Text(
        text = label,
        color = if (isSelected) RuleUpTheme.colors.brandStrong else RuleUpTheme.colors.textSlate,
        style = if (isSelected) RuleUpTheme.typography.bodyBold else RuleUpTheme.typography.bodyMedium,
        modifier =
            Modifier
                .clip(shape)
                .background(if (isSelected) RuleUpTheme.colors.brandSoft else RuleUpTheme.colors.surface)
                .border(
                    width = 1.2.dp,
                    color = if (isSelected) RuleUpTheme.colors.brand else RuleUpTheme.colors.border,
                    shape = shape,
                ).singleClickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

/** 선택한 앱 칩 행(시안 ③): 미니 아이콘 + 라벨 + 해제 ×. 스크롤 없이 선택 확인·해제. */
@Composable
private fun SelectedAppChips(
    apps: List<AppEntry>,
    onRemove: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        items(apps, key = { it.packageName }) { app ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(RuleUpTheme.colors.surface)
                        .border(1.2.dp, RuleUpTheme.colors.brandSoft, RoundedCornerShape(16.dp))
                        .padding(start = 6.dp, end = 10.dp, top = 5.dp, bottom = 5.dp),
            ) {
                AppIcon(app = app, size = 20, cornerRadius = 6)
                Text(
                    text = app.label,
                    color = RuleUpTheme.colors.textPrimary,
                    style = RuleUpTheme.typography.smallMedium,
                    maxLines = 1,
                )
                Icon(
                    painter = painterResource(com.ruleup.designsystem.R.drawable.ic_close),
                    contentDescription = "선택 해제",
                    tint = RuleUpTheme.colors.textMuted,
                    modifier =
                        Modifier
                            .size(14.dp)
                            .singleClickable { onRemove(app) },
                )
            }
        }
    }
}

/** 앱 리스트 1행(시안 ③): 아이콘 + 라벨/주간 사용 시간 + 라운드 체크. 선택 시 행 하이라이트. */
@Composable
private fun AppRow(
    app: AppEntry,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (checked) RuleUpTheme.colors.brandSoft else RuleUpTheme.colors.background)
                .singleClickable(onClick = onToggle)
                .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppIcon(app = app, size = 42, cornerRadius = 12)
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = app.label,
                color = RuleUpTheme.colors.textPrimary,
                style = RuleUpTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            weeklyUsageLabel(app.weeklyUsageMs)?.let { usage ->
                Text(
                    text = usage,
                    color = RuleUpTheme.colors.textSecondary,
                    style = RuleUpTheme.typography.small,
                    maxLines = 1,
                )
            }
        }
        if (checked) {
            Box(
                modifier =
                    Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(RuleUpTheme.colors.brandStrong),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(com.ruleup.designsystem.R.drawable.ic_check),
                    contentDescription = "선택됨",
                    tint = RuleUpTheme.colors.surface,
                    modifier = Modifier.size(14.dp),
                )
            }
        } else {
            Box(
                modifier =
                    Modifier
                        .size(24.dp)
                        .border(2.dp, RuleUpTheme.colors.borderStrong, CircleShape),
            )
        }
    }
}

/** 앱 아이콘. 로드 실패 시 브랜드 톤 배경에 첫 글자 폴백. */
@Composable
private fun AppIcon(
    app: AppEntry,
    size: Int,
    cornerRadius: Int,
) {
    val icon = app.icon
    if (icon != null) {
        Image(
            bitmap = icon,
            contentDescription = null,
            modifier =
                Modifier
                    .size(size.dp)
                    .clip(RoundedCornerShape(cornerRadius.dp)),
        )
    } else {
        Box(
            modifier =
                Modifier
                    .size(size.dp)
                    .clip(RoundedCornerShape(cornerRadius.dp))
                    .background(RuleUpTheme.colors.brandSoft),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = app.label.take(1),
                color = RuleUpTheme.colors.brandStrong,
                // 타일 크기에 비례하는 이니셜이라 스케일 값이 아니다.
                fontSize = (size * 0.4).sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private const val WEEK_MS = 7L * 24 * 60 * 60 * 1000

// 앱 아이콘 비트맵 한 변(px). 리스트 42dp 슬롯 기준 고밀도 대응.
private const val ICON_PX = 96
