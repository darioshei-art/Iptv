package com.example.m3uplayer.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** پلی‌لیست ذخیره‌شده (فایل محلی یا لینک اینترنتی) برای دسترسی سریع بعدی */
@Entity(tableName = "saved_playlists")
data class SavedPlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sourceUri: String,     // مسیر فایل محلی یا URL
    val isRemote: Boolean,
    val addedAt: Long = System.currentTimeMillis(),
    val lastOpenedAt: Long = System.currentTimeMillis()
)

/** آیتم‌های موردعلاقه، مستقل از اینکه از کدام پلی‌لیست آمده‌اند */
@Entity(tableName = "favorite_items")
data class FavoriteItemEntity(
    @PrimaryKey val uri: String,
    val title: String,
    val logoUrl: String? = null,
    val groupTitle: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)

/** تاریخچه‌ی پخش برای ادامه از همان‌جایی که مانده بود */
@Entity(tableName = "playback_history")
data class PlaybackHistoryEntity(
    @PrimaryKey val uri: String,
    val title: String,
    val positionMs: Long,
    val playedAt: Long = System.currentTimeMillis()
)
