package pics.spear.pet.overlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import pics.spear.pet.MainActivity
import pics.spear.pet.R

class CatOverlayService : Service() {
    companion object {
        const val ACTION_START = "pics.spear.pet.action.START_OVERLAY"
        const val ACTION_STOP = "pics.spear.pet.action.STOP_OVERLAY"
        private const val CHANNEL_ID = "pet_overlay"
        private const val NOTIFICATION_ID = 1001
    }

    private lateinit var windowManager: WindowManager
    private var overlayView: CatOverlayView? = null
    private var params: WindowManager.LayoutParams? = null
    private val handler = Handler(Looper.getMainLooper())

    private var dragOffsetX = 0f
    private var dragOffsetY = 0f
    private var screenWidth = 1
    private var screenHeight = 1

    private val tickRunnable = object : Runnable {
        override fun run() {
            overlayView?.invalidate()
            handler.postDelayed(this, 16L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        updateScreenBounds()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopOverlay()
                stopSelf()
            }
            else -> startOverlay()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopOverlay()
        super.onDestroy()
    }

    private fun startOverlay() {
        if (overlayView != null) return
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        updateScreenBounds()
        val previewView = CatOverlayView(this)
        val width = previewView.skinPack.width.coerceAtLeast(180)
        val height = previewView.skinPack.height.coerceAtLeast(150)
        val startX = (screenWidth / 2f - width / 2f).toInt().coerceAtLeast(0)
        val startY = (screenHeight / 2f - height / 2f).toInt().coerceAtLeast(0)

        val view = previewView
        val layoutParams = WindowManager.LayoutParams(
            width,
            height,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = startX
            y = startY
        }

        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    dragOffsetX = event.rawX - layoutParams.x
                    dragOffsetY = event.rawY - layoutParams.y
                    view.setMotion("walk")
                    true
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    moveOverlay(event.rawX, event.rawY)
                    true
                }
                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL -> {
                    finishDrag()
                    true
                }
                else -> false
            }
        }

        windowManager.addView(view, layoutParams)
        overlayView = view
        params = layoutParams
        handler.post(tickRunnable)
    }

    private fun stopOverlay() {
        handler.removeCallbacks(tickRunnable)
        overlayView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        overlayView = null
        params = null
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun moveOverlay(rawX: Float, rawY: Float) {
        val view = overlayView ?: return
        val layoutParams = params ?: return
        val newX = (rawX - dragOffsetX).toInt().coerceIn(0, (screenWidth - layoutParams.width).coerceAtLeast(0))
        val newY = (rawY - dragOffsetY).toInt().coerceIn(0, (screenHeight - layoutParams.height).coerceAtLeast(0))
        layoutParams.x = newX
        layoutParams.y = newY
        runCatching { windowManager.updateViewLayout(view, layoutParams) }
    }

    private fun finishDrag() {
        overlayView?.setMotion("idle")
    }

    private fun updateScreenBounds() {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(metrics)
        screenWidth = metrics.widthPixels.coerceAtLeast(1)
        screenHeight = metrics.heightPixels.coerceAtLeast(1)
    }

    private fun overlayType(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
        @Suppress("DEPRECATION")
        WindowManager.LayoutParams.TYPE_PHONE
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.app_name),
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(getString(R.string.app_name))
        .setContentText("Cat overlay is running")
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
        )
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .build()
}
