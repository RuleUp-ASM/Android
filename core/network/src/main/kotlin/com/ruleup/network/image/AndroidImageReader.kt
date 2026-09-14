package com.ruleup.network.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/** content URI 의 이미지를 다운샘플·EXIF 보정 후 JPEG 바이트로 읽는다. */
class AndroidImageReader
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ImageReader {
        override suspend fun read(uri: String): ImageBytes =
            withContext(Dispatchers.IO) {
                val parsed = uri.toUri()
                val resolver = context.contentResolver

                // inJustDecodeBounds 모드의 decodeStream 은 항상 null 이다 — 반환값이 아니라 채워진 크기로 판정한다.
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                val opened =
                    resolver.openInputStream(parsed)?.use { BitmapFactory.decodeStream(it, null, bounds) } != null ||
                        bounds.outWidth > 0
                if (!opened || bounds.outWidth <= 0 || bounds.outHeight <= 0) throw unreadable()

                val options =
                    BitmapFactory.Options().apply {
                        inSampleSize = computeInSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIMENSION)
                    }
                val decoded =
                    resolver.openInputStream(parsed)?.use { BitmapFactory.decodeStream(it, null, options) }
                        ?: throw unreadable()

                // BitmapFactory 는 EXIF 를 반영하지 않는다 — 직접 회전하지 않으면 세로 사진이 눕는다.
                val orientation =
                    resolver.openInputStream(parsed)?.use {
                        ExifInterface(it).getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL,
                        )
                    } ?: ExifInterface.ORIENTATION_NORMAL
                val oriented = decoded.applyExifOrientation(orientation)

                val output = ByteArrayOutputStream()
                oriented.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
                if (oriented !== decoded) decoded.recycle()
                oriented.recycle()

                ImageBytes(bytes = output.toByteArray(), mimeType = "image/jpeg")
            }

        // 메시지가 그대로 토스트로 뜨는 화면이 있다 — content URI 같은 내부 값을 싣지 않는다.
        private fun unreadable() = IllegalArgumentException("사진을 읽을 수 없어요. 다른 사진을 골라 주세요")

        // inSampleSize 는 2의 거듭제곱만 유효하다 — 다른 값은 BitmapFactory 가 아래로 내림한다.
        private fun computeInSampleSize(
            width: Int,
            height: Int,
            target: Int,
        ): Int {
            var sample = 1
            var longest = maxOf(width, height)
            while (longest / 2 >= target) {
                longest /= 2
                sample *= 2
            }
            return sample
        }

        private fun Bitmap.applyExifOrientation(orientation: Int): Bitmap {
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                else -> return this
            }
            return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
        }

        private companion object {
            const val MAX_DIMENSION = 1920
            const val JPEG_QUALITY = 85
        }
    }
