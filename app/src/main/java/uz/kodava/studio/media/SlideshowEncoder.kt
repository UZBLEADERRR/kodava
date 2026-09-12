package uz.kodava.studio.media

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaMuxer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/** Bitta kadr: rasm va u ekranda turadigan vaqt. */
data class SlideFrame(val bitmap: Bitmap, val durationSec: Int)

/**
 * Yaratilgan sahna rasmlaridan MP4 (H.264) slayd-shou yig'adi.
 * Sahnalar orasida yumshoq o'tish (crossfade) qo'shiladi. Tashqi kutubxona ishlatilmaydi.
 */
object SlideshowEncoder {

    private const val FPS = 24
    private const val TRANSITION_FRAMES = 12
    private const val TIMEOUT_US = 10_000L

    suspend fun encode(
        slides: List<SlideFrame>,
        width: Int,
        height: Int,
        output: File,
        onProgress: (Float) -> Unit = {}
    ): File = withContext(Dispatchers.Default) {
        require(slides.isNotEmpty()) { "Rasmlar topilmadi" }

        val w = width.roundToEven()
        val h = height.roundToEven()

        val colorFormat = pickColorFormat()
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, w, h).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat)
            setInteger(MediaFormat.KEY_BIT_RATE, (w * h * 6).coerceIn(2_000_000, 12_000_000))
            setInteger(MediaFormat.KEY_FRAME_RATE, FPS)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()

        val muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val state = MuxState(muxer)
        val bufferInfo = MediaCodec.BufferInfo()

        try {
            val canvasFrames = slides.map { fit(it.bitmap, w, h) }
            val totalFrames = slides.sumOf { (it.durationSec * FPS).coerceAtLeast(FPS) }
            var frameIndex = 0

            slides.forEachIndexed { index, slide ->
                val current = canvasFrames[index]
                val holdFrames = (slide.durationSec * FPS).coerceAtLeast(FPS) -
                    if (index < slides.lastIndex) TRANSITION_FRAMES else 0

                val staticYuv = toYuv(current, w, h, colorFormat)
                repeat(holdFrames.coerceAtLeast(1)) {
                    feed(codec, state, bufferInfo, staticYuv, frameIndex++)
                    onProgress((frameIndex.toFloat() / totalFrames).coerceIn(0f, 1f))
                }

                if (index < slides.lastIndex) {
                    val next = canvasFrames[index + 1]
                    val blend = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(blend)
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                    for (t in 1..TRANSITION_FRAMES) {
                        val alpha = t.toFloat() / (TRANSITION_FRAMES + 1)
                        canvas.drawColor(Color.BLACK)
                        paint.alpha = 255
                        canvas.drawBitmap(current, 0f, 0f, paint)
                        paint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
                        canvas.drawBitmap(next, 0f, 0f, paint)
                        feed(codec, state, bufferInfo, toYuv(blend, w, h, colorFormat), frameIndex++)
                        onProgress((frameIndex.toFloat() / totalFrames).coerceIn(0f, 1f))
                    }
                    blend.recycle()
                }
            }

            // Oxirgi kadrni yakunlash
            val inputIndex = codec.dequeueInputBuffer(TIMEOUT_US * 10)
            if (inputIndex >= 0) {
                codec.queueInputBuffer(inputIndex, 0, 0, ptsOf(frameIndex), MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            }
            drain(codec, state, bufferInfo, endOfStream = true)
            canvasFrames.forEach { it.recycle() }
            onProgress(1f)
        } finally {
            runCatching { codec.stop() }
            runCatching { codec.release() }
            runCatching { if (state.started) muxer.stop() }
            runCatching { muxer.release() }
        }
        output
    }

    private class MuxState(val muxer: MediaMuxer) {
        var trackIndex = -1
        var started = false
    }

    private fun ptsOf(frameIndex: Int): Long = frameIndex * 1_000_000L / FPS

    private fun feed(
        codec: MediaCodec,
        state: MuxState,
        info: MediaCodec.BufferInfo,
        yuv: ByteArray,
        frameIndex: Int
    ) {
        var queued = false
        while (!queued) {
            val index = codec.dequeueInputBuffer(TIMEOUT_US)
            if (index >= 0) {
                val buffer: ByteBuffer = codec.getInputBuffer(index) ?: continue
                buffer.clear()
                buffer.put(yuv)
                codec.queueInputBuffer(index, 0, yuv.size, ptsOf(frameIndex), 0)
                queued = true
            }
            drain(codec, state, info, endOfStream = false)
        }
    }

    private fun drain(codec: MediaCodec, state: MuxState, info: MediaCodec.BufferInfo, endOfStream: Boolean) {
        while (true) {
            val index = codec.dequeueOutputBuffer(info, if (endOfStream) TIMEOUT_US * 10 else 0)
            when {
                index == MediaCodec.INFO_TRY_AGAIN_LATER -> if (!endOfStream) return
                index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    if (!state.started) {
                        state.trackIndex = state.muxer.addTrack(codec.outputFormat)
                        state.muxer.start()
                        state.started = true
                    }
                }
                index >= 0 -> {
                    val encoded = codec.getOutputBuffer(index)
                    if (encoded != null && info.size > 0 && state.started &&
                        (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0
                    ) {
                        encoded.position(info.offset)
                        encoded.limit(info.offset + info.size)
                        state.muxer.writeSampleData(state.trackIndex, encoded, info)
                    }
                    codec.releaseOutputBuffer(index, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) return
                }
            }
        }
    }

    /** Rasmni video o'lchamiga to'ldirib joylash (markazdan kesish). */
    private fun fit(src: Bitmap, w: Int, h: Int): Bitmap {
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.BLACK)
        val scale = maxOf(w.toFloat() / src.width, h.toFloat() / src.height)
        val dw = src.width * scale
        val dh = src.height * scale
        val left = (w - dw) / 2f
        val top = (h - dh) / 2f
        canvas.drawBitmap(
            src,
            Rect(0, 0, src.width, src.height),
            RectF(left, top, left + dw, top + dh),
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        )
        return out
    }

    private fun pickColorFormat(): Int {
        val list = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        for (info in list.codecInfos) {
            if (!info.isEncoder) continue
            if (info.supportedTypes.none { it.equals(MediaFormat.MIMETYPE_VIDEO_AVC, true) }) continue
            val formats = info.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC).colorFormats
            if (formats.contains(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar)) {
                return MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
            }
            if (formats.contains(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar)) {
                return MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar
            }
        }
        return MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
    }

    /** ARGB -> YUV420 (NV12 yoki I420). */
    private fun toYuv(bitmap: Bitmap, w: Int, h: Int, colorFormat: Int): ByteArray {
        val argb = IntArray(w * h)
        bitmap.getPixels(argb, 0, w, 0, 0, w, h)
        val out = ByteArray(w * h * 3 / 2)
        val semiPlanar = colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
        val frameSize = w * h
        var uIndex = frameSize
        var vIndex = if (semiPlanar) frameSize + 1 else frameSize + frameSize / 4

        var index = 0
        for (y in 0 until h) {
            for (x in 0 until w) {
                val color = argb[index]
                val r = (color shr 16) and 0xFF
                val g = (color shr 8) and 0xFF
                val b = color and 0xFF

                val yy = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                out[index] = yy.coerceIn(0, 255).toByte()

                if (y % 2 == 0 && x % 2 == 0) {
                    val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                    val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
                    if (semiPlanar) {
                        out[uIndex] = u.coerceIn(0, 255).toByte()
                        out[vIndex] = v.coerceIn(0, 255).toByte()
                        uIndex += 2
                        vIndex += 2
                    } else {
                        out[uIndex] = u.coerceIn(0, 255).toByte()
                        out[vIndex] = v.coerceIn(0, 255).toByte()
                        uIndex++
                        vIndex++
                    }
                }
                index++
            }
        }
        return out
    }

    private fun Int.roundToEven(): Int = if (this % 2 == 0) this else this - 1
}
