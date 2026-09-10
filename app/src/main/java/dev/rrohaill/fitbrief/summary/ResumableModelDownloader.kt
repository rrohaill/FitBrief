package dev.rrohaill.fitbrief.summary

import android.content.Context
import dev.rrohaill.fitbrief.BuildConfig
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class ResumableModelDownloader(private val context: Context) {
    private val modelFile = File(context.filesDir, "models/gemma-3-1b-4bit.litertlm")
    private val partialFile = File("${modelFile.absolutePath}.part")

    fun configured(): Boolean = BuildConfig.LITERT_MODEL_URL.isNotBlank()

    fun ensureDownloaded(onProgress: (Float) -> Unit): File {
        if (modelFile.isFile && modelFile.length() > 0) return modelFile
        check(configured()) { "LiteRT-LM model URL is not configured." }
        modelFile.parentFile?.mkdirs()

        val offset = if (partialFile.exists()) partialFile.length() else 0L
        val connection = (URL(BuildConfig.LITERT_MODEL_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 60_000
            if (offset > 0) setRequestProperty("Range", "bytes=$offset-")
        }
        connection.connect()
        check(connection.responseCode in 200..299 || (offset > 0 && connection.responseCode == 206)) {
            "Model download failed with HTTP ${connection.responseCode}."
        }
        val total = (connection.contentLengthLong.takeIf { it > 0 } ?: 0L) + offset
        connection.inputStream.use { input ->
            FileOutputStream(partialFile, offset > 0 && connection.responseCode == 206).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var downloaded = if (offset > 0 && connection.responseCode == 206) offset else 0L
                var read: Int
                while (input.read(buffer).also { read = it } >= 0) {
                    if (read == 0) continue
                    output.write(buffer, 0, read)
                    downloaded += read
                    if (total > 0) onProgress((downloaded.toFloat() / total).coerceIn(0f, 1f))
                }
            }
        }
        connection.disconnect()
        check(partialFile.length() > 0) { "Model download returned an empty file." }
        check(partialFile.renameTo(modelFile)) { "Unable to finalize the downloaded model." }
        return modelFile
    }
}
