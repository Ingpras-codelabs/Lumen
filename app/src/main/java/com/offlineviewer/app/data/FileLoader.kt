package com.offlineviewer.app.data

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.offlineviewer.app.render.LoadedFile
import java.nio.charset.Charset

/**
 * Reads a content [Uri] into an in-memory [LoadedFile] via [ContentResolver],
 * enforcing a size guard so an oversized file yields an error instead of an OOM
 * crash.
 *
 * The Android-dependent plumbing (opening streams, querying the provider) lives
 * in [load]; all decision logic (MIME/extension resolution, the size guard,
 * text-vs-binary choice, charset defaulting) is factored into pure functions in
 * [FileLoaderLogic] so it can be unit-tested without a real ContentResolver.
 */
class FileLoader(
    private val resolver: ContentResolver,
    private val maxSizeBytes: Long = DEFAULT_MAX_SIZE_BYTES,
) {

    /** Outcome of a load attempt. */
    sealed interface Result {
        data class Success(val file: LoadedFile) : Result
        data class Failure(val message: String) : Result
    }

    fun load(uri: Uri): Result {
        return try {
            val displayName = queryDisplayName(uri) ?: FileLoaderLogic.fallbackDisplayName(uri.toString())
            val extension = FileLoaderLogic.extensionOf(displayName)
            val providerMime = resolver.getType(uri)
            val mimeType = FileLoaderLogic.resolveMimeType(
                providerMime = providerMime,
                extension = extension,
                mimeFromExtension = { ext -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) },
            )

            val declaredSize = querySize(uri)
            if (declaredSize != null) {
                FileLoaderLogic.sizeGuardMessage(declaredSize, maxSizeBytes)?.let {
                    return Result.Failure(it)
                }
            }

            val bytes = resolver.openInputStream(uri)?.use { input ->
                FileLoaderLogic.readWithLimit(input.readBytes(), maxSizeBytes)
            } ?: return Result.Failure("Unable to open the selected file.")

            FileLoaderLogic.sizeGuardMessage(bytes.size.toLong(), maxSizeBytes)?.let {
                return Result.Failure(it)
            }

            val asText = FileLoaderLogic.shouldDecodeAsText(mimeType, extension)
            val loaded = if (asText) {
                LoadedFile(
                    displayName = displayName,
                    mimeType = mimeType,
                    extension = extension,
                    text = bytes.toString(DEFAULT_CHARSET),
                    bytes = null,
                    sizeBytes = bytes.size.toLong(),
                )
            } else {
                LoadedFile(
                    displayName = displayName,
                    mimeType = mimeType,
                    extension = extension,
                    text = null,
                    bytes = bytes,
                    sizeBytes = bytes.size.toLong(),
                )
            }
            Result.Success(loaded)
        } catch (t: OutOfMemoryError) {
            Result.Failure("The file is too large to open on this device.")
        } catch (t: Throwable) {
            Result.Failure("Could not read the file: ${t.message ?: t.javaClass.simpleName}")
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
            }
        }.getOrNull()
    }

    private fun querySize(uri: Uri): Long? {
        return runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (idx >= 0 && cursor.moveToFirst() && !cursor.isNull(idx)) cursor.getLong(idx) else null
            }
        }.getOrNull()
    }

    companion object {
        /** Default in-memory guard: 32 MB (within the 25-50 MB target band). */
        const val DEFAULT_MAX_SIZE_BYTES: Long = 32L * 1024 * 1024

        val DEFAULT_CHARSET: Charset = Charsets.UTF_8
    }
}
