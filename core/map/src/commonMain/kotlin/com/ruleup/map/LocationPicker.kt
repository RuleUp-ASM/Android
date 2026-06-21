package com.ruleup.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * 핀 + 반경 + 라벨 위치 선택 UI(명세 §5.3). 결과는 {lat,lng,radiusM,label} 로 [onConfirm].
 *
 * 엣지케이스(§5.4): 위치 권한이 없으면 "현재 위치" 버튼만 비활성(타일·수동 핀은 허용),
 * 반경은 50~1000m 로 클램프, [confirmEnabled]=false 면 확정 차단(미설정 보류/차단).
 */
@Composable
fun LocationPickerContent(
    initialCenter: MapLatLng,
    initialRadiusM: Float,
    initialLabel: String,
    onConfirm: (lat: Double, lng: Double, radiusM: Float, label: String) -> Unit,
    modifier: Modifier = Modifier,
    confirmEnabled: Boolean = true,
) {
    var center by remember { mutableStateOf(initialCenter) }
    var radius by remember { mutableFloatStateOf(clampRadius(initialRadiusM)) }
    var label by remember { mutableStateOf(initialLabel) }

    val locator = rememberLocationLocator()
    val permissionGranted = rememberLocationPermissionGranted()
    val scope = rememberCoroutineScope()

    Column(modifier) {
        GeofenceMap(
            center = center,
            radiusM = radius,
            onCenterChange = { center = it },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("반경 ${radius.toInt()}m")
            OutlinedButton(
                onClick = { scope.launch { locator.locate()?.let { center = it } } },
                // 권한 거부 시 "현재 위치"만 비활성(명세 §5.4.2).
                enabled = permissionGranted,
            ) {
                Text("현재 위치")
            }
        }

        Slider(
            value = radius,
            onValueChange = { radius = clampRadius(it) },
            valueRange = MIN_RADIUS_M..MAX_RADIUS_M,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = label,
            onValueChange = { label = it },
            label = { Text("장소 이름 (예: 우리 동네 헬스장)") },
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = { onConfirm(center.lat, center.lng, clampRadius(radius), label.trim()) },
            enabled = confirmEnabled,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
        ) {
            Text("이 위치로 설정")
        }
    }
}
