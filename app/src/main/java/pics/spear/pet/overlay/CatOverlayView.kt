package pics.spear.pet.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import pics.spear.pet.overlay.skin.SkinAnimator
import pics.spear.pet.overlay.skin.SkinPack
import pics.spear.pet.overlay.skin.SkinPackParser

class CatOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    val skinPack: SkinPack = SkinPackParser.load(context)
    private val animator = SkinAnimator(skinPack)
    private val floorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x22000000
        style = Paint.Style.FILL
    }
    private val backdropPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x05000000
        style = Paint.Style.FILL
    }
    private val framePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    fun setMotion(name: String) {
        animator.setMotion(name)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat().coerceAtLeast(1f)
        val h = height.toFloat().coerceAtLeast(1f)
        val frame = animator.currentFrame() ?: return
        val bitmap = frame.bitmap
        val dest = RectF(
            (w - bitmap.width) * 0.5f,
            h - bitmap.height.toFloat(),
            (w + bitmap.width) * 0.5f,
            h.toFloat()
        )

        canvas.drawRoundRect(
            RectF(w * 0.16f, h * 0.82f, w * 0.84f, h * 0.90f),
            h * 0.04f,
            h * 0.04f,
            floorPaint
        )
        canvas.drawRoundRect(
            RectF(w * 0.08f, h * 0.12f, w * 0.92f, h * 0.88f),
            h * 0.08f,
            h * 0.08f,
            backdropPaint
        )
        canvas.drawBitmap(bitmap, null, dest, framePaint)
        postInvalidateOnAnimation()
    }
}
