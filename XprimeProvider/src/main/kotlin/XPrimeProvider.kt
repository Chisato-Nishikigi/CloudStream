package com.hexated

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.json.JSONObject

class XprimeProvider : MainAPI() {

    override var name = "Xprime"
    override var mainUrl = "https://xprime.today"
    override var lang = "id"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override var hasMainPage = true

    private val tmdbKey = "84259f99204eeb7d45c7e3d8e36c6123"
    private val tmdbImg = "https://image.tmdb.org/t/p/w500"

    // ================= HOME PAGE =================

    override val mainPage = mainPageOf(
        // Xprime native
        "https://db.xprime.stream/latest-releases" to "🔥 Latest Releases",
        "https://db.xprime.stream/netflix-movies" to "🎬 Netflix Collection",
        "https://db.xprime.stream/4k-releases" to "📺 4K Movies",

        // Netflix / Amazon / Disney style
        "tmdb://trending" to "⭐ Trending Now",
        "tmdb://new" to "🆕 New Movies",
        "tmdb://top" to "👑 Top Rated",
        "tmdb://action" to "💥 Action Movies",
        "tmdb://drama" to "🎭 Drama Movies",
        "tmdb://family" to "👨‍👩‍👧‍👦 Family & Animation",
        "tmdb://fantasy" to "🧙 Fantasy & Adventure"
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {

        val items: List<SearchResponse> = when {
            // Xprime list (ID → TMDB)
            request.data.startsWith("https://db.xprime.stream") -> {
                val json = JSONObject(app.get(request.data).text)
                val ids = json.getJSONArray("movies")

                val list = mutableListOf<SearchResponse>()
                for (i in 0 until ids.length()) {
                    buildFromTmdb(ids.getInt(i))?.let { list.add(it) }
                }
                list
            }

            // TMDB router
            request.data.startsWith("tmdb://") -> {
                val url = when (request.data) {
                    "tmdb://trending" ->
                        "https://api.themoviedb.org/3/trending/movie/week?api_key=$tmdbKey&language=id-ID"

                    "tmdb://new" ->
                        "https://api.themoviedb.org/3/movie/now_playing?api_key=$tmdbKey&language=id-ID"

                    "tmdb://top" ->
                        "https://api.themoviedb.org/3/movie/top_rated?api_key=$tmdbKey&language=id-ID"

                    "tmdb://action" ->
                        "https://api.themoviedb.org/3/discover/movie?api_key=$tmdbKey&with_genres=28&language=id-ID"

                    "tmdb://drama" ->
                        "https://api.themoviedb.org/3/discover/movie?api_key=$tmdbKey&with_genres=18&language=id-ID"

                    "tmdb://family" ->
                        "https://api.themoviedb.org/3/discover/movie?api_key=$tmdbKey&with_genres=16,10751&language=id-ID"

                    "tmdb://fantasy" ->
                        "https://api.themoviedb.org/3/discover/movie?api_key=$tmdbKey&with_genres=14,12&language=id-ID"

                    else -> return newHomePageResponse(request.name, emptyList())
                }

                val json = JSONObject(app.get(url).text)
                val results = json.getJSONArray("results")

                val list = mutableListOf<SearchResponse>()
                for (i in 0 until results.length()) {
                    buildFromTmdbJson(results.getJSONObject(i))?.let { list.add(it) }
                }
                list
            }

            else -> emptyList()
        }

        return newHomePageResponse(request.name, items)
    }

    // ================= DETAIL PAGE =================

    override suspend fun load(url: String): LoadResponse {
        val id = url.substringAfterLast("/")

        // Indonesia first, fallback English
        val idData = JSONObject(
            app.get(
                "https://api.themoviedb.org/3/movie/$id",
                params = mapOf("api_key" to tmdbKey, "language" to "id-ID")
            ).text
        )

        val tmdb = if (idData.optString("overview").isNotBlank()) {
            idData
        } else {
            JSONObject(
                app.get(
                    "https://api.themoviedb.org/3/movie/$id",
                    params = mapOf("api_key" to tmdbKey, "language" to "en-US")
                ).text
            )
        }

        val title = tmdb.optString("title")
        val poster = tmdb.optString("poster_path")

        val year = tmdb.optString("release_date")
            .takeIf { it.length >= 4 }
            ?.substring(0, 4)
            ?.toIntOrNull()

        val rating = tmdb.optDouble("vote_average", 0.0)

        val genres = tmdb.optJSONArray("genres")?.let {
            (0 until it.length()).mapNotNull { i ->
                it.getJSONObject(i).optString("name")
            }
        }

        return newMovieLoadResponse(
            title,
            url,
            TvType.Movie,
            url
        ) {
            posterUrl = "$tmdbImg$poster"
            plot = tmdb.optString("overview")
            this.year = year
            tags = genres
            this.rating = Rating(rating, 10)
        }
    }

    // ================= STREAM =================

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        val id = data.substringAfterLast("/")
        val m3u8 = fetchStreamUrl(id) ?: return false

        callback(
            newExtractorLink(
                source = name,
                name = "Xprime",
                url = m3u8,
                headers = mapOf(
                    "Referer" to "https://xprime.today/",
                    "User-Agent" to USER_AGENT
                )
            )
        )
        return true
    }

    // ================= HELPERS =================

    private suspend fun fetchStreamUrl(id: String): String? {
        val serversJson = app.get(
            "https://mzt4pr8wlkxnv0qsha5g.website/servers"
        ).text

        val servers = JSONObject(serversJson).getJSONArray("servers")

        for (i in 0 until servers.length()) {
            val server = servers.getJSONObject(i)
            if (server.optString("status") != "ok") continue

            val serverName = server.getString("name")
            val url = "$mainUrl/watch/$id?server=$serverName"

            val res = app.get(url, allowRedirects = false)
            res.headers["Location"]?.let {
                if (it.contains(".m3u8")) return it
            }

            val html = app.get(url).text
            Regex("""https?://[^\s'"]+\.m3u8""")
                .find(html)
                ?.let { return it.value }
        }
        return null
    }

    private suspend fun buildFromTmdb(id: Int): SearchResponse? {
        val tmdb = JSONObject(
            app.get(
                "https://api.themoviedb.org/3/movie/$id",
                params = mapOf("api_key" to tmdbKey, "language" to "id-ID")
            ).text
        )

        val title = tmdb.optString("title")
        if (title.isBlank()) return null

        val poster = tmdb.optString("poster_path")
        val year = tmdb.optString("release_date")
            .takeIf { it.length >= 4 }
            ?.substring(0, 4)
            ?.toIntOrNull()

        return newMovieSearchResponse(
            title,
            "$mainUrl/watch/$id",
            TvType.Movie
        ) {
            posterUrl = "$tmdbImg$poster"
            this.year = year
        }
    }

    private fun buildFromTmdbJson(obj: JSONObject): SearchResponse? {
        val title = obj.optString("title")
        if (title.isBlank()) return null

        val id = obj.optInt("id")
        val poster = obj.optString("poster_path")
        val year = obj.optString("release_date")
            .takeIf { it.length >= 4 }
            ?.substring(0, 4)
            ?.toIntOrNull()

        return newMovieSearchResponse(
            title,
            "$mainUrl/watch/$id",
            TvType.Movie
        ) {
            posterUrl = "$tmdbImg$poster"
            this.year = year
        }
    }
}
