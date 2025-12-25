package com.hexated

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.json.JSONObject

class XprimeProvider : MainAPI() {

    override var name = "Xprime"
    override var mainUrl = "https://xprime.today"
    override var lang = "id"
    override val supportedTypes = setOf(TvType.Movie)
    override var hasMainPage = true

    private val tmdbKey = "84259f99204eeb7d45c7e3d8e36c6123"
    private val tmdbImg = "https://image.tmdb.org/t/p/w500"

    override val mainPage = mainPageOf(
        "https://db.xprime.stream/latest-releases" to "🔥 Latest Releases"
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {

        val list = mutableListOf<SearchResponse>()

        val json = JSONObject(app.get(request.data).text)
        val ids = json.getJSONArray("movies")

        for (i in 0 until ids.length()) {
            val id = ids.getInt(i)

            val tmdb = JSONObject(
                app.get(
                    "https://api.themoviedb.org/3/movie/$id",
                    params = mapOf("api_key" to tmdbKey, "language" to "id-ID")
                ).text
            )

            val title = tmdb.optString("title")
            if (title.isBlank()) continue

            val poster = tmdb.optString("poster_path")
            val year = tmdb.optString("release_date")
                .takeIf { it.length >= 4 }
                ?.substring(0, 4)
                ?.toIntOrNull()

            list.add(
                newMovieSearchResponse(
                    title,
                    "$mainUrl/watch/$id",
                    TvType.Movie
                ) {
                    posterUrl = "$tmdbImg$poster"
                    this.year = year
                }
            )
        }

        return newHomePageResponse(request.name, list)
    }

    override suspend fun load(url: String): LoadResponse {
        val id = url.substringAfterLast("/")

        val tmdb = JSONObject(
            app.get(
                "https://api.themoviedb.org/3/movie/$id",
                params = mapOf("api_key" to tmdbKey, "language" to "id-ID")
            ).text
        )

        val title = tmdb.optString("title")
        val poster = tmdb.optString("poster_path")

        val year = tmdb.optString("release_date")
            .takeIf { it.length >= 4 }
            ?.substring(0, 4)
            ?.toIntOrNull()

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
        }
    }
override suspend fun loadLinks(
    data: String,
    isCasting: Boolean,
    subtitleCallback: (SubtitleFile) -> Unit,
    callback: (ExtractorLink) -> Unit
): Boolean {

    val id = data.substringAfterLast("/")

    val html = app.get("$mainUrl/watch/$id").text
    val m3u8 = Regex("""https?://[^\s'"]+\.m3u8""")
        .find(html)
        ?.value ?: return false

    callback(
        newExtractorLink(
            name,
            "Xprime",
            m3u8
        )
    )

    return true
}
