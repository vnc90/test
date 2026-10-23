package com.personal.carvideo

import okhttp3.OkHttpClient
import okhttp3.RequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.stream.StreamInfo

/** Resolves a YouTube watch URL to a directly playable stream URL (personal use). */
object YouTubeResolver {
    private val client = OkHttpClient()
    private var ready = false

    private object Dl : Downloader() {
        override fun execute(request: Request): Response {
            val body = request.dataToSend()?.let { RequestBody.create(null, it) }
            val b = okhttp3.Request.Builder().url(request.url())
                .method(request.httpMethod(), body)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; rv:128.0) Gecko/20100101 Firefox/128.0")
            request.headers().forEach { (k, vs) ->
                b.removeHeader(k)
                vs.forEach { b.addHeader(k, it) }
            }
            client.newCall(b.build()).execute().use { r ->
                return Response(r.code, r.message, r.headers.toMultimap(), r.body?.string(), r.request.url.toString())
            }
        }
    }

    /** Blocking; call off the main thread. */
    private val idRegex = Regex("""(?:[?&]v=|youtu\.be/|shorts/|live/|embed/)([\w-]{11})""")

    /** Accepts watch?v=, youtu.be/, shorts/, live/, embed/ links, or a bare 11-char video id. */
    fun normalize(input: String): String {
        val text = input.trim()
        val id = idRegex.find(text)?.groupValues?.get(1)
            ?: text.takeIf { Regex("[\\w-]{11}").matches(it) }
            ?: error("Link không có video. Hãy dán link dạng youtube.com/watch?v=... hoặc youtu.be/...")
        return "https://www.youtube.com/watch?v=$id"
    }

    fun resolve(input: String): Channel {
        if (!ready) { NewPipe.init(Dl); ready = true }
        val info = StreamInfo.getInfo(normalize(input))
        val url = info.hlsUrl.takeIf { it.isNotEmpty() }                       // live streams
            ?: info.videoStreams.filter { !it.isVideoOnly }                    // muxed video+audio
                .filter { it.height <= 720 }.maxByOrNull { it.height }?.content
            ?: info.videoStreams.firstOrNull { !it.isVideoOnly }?.content
            ?: error("No playable stream found")
        return Channel(info.name, url)
    }
}
