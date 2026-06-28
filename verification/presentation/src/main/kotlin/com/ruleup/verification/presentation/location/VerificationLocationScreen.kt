package com.ruleup.verification.presentation.location

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.map.LocationPickerContent
import com.ruleup.map.MapLatLng
import com.ruleup.ui.helper.LocalMessageHelper
import com.ruleup.ui.helper.singleClickable
import com.ruleup.verification.presentation.location.viewmodel.VerificationLocationEffect
import com.ruleup.verification.presentation.location.viewmodel.VerificationLocationIntent
import com.ruleup.verification.presentation.location.viewmodel.VerificationLocationViewModel
import kotlinx.coroutines.delay

/**
 * 지도 핀 화면(명세 §5). 생성/참여 시 GPS 루틴이면 진입하며, 핀+반경 확정 시 멤버 좌표 지오펜스를 등록한다.
 * 브랜드/지점 키워드를 입력하면 카카오 로컬로 자동완성(명세 §5.2)되고, 결과를 고르면 지도 중심이 그 좌표로 이동한다.
 * 초기 중심은 서울 시청 — 사용자가 검색/탭/현재위치로 옮긴다.
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

    val center = state.selected?.let { MapLatLng(lat = it.lat, lng = it.lng) } ?: MapLatLng(DEFAULT_LAT, DEFAULT_LNG)

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("브랜드·지점 검색 (예: 스포애니)") },
            singleLine = true,
            trailingIcon =
                if (state.isSearching) {
                    { CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) }
                } else {
                    null
                },
            modifier = Modifier.fillMaxWidth(),
        )

        // 자동완성 결과(상한 15개, 명세 §5.2). 선택 시 입력칸을 채우고 목록을 닫으며 지도 중심이 이동한다.
        state.places.forEach { place ->
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .singleClickable {
                            suppressSearch = true
                            query = place.name
                            viewModel.onIntent(VerificationLocationIntent.SelectPlace(place))
                            viewModel.onIntent(VerificationLocationIntent.ClearSearch)
                        }.padding(vertical = 8.dp),
            ) {
                Text(place.name)
                place.address?.let { Text(it) }
            }
        }

        // 선택 좌표가 바뀌면 LocationPickerContent 가 initialCenter 변경에 반응해 중심을 옮긴다(카카오 MapView 재생성 회피).
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
        ) {
            LocationPickerContent(
                initialCenter = center,
                initialRadiusM = defaultRadiusM,
                initialLabel = state.selected?.label.orEmpty(),
                onConfirm = { lat, lng, radiusM, label ->
                    viewModel.onIntent(
                        VerificationLocationIntent.Confirm(
                            challengeMemberId = challengeMemberId,
                            lat = lat,
                            lng = lng,
                            radiusM = radiusM,
                            dwellMinutes = dwellMinutes,
                            label = label,
                        ),
                    )
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private const val DEFAULT_LAT = 37.5665
private const val DEFAULT_LNG = 126.9780

// 자동완성 디바운스(ms): 타이핑이 멈춘 뒤 이만큼 지나면 검색.
private const val SEARCH_DEBOUNCE_MS = 300L
