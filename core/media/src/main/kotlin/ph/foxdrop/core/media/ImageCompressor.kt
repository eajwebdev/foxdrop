package ph.foxdrop.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import ph.foxdrop.core.common.dispatcher.Dispatcher
import ph.foxdrop.core.common.dispatcher.FoxDispatchers
import ph.foxdrop.core.domain.media.PhotoUpload

/** Turns a picked or taken photo into an upload: upright, at most [MAX_EDGE_PX] on its long edge, JPEG. */
interface ImageCompressor {
    /** Null when the image cannot be read (deleted, not an image). */
    suspend fun compress(uri: Uri, fileName: String): PhotoUpload?
}

@Singleton
internal class AndroidImageCompressor @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:Dispatcher(FoxDispatchers.IO) private val io: CoroutineDispatcher,
) : ImageCompressor {
    override suspend fun compress(uri: Uri, fileName: String): PhotoUpload? =
        withContext(io) {
            try {
                val resolver = context.contentResolver
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
                val options = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, MAX_EDGE_PX)
                }
                val decoded =
                    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                        ?: return@withContext null
                val rotation = resolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0
                val upright = scaleAndRotate(decoded, rotation)
                val out = ByteArrayOutputStream()
                upright.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                PhotoUpload(out.toByteArray(), fileName)
            } catch (_: IOException) {
                null
            } catch (_: SecurityException) {
                null
            }
        }

    private fun scaleAndRotate(bitmap: Bitmap, rotation: Int): Bitmap {
        val (width, height) = targetSize(bitmap.width, bitmap.height, MAX_EDGE_PX)
        val matrix = Matrix().apply {
            postScale(width.toFloat() / bitmap.width, height.toFloat() / bitmap.height)
            if (rotation != 0) postRotate(rotation.toFloat())
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    companion object {
        const val MAX_EDGE_PX = 1600
        const val JPEG_QUALITY = 85
    }
}

/** The largest power-of-two step that still leaves the long edge at or above [maxEdge] (cheap first decode). */
internal fun sampleSize(width: Int, height: Int, maxEdge: Int): Int {
    var sample = 1
    while (max(width, height) / (sample * 2) >= maxEdge) sample *= 2
    return sample
}

/** The final size: unchanged when it already fits, else scaled down so the long edge is [maxEdge]. */
internal fun targetSize(width: Int, height: Int, maxEdge: Int): Pair<Int, Int> {
    val longEdge = max(width, height)
    if (longEdge <= maxEdge) return width to height
    val scale = maxEdge.toDouble() / longEdge
    return (width * scale).roundToInt().coerceAtLeast(1) to (height * scale).roundToInt().coerceAtLeast(1)
}
