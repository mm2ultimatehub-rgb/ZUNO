/*
 * ZUNO (2026)
 * Based on ArchiveTune — GPL-3.0 License.
 * Preserve upstream notices and project license.
 */

package moe.rukamori.archivetune.branding

import android.graphics.Bitmap
import android.graphics.Canvas
import com.caverock.androidsvg.SVG
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ZunoStoreIcon(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val username: String,
    val assetUrl: String,
    val mimeType: String,
    val fileSize: Long,
)

object ZunoIconStoreRepository {
    private const val CatalogUrl =
        "https://jlyinisqhkupddyfcdut.supabase.co/rest/v1/zuno_icon_catalog" +
            "?select=id,title,description,category,username,asset_url,created_at,mime_type,file_size" +
            "&order=created_at.desc&limit=40"
    // Public Supabase publishable key. Never put a service-role key in the app.
    private const val PublishableKey = "sb_publishable_sjwsqCho2mlpdzBUFQQ_dQ_2uBeSDwl"
    private const val StorageHost = "jlyinisqhkupddyfcdut.supabase.co"
    private const val SupabaseFunctionsUrl = "https://jlyinisqhkupddyfcdut.supabase.co/functions/v1"
    private const val MaxImageBytes = 15 * 1024 * 1024

    suspend fun fetchCatalog(): List<ZunoStoreIcon> =
        withContext(Dispatchers.IO) {
            val connection = (URL(CatalogUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("apikey", PublishableKey)
                setRequestProperty("Accept", "application/json")
            }
            try {
                check(connection.responseCode in 200..299) {
                    "ZUNO Icon Store yanıt vermedi (HTTP ${connection.responseCode})."
                }
                val rows = JSONArray(connection.inputStream.bufferedReader().use { it.readText() })
                buildList {
                    for (index in 0 until rows.length()) {
                        val row = rows.getJSONObject(index)
                        val assetUrl = row.optString("asset_url")
                        val mimeType = row.optString("mime_type").lowercase()
                        val fileSize = row.optLong("file_size", 0L)
                        if (!isTrustedAssetUrl(assetUrl)) continue
                        if (mimeType !in setOf("image/png", "image/jpeg", "image/webp", "image/svg+xml")) continue
                        if (fileSize <= 0L || fileSize > MaxImageBytes) continue
                        add(
                            ZunoStoreIcon(
                                id = row.getString("id"),
                                title = row.optString("title", "ZUNO icon"),
                                description = row.optString("description"),
                                category = row.optString("category"),
                                username = row.optString("username"),
                                assetUrl = assetUrl,
                                mimeType = mimeType,
                                fileSize = fileSize,
                            ),
                        )
                    }
                }
            } finally {
                connection.disconnect()
            }
        }

    suspend fun downloadImage(icon: ZunoStoreIcon): ByteArray =
        withContext(Dispatchers.IO) {
            require(isTrustedAssetUrl(icon.assetUrl)) { "Güvenilmeyen ikon adresi." }
            val connection = (URL(icon.assetUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 20_000
            }
            try {
                check(connection.responseCode in 200..299) { "İkon indirilemedi." }
                val mimeType = connection.contentType.orEmpty().substringBefore(';').trim().lowercase()
                require(mimeType in setOf("image/png", "image/jpeg", "image/webp", "image/svg+xml")) {
                    "Bu ikon biçimi desteklenmiyor. PNG, JPEG, WebP veya SVG kullan."
                }
                val declaredLength = connection.contentLengthLong
                require(declaredLength <= MaxImageBytes || declaredLength < 0) {
                    "İkon 15 MB sınırını aşıyor."
                }
                val output = java.io.ByteArrayOutputStream()
                connection.inputStream.use { input ->
                    val buffer = ByteArray(8 * 1024)
                    var total = 0
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        require(total <= MaxImageBytes) { "İkon 15 MB sınırını aşıyor." }
                        output.write(buffer, 0, read)
                    }
                }
                val downloaded = output.toByteArray()
                val imageBytes =
                    if (mimeType == "image/svg+xml") {
                        val picture = SVG.getFromInputStream(ByteArrayInputStream(downloaded))
                            .renderToPicture(1024, 1024)
                        val bitmap = Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888)
                        try {
                            Canvas(bitmap).drawPicture(picture)
                            ByteArrayOutputStream().use { png ->
                                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, png)) {
                                    "SVG görseli PNG biçimine dönüştürülemedi."
                                }
                                png.toByteArray()
                            }
                        } finally {
                            bitmap.recycle()
                        }
                    } else {
                        downloaded
                    }
                runCatching { recordDownload(icon.id) }
                imageBytes
            } finally {
                connection.disconnect()
            }
        }

    private fun recordDownload(iconId: String) {
        val connection =
            (URL("$SupabaseFunctionsUrl/zuno-record-download").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 5_000
                readTimeout = 5_000
                doOutput = true
                setRequestProperty("apikey", PublishableKey)
                setRequestProperty("Authorization", "Bearer $PublishableKey")
                setRequestProperty("Content-Type", "application/json")
            }
        try {
            connection.outputStream.use { output ->
                output.write(JSONObject().put("icon_id", iconId).toString().toByteArray(Charsets.UTF_8))
            }
            connection.responseCode
        } finally {
            connection.disconnect()
        }
    }

    private fun isTrustedAssetUrl(value: String): Boolean =
        runCatching {
            val url = URL(value)
            url.protocol == "https" && url.host.equals(StorageHost, ignoreCase = true)
        }.getOrDefault(false)
}
