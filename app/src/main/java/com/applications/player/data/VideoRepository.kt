package com.applications.player.data

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.applications.player.model.Video
import com.applications.player.presentation.videosbyfolders.Folder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

class VideoRepository(private val context: Context) {

    /**
     * Fetches all videos from the device.
     */
    suspend fun getVideos(): List<Video> = withContext(Dispatchers.IO) {
        getVideosByFolder(null)
    }

    /**
     * Fetches videos, optionally filtered by a specific folder path.
     * Running on Dispatchers.IO for safe disk access.
     */
    suspend fun getVideosByFolder(folderPath: String?): List<Video> = withContext(Dispatchers.IO) {
        val videos = ArrayList<Video>()
        val contentResolver: ContentResolver = context.contentResolver

        val collection: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION, // Still need this column
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        )

        val selection = if (folderPath != null) {
            "${MediaStore.Video.Media.DATA} LIKE ?"
        } else {
            null
        }

        val selectionArgs = if (folderPath != null) {
            arrayOf("$folderPath/%")
        } else {
            null
        }

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        try {
            contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val folderNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val size = cursor.getLong(sizeColumn)

                    // 1. Attempt to read duration using the corrected getLong()
                    var duration = cursor.getLong(durationColumn)

                    val contentUri: Uri = Uri.withAppendedPath(collection, id.toString())
                    val dataPath = cursor.getString(dataColumn)

                    // 2. Fallback: If duration is 0, use MediaMetadataRetriever
                    if (duration <= 0) {
                        duration = retrieveDurationFromUri(contentUri)
                    }

                    val folderName = cursor.getString(folderNameColumn)
                    val thumbnailUri: Uri = Uri.fromFile(File(dataPath))

                    val video = Video(
                        id = id,
                        name = name,
                        uri = contentUri,
                        size = size,
                        duration = duration,
                        thumbnailUri = thumbnailUri,
                        folderName = folderName,
                        folderPath = File(dataPath).parentFile?.absolutePath ?: "Unknown"
                    )

                    videos.add(video)
                    // Log the final duration for confirmation
                    // Log.e("VideoRepository", "${video.folderPath} | duration: $duration | count: ${videos.size}")
                }
            }
        } catch (e: Exception) {
            Log.e("VideoRepository", "Error fetching videos for folder: $folderPath", e)
        }

        return@withContext videos
    }

    /**
     * **[NEW]** Deletes a video from the device and the MediaStore.
     * This operation is non-blocking and runs on Dispatchers.IO.
     *
     * @param video The Video object to delete, containing the necessary URI.
     * @return true if the deletion was successful (one row deleted), false otherwise.
     */
    suspend fun   deleteVideo(video: Video): Boolean = withContext(Dispatchers.IO) {
        try {
            val rowsDeleted = context.contentResolver.delete(video.uri, null, null)

            if (rowsDeleted > 0) {
                Log.d("VideoRepository", "Successfully deleted video: ${video.name}")
                // If rowsDeleted > 0, the file is usually deleted automatically by the ContentResolver.
                true
            } else {
                Log.e("VideoRepository", "Failed to delete video: ${video.name}. Rows deleted: $rowsDeleted")
                false
            }
        } catch (e: Exception) {
            Log.e("VideoRepository", "Exception during video deletion for URI: ${video.uri}", e)
            false
        }
    }


    // --- NEW FALLBACK UTILITY FUNCTION ---

    /**
     * Attempts to retrieve the video duration manually using MediaMetadataRetriever.
     */
    private fun retrieveDurationFromUri(uri: Uri): Long {
        var duration: Long = 0
        var retriever: MediaMetadataRetriever? = null
        try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            duration = durationStr?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            Log.e("VideoRepository", "Failed to retrieve duration for URI: $uri", e)
        } finally {
            try {
                retriever?.release()
            } catch (ignored: Exception) {
                // Ignore
            }
        }
        return duration
    }


    /**
     * **[NEW]** Renames a video file on the file system and updates its
     * entry in the MediaStore.
     * This operation is non-blocking and runs on Dispatchers.IO.
     *
     * @param video The video to rename.
     * @param newName The new file name (including extension).
     * @return The updated Video object if successful, null otherwise.
     */
    suspend fun renameVideo(video: Video, newName: String): Video? = withContext(Dispatchers.IO) {
        val file = File(video.folderPath, video.name)
        val newFile = File(video.folderPath, newName)

        if (!file.exists()) {
            Log.e("VideoRepository", "Rename failed: Source file not found at ${file.absolutePath}")
            return@withContext null
        }

        if (newFile.exists()) {
            Log.e("VideoRepository", "Rename failed: A file with the new name already exists.")
            return@withContext null
        }

        // 1. Rename the file on the file system
        if (file.renameTo(newFile)) {
            try {
                // 2. Update the MediaStore
                val contentValues = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, newName)
                    put(MediaStore.Video.Media.DATA, newFile.absolutePath)
                }

                val rowsUpdated = context.contentResolver.update(
                    video.uri,
                    contentValues,
                    null,
                    null
                )

                if (rowsUpdated > 0) {
                    Log.e("VideoRepository", "Successfully renamed '${video.name}' to '$newName'")
                    // 3. Return the updated video object
                    return@withContext video.copy(name = newName, uri = video.uri)
                } else {
                    // If MediaStore update fails, try to revert the file name
                    newFile.renameTo(file)
                    Log.e("VideoRepository", "MediaStore update failed for ${video.uri}. Reverted file rename.")
                    return@withContext null
                }
            } catch (e: Exception) {
                // Catch any exceptions during MediaStore update and revert
                newFile.renameTo(file)
                Log.e("VideoRepository", "Exception during MediaStore update. Reverted file rename.", e)
                return@withContext null
            }
        } else {
            Log.e("VideoRepository", "File system rename failed for '${file.absolutePath}'")
            return@withContext null
        }
    }

    // --------------------------------------------------------------------------
    // Folder Loading Logic (No change needed here)
    // --------------------------------------------------------------------------

    /**
     * Fetches all unique video folders and counts the videos in each.
     */
    suspend fun getFolders(): List<Folder> = withContext(Dispatchers.IO) {
        val folders = mutableListOf<Folder>()
        val contentResolver: ContentResolver = context.contentResolver

        val collection: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DATA
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
        val folderPaths = mutableSetOf<String>()

        contentResolver.query(
            collection,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val folderNameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)

            while (cursor.moveToNext()) {
                val folderName = cursor.getString(folderNameColumn)
                val folderPath = File(cursor.getString(dataColumn)).parentFile?.absolutePath

                if (folderPath != null && folderPaths.add(folderPath)) {
                    val videoCount = getVideosCountForFolder(folderPath, contentResolver)
                    val firstVideoThumbnailUri =
                        getFirstVideoThumbnailForFolder(folderPath, contentResolver)

                    folders.add(
                        Folder(
                            name = folderName,
                            path = folderPath,
                            videoCount = videoCount,
                            thumbnailUri = firstVideoThumbnailUri
                        )
                    )
                }
            }
        }
        return@withContext folders
    }

    /**
     * Helper function to get the video count for a specific folder path.
     */
    private fun getVideosCountForFolder(folderPath: String, contentResolver: ContentResolver): Int {
        val countProjection = arrayOf(MediaStore.Video.Media._ID)
        val countSelection = "${MediaStore.Video.Media.DATA} LIKE ?"
        val countSelectionArgs = arrayOf("$folderPath/%")

        var count = 0
        try {
            contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                countProjection,
                countSelection,
                countSelectionArgs,
                null
            )?.use { cursor ->
                count = cursor.count
            }
        } catch (e: Exception) {
            Log.e("VideoRepository", "Error getting video count for folder: $folderPath", e)
        }
        return count
    }

    /**
     * Helper function to get the URI of the first video's path (DATA) in a given folder.
     */
    private fun getFirstVideoThumbnailForFolder(folderPath: String, contentResolver: ContentResolver): Uri? {
        val projection = arrayOf(MediaStore.Video.Media.DATA)
        val selection = "${MediaStore.Video.Media.DATA} LIKE ?"
        val selectionArgs = arrayOf("$folderPath/%")
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        var thumbnailUri: Uri? = null

        try {
            contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val dataPath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA))
                    thumbnailUri = Uri.fromFile(File(dataPath))
                }
            }
        } catch (e: Exception) {
            Log.e("VideoRepository", "Error getting first video thumbnail for folder: $folderPath", e)
        }
        return thumbnailUri
    }






    /**
     * Fetches all videos from the device and emits them in chunks
     * using a Kotlin Flow. This is ideal for large datasets to update the
     * UI progressively.
     *
     * IMPORTANT: Uses flowOn(Dispatchers.IO) to ensure the ContentResolver query
     * and I/O-intensive operations run on the IO thread, fixing the Flow invariant
     * violation error.
     *
     * @return A Flow of List<Video>, emitting every time a chunk (10 videos) is ready.
     */
    fun getAllVideosChunked(): Flow<List<Video>> = flow {
        val CHUNK_SIZE = 10
        val videos = mutableListOf<Video>()
        val contentResolver: ContentResolver = context.contentResolver

        val collection: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        try {
            // The actual database query runs here
            contentResolver.query(
                collection,
                projection,
                null, // selection
                null, // selectionArgs
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val durationColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val folderNameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val size = cursor.getLong(sizeColumn)

                    var duration = cursor.getLong(durationColumn)
                    val contentUri: Uri = Uri.withAppendedPath(collection, id.toString())
                    val dataPath = cursor.getString(dataColumn)

                    // Fallback: If duration is 0, use MediaMetadataRetriever (Assumes retrieveDurationFromUri is defined)
                    if (duration <= 0) {
                        // Note: This function also performs I/O, but it's fine within the flow builder
                        // as flowOn(Dispatchers.IO) sets the context for the entire block.
                        duration = retrieveDurationFromUri(contentUri)
                    }

                    val folderName = cursor.getString(folderNameColumn)
                    val thumbnailUri: Uri = Uri.fromFile(File(dataPath))

                    val video = Video(
                        id = id,
                        name = name,
                        uri = contentUri,
                        size = size,
                        duration = duration,
                        thumbnailUri = thumbnailUri,
                        folderName = folderName,
                        folderPath = File(dataPath).parentFile?.absolutePath ?: "Unknown"
                    )

                    videos.add(video)

                    // Emit the list whenever the CHUNK_SIZE is reached
                    if (videos.size % CHUNK_SIZE == 0) {
                        // Emit a copy of the list up to this point
                        emit(videos.toList())
                    }
                }

                // Emit any remaining videos if the total count wasn't a multiple of CHUNK_SIZE
                if (videos.isNotEmpty() && videos.size % CHUNK_SIZE != 0) {
                    emit(videos.toList())
                }
            }
        } catch (e: Exception) {
            // Log the error for debugging
            Log.e("VideoRepository", "Error fetching videos in chunks", e)
        }
    }
        // Set the execution context for the upstream operations (the flow builder) to Dispatchers.IO
        .flowOn(Dispatchers.IO)
}
