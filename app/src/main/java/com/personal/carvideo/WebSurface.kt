package com.personal.carvideo

import android.annotation.SuppressLint
import android.app.Presentation
import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.Display
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.car.app.SurfaceContainer

/**
 * Renders the YouTube mobile website into the car's Surface: a WebView lives in a Presentation
 * shown on a VirtualDisplay whose output is the Surface. Touches from the car are replayed
 * into the WebView.
 */
class WebSurface(private val context: Context) {
    private var display: VirtualDisplay? = null
    private var presentation: WebPresentation? = null

    val active: Boolean get() = presentation != null

    fun start(c: SurfaceContainer) {
        stop()
        val surface = c.surface ?: return
        val dm = context.getSystemService(DisplayManager::class.java)
        val vd = dm.createVirtualDisplay(
            "carvideo-web", c.width, c.height, DPI, surface,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION
        )
        display = vd
        presentation = WebPresentation(context, vd.display).also { it.show() }
    }

    fun stop() {
        presentation?.release()
        presentation = null
        display?.release()
        display = null
    }

    fun click(x: Float, y: Float) = presentation?.click(x, y)
    fun scroll(dx: Float, dy: Float) = presentation?.web?.scrollBy(dx.toInt(), dy.toInt())
    fun fling(vx: Float, vy: Float) = presentation?.web?.flingScroll(-vx.toInt(), -vy.toInt())
    fun load(url: String) = presentation?.web?.loadUrl(url)
    fun home() = load(CarState.YOUTUBE_HOME)
    fun back() { presentation?.web?.let { if (it.canGoBack()) it.goBack() } }
    fun search(q: String) = load("https://m.youtube.com/results?search_query=" + Uri.encode(q))
    fun playPause() = presentation?.web?.evaluateJavascript(
        "(function(){var v=document.querySelector('video');if(v){v.paused?v.play():v.pause();}})()", null
    )

    companion object {
        /** Virtual display density: higher than the car's so the mobile site gets a phone-like layout. */
        const val DPI = 240
        const val HOME = CarState.YOUTUBE_HOME
    }
}

private class WebPresentation(outer: Context, display: Display) : Presentation(outer, display) {
    var web: WebView? = null
    private lateinit var root: FrameLayout
    private var customView: View? = null
    private var customCallback: WebChromeClient.CustomViewCallback? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
        root = FrameLayout(context)
        web = WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.userAgentString =
                "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/126.0.0.0 Mobile Safari/537.36"
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String?) { CarState.lastUrl = url }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                    customView = view
                    customCallback = callback
                    root.addView(view, FrameLayout.LayoutParams(-1, -1))
                }

                override fun onHideCustomView() {
                    customView?.let { root.removeView(it) }
                    customView = null
                    customCallback?.onCustomViewHidden()
                    customCallback = null
                }
            }
            val first = CarState.pendingUrl ?: CarState.lastUrl ?: CarState.YOUTUBE_HOME
            CarState.pendingUrl = null
            loadUrl(first)
        }
        root.addView(web, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)
    }

    fun click(x: Float, y: Float) {
        val w = web ?: return
        val t = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, x, y, 0)
        w.dispatchTouchEvent(down)
        down.recycle()
        w.postDelayed({
            val up = MotionEvent.obtain(t, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, x, y, 0)
            w.dispatchTouchEvent(up)
            up.recycle()
        }, 50)
    }

    fun release() {
        web?.let {
            CarState.lastUrl = it.url ?: CarState.lastUrl
            it.stopLoading()
            it.loadUrl("about:blank")
            it.destroy()
        }
        web = null
        dismiss()
    }
}
