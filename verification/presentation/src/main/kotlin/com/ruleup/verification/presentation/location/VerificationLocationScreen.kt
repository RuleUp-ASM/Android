package com.ruleup.verification.presentation.location

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.designsystem.component.RuleUpPrimaryButton
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.ui.helper.LocalMessageHelper
import com.ruleup.verification.domain.entity.LocationPin
import com.ruleup.verification.domain.entity.Place
import com.ruleup.verification.domain.entity.SetupAnchors
import com.ruleup.verification.presentation.location.map.GeofenceMap
import com.ruleup.verification.presentation.location.map.MapAnchor
import com.ruleup.verification.presentation.location.map.MapLatLng
import com.ruleup.verification.presentation.location.map.rememberLocationLocator
import com.ruleup.verification.presentation.location.map.rememberLocationPermissionGranted
import com.ruleup.verification.presentation.location.viewmodel.PendingSelection
import com.ruleup.verification.presentation.location.viewmodel.VerificationLocationEffect
import com.ruleup.verification.presentation.location.viewmodel.VerificationLocationIntent
import com.ruleup.verification.presentation.location.viewmodel.VerificationLocationState
import com.ruleup.verification.presentation.location.viewmodel.VerificationLocationViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 지도 위치 선택 화면(명세 §5, 피그마 "01 · 인증 셋업 UX 시안" ①②). 검색·지도 탭으로 찍은 핀을
 * 하단 시트에서 확인해 앵커로 담고, 앵커 목록 시트에서 삭제·제출한다. 자동완성은 카카오 로컬(§5.2).
 */
@Composable
fun VerificationLocationScreen(
    challengeId: String,
    defaultRadiusM: Float,
    dwellMinutes: Int,
    modifier: Modifier = Modifier,
    targetPackages: List<String> = emptyList(),
    viewModel: VerificationLocationViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val messageHelper = LocalMessageHelper.current
    var query by remember { mutableStateOf("") }
    // 결과 선택 직후 query 를 그 이름으로 채울 때, 디바운스 검색이 다시 도는 것을 한 번 건너뛴다.
    var suppressSearch by remember { mutableStateOf(false) }

    val locator = rememberLocationLocator()
    val permissionGranted = rememberLocationPermissionGranted()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is VerificationLocationEffect.ShowMessage -> messageHelper.showToast(effect.message)
            }
        }
    }

    // 진입 시 앵커 조회로 등록 여부 확인. 이미 등록돼 있으면 재등록 없이 종료된다.
    LaunchedEffect(Unit) {
        viewModel.onIntent(VerificationLocationIntent.Init(challengeId))
    }

    if (state.isChecking) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = RuleUpTheme.colors.brand)
        }
        return
    }

    LaunchedEffect(query) {
        if (suppressSearch) {
            suppressSearch = false
            return@LaunchedEffect
        }
        val keyword = query.trim()
        if (keyword.isBlank()) {
            viewModel.onIntent(VerificationLocationIntent.ClearSearch)
            return@LaunchedEffect
        }
        delay(SEARCH_DEBOUNCE_MS)
        viewModel.onIntent(VerificationLocationIntent.Search(query = keyword))
    }

    val pin = state.pending?.let { MapLatLng(lat = it.lat, lng = it.lng) }
    // 반경은 서버가 정한 값이 원본이다 — 아직 못 받았을 때만 호출자가 넘긴 값으로 그린다.
    // 화면이 임의 값을 그리면 지도 원과 실제 판정 범위가 어긋난다.
    val radiusM = state.serverRadiusM ?: defaultRadiusM

    Box(modifier = modifier.fillMaxSize()) {
        GeofenceMap(
            initialCenter = MapLatLng(DEFAULT_LAT, DEFAULT_LNG),
            pin = pin,
            radiusM = radiusM,
            onMapTap = { viewModel.onIntent(VerificationLocationIntent.TapMap(lat = it.lat, lng = it.lng)) },
            modifier = Modifier.fillMaxSize(),
            anchors = state.anchors.map { MapAnchor(lat = it.lat, lng = it.lng, radiusM = radiusM) },
        )

        Column(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FloatingSearchPill(
                query = query,
                isSearching = state.isSearching,
                onQueryChange = { query = it },
                onClear = {
                    query = ""
                    viewModel.onIntent(VerificationLocationIntent.ClearSearch)
                },
                onBack = { viewModel.onIntent(VerificationLocationIntent.Back) },
            )
            if (state.places.isNotEmpty()) {
                SearchResults(
                    places = state.places,
                    onPlaceClick = { place ->
                        suppressSearch = true
                        query = place.name
                        viewModel.onIntent(VerificationLocationIntent.SelectPlace(place))
                    },
                )
            }
        }

        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
        ) {
            if (permissionGranted) {
                CurrentLocationFab(
                    onClick = {
                        scope.launch {
                            locator.locate()?.let {
                                viewModel.onIntent(VerificationLocationIntent.TapMap(lat = it.lat, lng = it.lng))
                            }
                        }
                    },
                    modifier =
                        Modifier
                            .align(Alignment.End)
                            .padding(end = 16.dp, bottom = 12.dp),
                )
            }
            val pending = state.pending
            when {
                pending != null ->
                    SelectionSheet(
                        pending = pending,
                        isResolving = state.isResolving,
                        canAdd = state.anchors.size < SetupAnchors.MAX_COUNT,
                        radiusM = radiusM,
                        onCancel = { viewModel.onIntent(VerificationLocationIntent.CancelSelection) },
                        onAdd = { viewModel.onIntent(VerificationLocationIntent.AddAnchor) },
                    )

                state.anchors.isNotEmpty() ->
                    AnchorListSheet(
                        anchors = state.anchors,
                        radiusM = radiusM,
                        isSubmitting = state.isSubmitting,
                        // 편집 중 이번 달 변경이 남아 있지 않으면 저장을 잠근다 — 눌러 봐야 429 다.
                        submitEnabled = !state.isEditing || state.changeAvailable,
                        submitLabel = if (state.isEditing) "이 장소로 바꾸기" else "이 장소로 등록",
                        lockNotice = state.changeLockNotice(),
                        onRemove = { viewModel.onIntent(VerificationLocationIntent.RemoveAnchor(it)) },
                        onSubmit = {
                            viewModel.onIntent(
                                VerificationLocationIntent.Submit(
                                    challengeId = challengeId,
                                    dwellMinutes = dwellMinutes,
                                    targetPackages = targetPackages,
                                ),
                            )
                        },
                    )
            }
        }
    }
}

/** 뒤로가기를 품은 플로팅 검색 필(시안 ①, 네이버 지도식). */
@Composable
private fun FloatingSearchPill(
    query: String,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        shadowElevation = 8.dp,
        color = RuleUpTheme.colors.surface,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .height(52.dp)
                    .padding(start = 6.dp, end = 16.dp),
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
            Box(modifier = Modifier.weight(1f).padding(horizontal = 6.dp)) {
                if (query.isEmpty()) {
                    Text(
                        text = "장소·주소 검색 (예: 스포애니)",
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
            when {
                isSearching ->
                    CircularProgressIndicator(
                        color = RuleUpTheme.colors.brand,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                    )

                query.isNotEmpty() ->
                    Icon(
                        painter = painterResource(com.ruleup.designsystem.R.drawable.ic_close),
                        contentDescription = "지우기",
                        tint = RuleUpTheme.colors.textMuted,
                        modifier =
                            Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .singleClickable(onClick = onClear),
                    )

                else ->
                    Icon(
                        painter = painterResource(com.ruleup.designsystem.R.drawable.ic_search),
                        contentDescription = null,
                        tint = RuleUpTheme.colors.textSecondary,
                        modifier = Modifier.size(20.dp),
                    )
            }
        }
    }
}

/** 카카오 로컬 자동완성 목록(상한 15개, 명세 §5.2). 선택 시 핀이 찍히고 목록이 닫힌다. */
@Composable
private fun SearchResults(
    places: List<Place>,
    onPlaceClick: (Place) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 8.dp,
        color = RuleUpTheme.colors.surface,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier
                    .heightIn(max = 280.dp)
                    .verticalScroll(rememberScrollState()),
        ) {
            places.forEach { place ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .singleClickable { onPlaceClick(place) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = place.name,
                        color = RuleUpTheme.colors.textPrimary,
                        style = RuleUpTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    place.address?.let {
                        Text(
                            text = it,
                            color = RuleUpTheme.colors.textSecondary,
                            style = RuleUpTheme.typography.small,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/** 현재 위치 원형 FAB(시안 ①). 시트 위 우측에 얹혀 시트에 가리지 않는다. */
@Composable
private fun CurrentLocationFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = CircleShape,
        shadowElevation = 6.dp,
        color = RuleUpTheme.colors.surface,
        modifier = modifier.size(48.dp),
    ) {
        Box(
            modifier = Modifier.singleClickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(com.ruleup.designsystem.R.drawable.ic_my_location),
                contentDescription = "현재 위치",
                tint = RuleUpTheme.colors.textSlate,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/** 바텀시트 공통 컨테이너(핸들 포함, 시안 ①②). */
@Composable
private fun SheetContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        shadowElevation = 12.dp,
        color = RuleUpTheme.colors.surface,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(RuleUpTheme.colors.border),
            )
            content()
        }
    }
}

/**
 * 핀 위치 확인 시트(시안 ①·명세 §5.3). 역지오코딩 중이거나 앵커가 가득 차면 추가 버튼을 잠근다.
 */
@Composable
private fun SelectionSheet(
    pending: PendingSelection,
    isResolving: Boolean,
    canAdd: Boolean,
    radiusM: Float,
    onCancel: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SheetContainer(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = pending.name,
                color = RuleUpTheme.colors.textPrimary,
                style = RuleUpTheme.typography.section,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            // 카카오 로컬 카테고리의 마지막 단계만(예: "스포츠,레저 > 헬스장" → "헬스장").
            pending.category
                ?.substringAfterLast('>')
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { category ->
                    Text(
                        text = category,
                        color = RuleUpTheme.colors.brandStrong,
                        style = RuleUpTheme.typography.captionMedium,
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(RuleUpTheme.colors.brandSoft)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
        }
        Text(
            text = pending.address ?: if (isResolving) "주소 확인 중…" else "좌표로 선택한 위치",
            color = RuleUpTheme.colors.textSecondary,
            style = RuleUpTheme.typography.body,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(RuleUpTheme.colors.background)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Icon(
                painter = painterResource(com.ruleup.designsystem.R.drawable.ic_my_location),
                contentDescription = null,
                tint = RuleUpTheme.colors.brand,
                modifier = Modifier.size(20.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "인증 반경 ${radiusM.toInt()}m",
                    color = RuleUpTheme.colors.textPrimary,
                    style = RuleUpTheme.typography.bodyMedium,
                )
                Text(
                    text = "이 반경 안에 들어오면 자동으로 인증돼요",
                    color = RuleUpTheme.colors.textSecondary,
                    style = RuleUpTheme.typography.small,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier =
                    Modifier
                        .width(104.dp)
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.2.dp, RuleUpTheme.colors.border, RoundedCornerShape(14.dp))
                        .singleClickable(onClick = onCancel),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "취소",
                    color = RuleUpTheme.colors.textSlate,
                    style = RuleUpTheme.typography.labelMedium,
                )
            }
            val enabled = !isResolving && canAdd
            RuleUpPrimaryButton(
                text = "이 위치 추가",
                onClick = { if (enabled) onAdd() },
                modifier =
                    Modifier
                        .weight(1f)
                        .alpha(if (enabled) 1f else 0.5f),
            )
        }
    }
}

/**
 * 앵커 목록 시트(시안 ②). 담아둔 앵커를 번호 뱃지 + 이름/주소로 보여주고 개별 삭제한다.
 * 제출은 최초 등록이면 setup, 편집 중이면 my-location 교체다 — 그래서 라벨이 [submitLabel] 로 갈린다.
 */
@Composable
private fun AnchorListSheet(
    anchors: List<LocationPin>,
    radiusM: Float,
    isSubmitting: Boolean,
    submitEnabled: Boolean,
    submitLabel: String,
    lockNotice: String?,
    onRemove: (Int) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SheetContainer(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "등록한 인증 장소",
                color = RuleUpTheme.colors.textPrimary,
                style = RuleUpTheme.typography.section,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${anchors.size} / ${SetupAnchors.MAX_COUNT}",
                color = RuleUpTheme.colors.brandStrong,
                style = RuleUpTheme.typography.bodyBold,
            )
        }
        Column(
            modifier =
                Modifier
                    .heightIn(max = 220.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            anchors.forEachIndexed { index, anchor ->
                AnchorRow(
                    number = index + 1,
                    anchor = anchor,
                    radiusM = radiusM,
                    onRemove = if (isSubmitting) null else ({ onRemove(index) }),
                )
            }
        }
        // 왜 저장할 수 없는지 버튼 위에서 먼저 말한다 — 비활성 버튼만 있으면 이유를 알 수 없다.
        lockNotice?.let {
            Text(
                text = it,
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.caption,
            )
        }
        RuleUpPrimaryButton(
            text = if (isSubmitting) "저장 중…" else "$submitLabel (${anchors.size})",
            enabled = submitEnabled && !isSubmitting,
            onClick = { if (!isSubmitting && submitEnabled) onSubmit() },
        )
    }
}

/**
 * 변경이 잠긴 이유(편집 중 + 이번 달 소진일 때만). 언제부터 가능한지 모르면 날짜를 지어내지 않는다 —
 * 틀린 날짜를 확정처럼 보여주는 쪽이 더 나쁘다.
 */
internal fun VerificationLocationState.changeLockNotice(): String? {
    if (!isEditing || changeAvailable) return null
    val parts = nextChangeAvailableAt?.substringBefore('T')?.split('-')?.takeIf { it.size == 3 }
    val month = parts?.get(1)?.toIntOrNull()
    val day = parts?.get(2)?.toIntOrNull()
    return if (month == null || day == null) {
        "이번 달 변경 횟수를 모두 썼어요"
    } else {
        "이번 달 변경 횟수를 모두 썼어요 · ${month}월 ${day}일부터 가능해요"
    }
}

/** 앵커 1행(시안 ②): 번호 뱃지 + 이름/주소·반경 + 삭제. [onRemove] 가 null 이면 삭제 비활성. */
@Composable
private fun AnchorRow(
    number: Int,
    anchor: LocationPin,
    radiusM: Float,
    onRemove: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(RuleUpTheme.colors.background)
                .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(RuleUpTheme.colors.brandSoft),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "$number",
                color = RuleUpTheme.colors.brandStrong,
                style = RuleUpTheme.typography.bodyBold,
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = anchor.label ?: "선택한 위치",
                color = RuleUpTheme.colors.textPrimary,
                style = RuleUpTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${anchor.address ?: "지도에서 선택한 위치"} · 반경 ${radiusM.toInt()}m",
                color = RuleUpTheme.colors.textSecondary,
                style = RuleUpTheme.typography.small,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            painter = painterResource(com.ruleup.designsystem.R.drawable.ic_close),
            contentDescription = "삭제",
            tint = RuleUpTheme.colors.textMuted,
            modifier =
                Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .let { m -> if (onRemove != null) m.singleClickable(onClick = onRemove) else m },
        )
    }
}

// 초기 카메라 — 서울 시청. 사용자가 검색·탭·현재위치로 옮긴다.
private const val DEFAULT_LAT = 37.5665
private const val DEFAULT_LNG = 126.9780

// 자동완성 디바운스(ms): 타이핑이 멈춘 뒤 이만큼 지나면 검색.
private const val SEARCH_DEBOUNCE_MS = 300L
