package com.personal.carvideo

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient

/** Sign in to Google/YouTube once; the cookies are shared with the car screen's web view. */
class LoginActivity : Activity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.userAgentString = WebSurface.USER_AGENT
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String?) {
                    CookieManager.getInstance().flush()
                    if (url?.startsWith("https://m.youtube.com") == true) finish() // signed in, back to YouTube
                }
            }
            loadUrl(
                "https://accounts.google.com/ServiceLogin?service=youtube" +
                    "&continue=https%3A%2F%2Fm.youtube.com%2F"
            )
        }
        setContentView(web)
    }
}
