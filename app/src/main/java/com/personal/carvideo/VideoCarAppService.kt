package com.personal.carvideo

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.navigation.NavigationManager
import androidx.car.app.navigation.NavigationManagerCallback
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.model.Template
import androidx.car.app.validation.HostValidator

class VideoCarAppService : CarAppService() {
    // Personal use: accept any host (Android Auto in developer mode).
    override fun createHostValidator(): HostValidator = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    override fun onCreateSession(): Session = VideoSession()
}

class VideoSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen = VideoScreen(carContext)
}

/**
 * Uses the navigation template's raw Surface to draw ExoPlayer video.
 * Intended for use while parked only.
 */
class VideoScreen(carContext: CarContext) : Screen(carContext) {
    private val changed = { invalidate() }

    private val surfaceCallback = object : SurfaceCallback {
        override fun onSurfaceAvailable(c: SurfaceContainer) = PlayerController.setSurface(c.surface)
        override fun onSurfaceDestroyed(c: SurfaceContainer) { c.surface?.let(PlayerController::clearSurface) }
    }

    init {
        lifecycle.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
            override fun onCreate(owner: androidx.lifecycle.LifecycleOwner) {
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
            }

            override fun onDestroy(owner: androidx.lifecycle.LifecycleOwner) {
                PlayerController.removeListener(changed)
                PlayerController.setSurface(null)
                carContext.getCarService(NavigationManager::class.java).navigationEnded()
            }
        })
    }

    override fun onGetTemplate(): Template {
        fun action(label: String, onClick: () -> Unit) =
            Action.Builder().setTitle(label).setOnClickListener(onClick).build()

        val strip = ActionStrip.Builder()
            .addAction(action("⏮") { PlayerController.previous(carContext) })
            .addAction(action(if (PlayerController.isPlaying) "⏸" else "▶") { PlayerController.togglePlay() })
            .addAction(action("⏭") { PlayerController.next(carContext) })
            .build()

        return NavigationTemplate.Builder()
            .setActionStrip(strip)
            .setBackgroundColor(CarColor.createCustom(0xFF000000.toInt(), 0xFF000000.toInt()))
            .build()
    }
}
