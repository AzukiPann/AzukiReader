package io.legado.app.core.designsystem.wenku8.chrome

import androidx.compose.animation.core.CubicBezierEasing

/** Timings and spring converted from Wenku8's NovelMotion, in logical pixels. */
object Wenku8Motion {
    const val Page = 200
    const val Contact = 120
    const val Enter = 420
    const val Exit = 320
    // Flutter: mass .64, stiffness 270, damping 19. Compose assumes unit mass.
    const val SpringStiffness = 421.875f
    const val SpringDamping = .723f
    val Ease = CubicBezierEasing(.215f, .61f, .355f, 1f)
    val Retract = CubicBezierEasing(.55f, .055f, .675f, .19f)
}
