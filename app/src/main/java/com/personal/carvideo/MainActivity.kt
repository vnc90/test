package com.personal.carvideo

import android.app.Activity
import android.os.Bundle
import android.view.SurfaceView
import android.widget.*
import kotlin.concurrent.thread
import okhttp3.OkHttpClient
import okhttp3.Request

/** Phone-side UI: pick a YouTube link or an M3U (IPTV) playlist; the car screen plays it. */
class MainActivity : Activity() {
    companion object {
        // Danh sách IPTV Việt Nam do cộng đồng iptv-org duy trì (stream công khai, có thể hỏng/đổi bất kỳ lúc nào).
        const val DEFAULT_M3U = "https://iptv-org.github.io/iptv/countries/vn.m3u"
        // Video YouTube mặc định (sửa tuỳ ý).
        const val DEFAULT_YT = "https://www.youtube.com/watch?v=CL13X-8o4h0"
    }

    private val http = OkHttpClient()
    private lateinit var list: ListView
    private lateinit var m3uField: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("p", MODE_PRIVATE)

        val yt = EditText(this).apply {
            hint = "Link YouTube (watch?v=...)"
            setText(prefs.getString("yt", DEFAULT_YT))
        }
        val ytBtn = Button(this).apply { text = "Phát YouTube" }
        val m3u = EditText(this).apply {
            hint = "URL danh sách M3U (IPTV)"
            setText(prefs.getString("m3u", DEFAULT_M3U))
        }
        m3uField = m3u
        val m3uBtn = Button(this).apply { text = "Tải danh sách TV" }
        list = ListView(this).apply { layoutParams = LinearLayout.LayoutParams(-1, 0, 1f) }
        val preview = SurfaceView(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
            holder.addCallback(object : android.view.SurfaceHolder.Callback {
                override fun surfaceCreated(h: android.view.SurfaceHolder) = PlayerController.setSurface(h.surface)
                override fun surfaceChanged(h: android.view.SurfaceHolder, f: Int, w: Int, ht: Int) {}
                override fun surfaceDestroyed(h: android.view.SurfaceHolder) = PlayerController.clearSurface(h.surface)
            })
        }
        val brand = TextView(this).apply {
            text = "${getString(R.string.app_name)}\n${getString(R.string.brand_author)} - ${getString(R.string.brand_community)}"
            textSize = 16f
            setTextColor(0xFFB71C1C.toInt())
            setPadding(0, 0, 0, 24)
        }

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
            listOf(brand, preview, yt, ytBtn, m3u, m3uBtn, list).forEach { addView(it) }
        })

        ytBtn.setOnClickListener {
            prefs.edit().putString("yt", yt.text.toString().trim()).apply()
            io({ YouTubeResolver.resolve(yt.text.toString().trim()) }) {
                PlayerController.setChannels(this, listOf(it))
            }
        }
        m3uBtn.setOnClickListener { loadM3u() }
        loadM3u() // tự tải danh sách TV khi mở app
    }

    private fun loadM3u() {
        val prefs = getSharedPreferences("p", MODE_PRIVATE)
        run {
            val url = m3uField.text.toString().trim()
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
