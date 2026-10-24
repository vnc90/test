package com.personal.carvideo

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.car.app.CarAppService
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.Template
import androidx.car.app.navigation.NavigationManager
import androidx.car.app.navigation.NavigationManagerCallback
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.validation.HostValidator
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class VideoCarAppService : CarAppService() {
    // Personal use: accept any host (Android Auto in developer mode).
    override fun createHostValidator(): HostValidator = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    override fun onCreateSession(): Session = VideoSession()
}

class VideoSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen = VideoScreen(carContext)
}

/**
 * Draws on the navigation template's raw Surface: either the YouTube mobile site (WebView on a
 * virtual display, driven by the car touchscreen) or an ExoPlayer stream.
 */
class VideoScreen(carContext: CarContext) : Screen(carContext) {
    private val web = WebSurface(carContext)
    private var container: SurfaceContainer? = null
    private var recognizer: SpeechRecognizer? = null

    private val changed = {
        render()
        invalidate()
    }

    private val surfaceCallback = object : SurfaceCallback {
        override fun onSurfaceAvailable(c: SurfaceContainer) {
            container = c
            render()
        }

        override fun onSurfaceDestroyed(c: SurfaceContainer) {
            web.stop()
            c.surface?.let(PlayerController::clearSurface)
            container = null
        }

        override fun onClick(x: Float, y: Float) { web.click(x, y) }
        override fun onScroll(distanceX: Float, distanceY: Float) { web.scroll(distanceX, distanceY) }
        override fun onFling(velocityX: Float, velocityY: Float) { web.fling(velocityX, velocityY) }
    }

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                carContext.getCarService(androidx.car.app.AppManager::class.java)
                    .setSurfaceCallback(surfaceCallback)
                carContext.getCarService(NavigationManager::class.java).apply {
                    setNavigationManagerCallback(object : NavigationManagerCallback {
                        override fun onStopNavigation() {}
                        override fun onAutoDriveEnabled() {}
                    })
                    navigationStarted() // host only shows the template while "navigating"
                }
                PlayerController.addListener(changed)
                CarState.addListener(changed)
            }

            override fun onDestroy(owner: LifecycleOwner) {
                PlayerController.removeListener(changed)
                CarState.removeListener(changed)
                recognizer?.destroy()
                web.stop()
                PlayerController.setSurface(null)
                carContext.getCarService(NavigationManager::class.java).navigationEnded()
            }
        })
    }

    /** Point the Surface at the right producer for the current mode. */
    private fun render() {
        val c = container ?: return
        when (CarState.mode) {
            CarState.Mode.YOUTUBE -> {
                PlayerController.clearSurface(c.surface ?: return)
                val pending = CarState.pendingUrl
                if (!web.active) web.start(c)
                else if (pending != null) {
                    CarState.pendingUrl = null
                    web.load(pending)
                }
            }
            CarState.Mode.TV -> {
                web.stop()
                PlayerController.setSurface(c.surface)
            }
        }
    }

    private fun startVoiceSearch() {
        if (!SpeechRecognizer.isRecognitionAvailable(carContext)) {
            toast("Điện thoại không hỗ trợ nhận giọng nói")
            return
        }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(carContext).also { sr ->
            sr.setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle) {
                    val text = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (text.isNullOrBlank()) toast("Không nghe rõ, thử lại") else {
                        toast("Tìm: $text")
                        CarState.setMode(CarState.Mode.YOUTUBE)
                        web.search(text)
                    }
                }

                override fun onError(error: Int) =
                    toast("Lỗi giọng nói ($error). Đã cấp quyền micro cho app chưa?")

                override fun onReadyForSpeech(params: Bundle?) = toast("Đang nghe...")
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            sr.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
            })
        }
    }

    private fun toast(s: String) = CarToast.makeText(carContext, s, CarToast.LENGTH_SHORT).show()

    override fun onGetTemplate(): Template {
        fun action(label: String, onClick: () -> Unit) =
            Action.Builder().setTitle(label).setOnClickListener(onClick).build()

        val strip = ActionStrip.Builder().apply {
            if (CarState.mode == CarState.Mode.YOUTUBE) {
                addAction(action("🏠") { web.home() })
                addAction(action("⬅") { web.back() })
                addAction(action("🎤") { startVoiceSearch() })
                addAction(action("⏯") { web.playPause() })
            } else {
                addAction(action("⏮") { PlayerController.previous(carContext) })
                addAction(action(if (PlayerController.isPlaying) "⏸" else "▶") { PlayerController.togglePlay() })
                addAction(action("⏭") { PlayerController.next(carContext) })
                addAction(action("YouTube") { CarState.openYouTube(null) })
            }
        }.build()

        return NavigationTemplate.Builder()
            .setActionStrip(strip)
            .setBackgroundColor(CarColor.createCustom(0xFF000000.toInt(), 0xFF000000.toInt()))
            .build()
    }
}
