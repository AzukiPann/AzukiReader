package io.legado.app.feature.bookshelf.archive

import kotlin.math.*

/** Port of Wenku8 ArchiveScrollSimulation: exponential coast followed by a finite cubic landing. */
internal class ArchiveShelfScrollSimulation(
    private val start: Float,
    velocity: Float,
    pitch: Float = 1f,
    minimumVelocity: Float = 50f,
    destination: Float? = null,
) {
    private val directional = abs(velocity) >= minimumVelocity && destination == null
    private val release = if (directional) velocity else 0f
    val target: Float
    private val coastDuration: Float
    private val handoffPosition: Float
    private val handoffVelocity: Float
    private val landingDuration: Float

    init {
        require(pitch > 0)
        var end = destination ?: (archiveLaneRound((start + release / 4.4f) / pitch) * pitch)
        if (directional && (end - start) * sign(release) < abs(release) / 12f) {
            val runway = (start + release / 12f) / pitch
            end = (if (release > 0) ceil(runway) else floor(runway)) * pitch
        }
        target = end
        coastDuration = if (directional && abs(release) > pitch * 4f) ln(abs(release) / (pitch * 4f)) / 4.4f else 0f
        handoffPosition = coast(coastDuration)
        handoffVelocity = release * exp(-4.4f * coastDuration)
        landingDuration = if (directional) 3f * (target - handoffPosition) / handoffVelocity else 0f
    }

    private fun coast(time: Float) = start + release / 4.4f * (1f - exp(-4.4f * time))
    fun position(time: Float): Float {
        if (landingDuration <= 0f) return target + (start - target) * (1f + 12f * time) * exp(-12f * time)
        if (time < coastDuration) return coast(time)
        val remaining = (1f - (time - coastDuration) / landingDuration).coerceIn(0f, 1f)
        return target - (target - handoffPosition) * remaining.pow(3)
    }
    fun velocity(time: Float): Float {
        if (landingDuration <= 0f) return -(start - target) * 144f * time * exp(-12f * time)
        if (time < coastDuration) return release * exp(-4.4f * time)
        val remaining = (1f - (time - coastDuration) / landingDuration).coerceIn(0f, 1f)
        return handoffVelocity * remaining * remaining
    }
    val duration: Float get() {
        if (landingDuration > 0f) return coastDuration + landingDuration
        var time = 0f
        while (time < 3f && (abs(position(time) - target) > .0001f || abs(velocity(time)) > .001f)) time += .016f
        return time.coerceAtLeast(.016f)
    }
}

internal fun archiveLaneRound(value: Float): Float = sign(value) * floor(abs(value) + .5f)
