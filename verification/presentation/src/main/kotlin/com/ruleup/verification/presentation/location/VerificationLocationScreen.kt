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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.ruleup.verification.presentation.location.viewmodel.VerificationLocationViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 지도 위치 선택 화면(명세 §5, 피그마 "01 · 인증 셋업 UX 시안" ①②). 네이버·카카오 지도식 —
 * 뒤로가기를 품은 플로팅 검색 필에서 검색하거나 지도를 탭하면 핀 + 인증 반경이 그려지고,
 * 하단 바텀시트에서 장소를 확인해 [이 위치 추가]로 담는다. 담아둔 앵커는 지도에 번호 핀 +
 * 반경 원으로 표시되고, 앵커 목록 시트에서 삭제·제출한다. 브랜드/지점 키워드는 카카오 로컬로
 * 자동완성(§5.2)된다. 초기 카메라는 서울 시청 — 사용자가 검색/탭/현재위치로 옮긴다.
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

    // 확인 전에는 지도 대신 로딩만 노출.
    if (state.isChecking) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = RuleUpTheme.colors.brand)
        }
        return
    }

    // 입력이 멈추면(디바운스) 카카오 로컬 자동완성. 공백이면 목록을 닫는다.
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

    Box(modifier = modifier.fillMaxSize()) {
        // 전체화면 지도. 탭하면 그 좌표가 확인 대기 핀으로(역지오코딩 후 주소 채움).
        // 담아둔 앵커는 번호 핀 + 반경 원으로 함께 표시된다.
        GeofenceMap(
            initialCenter = MapLatLng(DEFAULT_LAT, DEFAULT_LNG),
            pin = pin,
            radiusM = defaultRadiusM,
            onMapTap = { viewModel.onIntent(VerificationLocationIntent.TapMap(lat = it.lat, lng = it.lng)) },
            modifier = Modifier.fillMaxSize(),
            anchors = state.anchors.map { MapAnchor(lat = it.lat, lng = it.lng, radiusM = it.radiusM) },
        )

        // 상단: 플로팅 검색 필 + 자동완성 목록.
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
                        // 결과 선택: 디바운스 재검색 1회 건너뛰고 입력칸을 이름으로 채운 뒤 핀 요청.
                        suppressSearch = true
                        query = place.name
                        viewModel.onIntent(VerificationLocationIntent.SelectPlace(place))
                    },
                )
            }
        }

        // 하단: 현재 위치 FAB + 상태별 바텀시트. FAB 는 시트에 가리지 않게 시트 위에 얹는다.
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
                        radiusM = defaultRadiusM,
                        onCancel = { viewModel.onIntent(VerificationLocationIntent.CancelSelection) },
                        onAdd = { viewModel.onIntent(VerificationLocationIntent.AddAnchor(radiusM = defaultRadiusM)) },
                    )

                state.anchors.isNotEmpty() ->
                    AnchorListSheet(
                        anchors = state.anchors,
                        isSubmitting = state.isSubmitting,
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

/**
 * 뒤로가기를 품은 플로팅 검색 필(시안 ①, 네이버 지도식). 입력 중엔 지우기 버튼,
 * 검색 중엔 스피너, 그 외엔 검색 아이콘을 트레일링으로 보여준다.
 */
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
                        fontSize = 15.sp,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle =
                        TextStyle(
                            color = RuleUpTheme.colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                        ),
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
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    place.address?.let {
                        Text(
                            text = it,
                            color = RuleUpTheme.colors.textSecondary,
                            fontSize = 12.sp,
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
 * 핀 위치 확인 시트(시안 ①·명세 §5.3). 장소명·카테고리·주소와 인증 반경 안내를 보여주고
 * [이 위치 추가]로 앵커 목록에 담는다. 역지오코딩 중([isResolving])이거나 앵커가 가득 차면
 * ([canAdd]=false) 추가 버튼을 잠근다.
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
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
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
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
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
            fontSize = 13.sp,
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
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "이 반경 안에 들어오면 자동으로 인증돼요",
                    color = RuleUpTheme.colors.textSecondary,
                    fontSize = 12.sp,
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
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            // 주소 확인 전(resolving)·앵커 가득 차면 추가 차단.
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
 * 앵커 목록 시트(시안 ②·명세 setup). 담아둔 앵커를 번호 뱃지 + 이름/주소로 보여주고(개별 삭제),
 * [등록 완료]로 setup 을 송신한다. 제출 중([isSubmitting])엔 삭제·재제출을 막는다.
 */
@Composable
private fun AnchorListSheet(
    anchors: List<LocationPin>,
    isSubmitting: Boolean,
    onRemove: (Int) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SheetContainer(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "등록한 인증 장소",
                color = RuleUpTheme.colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${anchors.size} / ${SetupAnchors.MAX_COUNT}",
                color = RuleUpTheme.colors.brandStrong,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
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
                    // 제출 중엔 삭제를 막는다(목록 고정).
                    onRemove = if (isSubmitting) null else ({ onRemove(index) }),
                )
            }
        }
        RuleUpPrimaryButton(
            text = if (isSubmitting) "등록 중…" else "등록 완료 (${anchors.size})",
            onClick = { if (!isSubmitting) onSubmit() },
        )
    }
}

/** 앵커 1행(시안 ②): 번호 뱃지 + 이름/주소·반경 + 삭제. [onRemove] 가 null 이면 삭제 비활성. */
@Composable
private fun AnchorRow(
    number: Int,
    anchor: LocationPin,
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
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = anchor.label ?: "선택한 위치",
                color = RuleUpTheme.colors.textPrimary,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${anchor.address ?: "지도에서 선택한 위치"} · 반경 ${anchor.radiusM.toInt()}m",
                color = RuleUpTheme.colors.textSecondary,
                fontSize = 12.sp,
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

private const val DEFAULT_LAT = 37.5665
private const val DEFAULT_LNG = 126.9780

// 자동완성 디바운스(ms): 타이핑이 멈춘 뒤 이만큼 지나면 검색.
private const val SEARCH_DEBOUNCE_MS = 300L
