package com.ruleup.verification.presentation.location

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ruleup.map.LocationPickerContent
import com.ruleup.map.MapLatLng
import com.ruleup.ui.helper.LocalMessageHelper
import com.ruleup.verification.presentation.location.viewmodel.VerificationLocationEffect
import com.ruleup.verification.presentation.location.viewmodel.VerificationLocationIntent
import com.ruleup.verification.presentation.location.viewmodel.VerificationLocationViewModel
import dev.zacsweers.metrox.viewmodel.metroViewModel

/**
 * 지도 핀 화면(명세 §5). 생성/참여 시 GPS 루틴이면 진입하며, 핀+반경 확정 시 멤버 좌표 지오펜스를 등록한다.
 * 초기 중심은 서울 시청 — 사용자가 탭/현재위치로 옮긴다.
 */
@Composable
fun VerificationLocationScreen(
    challengeMemberId: String,
    defaultRadiusM: Float,
    dwellMinutes: Int,
    modifier: Modifier = Modifier,
) {
    val viewModel = metroViewModel<VerificationLocationViewModel>()
    val messageHelper = LocalMessageHelper.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is VerificationLocationEffect.ShowMessage -> messageHelper.showToast(effect.message)
            }
        }
    }

    LocationPickerContent(
        initialCenter = MapLatLng(lat = DEFAULT_LAT, lng = DEFAULT_LNG),
        initialRadiusM = defaultRadiusM,
        initialLabel = "",
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
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
    )
}

private const val DEFAULT_LAT = 37.5665
private const val DEFAULT_LNG = 126.9780
