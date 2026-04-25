package com.timome.screenlock.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.timome.screenlock.MainActivity
import com.timome.screenlock.R
import com.timome.screenlock.data.SettingsDataStore
import com.timome.screenlock.ui.theme.ScreenlockTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LockScreenService : Service() {

    private val binder = LocalBinder()
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var progressBarView: View? = null
    private var progressBarLifecycleOwner: ServiceLifecycleOwner? = null
    private val progressValue = mutableStateOf(0f)
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private val _remainingTimeMs = MutableStateFlow(0L)
    val remainingTimeMs: StateFlow<Long> = _remainingTimeMs

    private var timerJob: Job? = null
    private var longPressJob: Job? = null
    private var settingsJob: Job? = null

    private val _longPressDurationMs = MutableStateFlow(3000L)
    private var longPressDurationMs: StateFlow<Long> = _longPressDurationMs

    private val _enabledPositions = MutableStateFlow<Set<Position>>(setOf(Position.TOP_LEFT, Position.TOP_RIGHT))
    private var enabledPositions: StateFlow<Set<Position>> = _enabledPositions

    // 同时长按追踪
    private val activeTouches = mutableMapOf<Int, Position>()
    private var isLongPressing = false

    enum class Position {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    inner class LocalBinder : Binder() {
        fun getService(): LockScreenService = this@LockScreenService
    }

    private class ServiceLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateRegistryController = SavedStateRegistryController.create(this)

        override val lifecycle: Lifecycle
            get() = lifecycleRegistry

        override val savedStateRegistry: SavedStateRegistry
            get() = savedStateRegistryController.savedStateRegistry

        fun create() {
            savedStateRegistryController.performRestore(null)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        }

        fun resume() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        fun destroy() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
    }

    companion object {
        const val CHANNEL_ID = "lock_screen_service_channel"
        const val CHANNEL_NAME = "锁屏服务"
        const val NOTIFICATION_ID = 1

        const val ACTION_START = "com.timome.screenlock.ACTION_START"
        const val ACTION_STOP = "com.timome.screenlock.ACTION_STOP"
        const val ACTION_CANCEL_TIMER = "com.timome.screenlock.ACTION_CANCEL_TIMER"

        const val EXTRA_TIMER_DURATION = "timer_duration"
        const val EXTRA_TARGET_TIME_MILLIS = "target_time_millis"
        const val EXTRA_LONG_PRESS_DURATION = "long_press_duration"
        const val EXTRA_POSITIONS = "positions"

        private val _serviceRunning = MutableStateFlow(false)
        val serviceRunning: StateFlow<Boolean> = _serviceRunning

        private val _countdownActive = MutableStateFlow(false)
        val countdownActive: StateFlow<Boolean> = _countdownActive

        fun startService(
            context: Context,
            timerDurationMs: Long = 0,
            longPressDurationMs: Long = 3000,
            positions: Set<Position> = setOf(Position.TOP_LEFT, Position.TOP_RIGHT)
        ) {
            val intent = Intent(context, LockScreenService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TIMER_DURATION, timerDurationMs)
                putExtra(EXTRA_LONG_PRESS_DURATION, longPressDurationMs)
                putExtra(EXTRA_POSITIONS, positions.map { it.name }.toTypedArray())
            }
            context.startService(intent)
        }

        fun startServiceAtTime(
            context: Context,
            targetTimeMillis: Long,
            longPressDurationMs: Long = 3000,
            positions: Set<Position> = setOf(Position.TOP_LEFT, Position.TOP_RIGHT)
        ) {
            val intent = Intent(context, LockScreenService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TARGET_TIME_MILLIS, targetTimeMillis)
                putExtra(EXTRA_LONG_PRESS_DURATION, longPressDurationMs)
                putExtra(EXTRA_POSITIONS, positions.map { it.name }.toTypedArray())
            }
            context.startService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, LockScreenService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun cancelTimer(context: Context) {
            val intent = Intent(context, LockScreenService::class.java).apply {
                action = ACTION_CANCEL_TIMER
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val timerDuration = intent.getLongExtra(EXTRA_TIMER_DURATION, 0)
                val targetTimeMillis = intent.getLongExtra(EXTRA_TARGET_TIME_MILLIS, 0)
                val longPressDuration = intent.getLongExtra(EXTRA_LONG_PRESS_DURATION, 3000)
                val positionNames = intent.getStringArrayExtra(EXTRA_POSITIONS)
                val positions = positionNames?.map { Position.valueOf(it) }?.toSet()
                    ?: setOf(Position.TOP_LEFT, Position.TOP_RIGHT)

                _longPressDurationMs.value = longPressDuration
                _enabledPositions.value = positions

                startSettingsObserver()
                startLockScreen(timerDuration, targetTimeMillis)
            }
            ACTION_STOP -> {
                stopLockScreen()
            }
            ACTION_CANCEL_TIMER -> {
                cancelTimer()
            }
        }
        return START_STICKY
    }

    private fun startSettingsObserver() {
        settingsJob?.cancel()
        val settingsDataStore = SettingsDataStore(this)
        settingsJob = serviceScope.launch {
            launch {
                settingsDataStore.enabledPositions.collect { positions ->
                    _enabledPositions.value = positions.map { Position.valueOf(it.name) }.toSet()
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

    private fun createNotification(remainingTime: Long = 0): Notification {
        val contentText = if (remainingTime > 0) {
            val minutes = remainingTime / 60000
            val seconds = (remainingTime % 60000) / 1000
            "剩余时间: ${minutes}分${seconds}秒"
        } else {
            "锁屏服务运行中"
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, LockScreenService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("屏幕锁")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(0, "停止服务", stopPendingIntent)
            .build()
    }

    private fun startLockScreen(timerDurationMs: Long, targetTimeMillis: Long = 0) {
        _isRunning.value = true
        _serviceRunning.value = true

        val initialRemaining = when {
            targetTimeMillis > 0 -> maxOf(0L, targetTimeMillis - System.currentTimeMillis())
            timerDurationMs > 0 -> timerDurationMs
            else -> 0L
        }
        startForeground(NOTIFICATION_ID, createNotification(initialRemaining))

        if (targetTimeMillis > 0) {
            _countdownActive.value = true
            startTargetTimeTimer(targetTimeMillis)
        } else if (timerDurationMs > 0) {
            _countdownActive.value = true
            startCountdownTimer(timerDurationMs)
        } else {
            showOverlay()
        }
    }

    private fun startCountdownTimer(durationMs: Long) {
        _remainingTimeMs.value = durationMs
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (_remainingTimeMs.value > 0) {
                delay(1000)
                _remainingTimeMs.value -= 1000
                updateNotification()
            }
            _countdownActive.value = false
            showOverlay()
        }
    }

    private fun startTargetTimeTimer(targetTimeMillis: Long) {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (true) {
                val remaining = targetTimeMillis - System.currentTimeMillis()
                if (remaining <= 0) {
                    _remainingTimeMs.value = 0
                    updateNotification()
                    break
                }
                _remainingTimeMs.value = remaining
                updateNotification()
                delay(1000)
            }
            _countdownActive.value = false
            showOverlay()
        }
    }

    private fun cancelTimer() {
        timerJob?.cancel()
        _remainingTimeMs.value = 0
        stopLockScreen()
    }

    private fun updateNotification() {
        val notification = createNotification(_remainingTimeMs.value)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    // ========== 覆盖层与触摸 ==========

    private fun showOverlay() {
        if (overlayView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        overlayView = View(this).apply {
            setBackgroundColor(0x00000000) // 完全透明
            setOnTouchListener { _, event ->
                handleTouch(event)
                true
            }
        }

        windowManager.addView(overlayView, params)
    }

    private fun handleTouch(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                val x = event.getX(pointerIndex)
                val y = event.getY(pointerIndex)

                val position = detectPosition(x, y)
                if (position != null && enabledPositions.value.contains(position)) {
                    activeTouches[pointerId] = position
                    checkSimultaneousLongPress()
                } else {
                    // 点击非解锁区域，提示已锁定
                    android.widget.Toast.makeText(this, "屏幕已被锁定！", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                activeTouches.remove(pointerId)

                if (activeTouches.size < enabledPositions.value.size) {
                    cancelLongPress()
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                activeTouches.clear()
                cancelLongPress()
            }
        }
    }

    private fun detectPosition(x: Float, y: Float): Position? {
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        // 100dp ≈ 2.5cm，允许几厘米偏差
        val cornerSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 100f, resources.displayMetrics
        ).toInt()

        return when {
            x < cornerSize && y < cornerSize -> Position.TOP_LEFT
            x > screenWidth - cornerSize && y < cornerSize -> Position.TOP_RIGHT
            x < cornerSize && y > screenHeight - cornerSize -> Position.BOTTOM_LEFT
            x > screenWidth - cornerSize && y > screenHeight - cornerSize -> Position.BOTTOM_RIGHT
            else -> null
        }
    }

    private fun checkSimultaneousLongPress() {
        val pressedPositions = activeTouches.values.toSet()
        val allPositionsPressed = enabledPositions.value.all { it in pressedPositions }

        if (allPositionsPressed && !isLongPressing) {
            startLongPress()
        }
    }

    private fun startLongPress() {
        isLongPressing = true
        showProgressBar()
        longPressJob?.cancel()
        longPressJob = serviceScope.launch {
            val startTime = System.currentTimeMillis()
            val duration = longPressDurationMs.value
            while (System.currentTimeMillis() - startTime < duration) {
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed * 100 / duration).toInt()
                progressValue.value = progress / 100f
                delay(16) // ~60fps
            }
            // 长按完成，停止服务
            vibrate()
            stopLockScreen()
        }
    }

    private fun cancelLongPress() {
        isLongPressing = false
        longPressJob?.cancel()
        hideProgressBar()
    }

    // ========== 进度条 ==========

    private fun showProgressBar() {
        if (progressBarView != null) return

        val size = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 96f, resources.displayMetrics
        ).toInt()

        val params = WindowManager.LayoutParams(
            size, size,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val lifecycleOwner = ServiceLifecycleOwner()
        lifecycleOwner.create()

        val composeView = ComposeView(this)
        composeView.setViewTreeLifecycleOwner(lifecycleOwner)
        composeView.setViewTreeSavedStateRegistryOwner(lifecycleOwner)

        composeView.setContent {
            ScreenlockTheme {
                CircularProgressIndicator(
                    progress = { progressValue.value },
                    modifier = Modifier.size(96.dp),
                )
            }
        }

        lifecycleOwner.resume()
        progressBarLifecycleOwner = lifecycleOwner
        progressValue.value = 0f
        windowManager.addView(composeView, params)
        progressBarView = composeView
    }

    private fun hideProgressBar() {
        progressBarView?.let {
            progressBarLifecycleOwner?.destroy()
            progressBarLifecycleOwner = null
            windowManager.removeView(it)
            progressBarView = null
        }
    }

    private fun vibrate() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(200)
        }
    }

    private fun stopLockScreen() {
        _isRunning.value = false
        _serviceRunning.value = false
        _countdownActive.value = false
        timerJob?.cancel()
        settingsJob?.cancel()
        cancelLongPress()
        activeTouches.clear()
        hideProgressBar()
        overlayView?.let {
            windowManager.removeView(it)
            overlayView = null
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        stopLockScreen()
    }
}
