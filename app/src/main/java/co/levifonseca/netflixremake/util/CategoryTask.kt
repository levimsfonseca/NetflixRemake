package co.levifonseca.netflixremake.util

import android.os.Handler
import android.os.Looper
import android.text.method.MovementMethod
import android.util.Log
import co.levifonseca.netflixremake.model.Category
import co.levifonseca.netflixremake.model.Movie
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class CategoryTask(private val callback: Callback) {

    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()

    interface Callback {
        fun onPresExecute ()
        fun onResult(categories: List<Category>)
        fun onFailure(message: String)
    }

    fun execute(url: String){
        callback.onPresExecute()
        // nesse momento, estamos utilizando a UI-thread (1)

        executor.execute {
            var urlConnection: HttpURLConnection? = null
            var buffer: BufferedInputStream? = null
            var stream: InputStream? = null
            try {
                // nesse momento, estamos utilizando a NOVA-thread[processo paralelo] (2)
                val requestURL = URL(url) // abrir uma URL
                urlConnection = requestURL.openConnection() as HttpURLConnection // abrir a conexão
                urlConnection.readTimeout = 2000 // tempo leitura (2s)
                urlConnection.connectTimeout = 2000 // tempo conexão (2s)

                val statusCode = urlConnection.responseCode
                if (statusCode > 400) {
                    throw IOException ("Erro na comunicação com o servidor!")
                }
                stream = urlConnection.inputStream // sequencia de bytes
                // forma1: simples e rápida

                // val jsonAsString =  stream.bufferedReader().use { it.readText() }


                // forma 2: bytes -> string
                buffer = BufferedInputStream(stream)
                val jsonAsString = toString(buffer)

                val categories = toCategories(jsonAsString)

                handler.post {
                    // aqui roda dentro da UI-thread
                    callback.onResult(categories)
                }

            } catch (e: IOException) {
                val message = e.message ?: "erro desconhecido"
                Log.e("Teste", message, e)
                handler.post {
                    callback.onFailure(message)
                }
            } finally {
                urlConnection?.disconnect()
                stream?.close()
                buffer?.close()
            }
        }
    }

    private fun toCategories(jsonAsString: String) : List <Category>{
        val categories = mutableListOf<Category>()

        val jsonRoot = JSONObject(jsonAsString)
        val jsonCategories = jsonRoot.getJSONArray("category")
        for (i in 0 until jsonCategories.length()) {
            val jsonCategory = jsonCategories.getJSONObject(i)

            val title = jsonCategory.getString("title")
            val jsonMovies = jsonCategory.getJSONArray("movie")

            val movies = mutableListOf<Movie>()
            for (j in 0 until jsonMovies.length()) {
                val jsonMovie = jsonMovies.getJSONObject(j)
                val id = jsonMovie.getInt("id")
                val coverUrl = jsonMovie.getString("cover_url")

                movies.add(Movie(id, coverUrl))
            }

            categories.add(Category(title, movies))
        }

        return categories
    }

        private fun toString (stream: InputStream): String {
            val bytes = ByteArray ( 1024)
            val baos = ByteArrayOutputStream ()
            var read: Int
            while (true) {
                read = stream.read(bytes)
                if (read <=0){
                    break
                }
                baos.write(bytes, 0, read)
            }
            return String (baos.toByteArray())
        }
}