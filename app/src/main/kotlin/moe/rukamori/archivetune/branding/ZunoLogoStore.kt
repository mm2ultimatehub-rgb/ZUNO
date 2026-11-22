/*
 * ZUNO (2026)
 * Based on ArchiveTune — GPL-3.0 License.
 * Preserve upstream notices and project license.
 */

package moe.rukamori.archivetune.branding

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class ZunoLogoState(
    val selectedId: String = ZunoLogoCatalog.DefaultId,
    val customFileNames: List<String> = emptyList(),
    val isInitialized: Boolean = false,
)

data class ZunoLogoPreset(val id: String, val title: String)

object ZunoLogoCatalog {
    const val DefaultId = "neon"

    val presets = listOf(
        ZunoLogoPreset("anime", "Anime"),
        ZunoLogoPreset("ornate", "Ornate"),
        ZunoLogoPreset("butterfly", "Butterfly"),
        ZunoLogoPreset("slime", "Slime"),
        ZunoLogoPreset("bloom", "Bloom"),
        ZunoLogoPreset("neon", "ZUNO"),
        ZunoLogoPreset("ink", "Ink"),
        ZunoLogoPreset("pixel", "Pixel"),
    )
}

/**
 * Keeps the selected in-app brand mark and imported logo files in private app storage.
 * These are UI logos; Android launcher aliases remain managed by the existing icon pack.
 */
object ZunoLogoStore {
    private const val PreferencesName = "zuno_logo_preferences"
    private const val SelectedIdKey = "selected_logo_id"
    private const val CustomFilesKey = "custom_logo_files"
    private const val PresetAssetDirectory = "zuno_logos"
    private const val CustomPrefix = "custom:"
    private const val MaxImportBytes = 15L * 1024L * 1024L
    private const val MaxImageEdge = 1024

    private val mutableState = MutableStateFlow(ZunoLogoState())
    val state: StateFlow<ZunoLogoState> = mutableState.asStateFlow()

    @Volatile
    private var initialized = false

    @Synchronized
    fun initialize(context: Context) {
        if (initialized) return
        val appContext = context.applicationContext
        ensurePresetFiles(appContext)
        val preferences = appContext.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        val files = customDirectory(appContext)
        val customFiles = preferences.getStringSet(CustomFilesKey, emptySet())
            .orEmpty()
            .filter { name -> File(files, name).isFile && name.matches(CustomFilePattern) }
            .sortedDescending()
        val savedId = preferences.getString(SelectedIdKey, ZunoLogoCatalog.DefaultId)
            ?: ZunoLogoCatalog.DefaultId
        val selectedId = when {
            savedId in ZunoLogoCatalog.presets.map(ZunoLogoPreset::id) -> savedId
            savedId.startsWith(CustomPrefix) && savedId.removePrefix(CustomPrefix) in customFiles -> savedId
            else -> ZunoLogoCatalog.DefaultId
        }
        mutableState.value = ZunoLogoState(selectedId, customFiles, isInitialized = true)
        if (selectedId != savedId || customFiles.size != preferences.getStringSet(CustomFilesKey, emptySet()).orEmpty().size) {
            preferences.edit()
                .putString(SelectedIdKey, selectedId)
                .putStringSet(CustomFilesKey, customFiles.toSet())
                .apply()
        }
        initialized = true
    }

    fun selectPreset(context: Context, id: String) {
        require(ZunoLogoCatalog.presets.any { it.id == id }) { "Unknown ZUNO logo preset." }
        persist(context, ZunoLogoState(id, mutableState.value.customFileNames))
    }

    fun selectCustom(context: Context, fileName: String) {
        require(fileName.matches(CustomFilePattern)) { "Invalid custom logo name." }
        require(File(customDirectory(context.applicationContext), fileName).isFile) { "Custom logo not found." }
        persist(context, ZunoLogoState("$CustomPrefix$fileName", mutableState.value.customFileNames))
    }

    fun importCustom(context: Context, uri: Uri): String {
        val appContext = context.applicationContext
        val source = File(customDirectory(appContext).apply { mkdirs() }, "import_${UUID.randomUUID()}.tmp")
        try {
            val resolver = appContext.contentResolver
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(source).use { output ->
                    copyLimited(input, output)
                }
            } ?: error("Unable to open the selected image.")
            return saveImportedImage(appContext, source)
        } finally {
            source.delete()
        }
    }

    fun importCustomFromBytes(context: Context, bytes: ByteArray): String {
        require(bytes.isNotEmpty() && bytes.size.toLong() <= MaxImportBytes) {
            "Image must be between 1 byte and 15 MB."
        }
        val appContext = context.applicationContext
        val directory = customDirectory(appContext).apply { mkdirs() }
        val source = File(directory, "store_${UUID.randomUUID()}.tmp")
        try {
            FileOutputStream(source).use { output -> output.write(bytes) }
            return saveImportedImage(appContext, source)
        } finally {
            source.delete()
        }
    }

    private fun copyLimited(input: java.io.InputStream, output: FileOutputStream) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            require(total <= MaxImportBytes) { "Image is larger than 15 MB." }
            output.write(buffer, 0, read)
        }
    }

    private fun saveImportedImage(context: Context, source: File): String {
        val directory = customDirectory(context).apply { mkdirs() }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.absolutePath, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) {
            "The selected file is not a supported image."
        }

        var sampleSize = 1
        while (bounds.outWidth / sampleSize > MaxImageEdge * 2 ||
            bounds.outHeight / sampleSize > MaxImageEdge * 2
        ) {
            sampleSize *= 2
        }
        val bitmap = BitmapFactory.decodeFile(
            source.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sampleSize },
        ) ?: error("Unable to decode the selected image.")

        val scaled = if (bitmap.width > MaxImageEdge || bitmap.height > MaxImageEdge) {
            val ratio = minOf(
                MaxImageEdge.toFloat() / bitmap.width,
                MaxImageEdge.toFloat() / bitmap.height,
            )
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * ratio).toInt().coerceAtLeast(1),
                (bitmap.height * ratio).toInt().coerceAtLeast(1),
                true,
            ).also { if (it !== bitmap) bitmap.recycle() }
        } else {
            bitmap
        }

        val fileName = "custom_${UUID.randomUUID()}.webp"
        val destination = File(directory, fileName)
        try {
            FileOutputStream(destination).use { output ->
                check(scaled.compress(Bitmap.CompressFormat.WEBP, 90, output)) {
                    "Unable to save the selected logo."
                }
            }
        } catch (error: Exception) {
            destination.delete()
            throw error
        } finally {
            scaled.recycle()
        }

        val nextFiles = (mutableState.value.customFileNames + fileName).distinct()
        persist(context, ZunoLogoState("custom:$fileName", nextFiles))
        return fileName
    }

    fun deleteCustom(context: Context, fileName: String) {
        require(fileName.matches(CustomFilePattern)) { "Invalid custom logo name." }
        val appContext = context.applicationContext
        File(customDirectory(appContext), fileName).delete()
        val nextFiles = mutableState.value.customFileNames - fileName
        val nextSelected = if (mutableState.value.selectedId == "$CustomPrefix$fileName") {
            ZunoLogoCatalog.DefaultId
        } else {
            mutableState.value.selectedId
        }
        persist(appContext, ZunoLogoState(nextSelected, nextFiles))
    }

    fun customFile(context: Context, fileName: String): File? {
        if (!fileName.matches(CustomFilePattern)) return null
        return File(customDirectory(context.applicationContext), fileName).takeIf { it.isFile }
    }

    fun logoFile(context: Context, id: String): File? {
        val appContext = context.applicationContext
        return if (id.startsWith(CustomPrefix)) {
            customFile(appContext, id.removePrefix(CustomPrefix))
        } else {
            if (ZunoLogoCatalog.presets.none { it.id == id }) return null
            File(customDirectory(appContext), "preset_$id.webp").takeIf { it.isFile }
        }
    }

    private fun ensurePresetFiles(context: Context) {
        val directory = customDirectory(context).apply { mkdirs() }
        ZunoLogoCatalog.presets.forEach { preset ->
            val target = File(directory, "preset_${preset.id}.webp")
            // Always refresh bundled presets from the APK. Older builds copied these files
            // into private storage once, so an existence-only check kept stale logos forever.
            val encoded = context.assets.open("$PresetAssetDirectory/${preset.id}.webp.b64")
                .bufferedReader()
                .use { it.readText() }
            target.writeBytes(Base64.decode(encoded, Base64.DEFAULT))
        }
    }

    private fun persist(context: Context, next: ZunoLogoState) {
        context.applicationContext
            .getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
            .edit()
            .putString(SelectedIdKey, next.selectedId)
            .putStringSet(CustomFilesKey, next.customFileNames.toSet())
            .apply()
        mutableState.value = next.copy(isInitialized = true)
    }

    private fun customDirectory(context: Context): File = File(context.filesDir, "zuno-logos")

    private val CustomFilePattern = Regex("custom_[A-Za-z0-9-]+\\.webp")
}
