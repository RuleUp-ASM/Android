package com.ruleup.verification.presentation.location

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.map.LocationPickerContent
import com.ruleup.map.MapLatLng
import com.ruleup.ui.helper.LocalMessageHelper
import com.ruleup.verification.presentation.location.viewmodel.VerificationLocationEffect
import com.ruleup.verification.presentation.location.viewmodel.VerificationLocationIntent
import com.ruleup.verification.presentation.location.viewmodel.VerificationLocationViewModel
import dev.zacsweers.metrox.viewmodel.metroViewModel

/**
 * 지도 핀 화면(명세 §5). 생성/참여 시 GPS 루틴이면 진입하며, 핀+반경 확정 시 멤버 좌표 지오펜스를 등록한다.
 * 브랜드/지점 키워드로 검색(명세 §5.2·§11.7)해 결과를 고르면 지도 중심이 그 좌표로 이동한다.
 * 초기 중심은 서울 시청 — 사용자가 검색/탭/현재위치로 옮긴다.
 */
@Composable
fun VerificationLocationScreen(
    challengeMemberId: String,
    defaultRadiusM: Float,
    dwellMinutes: Int,
    modifier: Modifier = Modifier,
) {
    val viewModel = metroViewModel<VerificationLocationViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val messageHelper = LocalMessageHelper.current
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is VerificationLocationEffect.ShowMessage -> messageHelper.showToast(effect.message)
            }
        }
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
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                viewModel.onIntent(
                    VerificationLocationIntent.Search(
                        query = query,
                        lat = DEFAULT_LAT,
                        lng = DEFAULT_LNG,
                        radiusM = NEARBY_RADIUS_M,
                    ),
                )
            },
            enabled = !state.isSearching && query.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (state.isSearching) "검색 중…" else "검색")
        }

        // 결과는 상한 10개(명세 §5.2)라 단순 나열. 선택 시 지도 중심이 이동한다.
        state.places.forEach { place ->
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.onIntent(VerificationLocationIntent.SelectPlace(place)) }
                        .padding(vertical = 8.dp),
            ) {
                Text(place.name)
                place.address?.let { Text(it) }
            }
        }

        // 선택 좌표가 바뀌면 picker 를 재구성해 중심을 옮긴다(LocationPickerContent 는 initialCenter 만 받음).
        // weight 는 ColumnScope 멤버라 Box 에 걸고, 재구성은 그 안의 key() 로 처리한다.
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
        ) {
            key(state.selected) {
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
}

private const val DEFAULT_LAT = 37.5665
private const val DEFAULT_LNG = 126.9780

// NEARBY_BRAND 기본 검색 반경(명세 §5.2, 0.5~5km 중 기본 1.5km).
private const val NEARBY_RADIUS_M = 1500