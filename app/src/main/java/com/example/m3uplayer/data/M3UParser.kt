package com.example.m3uplayer.data

/**
 * پارسر فایل‌های m3u و m3u8
 * پشتیبانی از:
 *  - m3u ساده (فقط لیست مسیر/لینک فایل‌ها)
 *  - m3u توسعه‌یافته (Extended M3U) با تگ #EXTINF و متادیتای IPTV مثل:
 *      #EXTINF:-1 tvg-logo="..." group-title="...",نام کانال
 */
object M3UParser {

    private val extInfRegex = Regex("""#EXTINF:\s*(-?\d+(?:\.\d+)?)\s*(.*)""")
    private val attrRegex = Regex("""([a-zA-Z0-9\-]+)="([^"]*)"""")

    fun parse(content: String): List<M3UItem> {
        val items = mutableListOf<M3UItem>()
        var pendingTitle: String? = null
        var pendingDuration: Long = -1
        var pendingGroup: String? = null
        var pendingLogo: String? = null

        content.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEach

            when {
                line.startsWith("#EXTM3U") -> {
                    // هدر فایل، نیازی به پردازش خاص نیست
                }

                line.startsWith("#EXTINF") -> {
                    val match = extInfRegex.find(line)
                    if (match != null) {
                        val durationSec = match.groupValues[1].toDoubleOrNull() ?: -1.0
                        val rest = match.groupValues[2]

                        // جدا کردن ویژگی‌ها (tvg-logo, group-title و ...) از عنوان بعد از کاما
                        val commaIndex = rest.lastIndexOf(',')
                        val attrsPart = if (commaIndex != -1) rest.substring(0, commaIndex) else rest
                        val titlePart = if (commaIndex != -1) rest.substring(commaIndex + 1).trim() else "بدون عنوان"

                        pendingGroup = attrRegex.findAll(attrsPart)
                            .firstOrNull { it.groupValues[1].equals("group-title", true) }
                            ?.groupValues?.get(2)
                        pendingLogo = attrRegex.findAll(attrsPart)
                            .firstOrNull { it.groupValues[1].equals("tvg-logo", true) }
                            ?.groupValues?.get(2)

                        pendingDuration = if (durationSec > 0) (durationSec * 1000).toLong() else -1
                        pendingTitle = titlePart.ifBlank { "بدون عنوان" }
                    }
                }

                line.startsWith("#") -> {
                    // سایر تگ‌ها (#EXTVLCOPT، #EXTGRP، کامنت‌ها و...) نادیده گرفته می‌شوند
                }

                else -> {
                    // این خط، خودِ مسیر/لینک رسانه است
                    val isLive = pendingDuration <= 0 &&
                        (line.contains(".m3u8") || line.startsWith("http"))

                    items.add(
                        M3UItem(
                            title = pendingTitle ?: guessTitleFromUri(line),
                            uri = line,
                            durationMs = pendingDuration,
                            groupTitle = pendingGroup,
                            logoUrl = pendingLogo,
                            isLive = isLive
                        )
                    )
                    pendingTitle = null
                    pendingDuration = -1
                    pendingGroup = null
                    pendingLogo = null
                }
            }
        }
        return items
    }

    private fun guessTitleFromUri(uri: String): String {
        return uri.substringAfterLast('/').substringBefore('?').ifBlank { uri }
    }
}
