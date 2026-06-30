package com.ruleup.verification.presentation.location

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.map.GeofenceMap
import com.ruleup.map.MapLatLng
import com.ruleup.map.rememberLocationLocator
import com.ruleup.map.rememberLocationPermissionGranted
import com.ruleup.ui.helper.LocalMessageHelper
import com.ruleup.ui.helper.rememberSingleClick
import com.ruleup.ui.helper.singleClickable
import com.ruleup.verification.presentation.location.viewmodel.PendingSelection
import com.ruleup.verification.presentation.location.viewmodel.VerificationLocationEffect
import com.ruleup.verification.presentation.location.viewmodel.VerificationLocationIntent
import com.ruleup.verification.presentation.location.viewmodel.VerificationLocationViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 지도 위치 선택 화면(명세 §5). 네이버 지도식 — 전체화면 지도에서 가게/지점을 탭하거나 검색 결과를 고르면
 * 그 지점에 핀이 뜨고, 하단 카드에서 "이 위치를 선택하시겠습니까?" 로 확정한다. 확정 시 멤버 좌표 지오펜스를
 * 등록하고 화면을 닫는다(뒤로가기). 브랜드/지점 키워드는 카카오 로컬로 자동완성(§5.2)된다.
 * 초기 카메라는 서울 시청 — 사용자가 검색/탭/현재위치로 옮긴다.
 */
@Composable
fun VerificationLocationScreen(
    challengeMemberId: String,
    defaultRadiusM: Float,
    dwellMinutes: Int,
    modifier: Modifier = Modifier,
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
        GeofenceMap(
            initialCenter = MapLatLng(DEFAULT_LAT, DEFAULT_LNG),
            pin = pin,
            radiusM = defaultRadiusM,
            onMapTap = { viewModel.onIntent(VerificationLocationIntent.TapMap(lat = it.lat, lng = it.lng)) },
            modifier = Modifier.fillMaxSize(),
        )

        // 상단 검색 오버레이(검색창 + 자동완성 목록).
        Column(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("브랜드·지점 검색 (예: 스포애니)") },
                    singleLine = true,
                    trailingIcon =
                        if (state.isSearching) {
                            { CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) }
                        } else {
                            null
                        },
                    // 컨테이너(Surface)가 배경을 그리므로 테두리는 지운다.
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                        ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // 자동완성 결과(상한 15개, 명세 §5.2). 선택 시 핀이 찍히고 목록이 닫힌다.
            if (state.places.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier =
                            Modifier
                                .heightIn(max = 280.dp)
                                .verticalScroll(rememberScrollState()),
                    ) {
                        state.places.forEach { place ->
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .singleClickable {
                                            suppressSearch = true
                                            query = place.name
                                            viewModel.onIntent(VerificationLocationIntent.SelectPlace(place))
                                        }.padding(horizontal = 16.dp, vertical = 12.dp),
                            ) {
                                Text(place.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                place.address?.let {
                                    Text(
                                        it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 현재 위치 — 확인 카드가 없을 때만 노출(카드와 겹치지 않게). 권한 거부 시 숨김(명세 §5.4.2).
        if (state.pending == null && permissionGranted) {
            FilledTonalButton(
                onClick =
                    rememberSingleClick {
                        scope.launch {
                            locator.locate()?.let {
                                viewModel.onIntent(VerificationLocationIntent.TapMap(lat = it.lat, lng = it.lng))
                            }
                        }
                    },
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
            ) {
                Text("현재 위치")
            }
        }

        // 하단 확인 카드(네이버 스타일). 핀이 찍히면 올라온다.
        state.pending?.let { pending ->
            SelectionCard(
                pending = pending,
                isResolving = state.isResolving,
                isBinding = state.isBinding,
                onCancel = { viewModel.onIntent(VerificationLocationIntent.CancelSelection) },
                onConfirm = {
                    viewModel.onIntent(
                        VerificationLocationIntent.Confirm(
                            challengeMemberId = challengeMemberId,
                            radiusM = defaultRadiusM,
                            dwellMinutes = dwellMinutes,
                        ),
                    )
                },
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
            )
        }
    }
}

/**
 * 핀 위치 확인 카드(명세 §5.3). 장소명·주소를 보여주고 "이 위치 선택" 으로 확정한다.
 * 역지오코딩 중([isResolving])엔 확정 버튼을 잠그고, 등록 중([isBinding])엔 스피너를 띄운다.
 */
@Composable
private fun SelectionCard(
    pending: PendingSelection,
    isResolving: Boolean,
    isBinding: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("이 위치를 선택하시겠습니까?", style = MaterialTheme.typography.titleMedium)
            Text(
                pending.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            pending.address?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = rememberSingleClick { onCancel() },
                    enabled = !isBinding,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("취소")
                }
                Button(
                    onClick = rememberSingleClick { onConfirm() },
                    // 주소 확인 전(resolving)·등록 중엔 확정 차단.
                    enabled = !isBinding && !isResolving,
                    modifier = Modifier.weight(1f),
                ) {
                    if (isBinding) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("이 위치 선택")
                    }
                }
            }
        }
    }
}

private const val DEFAULT_LAT = 37.5665
private const val DEFAULT_LNG = 126.9780

// 자동완성 디바운스(ms): 타이핑이 멈춘 뒤 이만큼 지나면 검색.
private const val SEARCH_DEBOUNCE_MS = 300L
