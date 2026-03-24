package pics.spear.pet.overlay.skin

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Xml
import androidx.core.content.ContextCompat
import java.io.IOException

object SkinPackParser {
    fun load(context: Context, assetPath: String = "aneko/cat/skin.xml"): SkinPack {
        context.assets.open(assetPath).use { input ->
            val parser = Xml.newPullParser()
            parser.setInput(input, Charsets.UTF_8.name())

            var name = "default"
            var width = 320
            var height = 240
            var hotspotX = width / 2
            var hotspotY = (height * 0.72f).toInt()
            val motions = linkedMapOf<String, MutableList<SkinFrame>>()
            var currentMotion = "idle"

            var eventType = parser.eventType
            while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    org.xmlpull.v1.XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "skin" -> {
                                name = parser.getAttributeValue(null, "name") ?: name
                                width = parser.attrInt("width", width)
                                height = parser.attrInt("height", height)
                                hotspotX = parser.attrInt("hotspotX", hotspotX)
                                hotspotY = parser.attrInt("hotspotY", hotspotY)
                            }
                            "motion" -> {
                                currentMotion = parser.getAttributeValue(null, "name") ?: "idle"
                                motions.getOrPut(currentMotion) { mutableListOf() }
                            }
                            "frame" -> {
                                val src = parser.getAttributeValue(null, "src")
                                    ?: throw IOException("Missing frame src in $assetPath")
                                val durationMs = parser.attrLong("durationMs", 120L)
                                val frame = loadFrame(context, assetPath, src, durationMs, width, height)
                                motions.getOrPut(currentMotion) { mutableListOf() }.add(frame)
                            }
                        }
                    }
                }
                eventType = parser.next()
            }

            return SkinPack(
                name = name,
                width = width,
                height = height,
                hotspotX = hotspotX,
                hotspotY = hotspotY,
                motions = motions.mapValues { (motionName, frames) ->
                    SkinMotion(name = motionName, frames = frames.toList())
                }
            )
        }
    }

    private fun loadFrame(
        context: Context,
        skinAssetPath: String,
        fileName: String,
        durationMs: Long,
        targetWidth: Int,
        targetHeight: Int,
    ): SkinFrame {
        val drawableName = fileName.substringAfterLast('/')
        val drawableId = context.resources.getIdentifier(drawableName, "drawable", context.packageName)
        if (drawableId == 0) throw IOException("Missing drawable resource $drawableName")
        val bitmap = drawableToBitmap(ContextCompat.getDrawable(context, drawableId), targetWidth, targetHeight)
        return SkinFrame(bitmap = bitmap, durationMs = durationMs)
    }

    private fun drawableToBitmap(drawable: android.graphics.drawable.Drawable?, targetWidth: Int, targetHeight: Int): Bitmap {
        val safeDrawable = drawable ?: throw IOException("Unable to load drawable")
        val width = targetWidth.coerceAtLeast(1)
        val height = targetHeight.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        safeDrawable.setBounds(0, 0, width, height)
        safeDrawable.draw(canvas)
        return bitmap
    }

    private fun org.xmlpull.v1.XmlPullParser.attrInt(name: String, default: Int): Int =
        getAttributeValue(null, name)?.toIntOrNull() ?: default

    private fun org.xmlpull.v1.XmlPullParser.attrLong(name: String, default: Long): Long =
        getAttributeValue(null, name)?.toLongOrNull() ?: default
}
