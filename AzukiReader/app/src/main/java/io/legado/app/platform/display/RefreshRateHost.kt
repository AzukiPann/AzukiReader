package io.legado.app.platform.display

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.view.Display
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlin.math.abs
import kotlin.math.roundToInt

private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

/** Same-resolution modes only: requesting a refresh rate must never change screen resolution. */
internal fun selectRefreshMode(modes: Array<Display.Mode>, current: Display.Mode, target: Int): Display.Mode? {
    if (target < 0) return null
    val matching = modes.filter { it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight }
    return if (target == 0) matching.maxByOrNull { it.refreshRate }
    else matching.filter { abs(it.refreshRate - target) < 1f }.maxByOrNull { it.refreshRate }
}

@Composable
fun ApplyRefreshRate(target: Int, requestUnsupportedRate: Boolean = false) {
    val view = LocalView.current
    val activity = LocalContext.current.activity() ?: return
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(activity, view, lifecycle, target, requestUnsupportedRate) {
        val window = activity.window
        val originalMode = window.attributes.preferredDisplayModeId
        val originalRate = window.attributes.preferredRefreshRate
        fun apply() {
            val display = view.display ?: return
            val mode = selectRefreshMode(display.supportedModes, display.mode, target)
            // Reading can request 30 Hz even when the display exposes only higher physical modes.
            val id = mode?.modeId ?: 0
            val rate = requestedRefreshRate(mode, target, requestUnsupportedRate)
            val attributes = window.attributes
            if (attributes.preferredDisplayModeId != id || attributes.preferredRefreshRate != rate) {
                attributes.preferredDisplayModeId = id
                attributes.preferredRefreshRate = rate
                window.attributes = attributes
            }
        }
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) apply() }
        val manager = activity.getSystemService(DisplayManager::class.java)
        val listener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(id: Int) = apply()
            override fun onDisplayRemoved(id: Int) = apply()
            override fun onDisplayChanged(id: Int) = apply()
        }
        lifecycle.addObserver(observer)
        manager.registerDisplayListener(listener, Handler(Looper.getMainLooper()))
        apply()
        onDispose {
            lifecycle.removeObserver(observer)
            manager.unregisterDisplayListener(listener)
            window.attributes = window.attributes.apply {
                preferredDisplayModeId = originalMode
                preferredRefreshRate = originalRate
            }
        }
    }
}

internal fun requestedRefreshRate(mode: Display.Mode?, target: Int, requestUnsupportedRate: Boolean): Float =
    mode?.refreshRate ?: if (requestUnsupportedRate && target > 0) target.toFloat() else 0f

@Stable
data class RefreshRateInfo(val current: Float, val rates: ImmutableList<Int>)

@Composable
fun rememberRefreshRateInfo(): RefreshRateInfo? {
    val view = LocalView.current
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var info by remember(view) { mutableStateOf<RefreshRateInfo?>(null) }
    DisposableEffect(view, context, lifecycle) {
        fun update() {
            val display = view.display
            info = display?.let {
                val mode = it.mode
                RefreshRateInfo(it.refreshRate, it.supportedModes.filter { candidate ->
                    candidate.physicalWidth == mode.physicalWidth && candidate.physicalHeight == mode.physicalHeight
                }.map { candidate -> candidate.refreshRate.roundToInt() }.distinct().sorted().toImmutableList())
            }
        }
        val manager = context.getSystemService(DisplayManager::class.java)
        val listener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(id: Int) = update()
            override fun onDisplayRemoved(id: Int) = update()
            override fun onDisplayChanged(id: Int) = update()
        }
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) update() }
        manager.registerDisplayListener(listener, Handler(Looper.getMainLooper()))
        lifecycle.addObserver(observer)
        update()
        onDispose { manager.unregisterDisplayListener(listener); lifecycle.removeObserver(observer) }
    }
    return info
}
