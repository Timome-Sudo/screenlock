package com.timome.screenlock.ui.main

import android.content.Context
import com.timome.screenlock.R
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.timome.screenlock.data.ServiceMode
import com.timome.screenlock.data.SettingsDataStore
import com.timome.screenlock.service.FloatingBallService
import com.timome.screenlock.service.LockScreenService
import com.timome.screenlock.service.NotificationHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun LaunchScreen(
    settingsDataStore: SettingsDataStore,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val serviceMode by settingsDataStore.serviceMode.collectAsState(initial = ServiceMode.TIMER)
    val springDamping by settingsDataStore.springDampingRatio.collectAsState(
        initial = SettingsDataStore.DEFAULT_SPRING_DAMPING_RATIO
    )
    var isServiceRunning by remember { mutableStateOf(false) }
    var isTimerRunning by remember { mutableStateOf(false) }

    // 观察服务端状态，自动同步 UI
    val isCountdownActive by LockScreenService.countdownActive.collectAsState(initial = false)
    val isServiceRunningByService by LockScreenService.serviceRunning.collectAsState(initial = false)

    LaunchedEffect(isCountdownActive) {
        if (!isCountdownActive) isTimerRunning = false
    }

    LaunchedEffect(isServiceRunningByService) {
        if (!isServiceRunningByService) {
            isTimerRunning = false
            isServiceRunning = false
        }
    }

    val isAnyServiceRunning = isTimerRunning || isServiceRunning

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "启动",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "选择运作模式",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 模式选择 - 每种模式一个卡片
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModeCard(
                text = "定时模式",
                description = "在指定时间自动锁定屏幕",
                selected = serviceMode == ServiceMode.TIMER,
                enabled = !isAnyServiceRunning,
                springDamping = springDamping,
                onClick = {
                    scope.launch { settingsDataStore.setServiceMode(ServiceMode.TIMER) }
                }
            )

            ModeCard(
                text = "倒计时模式",
                description = "设置倒计时时长，到时自动锁定屏幕",
                selected = serviceMode == ServiceMode.COUNTDOWN,
                enabled = !isAnyServiceRunning,
                springDamping = springDamping,
                onClick = {
                    scope.launch { settingsDataStore.setServiceMode(ServiceMode.COUNTDOWN) }
                }
            )

            ModeCard(
                text = "通知模式",
                description = "通过通知启动锁屏服务",
                selected = serviceMode == ServiceMode.NOTIFICATION,
                enabled = !isAnyServiceRunning,
                springDamping = springDamping,
                onClick = {
                    scope.launch { settingsDataStore.setServiceMode(ServiceMode.NOTIFICATION) }
                }
            )

            ModeCard(
                text = "悬浮球模式",
                description = "使用悬浮球快速启动锁屏",
                selected = serviceMode == ServiceMode.FLOATING_BALL,
                enabled = !isAnyServiceRunning,
                springDamping = springDamping,
                onClick = {
                    scope.launch { settingsDataStore.setServiceMode(ServiceMode.FLOATING_BALL) }
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 根据模式显示不同的操作按钮
        when (serviceMode) {
            ServiceMode.TIMER -> {
                TimerModeButtons(
                    context = context,
                    settingsDataStore = settingsDataStore,
                    isTimerRunning = isTimerRunning,
                    onTimerStateChange = { isTimerRunning = it }
                )
            }
            ServiceMode.COUNTDOWN -> {
                CountdownModeButtons(
                    context = context,
                    settingsDataStore = settingsDataStore,
                    isTimerRunning = isTimerRunning,
                    onTimerStateChange = { isTimerRunning = it }
                )
            }
            ServiceMode.NOTIFICATION -> {
                NotificationModeButtons(
                    context = context,
                    isServiceRunning = isServiceRunning,
                    onServiceStateChange = { isServiceRunning = it }
                )
            }
            ServiceMode.FLOATING_BALL -> {
                FloatingBallModeButtons(
                    context = context,
                    settingsDataStore = settingsDataStore,
                    isServiceRunning = isServiceRunning,
                    onServiceStateChange = { isServiceRunning = it }
                )
            }
        }
    }
}

@Composable
private fun ModeCard(
    text: String,
    description: String,
    selected: Boolean,
    enabled: Boolean,
    springDamping: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val targetColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 300),
        label = "modeCardColor"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = springDamping,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "modeCardScale"
    )

    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale },
        colors = CardDefaults.cardColors(containerColor = animatedColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimerModeButtons(
    context: Context,
    settingsDataStore: SettingsDataStore,
    isTimerRunning: Boolean,
    onTimerStateChange: (Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    var showTimePicker by remember { mutableStateOf(false) }

    if (showTimePicker) {
        val now = Calendar.getInstance()
        now.add(Calendar.MINUTE, 2)
        val timePickerState = rememberTimePickerState(
            initialHour = now.get(Calendar.HOUR_OF_DAY),
            initialMinute = now.get(Calendar.MINUTE),
            is24Hour = true
        )
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Card {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "选择定时时间",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    TimePicker(state = timePickerState)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text("取消")
                        }
                        TextButton(onClick = {
                            val target = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                set(Calendar.MINUTE, timePickerState.minute)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            // 如果目标时间已过，设为明天
                            if (target.timeInMillis <= System.currentTimeMillis()) {
                                target.add(Calendar.DAY_OF_YEAR, 1)
                            }

                            scope.launch {
                                val longPressDuration =
                                    settingsDataStore.longPressDurationMs.first()
                                val positions = settingsDataStore.enabledPositions.first()
                                    .map { LockScreenService.Position.valueOf(it.name) }
                                    .toSet()

                                LockScreenService.startServiceAtTime(
                                    context,
                                    target.timeInMillis,
                                    longPressDuration.toLong(),
                                    positions
                                )
                                onTimerStateChange(true)
                            }
                            showTimePicker = false
                        }) {
                            Text("确定")
                        }
                    }
                }
            }
        }
    }

    if (!isTimerRunning) {
        Button(
            onClick = { showTimePicker = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "开始服务",
                style = MaterialTheme.typography.titleMedium
            )
        }
    } else {
        Button(
            onClick = {
                LockScreenService.cancelTimer(context)
                onTimerStateChange(false)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text(
                text = "取消定时",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun CountdownModeButtons(
    context: Context,
    settingsDataStore: SettingsDataStore,
    isTimerRunning: Boolean,
    onTimerStateChange: (Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    var hoursText by remember { mutableStateOf("") }
    var minutesText by remember { mutableStateOf("") }
    var secondsText by remember { mutableStateOf("10") }

    if (showDialog) {
        val parsedMinutes = minutesText.toIntOrNull()
        val parsedSeconds = secondsText.toIntOrNull()
        val minutesEmpty = minutesText.isEmpty()
        val secondsEmpty = secondsText.isEmpty()
        val minutesOutOfRange = !minutesEmpty && parsedMinutes !in 0..60
        val secondsOutOfRange = !secondsEmpty && parsedSeconds !in 1..60
        val minutesHasError = minutesOutOfRange
        val secondsHasError = secondsOutOfRange || secondsEmpty
        val confirmEnabled = !minutesHasError && !secondsHasError

        fun minutesErrorText() = when {
            minutesOutOfRange -> "范围为0-60"
            else -> ""
        }

        fun secondsErrorText() = when {
            secondsEmpty -> "不能为空"
            secondsOutOfRange -> "范围为1-60"
            else -> ""
        }

        AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false),
            title = {
                Text(
                    text = "设置倒计时",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BouncyOutlinedTextField(
                        value = hoursText,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= 3) {
                                hoursText = input
                            }
                        },
                        labelText = "小时",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    BouncyOutlinedTextField(
                        value = minutesText,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= 3) {
                                minutesText = input
                            }
                        },
                        labelText = "分钟",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        isError = minutesHasError,
                        trailingIcon = if (minutesHasError) {
                            {
                                Icon(
                                    painter = painterResource(R.drawable.ic_error),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        } else null,
                        supportingText = if (minutesHasError) {
                            { Text(minutesErrorText()) }
                        } else null
                    )
                    BouncyOutlinedTextField(
                        value = secondsText,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= 3) {
                                secondsText = input
                            }
                        },
                        labelText = "秒",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        isError = secondsHasError,
                        trailingIcon = if (secondsHasError) {
                            {
                                Icon(
                                    painter = painterResource(R.drawable.ic_error),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        } else null,
                        supportingText = if (secondsHasError) {
                            { Text(secondsErrorText()) }
                        } else null
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val hours = hoursText.toIntOrNull() ?: 0
                        val mins = minutesText.toIntOrNull() ?: 0
                        val secs = secondsText.toIntOrNull() ?: 0
                        val totalMs = (hours * 3600L + mins * 60L + secs) * 1000L
                        if (totalMs > 0) {
                            scope.launch {
                                val longPressDuration =
                                    settingsDataStore.longPressDurationMs.first()
                                val positions = settingsDataStore.enabledPositions.first()
                                    .map { LockScreenService.Position.valueOf(it.name) }
                                    .toSet()
                                LockScreenService.startService(
                                    context,
                                    totalMs,
                                    longPressDuration.toLong(),
                                    positions
                                )
                                onTimerStateChange(true)
                            }
                        }
                        showDialog = false
                    },
                    enabled = confirmEnabled
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (!isTimerRunning) {
        Button(
            onClick = { showDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "开始服务",
                style = MaterialTheme.typography.titleMedium
            )
        }
    } else {
        Button(
            onClick = {
                LockScreenService.cancelTimer(context)
                onTimerStateChange(false)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text(
                text = "取消倒计时",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun NotificationModeButtons(
    context: Context,
    isServiceRunning: Boolean,
    onServiceStateChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!isServiceRunning) {
            Button(
                onClick = {
                    NotificationHelper.createChannels(context)
                    val notification = NotificationHelper.createNotificationModeNotification(context)
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                            as android.app.NotificationManager
                    notificationManager.notify(
                        NotificationHelper.NOTIFICATION_ID_MODE,
                        notification
                    )
                    onServiceStateChange(true)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "启动服务",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        } else {
            Button(
                onClick = {
                    NotificationHelper.cancelNotificationModeNotification(context)
                    onServiceStateChange(false)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(
                    text = "关闭服务",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun FloatingBallModeButtons(
    context: Context,
    settingsDataStore: SettingsDataStore,
    isServiceRunning: Boolean,
    onServiceStateChange: (Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!isServiceRunning) {
            Button(
                onClick = {
                    scope.launch {
                        val longPressDuration = settingsDataStore.longPressDurationMs.first()
                        val positions = settingsDataStore.enabledPositions.first()
                            .map { LockScreenService.Position.valueOf(it.name) }
                            .toSet()

                        FloatingBallService.startService(
                            context,
                            longPressDuration.toLong(),
                            positions
                        )
                        onServiceStateChange(true)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "启动服务",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        } else {
            Button(
                onClick = {
                    FloatingBallService.stopService(context)
                    onServiceStateChange(false)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(
                    text = "关闭悬浮球",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

/**
 * OutlinedTextField with a spring-bounce animation on the floating label.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BouncyOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    labelText: String,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = false,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val isFocused by interactionSource.collectIsFocusedAsState()
    var labelBounceTarget by remember(isFocused) { mutableStateOf(1.08f) }
    val labelBounceScale by animateFloatAsState(
        targetValue = labelBounceTarget,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "labelBounceScale"
    )
    LaunchedEffect(labelBounceTarget) {
        if (labelBounceTarget > 1f) {
            labelBounceTarget = 1f
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = {
            Text(
                text = labelText,
                modifier = Modifier.graphicsLayer {
                    scaleX = labelBounceScale
                    scaleY = labelBounceScale
                }
            )
        },
        enabled = enabled,
        readOnly = readOnly,
        singleLine = singleLine,
        isError = isError,
        keyboardOptions = keyboardOptions,
        trailingIcon = trailingIcon,
        supportingText = supportingText,
        interactionSource = interactionSource
    )
}
