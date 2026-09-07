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

                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(parsed)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                    ?: throw IllegalArgumentException("이미지를 읽을 수 없습니다: $uri")

                val options =
                    BitmapFactory.Options().apply {
                        inSampleSize = computeInSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIMENSION)
                    }
                val decoded =
                    resolver.openInputStream(parsed)?.use { BitmapFactory.decodeStream(it, null, options) }
                        ?: throw IllegalArgumentException("이미지를 읽을 수 없습니다: $uri")

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
