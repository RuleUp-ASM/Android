package com.ruleup.verification.presentation.location.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.Label
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.label.LabelTextBuilder
import com.kakao.vectormap.label.LabelTextStyle
import com.kakao.vectormap.shape.DotPoints
import com.kakao.vectormap.shape.Polygon
import com.kakao.vectormap.shape.PolygonOptions
import com.kakao.vectormap.shape.PolygonStyles
import com.kakao.vectormap.shape.PolygonStylesSet
import com.ruleup.designsystem.theme.RuleUpPalette
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.observability.domain.api.w
import com.ruleup.tti.domain.TtiTimeline
import com.ruleup.tti.presentation.TtiSpanEffect
import com.ruleup.ui.helper.LocalObservability
import com.ruleup.verification.presentation.R
import kotlinx.coroutines.tasks.await

/** 카카오 지도 기반 위치 선택. */
@Composable
fun GeofenceMap(
    initialCenter: MapLatLng,
    pin: MapLatLng?,
    radiusM: Float,
    onMapTap: (MapLatLng) -> Unit,
    modifier: Modifier = Modifier,
    anchors: List<MapAnchor> = emptyList(),
) {
    if (androidx.compose.ui.platform.LocalInspectionMode.current) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("지도 미리보기 · 반경 ${radiusM.toInt()}m")
        }
    } else {
        LiveGeofenceMap(initialCenter, pin, radiusM, onMapTap, modifier, anchors)
    }
}

@Composable
private fun LiveGeofenceMap(
    initialCenter: MapLatLng,
    pin: MapLatLng?,
    radiusM: Float,
    onMapTap: (MapLatLng) -> Unit,
    modifier: Modifier = Modifier,
    anchors: List<MapAnchor> = emptyList(),
) {
    val context = LocalContext.current
    val observability = LocalObservability.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // 지도 콜백은 컴포지션 밖에서 호출되므로 항상 최신 람다를 가리키게 한다.
    val currentOnMapTap by rememberUpdatedState(onMapTap)

    // 지도 SDK 초기화 실패 처리.
    val mapView = remember { runCatching { MapView(context) }.getOrNull() }
    var loading by remember { mutableStateOf(mapView != null) }
    TtiSpanEffect(TtiTimeline.BIG_PART_LOADING, loading)
    if (mapView == null) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("이 기기에서는 지도를 표시할 수 없어요")
        }
        return
    }
    val objects = remember { GeofenceMapObjects() }

    DisposableEffect(Unit) {
        runCatching {
            mapView.start(
                object : MapLifeCycleCallback() {
                    override fun onMapDestroy() = Unit

                    // 지도 인증·렌더링 오류 처리.
                    override fun onMapError(error: Exception) {
                        loading = false
                        observability.w("KakaoMap", error) { "지도 인증/렌더 실패 — 키해시·패키지명·네이티브앱키 확인" }
                    }
                },
                object : KakaoMapReadyCallback() {
                    override fun onMapReady(kakaoMap: KakaoMap) {
                        loading = false
                        objects.kakaoMap = kakaoMap
                        kakaoMap.setOnMapClickListener { _, position, _, _ ->
                            currentOnMapTap(MapLatLng(position.latitude, position.longitude))
                        }
                        // 진입 시 이미 핀/앵커가 정해져 있으면(편집 등) 그려둔다.
                        objects.drawPin(pin)
                        objects.drawCircle(pin, radiusM)
                        objects.drawAnchors(anchors)
                    }

                    override fun getPosition(): LatLng {
                        val start = pin ?: initialCenter
                        return LatLng.from(start.lat, start.lng)
                    }

                    override fun getZoomLevel(): Int = DEFAULT_ZOOM_LEVEL
                },
            )
        }.onFailure { loading = false }
        onDispose { objects.kakaoMap = null }
    }

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                runCatching {
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> mapView.resume()
                        Lifecycle.Event.ON_PAUSE -> mapView.pause()
                        else -> Unit
                    }
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(pin, radiusM, anchors) {
        val kakaoMap = objects.kakaoMap ?: return@LaunchedEffect
        if (pin != null) {
            kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(LatLng.from(pin.lat, pin.lng)))
        }
        objects.drawPin(pin)
        objects.drawCircle(pin, radiusM)
        objects.drawAnchors(anchors)
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

/** 지도·핀·원 참조. */
private class GeofenceMapObjects {
    var kakaoMap: KakaoMap? = null
    private var label: Label? = null
    private var circle: Polygon? = null
    private var styles: LabelStyles? = null
    private var anchorStyles: LabelStyles? = null
    private var anchorLabels: List<Label> = emptyList()
    private var anchorCircles: List<Polygon> = emptyList()
    private var drawnAnchors: List<MapAnchor> = emptyList()

    fun drawPin(pin: MapLatLng?) {
        val manager = kakaoMap?.labelManager ?: return
        if (pin == null) {
            label?.let { manager.layer?.remove(it) }
            label = null
            return
        }
        val position = LatLng.from(pin.lat, pin.lng)
        val current = label
        if (current != null) {
            current.moveTo(position)
            return
        }
        var style = styles
        if (style == null) {
            style = manager.addLabelStyles(LabelStyles.from(LabelStyle.from(R.drawable.ic_map_pin)))
            styles = style
        }
        label = manager.layer?.addLabel(LabelOptions.from(position).setStyles(style))
    }

    fun drawCircle(
        pin: MapLatLng?,
        radiusM: Float,
    ) {
        val manager = kakaoMap?.shapeManager ?: return
        circle?.let { manager.layer?.remove(it) }
        circle = null
        if (pin == null) return
        circle = addCircle(pin.lat, pin.lng, radiusM)
    }

    fun drawAnchors(anchors: List<MapAnchor>) {
        val kakaoMap = kakaoMap ?: return
        if (anchors == drawnAnchors) return
        anchorLabels.forEach { kakaoMap.labelManager?.layer?.remove(it) }
        anchorCircles.forEach { kakaoMap.shapeManager?.layer?.remove(it) }
        anchorLabels = emptyList()
        anchorCircles = emptyList()
        drawnAnchors = anchors
        if (anchors.isEmpty()) return

        var style = anchorStyles
        if (style == null) {
            val textStyle =
                LabelTextStyle.from(
                    ANCHOR_TEXT_SIZE,
                    ANCHOR_TEXT_ARGB,
                    ANCHOR_TEXT_STROKE,
                    ANCHOR_TEXT_STROKE_ARGB,
                )
            style =
                kakaoMap.labelManager?.addLabelStyles(
                    LabelStyles.from(LabelStyle.from(R.drawable.ic_map_pin).setTextStyles(textStyle)),
                ) ?: return
            anchorStyles = style
        }
        anchorLabels =
            anchors.mapIndexedNotNull { index, anchor ->
                kakaoMap.labelManager?.layer?.addLabel(
                    LabelOptions
                        .from(LatLng.from(anchor.lat, anchor.lng))
                        .setStyles(style)
                        .setTexts(LabelTextBuilder().setTexts("${index + 1}")),
                )
            }
        anchorCircles = anchors.mapNotNull { addCircle(it.lat, it.lng, it.radiusM) }
    }

    private fun addCircle(
        lat: Double,
        lng: Double,
        radiusM: Float,
    ): Polygon? {
        val manager = kakaoMap?.shapeManager ?: return null
        val dots = DotPoints.fromCircle(LatLng.from(lat, lng), radiusM)
        val stylesSet = PolygonStylesSet.from(PolygonStyles.from(CIRCLE_FILL_ARGB))
        return manager.layer?.addPolygon(PolygonOptions.from(dots, stylesSet))
    }

    companion object {
        // 반경 원 채움색(반투명 브랜드색).
        private val CIRCLE_FILL_ARGB = RuleUpPalette.Primary600.copy(alpha = CIRCLE_FILL_ALPHA).toArgb()
        private const val CIRCLE_FILL_ALPHA = 0.14f

        // 앵커 번호 텍스트: 브랜드색 본문 + 흰색 외곽선(밝은 지도 위 가독성).
        private const val ANCHOR_TEXT_SIZE = 28
        private val ANCHOR_TEXT_ARGB = RuleUpPalette.Primary600.toArgb()
        private const val ANCHOR_TEXT_STROKE = 3
        private val ANCHOR_TEXT_STROKE_ARGB = RuleUpPalette.BgSurface.toArgb()
    }
}

@Composable
fun rememberLocationLocator(): LocationLocator {
    val context = LocalContext.current
    val observability = LocalObservability.current
    return remember(context) { FusedLocationLocator(context) }
}

@Composable
fun rememberLocationPermissionGranted(): Boolean {
    val context = LocalContext.current
    val observability = LocalObservability.current
    return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
}

private class FusedLocationLocator(
    context: Context,
) : LocationLocator {
    private val appContext = context.applicationContext
    private val fused by lazy { LocationServices.getFusedLocationProviderClient(appContext) }

    // locate() 진입 시 checkSelfPermission 으로 직접 권한을 확인한다(아래 가드).
    @SuppressLint("MissingPermission")
    override suspend fun locate(): MapLatLng? {
        if (appContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        return try {
            val location =
                fused
                    .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token)
                    .await()
            location?.let { MapLatLng(it.latitude, it.longitude) }
        } catch (e: SecurityException) {
            null
        }
    }
}

// 카카오 지도 줌 레벨(구글 zoom 15f 와 유사한 동네 단위).
private const val DEFAULT_ZOOM_LEVEL = 15

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun GeofenceMapPreview() {
    RuleUpTheme {
        GeofenceMap(
            initialCenter =
                com.ruleup.verification.presentation.location.map.MapLatLng(
                    lat = 0.75,
                    lng = 0.75,
                ),
            pin = null,
            radiusM = 100f,
            onMapTap = {
            },
        )
    }
}
