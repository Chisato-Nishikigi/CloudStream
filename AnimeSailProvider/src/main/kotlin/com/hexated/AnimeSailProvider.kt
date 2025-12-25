package com.hexated

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addAniListId
import com.lagradost.cloudstream3.LoadResponse.Companion.addMalId
import com.lagradost.cloudstream3.mvvm.safeApiCall
import com.lagradost.cloudstream3.newEpisode
import com.lagradost.cloudstream3.newExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.nicehttp.NiceResponse
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class AnimeSailProvider : MainAPI() {

    override var mainUrl = "https://154.26.137.28"
    override var name = "AnimeSail"
    override val hasMainPage = true
    override var lang = "id"
    override val hasDownloadSupport = true

    override val supportedTypes = setOf(
        TvType.Anime,
        TvType.AnimeMovie,
        TvType.OVA
    )

    companion object {
        fun getType(t: String): TvType =
            when {
                t.contains("OVA", true) || t.contains("Special", true) -> TvType.OVA
                t.contains("Movie", true) -> TvType.AnimeMovie
                else -> TvType.Anime
            }

        fun getStatus(t: String): ShowStatus =
            when (t) {
                "Completed" -> ShowStatus.Completed
                "Ongoing" -> ShowStatus.Ongoing
                else -> ShowStatus.Completed
            }
    }

    private suspend fun request(url: String, ref: String? = null): NiceResponse =
        app.get(
            url,
            headers = mapOf(
                "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"
            ),
            cookies = mapOf("_as_ipin_ct" to "ID"),
            referer = ref
        )

    override val mainPage = mainPageOf(
        "$mainUrl/page/" to "Episode Terbaru",
        "$mainUrl/movie-terbaru/page/" to "Movie Terbaru",
        "$mainUrl/genres/donghua/page/" to "Donghua"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = request(request.data + page).document
        val home = document.select("article").map { it.toSearchResult() }
        return newHomePageResponse(request.name, home)
    }

    private fun getProperAnimeLink(uri: String): String =
        if (uri.contains("/anime/")) uri else {
            var title = uri.substringAfter("$mainUrl/")
            title = when {
                title.contains("-episode") && !title.contains("-movie") ->
                    title.substringBefore("-episode")
                title.contains("-movie") ->
                    title.substringBefore("-movie")
                else -> title
            }
            "$mainUrl/anime/$title"
        }

    private fun Element.toSearchResult(): AnimeSearchResponse {
        val href =
            getProperAnimeLink(fixUrlNull(selectFirst("a")?.attr("href")).toString())
        val title = select(".tt > h2").text().trim()
        val posterUrl = fixUrlNull(selectFirst("div.limit img")?.attr("src"))
        val epNum = Regex("Episode\\s?(\\d+)")
            .find(title)?.groupValues?.getOrNull(1)?.toIntOrNull()

        return newAnimeSearchResponse(title, href, TvType.Anime) {
            this.posterUrl = posterUrl
            addSub(epNum)
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = request("$mainUrl/?s=$query").document
        return document.select("div.listupd article").map { it.toSearchResult() }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = request(url).document

        val title = document.selectFirst("h1.entry-title")
            ?.text()
            ?.replace("Subtitle Indonesia", "")
            ?.trim()
            ?: "AnimeSail"

        val poster = document.selectFirst("div.entry-content > img")?.attr("src")
        val type = getType(
            document.select("tbody th:contains(Tipe)").next().text()
        )
        val year = document.select("tbody th:contains(Dirilis)")
            .next()
            .text()
            .trim()
            .toIntOrNull()

        val episodes = document.select("ul.daftar > li").map {
            val link = fixUrl(it.select("a").attr("href"))
            val name = it.select("a").text()
            val ep = Regex("Episode\\s?(\\d+)")
                .find(name)?.groupValues?.getOrNull(1)?.toIntOrNull()

            newEpisode {
                data = link
                episode = ep
            }
        }.reversed()

        val tracker =
            APIHolder.getTracker(listOf(title), TrackerType.getTypes(type), year, true)

        return newAnimeLoadResponse(title, url, type) {
            posterUrl = tracker?.image ?: poster
            backgroundPosterUrl = tracker?.cover
            this.year = year
            addEpisodes(DubStatus.Subbed, episodes)
            showStatus = getStatus(
                document.select("tbody th:contains(Status)").next().text().trim()
            )
            plot = document.selectFirst("div.entry-content > p")?.text()
            tags =
                document.select("tbody th:contains(Genre)").next().select("a").map { it.text() }
            addMalId(tracker?.malId)
            addAniListId(tracker?.aniId?.toIntOrNull())
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        val document = request(data).document

        document.select(".mobius > .mirror > option").map {
            safeApiCall {
                val iframe = fixUrl(
                    Jsoup.parse(
                        base64Decode(it.attr("data-em"))
                    ).select("iframe").attr("src")
                )

                val quality = getIndexQuality(it.text())

                when {
                    iframe.contains("/utils/player/arch/") ||
                            iframe.contains("/utils/player/race/") -> {
                        val src =
                            request(iframe, ref = data).document.select("source").attr("src")
                        val source =
                            if (iframe.contains("/arch/")) "Arch" else "Race"

                        callback(
                            newExtractorLink(
                                source = source,
                                name = source,
                                url = src,
                                referer = mainUrl
                            ) {
                                this.quality = quality ?: Qualities.Unknown.value
                                isM3u8 = true
                            }
                        )
                    }

                    else -> loadFixedExtractor(
                        iframe,
                        quality,
                        mainUrl,
                        subtitleCallback,
                        callback
                    )
                }
            }
        }
        return true
    }

    private suspend fun loadFixedExtractor(
        url: String,
        quality: Int?,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        loadExtractor(url, referer, subtitleCallback) { link ->
            callback(
                newExtractorLink(
                    source = link.name,
                    name = link.name,
                    url = link.url,
                    referer = link.referer
                ) {
                    this.quality =
                        if (link.type == ExtractorLinkType.M3U8)
                            link.quality
                        else
                            quality ?: Qualities.Unknown.value
                    this.type = link.type
                    this.headers = link.headers
                    this.extractorData = link.extractorData
                }
            )
        }
    }

    private fun getIndexQuality(str: String): Int =
        Regex("(\\d{3,4})[pP]").find(str)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: Qualities.Unknown.value
}
