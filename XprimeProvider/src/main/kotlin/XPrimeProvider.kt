package com.xprime

import com.lagradost.cloudstream3.*

class XPrimeProvider : MainAPI() {

    override var name = "XPrime Movies"
    override var mainUrl = "https://db.xprime.stream"
    override var lang = "en"
    override val supportedTypes = setOf(TvType.Movie)

    override fun getMainPage(page: Int): HomePageResponse {
        return HomePageResponse(
            listOf(
                HomePageList(
                    "Test",
                    listOf(
                        newMovieSearchResponse(
                            "Test Movie",
                            "1435092",
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
