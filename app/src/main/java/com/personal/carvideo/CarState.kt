package com.personal.carvideo

/** What the car screen is showing: the YouTube web UI or an ExoPlayer stream (IPTV / NewPipe). */
object CarState {
    enum class Mode { YOUTUBE, TV }

    const val YOUTUBE_HOME = "https://m.youtube.com/"

    var mode = Mode.YOUTUBE
        private set

    /** Last page shown by the web UI; restored when the web view is re-created. */
    var lastUrl: String? = null

    /** URL requested from the phone, consumed once by the car screen. */
    var pendingUrl: String? = null

    private val listeners = mutableSetOf<() -> Unit>()
    fun addListener(l: () -> Unit) { listeners += l }
    fun removeListener(l: () -> Unit) { listeners -= l }

    fun setMode(m: Mode) {
        if (m == Mode.YOUTUBE) PlayerController.pause()
        mode = m
        listeners.toList().forEach { it() }
    }

    fun openYouTube(url: String?) {
        pendingUrl = url ?: YOUTUBE_HOME
        setMode(Mode.YOUTUBE)
    }
}
