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
            if (title.isBlank()) return@mapNotNull null

            val poster = tmdb.optString("poster_path")
            val year = tmdb.optString("release_date")
                .takeIf { it.length >= 4 }
                ?.substring(0, 4)
                ?.toIntOrNull()

            newMovieSearchResponse(
                title,
                "$mainUrl/watch/$id",
                TvType.Movie
            ) {
                posterUrl = "$tmdbImg$poster"
                this.year = year
            }
        }

        return newHomePageResponse(request.name, items)
    }

    override suspend fun load(url: String): LoadResponse {
        val id = url.substringAfterLast("/")

        val tmdb = JSONObject(
            app.get(
                "https://api.themoviedb.org/3/movie/$id",
                params = mapOf(
                    "api_key" to tmdbKey,
                    "language" to "id-ID"
                )
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

    val serversJson = app.get(
        "https://mzt4pr8wlkxnv0qsha5g.website/servers"
    ).text

    val servers = JSONObject(serversJson).getJSONArray("servers")
    var found = false

    for (i in 0 until servers.length()) {
        val server = servers.getJSONObject(i)
        if (server.optString("status") != "ok") continue

        val serverName = server.getString("name")

        val apiUrl =
            "https://mzt4pr8wlkxnv0qsha5g.website/$serverName" +
            "?id=$id&turnstile=0"

        val res = app.get(
            apiUrl,
            allowRedirects = false,
            headers = mapOf(
                "User-Agent" to USER_AGENT,
                "Referer" to "https://xprime.today/",
                "Origin" to "https://xprime.today"
            )
        )

        val m3u8 = res.headers["Location"]
            ?: Regex("""https?://[^\s'"]+\.m3u8""")
                .find(res.text)
                ?.value

        if (m3u8 != null) {
            callback(
                newExtractorLink(
                    source = "Xprime",
                    name = "Xprime - $serverName",
                    url = m3u8,
                    type = ExtractorLinkType.M3U8
                ) {
                    referer = "https://xprime.today/"
                    headers = mapOf(
                        "User-Agent" to USER_AGENT,
                        "Origin" to "https://xprime.today"
                    )
                }
            )
            found = true
        }
    }

    return found
}
