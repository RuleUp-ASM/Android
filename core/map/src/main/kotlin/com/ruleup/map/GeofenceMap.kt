package com.ruleup.map

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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.tasks.await
import timber.log.Timber

/**
 * 카카오 지도 기반 위치 선택(명세 §5.3). 지도를 탭하면 그 좌표를 [onMapTap] 으로 올려보내고(확인 대기),
 * 외부에서 [pin] 이 정해지면(탭 역지오코딩·검색 결과) 그 자리에 핀 + 반경 원을 그리고 카메라가 따라간다.
 * [pin] 이 null 이면 핀/원을 지운다(아직 선택 전·취소).
 * [anchors] 는 이미 담아둔 앵커들 — 각각 번호(1부터) 핀 + 반경 원으로 고정 표시된다.
 *
 * Kakao [MapView] 는 GLSurfaceView 기반 네이티브 뷰라 [AndroidView] 로 감싸고, 호스트의
 * resume/pause 를 전달해야 한다(카카오 가이드, 미전달 시 렌더링 크래시).
 */
@Composable
fun GeofenceMap(
    initialCenter: MapLatLng,
    pin: MapLatLng?,
    radiusM: Float,
    onMapTap: (MapLatLng) -> Unit,
    modifier: Modifier = Modifier,
    anchors: List<MapAnchor> = emptyList(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // 지도 콜백은 컴포지션 밖에서 호출되므로 항상 최신 람다를 가리키게 한다.
    val currentOnMapTap by rememberUpdatedState(onMapTap)

    // 카카오 지도 네이티브 라이브러리가 없는 ABI(x86_64 에뮬레이터 등)에서는 MapView 생성/시작이 던진다.
    // 화면 전체가 죽지 않도록 막고, 지도 자리에 안내를 보여준다(나머지 picker 는 정상 동작).
    val mapView = remember { runCatching { MapView(context) }.getOrNull() }
    if (mapView == null) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("이 기기에서는 지도를 표시할 수 없어요")
        }
        return
    }
    val objects = remember { GeofenceMapObjects() }

    // 지도 시작/정리(컴포저블 수명 1회). getPosition/getZoomLevel 로 최초 카메라를 잡는다.
    DisposableEffect(Unit) {
        runCatching {
            mapView.start(
                object : MapLifeCycleCallback() {
                    override fun onMapDestroy() = Unit

                    // 인증 실패(키해시·패키지명·앱키 불일치)·렌더 오류는 여기로만 온다. 삼키면 "빈 지도"로만 보이므로
                    // 'KakaoMap' 태그로 남겨 디버그 오버레이/Logcat 에서 실제 사유를 확인한다.
                    override fun onMapError(error: Exception) {
                        Timber.tag("KakaoMap").w(error, "지도 인증/렌더 실패 — 키해시·패키지명·네이티브앱키 확인")
                    }
                },
                object : KakaoMapReadyCallback() {
                    override fun onMapReady(kakaoMap: KakaoMap) {
                        objects.kakaoMap = kakaoMap
                        // 탭한 좌표를 화면으로 올려보낸다(확인 대기 핀 → 카드, 명세 §5.3).
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
        }
        onDispose { objects.kakaoMap = null }
    }

    // 호스트 resume/pause 를 MapView 에 전달.
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

    // 외부 pin/radius/anchors 변경 → 카메라·핀·원 동기화. pin 이 null 이면 핀/원을 지운다.
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

/**
 * KakaoMap·핀·원 참조를 recomposition/콜백 간 보관한다(컴포지션 밖 SDK 호출용).
 * 핀은 생성 후 [Label.moveTo] 로 이동, 원은 반경이 바뀔 수 있으니 매번 지우고 다시 그린다.
 * [pin] 이 null 이면 둘 다 레이어에서 제거한다(선택 전·취소).
 * 앵커(등록된 인증 장소)는 번호 텍스트 핀 + 반경 원 묶음으로, 목록이 바뀌면 전체를 다시 그린다.
 */
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
        // 반경 원 채움색(반투명 인디고, Design System v2.0 brand). Compose Color → ARGB Int.
        private val CIRCLE_FILL_ARGB = Color(0x246366F1).toArgb()

        // 앵커 번호 텍스트: 인디고 본문 + 흰색 외곽선(밝은 지도 위 가독성).
        private const val ANCHOR_TEXT_SIZE = 28
        private val ANCHOR_TEXT_ARGB = Color(0xFF4F46E5).toArgb()
        private const val ANCHOR_TEXT_STROKE = 3
        private val ANCHOR_TEXT_STROKE_ARGB = Color(0xFFFFFFFF).toArgb()
    }
}

@Composable
fun rememberLocationLocator(): LocationLocator {
    val context = LocalContext.current
    return remember(context) { FusedLocationLocator(context) }
}

@Composable
fun rememberLocationPermissionGranted(): Boolean {
    val context = LocalContext.current
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
