package com.yourname.uimax

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.metaproviders.TmdbProvider
import com.lagradost.cloudstream3.metaproviders.TmdbLink
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.mvvm.safeApiCall

/**
 * UI-MAXIMIZED TEMPLATE
 * - ⭐ Rating badge → dari TMDB
 * - 🏷️ HD badge     → dari SearchResponse.quality
 * - 🎬 HBO look     → horizontal posters
 *
 * Video source masih dummy (fokus UI dulu)
 */
class UiMaxTemplate : TmdbProvider() {

    override var name = "UI-MAX Template"
    override var lang = "en"

    // === UI CAPABILITIES ===
    override val hasMainPage = true
    override val hasQuickSearch = true
    override val useMetaLoadResponse = true
    override val instantLinkLoading = true

    // === CONTENT TYPES ===
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries
    )

    /**
     * =========================
     * HOME PAGE (HBO STYLE)
     * =========================
     */
    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {

        // Ambil data populer dari TMDB (ditangani oleh TmdbProvider)
        val movies = getMoviePopular(page)
        val series = getTvPopular(page)

        return newHomePageResponse(
            listOf(
                HomePageList(
                    name = "Popular Movies",
                    list = movies.map { it.withHdBadge() },
                    isHorizontalImages = true // ← HBO / Netflix look
                ),
                HomePageList(
                    name = "Popular Series",
                    list = series.map { it.withHdBadge() },
                    isHorizontalImages = true
                )
            ),
            hasNext = true
        )
    }

    /**
     * =========================
     * SEARCH (HD BADGE AKTIF)
     * =========================
     */
    override suspend fun search(query: String): List<SearchResponse> {
        val results = super.search(query)

        // Pastikan HD badge muncul
        return results.map { it.withHdBadge() }
    }

    /**
     * =========================
     * LOAD DETAIL
     * =========================
     * Rating ⭐ otomatis dari TMDB
     * Background cinematic otomatis
     */
    override suspend fun load(url: String): LoadResponse? {
        return super.load(url)
        // Tidak perlu override apa-apa
        // TMDB → score → rating badge UI
    }

    /**
     * =========================
     * LOAD LINKS (DUMMY)
     * =========================
     * Fokus UI dulu
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
     * HELPER — HD BADGE TRIGGER
     * =========================
     */
    private fun SearchResponse.withHdBadge(): SearchResponse {
        this.quality = SearchQuality.HD // ← MEMICU BADGE “HD”
        return this
    }
}
