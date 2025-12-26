package com.yourname.uimax

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.metaproviders.TmdbProvider
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.mvvm.safeApiCall

class UiMaxTemplate : TmdbProvider() {

    override var name = "UI-MAX Template"
    override var lang = "en"

    override val hasMainPage = true
    override val hasQuickSearch = true
    override val useMetaLoadResponse = true
    override val instantLinkLoading = true

    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries
    )

    /**
     * =========================
     * TMDB HOME SECTIONS (RESMI)
     * =========================
     */
    override val mainPage = mainPageOf(
        "movie/popular" to "Popular Movies",
        "tv/popular" to "Popular Series"
    )

    /**
     * =========================
     * HOME PAGE RENDER
     * =========================
     */
    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {

        val items = getMainPageItems(
            request,
            page
        ).mapNotNull { it?.withHdBadge() }

        return newHomePageResponse(
            listOf(
                HomePageList(
                    request.name,
                    items,
                    isHorizontalImages = true // HBO / Netflix style
                )
            ),
            hasNext = true
        )
    }

    /**
     * =========================
     * SEARCH (HD BADGE)
     * =========================
     */
    override suspend fun search(query: String): List<SearchResponse> {
        return super.search(query).mapNotNull { it.withHdBadge() }
    }

    /**
     * =========================
     * LOAD DETAIL
     * =========================
     * ⭐ rating otomatis dari TMDB
     */
    override suspend fun load(url: String): LoadResponse? {
        return super.load(url)
    }

    /**
     * =========================
     * LOAD LINKS (DUMMY)
     * =========================
     */
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean = safeApiCall {
        callback(
            newExtractorLink(
                source = name,
                name = "Dummy 1080p",
                url = "https://example.com/dummy.m3u8",
                type = ExtractorLinkType.M3U8
            ) {
                this.quality = Qualities.P1080.value
            }
        )
        true
    }

    /**
     * =========================
     * HD BADGE TRIGGER
     * =========================
     */
    private fun SearchResponse.withHdBadge(): SearchResponse {
        this.quality = SearchQuality.HD
        return this
    }
}
