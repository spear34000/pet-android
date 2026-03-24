package pics.spear.pet.overlay.skin

import android.graphics.Bitmap

data class SkinPack(
    val name: String,
    val width: Int,
    val height: Int,
    val hotspotX: Int,
    val hotspotY: Int,
    val motions: Map<String, SkinMotion>,
)

data class SkinMotion(
    val name: String,
    val frames: List<SkinFrame>,
)

data class SkinFrame(
    val bitmap: Bitmap,
    val durationMs: Long,
)
