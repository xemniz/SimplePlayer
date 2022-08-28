package ru.tinkoff.player

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import android.os.FileUtils
import android.os.Handler
import android.os.HandlerThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import linc.com.amplituda.Amplituda
import linc.com.amplituda.Cache
import linc.com.amplituda.Compress
import java.lang.Math.sqrt
import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch
import kotlin.math.pow

class Dependencies(private val context: Context) {

    fun getPlayerViewModel(): PlayerViewModel {
        return PlayerViewModel(amplitudaSamplesExtractor())
    }

    private fun amplitudaSamplesExtractor() = object : SamplesExtractor {
        private val amplituda: Amplituda = Amplituda(context)
        override suspend fun samples(uri: Uri): IntArray {
            return withContext(Dispatchers.Default) {

                amplituda.processAudio(
                    context.contentResolver.openInputStream(uri),
                    Compress.withParams(Compress.AVERAGE, 5),
                    Cache.withParams(Cache.REUSE),
                    null
                ).get().amplitudesAsList().toIntArray()

            }
        }
    }
}