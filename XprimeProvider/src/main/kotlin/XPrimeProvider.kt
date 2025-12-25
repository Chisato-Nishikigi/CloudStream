import com.lagradost.cloudstream3.*

class XprimeProvider : MainAPI() {

    override var name = "XPrime"
    override var mainUrl = "https://example.com"
    override var lang = "en"

    override val supportedTypes = setOf(
        TvType.Movie
    )

    override fun getMainPage(page: Int): HomePageResponse {
        return HomePageResponse(
            listOf(
                HomePageList(
                    "Test",
                    listOf(
                        newMovieSearchResponse(
                            "Test Movie",
                            "https://example.com/test",
                            TvType.Movie
                        )
                    )
                )
            )
        )
    }

    override fun load(url: String): LoadResponse {
        return newMovieLoadResponse(
            "Test Movie",
            url,
            TvType.Movie,
            url
        )
    }
}
