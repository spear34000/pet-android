package pics.spear.pet.overlay.skin

import android.os.SystemClock

class SkinAnimator(private val skinPack: SkinPack) {
    private var motionName: String = skinPack.motions.keys.firstOrNull() ?: "idle"
    private var motionStartMs: Long = SystemClock.uptimeMillis()

    fun setMotion(name: String) {
        if (motionName == name) return
        motionName = if (skinPack.motions.containsKey(name)) name else skinPack.motions.keys.firstOrNull() ?: name
        motionStartMs = SystemClock.uptimeMillis()
    }

    fun currentFrame(): SkinFrame? {
        val frames = skinPack.motions[motionName]?.frames ?: return null
        if (frames.isEmpty()) return null
        if (frames.size == 1) return frames.first()

        val elapsed = SystemClock.uptimeMillis() - motionStartMs
        var cursor = elapsed
        for (frame in frames) {
            if (cursor < frame.durationMs) return frame
            cursor -= frame.durationMs
        }
        return frames.last()
    }
}
