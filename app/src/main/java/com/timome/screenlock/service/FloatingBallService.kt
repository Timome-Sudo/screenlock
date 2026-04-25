package com.timome.screenlock.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.timome.screenlock.MainActivity
import com.timome.screenlock.R
import com.timome.screenlock.data.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class FloatingBallService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingBallView: View? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var settingsJob: Job? = null

    private val _longPressDurationMs = MutableStateFlow(3000L)
    private val _positions = MutableStateFlow<Set<LockScreenService.Position>>(
        setOf(LockScreenService.Position.TOP_LEFT, LockScreenService.Position.TOP_RIGHT)
    )

    companion object {
        const val CHANNEL_ID = "floating_ball_service_channel"
        const val CHANNEL_NAME = "悬浮球服务"
        const val NOTIFICATION_ID = 2

        const val ACTION_START = "com.timome.screenlock.ACTION_START_BALL"
        const val ACTION_STOP = "com.timome.screenlock.ACTION_STOP_BALL"

        const val EXTRA_LONG_PRESS_DURATION = "long_press_duration"
        const val EXTRA_POSITIONS = "positions"

        fun startService(
            context: Context,
            longPressDurationMs: Long = 3000,
            positions: Set<LockScreenService.Position> = setOf(
                LockScreenService.Position.TOP_LEFT,
                LockScreenService.Position.TOP_RIGHT
            )
        ) {
            val intent = Intent(context, FloatingBallService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_LONG_PRESS_DURATION, longPressDurationMs)
                putExtra(EXTRA_POSITIONS, positions.map { it.name }.toTypedArray())
            }
            context.startService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FloatingBallService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val longPressDuration = intent.getLongExtra(EXTRA_LONG_PRESS_DURATION, 3000)
                val positionNames = intent.getStringArrayExtra(EXTRA_POSITIONS)
                val positions = positionNames?.map { LockScreenService.Position.valueOf(it) }?.toSet()
                    ?: setOf(LockScreenService.Position.TOP_LEFT, LockScreenService.Position.TOP_RIGHT)

                _longPressDurationMs.value = longPressDuration
                _positions.value = positions

                startSettingsObserver()
                startFloatingBall()
            }
            ACTION_STOP -> {
                stopFloatingBall()
            }
        }
        return START_STICKY
    }

    private fun startSettingsObserver() {
        settingsJob?.cancel()
        val settingsDataStore = SettingsDataStore(this)
        settingsJob = serviceScope.launch {
            launch {
                settingsDataStore.enabledPositions.collect { newPositions ->
                    _positions.value = newPositions.map { LockScreenService.Position.valueOf(it.name) }.toSet()
                }
            }
            launch {
                settingsDataStore.longPressDurationMs.collect { duration ->
                    _longPressDurationMs.value = duration.toLong()
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setSound(null, null)
                enableVibration(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, FloatingBallService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("屏幕锁 - 悬浮球")
            .setContentText("点击悬浮球启动锁屏")
            .setSmallIcon(R.drawable.ic_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(0, "关闭悬浮球", stopPendingIntent)
            .build()
    }

    private fun startFloatingBall() {
        startForeground(NOTIFICATION_ID, createNotification())
        showFloatingBall()
    }

    private fun showFloatingBall() {
        if (floatingBallView != null) return

        val params = WindowManager.LayoutParams(
            100,
            100,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        val inflater = LayoutInflater.from(this)
        floatingBallView = inflater.inflate(R.layout.layout_floating_ball, null)

        // 获取应用主题色并设置到锁图标
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
        val primaryColor = typedValue.data
        floatingBallView?.findViewById<android.widget.ImageView>(R.id.lockIcon)?.setColorFilter(primaryColor)

        floatingBallView?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(floatingBallView, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        // 点击悬浮球，启动锁屏服务
                        LockScreenService.startService(
                            this@FloatingBallService,
                            0,
                            _longPressDurationMs.value,
                            _positions.value
                        )
                        stopFloatingBall()
                        return true
                    }
                }
                return false
            }
        })

        windowManager.addView(floatingBallView, params)
    }

    private fun stopFloatingBall() {
        settingsJob?.cancel()
        floatingBallView?.let {
            windowManager.removeView(it)
            floatingBallView = null
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        stopFloatingBall()
    }
}
