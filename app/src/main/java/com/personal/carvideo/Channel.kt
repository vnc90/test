package com.personal.carvideo

data class Channel(val name: String, val url: String)

object M3uParser {
    /** Minimal #EXTM3U parser: pairs each #EXTINF title with the URL on the next line. */
    fun parse(text: String): List<Channel> {
        val out = mutableListOf<Channel>()
        var title: String? = null
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            when {
                line.startsWith("#EXTINF") -> title = line.substringAfterLast(',').trim()
                line.isEmpty() || line.startsWith("#") -> {}
                else -> {
                    out += Channel(title?.ifEmpty { null } ?: line, line)
                    title = null
                }
            }
        }
        return out
    }
}
