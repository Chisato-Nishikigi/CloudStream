override suspend fun loadLinks(
    data: String,
    isCasting: Boolean,
    subtitleCallback: (SubtitleFile) -> Unit,
    callback: (ExtractorLink) -> Unit
): Boolean {

    val doc = app.get(data).document

    // Ambil semua <source> di dalam <video>
    val sources = doc.select("video#player source")

    if (sources.isEmpty()) return false

    for (source in sources) {
        val url = source.attr("src")
        if (url.isNullOrEmpty()) continue

        val quality = source.attr("size").toIntOrNull() ?: Qualities.Unknown.value

        callback(
            newExtractorLink(
                source = "Kuramanime",
                name = "Kuramanime ${quality}p",
                url = url,
                type = ExtractorLinkType.VIDEO
            ) {
                this.quality = quality
                this.referer = data
                this.headers = mapOf(
                    "Referer" to data,
                    "Origin" to "https://v8.kuramanime.tel"
                )
            }
        )
    }

    return true
}
