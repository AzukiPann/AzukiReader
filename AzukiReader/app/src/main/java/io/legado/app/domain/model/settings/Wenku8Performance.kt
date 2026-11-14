package io.legado.app.domain.model.settings

/** The lightweight preset overlays preferences without overwriting the normal appearance. */
enum class Wenku8PerformanceEffect(val bit: Int) {
    Glass(1), Blur(2), EdgeBlur(4), Motion(8), Shelf3d(16);
}

fun ThemeSettings.allows(effect: Wenku8PerformanceEffect): Boolean =
    !wenku8LowPerformance || wenku8RetainedEffects and effect.bit != 0

fun ThemeSettings.retain(effect: Wenku8PerformanceEffect, enabled: Boolean): ThemeSettings = copy(
    wenku8RetainedEffects = if (enabled) wenku8RetainedEffects or effect.bit
    else wenku8RetainedEffects and effect.bit.inv(),
)
