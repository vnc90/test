package com.personal.carvideo

import android.app.Activity
import android.os.Bundle
import android.widget.*
import kotlin.concurrent.thread
import okhttp3.OkHttpClient
import okhttp3.Request

/** Phone-side UI: pick a YouTube link or an M3U (IPTV) playlist; the car screen plays it. */
class MainActivity : Activity() {
    private val http = OkHttpClient()
    private lateinit var list: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("p", MODE_PRIVATE)

        val yt = EditText(this).apply { hint = "Link YouTube (watch?v=...)" }
        val ytBtn = Button(this).apply { text = "Phát YouTube" }
        val m3u = EditText(this).apply {
            hint = "URL danh sách M3U (IPTV)"
            setText(prefs.getString("m3u", ""))
        }
        val m3uBtn = Button(this).apply { text = "Tải danh sách TV" }
        list = ListView(this)

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
            listOf(yt, ytBtn, m3u, m3uBtn, list).forEach { addView(it) }
        })

        ytBtn.setOnClickListener {
            io({ YouTubeResolver.resolve(yt.text.toString().trim()) }) {
                PlayerController.setChannels(this, listOf(it))
            }
        }
        m3uBtn.setOnClickListener {
            val url = m3u.text.toString().trim()
            prefs.edit().putString("m3u", url).apply()
            io({
                http.newCall(Request.Builder().url(url).build()).execute().use { it.body!!.string() }
                    .let(M3uParser::parse)
            }) { channels ->
                list.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, channels.map { it.name })
                list.setOnItemClickListener { _, _, i, _ -> PlayerController.setChannels(this, channels, i) }
                toast("${channels.size} kênh")
            }
        }
    }

    private fun <T> io(work: () -> T, done: (T) -> Unit) = thread {
        try {
            val r = work()
            runOnUiThread { done(r) }
        } catch (e: Exception) {
            runOnUiThread { toast("Lỗi: ${e.message}") }
        }
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()
}
