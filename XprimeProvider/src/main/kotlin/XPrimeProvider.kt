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
    // MAIN PAGE (DUMMY UNTUK TEST)
    // =========================
    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {

        val items = listOf(
            newTvSeriesSearchResponse(
                name = "Cengjing You Yongshi",
                url = "https://v8.kuramanime.tel/anime/4290/cengjing-you-yongshi"
            )
        )

        return newHomePageResponse(
            listOf(HomePageList("Test Anime", items)),
            hasNext = false
        )
    }

    // =========================
    // SEARCH (BIAR TIDAK ERROR)
    // =========================
    override suspend fun search(query: String): List<SearchResponse> {
        return emptyList()
    }

    // =========================
    // LOAD (DETAIL + EPISODE LIST)
    // =========================
    override suspend fun load(url: String): LoadResponse? {
    val doc = app.get(url).document

    val title = doc.selectFirst("h1")?.text()
        ?: doc.selectFirst("title")?.text()
        ?: return null

   val episodes = doc.select("div.episode__list a")
    .mapIndexedNotNull { index, el ->
        val href = el.attr("href")
        if (href.contains("/episode/")) {
            newEpisode(fixUrl(href)) {
                this.name = el.text().ifBlank { "Episode ${index + 1}" }
                this.episode = index + 1
            }
        } else null
    }

    Log.d("XPrime", "Episodes found: ${episodes.size}")

    return newTvSeriesLoadResponse(
        title,
        url,
        TvType.TvSeries,
        episodes
    )
}

    // =========================
    // LOAD LINKS (VIDEO PLAYER)
    // =========================
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        val doc = app.get(data).document
        val sources = doc.select("video#player source")

        if (sources.isEmpty()) {
            Log.d("XPrime", "No video sources found")
            return false
        }

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
