package io.legado.app.core.designsystem.wenku8.effects

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme
import io.legado.app.core.designsystem.wenku8.chrome.Wenku8Motion

/** Shared Material controls inherit the archive timing without a second popup implementation. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object Wenku8MotionScheme : MotionScheme {
    override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> =
        tween(Wenku8Motion.Enter, easing = Wenku8Motion.Ease)
    override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> =
        spring(Wenku8Motion.SpringDamping, Wenku8Motion.SpringStiffness)
    override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> =
        tween(Wenku8Motion.Enter, easing = Wenku8Motion.Ease)
    override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> =
        tween(Wenku8Motion.Exit, easing = Wenku8Motion.Ease)
    override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> =
        tween(Wenku8Motion.Contact, easing = Wenku8Motion.Ease)
    override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> =
        tween(Wenku8Motion.Enter, easing = Wenku8Motion.Ease)
}
