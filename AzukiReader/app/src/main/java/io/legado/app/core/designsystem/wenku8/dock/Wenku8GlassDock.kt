package io.legado.app.core.designsystem.wenku8.dock

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.shadow.Shadow
import io.legado.app.core.designsystem.wenku8.chrome.Wenku8Motion
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

/** Geometry follows glass_dock.dart: 54 high, r27, 6x3 inset, 184/3 per slot. */
val Wenku8DockSlotWidth = (184f / 3f).dp
val Wenku8DockHeight = 54.dp

@Stable
data class Wenku8DockItem(val id: String, val label: String, val icon: ImageVector)

/** Only release commits navigation. Pointer preview never owns the current page. */
@Composable
fun Wenku8GlassDock(
    items: ImmutableList<Wenku8DockItem>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    reduceMotion: Boolean = false,
) {
    if (items.isEmpty()) return
    val scheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val motionOff = reduceMotion || scope.coroutineContext[MotionDurationScale]?.scaleFactor == 0f
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val identities = items.map { it.id }
    var preview by remember(identities) { mutableStateOf<Float?>(null) }
    var touching by remember(identities) { mutableStateOf(false) }
    var dragging by remember(identities) { mutableStateOf(false) }
    var flow by remember(identities) { mutableFloatStateOf(0f) }
    var width by remember { mutableFloatStateOf(1f) }
    val commit by rememberUpdatedState(onSelected)
    // A committed destination gets one finite jelly recoil, including semantic activation.
    // Do not replay on initial composition, re-tapping the current tab or cancelled preview.
    val recoil = remember(identities) { Animatable(1f) }
    var previousSelection by remember(identities) { mutableIntStateOf(selectedIndex) }
    var recoilDirection by remember(identities) { mutableFloatStateOf(1f) }
    LaunchedEffect(selectedIndex, identities, motionOff, rtl) {
        val changed = previousSelection != selectedIndex
        val direction = if (selectedIndex > previousSelection) 1f else -1f
        previousSelection = selectedIndex
        if (motionOff) recoil.snapTo(1f)
        else if (changed) {
            recoilDirection = direction * if (rtl) -1f else 1f
            recoil.snapTo(0f)
            recoil.animateTo(1f, tween(Wenku8Motion.Enter, easing = LinearEasing))
        } else recoil.snapTo(1f)
    }
    val position by animateFloatAsState(
        (preview ?: selectedIndex.toFloat()).coerceIn(0f, items.lastIndex.toFloat()),
        if (motionOff || dragging) snap() else spring(Wenku8Motion.SpringDamping, Wenku8Motion.SpringStiffness), label = "wenku8DockPosition",
    )
    val pressure by animateFloatAsState(
        if (touching && !motionOff) 1f else 0f,
        when {
            motionOff -> snap()
            touching -> tween(Wenku8Motion.Contact, easing = Wenku8Motion.Ease)
            else -> spring(Wenku8Motion.SpringDamping, Wenku8Motion.SpringStiffness)
        }, label = "wenku8DockPressure",
    )
    val stretch by animateFloatAsState(
        if (motionOff) 0f else flow,
        if (motionOff || dragging) snap() else spring(.723f, 421.875f), label = "wenku8DockFlow",
    )
    val policy = io.legado.app.core.designsystem.wenku8.effects.rememberWenku8Effects()
    val appearance = io.legado.app.ui.theme.LocalAppUiConfiguration.current.theme
    val opacity = appearance.wenku8GlassOpacity.coerceIn(0f, 1f)
    val dark = scheme.background.luminanceForDock() < .5f
    val tint = if (dark) Color(0xFF3F3F3F).copy(alpha = opacity) else Color(0xFFFEFDF8).copy(alpha = opacity)
    val capsule = RoundedCornerShape(27.dp)
    val surface = if (backdrop != null && (policy.optics || policy.blur) && Build.VERSION.SDK_INT >= 33) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { capsule },
            effects = {
                if (policy.blur) blur(appearance.wenku8GlassBlur.coerceIn(0f, 24f).dp.toPx())
                if (policy.optics) lens(16.dp.toPx() + pressure * 2.dp.toPx(), 16.dp.toPx())
            },
            highlight = null,
            shadow = { Shadow.Default.copy(color = Color.Black.copy(alpha = if (dark) .2f else .08f)) },
            onDrawSurface = { drawRect(tint) },
        )
    } else Modifier.shadow(4.dp, capsule).background(tint.copy(alpha = 1f), capsule)

    Box(
        modifier = modifier
            .height(Wenku8DockHeight)
            .graphicsLayer {
                val t = recoil.value
                val jelly = if (motionOff) 0f else sin(t * Math.PI.toFloat() * 3f) * (1f - t) * (1f - t)
                translationX = (stretch * .16f + jelly * recoilDirection * 4f) * density
                translationY = -pressure * .6f * density
                scaleX = 1f + pressure * .022f + abs(stretch) * .00125f + jelly * .026f
                scaleY = 1f + pressure * .065f - abs(stretch) * .0006f - jelly * .042f
            }
            .then(surface)
            .padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
        Row(
            Modifier.fillMaxSize().selectableGroup().onSizeChanged { width = it.width.toFloat() }
                .pointerInput(identities, rtl, motionOff) {
                    var rest: Job? = null
                    fun finger(x: Float): Float {
                        val physical = x / (width / items.size).coerceAtLeast(1f) - .5f
                        return (if (rtl) items.lastIndex - physical else physical)
                            .coerceIn(0f, items.lastIndex.toFloat())
                    }
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        touching = true
                        preview = finger(down.position.x)
                        var lastX = down.position.x
                        var lastTime = down.uptimeMillis
                        var lastMotionTime = lastTime
                        var velocity = 0f
                        var cancelled = false
                        try {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                val delta = change.position - down.position
                                if (!dragging && abs(delta.y) > viewConfiguration.touchSlop && abs(delta.y) > abs(delta.x)) {
                                    change.consume()
                                    cancelled = true
                                    break
                                }
                                if (!dragging && abs(delta.x) > viewConfiguration.touchSlop) dragging = true
                                if (dragging) {
                                    if (change.isConsumed) { cancelled = true; break }
                                    val speed = (change.position.x - lastX) * 1000f / (change.uptimeMillis - lastTime).coerceAtLeast(1L)
                                    if (change.position.x != lastX) {
                                        velocity = speed / (width / items.size) * if (rtl) -1f else 1f
                                        lastMotionTime = change.uptimeMillis
                                    }
                                    val target = finger(change.position.x)
                                    preview = if (motionOff) target else (preview ?: target) + (target - (preview ?: target)) * .8f
                                    flow += ((speed / density * .028f).coerceIn(-24f, 24f) - flow) * .55f
                                    rest?.cancel()
                                    rest = scope.launch { delay(100); flow = 0f }
                                    change.consume()
                                }
                                lastX = change.position.x
                                lastTime = change.uptimeMillis
                                if (!change.pressed) {
                                    if (dragging && !cancelled) {
                                        val releaseVelocity = if (change.uptimeMillis - lastMotionTime < 100) velocity else 0f
                                        val projected = finger(change.position.x) + (releaseVelocity * .055f).coerceIn(-.22f, .22f)
                                        val index = projected.roundToInt().coerceIn(0, items.lastIndex)
                                        commit(index)
                                    }
                                    break
                                }
                            }
                        } finally {
                            rest?.cancel()
                            touching = false
                            dragging = false
                            flow = 0f
                            preview = null
                        }
                    }
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                key(item.id) {
                    val proximity = (1f - abs(position - index)).coerceIn(0f, 1f)
                    val color = lerp(scheme.onSurfaceVariant, scheme.primary, proximity)
                    Column(
                        Modifier.weight(1f).height(48.dp)
                            .selectable(
                                selectedIndex == index, role = Role.Tab,
                                interactionSource = remember { MutableInteractionSource() }, indication = null,
                                onClick = { commit(index) },
                            )
                            .semantics { contentDescription = item.label },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(item.icon, null, Modifier.size(20.dp), tint = color)
                        Spacer(Modifier.height(3.dp))
                        Text(
                            item.label, Modifier.clearAndSetSemantics {}, color = color,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 11.sp, lineHeight = 13.sp,
                                fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Medium,
                            ), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(3.dp))
                        Box(Modifier.width(12.dp).height(1.5.dp).background(scheme.onSurface.copy(alpha = proximity)))
                    }
                }
            }
        }
    }
}

private fun Color.luminanceForDock(): Float = .2126f * red + .7152f * green + .0722f * blue
