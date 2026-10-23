package com.personal.carvideo

import android.content.Context
import android.view.Surface
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/** Process-wide player shared by the phone UI and the car screen. Main thread only. */
object PlayerController {
    private var player: ExoPlayer? = null
    private var surface: Surface? = null

    var channels: List<Channel> = emptyList()
        private set
    var index = -1
        private set
    val current: Channel? get() = channels.getOrNull(index)
    val isPlaying: Boolean get() = player?.isPlaying == true

    private val listeners = mutableSetOf<() -> Unit>()
    fun addListener(l: () -> Unit) { listeners += l }
    fun removeListener(l: () -> Unit) { listeners -= l }
    private fun notifyChanged() = listeners.toList().forEach { it() }

    private fun ensure(context: Context): ExoPlayer =
        player ?: ExoPlayer.Builder(context.applicationContext).build().also { p ->
            p.setWakeMode(C.WAKE_MODE_NETWORK) // keep playing with the phone screen off
            p.setVideoSurface(surface)
            p.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) = notifyChanged()
            })
            player = p
        }

    fun setSurface(s: Surface?) {
        surface = s
        player?.setVideoSurface(s)
    }

    /** Release [s] only if it is still the active surface (phone preview vs. car screen). */
    fun clearSurface(s: Surface) {
        if (surface === s) setSurface(null)
    }

    fun setChannels(context: Context, list: List<Channel>, start: Int = 0) {
        channels = list
        select(context, start)
    }

    fun select(context: Context, i: Int) {
        if (channels.isEmpty()) return
        index = ((i % channels.size) + channels.size) % channels.size
        ensure(context).apply {
            setMediaItem(MediaItem.fromUri(channels[index].url))
            prepare()
            playWhenReady = true
        }
        notifyChanged()
    }

    fun next(context: Context) = select(context, index + 1)
    fun previous(context: Context) = select(context, index - 1)

    fun togglePlay() {
        player?.let { it.playWhenReady = !it.playWhenReady }
        notifyChanged()
    }
}
