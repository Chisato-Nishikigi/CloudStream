package com.hexated

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.json.JSONObject

class XprimeProvider : MainAPI() {

    override var name = "Xprime"
    override var mainUrl = "https://xprime.today"
    override var lang = "en"
    override val supportedTypes = setOf(TvType.Movie)
    override var hasMainPage = true

    private val tmdbKey = "84259f99204eeb7d45c7e3d8e36c6123"
    private val tmdbImg = "https://image.tmdb.org/t/p/w500"

    override val mainPage = mainPageOf(
    // Xprime native
    "https://db.xprime.stream/latest-releases" to "🔥 Latest Releases",
    "https://db.xprime.stream/netflix-movies" to "🎬 Netflix Collection",
    "https://db.xprime.stream/4k-releases" to "📺 4K Movies",
)
override suspend fun getMainPage(
    page: Int,
    request: MainPageRequest
): HomePageResponse {

    val json = JSONObject(app.get(request.data).text)
    val ids = json.getJSONArray("movies")

    val items = (0 until ids.length()).mapNotNull { i ->
        val id = ids.getInt(i)

        val tmdb = JSONObject(
            app.get(
                "https://api.themoviedb.org/3/movie/$id",
                params = mapOf("api_key" to tmdbKey)
            ).text
        )

        val title = tmdb.optString("title")
        val poster = tmdb.optString("poster_path")
        if (title.isBlank()) return@mapNotNull null

        newMovieSearchResponse(
            title,
            "$mainUrl/watch/$id",
            TvType.Movie
        ) {
            posterUrl = "$tmdbImg$poster"
        }
    }

    return newHomePageResponse(request.name, items)
}

    override suspend fun load(url: String): LoadResponse {
        val id = url.substringAfterLast("/")
        val tmdb = JSONObject(
            app.get(
                "https://api.themoviedb.org/3/movie/$id",
                params = mapOf("api_key" to tmdbKey)
            ).text
        )

        return newMovieLoadResponse(
            tmdb.optString("title"),
            url,
            TvType.Movie,
            url
        ) {
            posterUrl = "$tmdbImg${tmdb.optString("poster_path")}"
            plot = tmdb.optString("overview")
        }
    }

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
            url = m3u8
        )
    )

    return true
}

    private suspend fun fetchStreamUrl(id: String): String? {
        val serversJson = app.get(
            "https://mzt4pr8wlkxnv0qsha5g.website/servers"
        ).text

        val servers = JSONObject(serversJson)
            .getJSONArray("servers")

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
}
