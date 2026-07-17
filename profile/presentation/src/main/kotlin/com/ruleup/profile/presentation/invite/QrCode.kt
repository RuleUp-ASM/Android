package com.ruleup.profile.presentation.invite

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import android.graphics.Color as AndroidColor

/**
 * 초대 링크 QR 비트맵 생성 (스펙: QR 은 클라 렌더링 — 서버 생성 미채택).
 * 픽셀 그대로 그려도 이미지 뷰가 확대하므로 모듈당 1px 매트릭스로 만든다.
 */
@Composable
internal fun rememberQrBitmap(
    content: String,
    sizePx: Int = 512,
): ImageBitmap? =
    remember(content, sizePx) {
        runCatching {
            val matrix =
                QRCodeWriter().encode(
                    content,
                    BarcodeFormat.QR_CODE,
                    sizePx,
                    sizePx,
                    mapOf(EncodeHintType.MARGIN to 1),
                )
            val pixels =
                IntArray(matrix.width * matrix.height) { index ->
                    val x = index % matrix.width
                    val y = index / matrix.width
                    if (matrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE
                }
            Bitmap
                .createBitmap(pixels, matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
                .asImageBitmap()
        }.getOrNull()
    }
