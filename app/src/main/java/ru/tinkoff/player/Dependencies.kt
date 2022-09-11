package ru.tinkoff.player

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import linc.com.amplituda.Amplituda
import linc.com.amplituda.Cache
import linc.com.amplituda.Compress

class Dependencies(private val context: Context) {

    fun getPlayerViewModel(): PlayerViewModel {
        return PlayerViewModel(amplitudaSamplesExtractor(), player = Player(context))
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