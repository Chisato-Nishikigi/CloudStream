package com.yourname.xprime

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import android.util.Log

class XPrimeProvider : MainAPI() {

    override var name = "XPrime"
    override var mainUrl = "https://v8.kuramanime.tel"
    override var lang = "id"

    override val supportedTypes = setOf(TvType.TvSeries)
    override val hasMainPage = true

    // =========================
    // LOAD LINKS (PASTE DI SINI)
    // =========================
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        val doc = app.get(data).document

        val sources = doc.select("video#player source")
        if (sources.isEmpty()) return false

        for (source in sources) {
            val url = source.attr("src")
            if (url.isNullOrEmpty()) continue

            val quality = source.attr("size").toIntOrNull()
                ?: Qualities.Unknown.value

            Log.d("XPrime", "Found video: $url ($quality p)")

            callback(
                newExtractorLink(
                    source = name,
                    name = "Kuramanime ${quality}p",
                    url = url,
                    type = ExtractorLinkType.VIDEO
                ) {
                    this.quality = quality
                    this.referer = data
                    this.headers = mapOf(
                        "Referer" to data,
                        "Origin" to "https://v8.kuramanime.tel"
                    )
                }
            )
        }

        return true
    }
}
