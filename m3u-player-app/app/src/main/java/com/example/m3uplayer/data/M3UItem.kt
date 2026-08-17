package com.example.m3uplayer.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * یک آیتم پخش داخل پلی‌لیست m3u
 * می‌تواند یک فایل محلی، یک لینک صوتی، یا یک استریم زنده HLS/IPTV باشد.
 */
@Parcelize
data class M3UItem(
    val title: String,
    val uri: String,
    val durationMs: Long = -1,       // -1 یعنی نامشخص (معمولاً استریم زنده)
    val groupTitle: String? = null,  // مثلاً دسته‌بندی کانال در IPTV (#EXTGRP یا group-title=)
    val logoUrl: String? = null,     // آیکون/لوگوی کانال (tvg-logo=)
    val isLive: Boolean = false
) : Parcelable

data class M3UPlaylist(
    val name: String,
    val sourceUri: String,
    val items: List<M3UItem>
)
