package com.example.m3uplayer.data

import android.content.Context
import android.net.Uri
import com.example.m3uplayer.data.db.AppDatabase
import com.example.m3uplayer.data.db.SavedPlaylistEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

sealed class LoadResult {
    data class Success(val playlist: M3UPlaylist) : LoadResult()
    data class Error(val message: String) : LoadResult()
}

class M3URepository(private val context: Context) {

    private val http = OkHttpClient()
    private val db = AppDatabase.get(context)

    /** باز کردن فایل m3u از content:// یا file:// (مثلاً از اپ فایل‌منیجر) */
    suspend fun loadFromLocalUri(uri: Uri): LoadResult = withContext(Dispatchers.IO) {
        try {
            val text = context.contentResolver.openInputStream(uri)
                ?.bufferedReader()?.use { it.readText() }
                ?: return@withContext LoadResult.Error("امکان خواندن فایل وجود ندارد")

            val items = M3UParser.parse(text)
            if (items.isEmpty()) {
                return@withContext LoadResult.Error("فایل انتخاب‌شده هیچ آیتم قابل‌پخشی ندارد")
            }
            val name = queryDisplayName(uri) ?: "پلی‌لیست محلی"
            db.playlistDao().upsert(
                SavedPlaylistEntity(name = name, sourceUri = uri.toString(), isRemote = false)
            )
            LoadResult.Success(M3UPlaylist(name, uri.toString(), items))
        } catch (e: Exception) {
            LoadResult.Error("خطا در خواندن فایل: ${e.message}")
        }
    }

    /** باز کردن پلی‌لیست از یک لینک اینترنتی (http/https) — مثلاً یک لیست IPTV آنلاین */
    suspend fun loadFromUrl(url: String): LoadResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext LoadResult.Error("سرور خطای ${response.code} برگرداند")
                }
                val text = response.body?.string()
                    ?: return@withContext LoadResult.Error("پاسخ سرور خالی بود")

                val items = M3UParser.parse(text)
                if (items.isEmpty()) {
                    return@withContext LoadResult.Error("این لینک هیچ آیتم قابل‌پخشی ندارد")
                }
                val name = url.substringAfterLast('/').ifBlank { "پلی‌لیست آنلاین" }
                db.playlistDao().upsert(
                    SavedPlaylistEntity(name = name, sourceUri = url, isRemote = true)
                )
                LoadResult.Success(M3UPlaylist(name, url, items))
            }
        } catch (e: IOException) {
            LoadResult.Error("اتصال به اینترنت برقرار نشد: ${e.message}")
        } catch (e: Exception) {
            LoadResult.Error("خطای نامشخص: ${e.message}")
        }
    }

    /** بازخوانی یکی از پلی‌لیست‌های ذخیره‌شده از تاریخچه */
    suspend fun reload(entity: SavedPlaylistEntity): LoadResult {
        db.playlistDao().touch(entity.id)
        return if (entity.isRemote) loadFromUrl(entity.sourceUri)
        else loadFromLocalUri(Uri.parse(entity.sourceUri))
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1) cursor.getString(nameIndex) else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
