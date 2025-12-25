package com.hexated

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.Jsoup

class XprimeProvider : MainAPI() {

    override var mainUrl = "https://db.xprime.stream"
    override var name = "Xprime"
    override var lang = "en"
    override var hasMainPage = true

    override val supportedTypes = setOf(TvType.Movie)

    override val mainPage = mainPageOf(
        "$mainUrl/latest-releases" to "Latest Releases",
        "$mainUrl/netflix-movies" to "Netflix Movies",
        "$mainUrl/4k-releases" to "4K Movies"
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {

        val document = app.get(request.data).document

        val items = document.select("a[href*='/movie/']").mapNotNull { el ->
            val url = el.attr("href")
            val title = el.selectFirst("h3, h2")?.text() ?: return@mapNotNull null
            val poster = el.selectFirst("img")?.attr("src")

            newMovieSearchResponse(
                name = title,
                url = fixUrl(url),
                type = TvType.Movie
            ) {
                this.posterUrl = poster
            }
        }

        return HomePageResponse(
            listOf(HomePageList(request.name, items)),
            hasNext = false
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/search?q=$query").document

        return document.select("a[href*='/movie/']").mapNotNull { el ->
            val url = el.attr("href")
            val title = el.selectFirst("h3, h2")?.text() ?: return@mapNotNull null
            val poster = el.selectFirst("img")?.attr("src")

            newMovieSearchResponse(
                name = title,
                url = fixUrl(url),
                type = TvType.Movie
            ) {
                this.posterUrl = poster
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document

        val title = document.selectFirst("h1")?.text() ?: "Unknown"
        val poster = document.selectFirst("img")?.attr("src")
        val plot = document.selectFirst("p, div.description")?.text()

        return newMovieLoadResponse(
            name = title,
            url = url,
            type = TvType.Movie
        ) {
            this.posterUrl = poster
            this.plot = plot
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        val document = app.get(data).document

        val iframe = document.selectFirst("iframe")?.attr("src")
            ?: return false

        loadExtractor(
            fixUrl(iframe),
            referer = mainUrl,
            subtitleCallback,
            callback
        )

        return true
    }
}
