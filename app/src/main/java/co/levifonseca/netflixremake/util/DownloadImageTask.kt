package co.levifonseca.netflixremake.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Log
import co.levifonseca.netflixremake.model.Category
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors


class DownloadImageTask(private val callback: Callback) {

    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()

    interface Callback {
        fun onResult(bitmap: Bitmap)
    }

    fun execute(url: String) {
        executor.execute {
            var urlConnection: HttpURLConnection? = null
            var stream: InputStream? = null
            try {
                val requestURL = URL(url) // abrir uma URL
                urlConnection = requestURL.openConnection() as HttpURLConnection // abrir a conexão
                urlConnection.readTimeout = 2000 // tempo leitura (2s)
                urlConnection.connectTimeout = 2000 // tempo conexão (2s)

                val statusCode = urlConnection.responseCode
                if (statusCode > 400) {
                    throw IOException ("Erro na comunicação com o servidor!")
                }

                stream = urlConnection.inputStream // sequencia de bytes
                val bitmap = BitmapFactory.decodeStream(stream)

                handler.post {
                    callback.onResult(bitmap)
                }
            } catch (e: IOException) {
                val message = e.message ?: "erro desconhecido"
                Log.e("Teste", message, e)
            } finally {
                urlConnection?.disconnect()
                stream?.close()
            }
        }
    }
}