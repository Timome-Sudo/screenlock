package com.timome.screenlock.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.timome.screenlock.R
import com.timome.screenlock.data.LongPressPosition
import com.timome.screenlock.data.SettingsDataStore
import kotlinx.coroutines.launch

// ========== 导航状态 ==========
private sealed class SettingsRoute {
    data object Main : SettingsRoute()
    data object LongPress : SettingsRoute()
    data object Spring : SettingsRoute()
    data object About : SettingsRoute()
}

@Composable
fun SettingsScreen(
    settingsDataStore: SettingsDataStore,
    modifier: Modifier = Modifier
) {
    var currentRoute by remember { mutableStateOf<SettingsRoute>(SettingsRoute.Main) }

    when (currentRoute) {
        is SettingsRoute.Main -> SettingsMainMenu(
            settingsDataStore = settingsDataStore,
            onNavigate = { currentRoute = it },
            modifier = modifier
        )
        is SettingsRoute.LongPress -> LongPressSettings(
            settingsDataStore = settingsDataStore,
            onBack = { currentRoute = SettingsRoute.Main },
            modifier = modifier
        )
        is SettingsRoute.Spring -> SpringSettings(
            settingsDataStore = settingsDataStore,
            onBack = { currentRoute = SettingsRoute.Main },
            modifier = modifier
        )
        is SettingsRoute.About -> AboutPage(
            onBack = { currentRoute = SettingsRoute.Main },
            modifier = modifier
        )
    }
}

// ========== 主菜单 ==========
@Composable
private fun SettingsMainMenu(
    settingsDataStore: SettingsDataStore,
    onNavigate: (SettingsRoute) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val positionTopLeft by settingsDataStore.positionTopLeft.collectAsState(initial = true)
    val positionTopRight by settingsDataStore.positionTopRight.collectAsState(initial = true)
    val positionBottomLeft by settingsDataStore.positionBottomLeft.collectAsState(initial = false)
    val positionBottomRight by settingsDataStore.positionBottomRight.collectAsState(initial = false)
    val longPressDuration by settingsDataStore.longPressDurationMs.collectAsState(
        initial = SettingsDataStore.DEFAULT_LONG_PRESS_DURATION_MS
    )
    val springDamping by settingsDataStore.springDampingRatio.collectAsState(
        initial = SettingsDataStore.DEFAULT_SPRING_DAMPING_RATIO
    )

    // 构建子标题
    val longPressSubtitle = buildString {
        val positions = mutableListOf<String>()
        if (positionTopLeft) positions.add("左上")
        if (positionTopRight) positions.add("右上")
        if (positionBottomLeft) positions.add("左下")
        if (positionBottomRight) positions.add("右下")
        append(positions.joinToString(", "))
        append(" · ")
        append((longPressDuration / 1000).toInt())
        append("秒")
    }

    val springSubtitle = "回弹力度: ${springDampingLabel(springDamping)}"

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "调整",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "自定义锁屏设置",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        SettingCategoryCard(
            iconRes = R.drawable.ic_touch,
            title = "长按设置",
            subtitle = longPressSubtitle,
            onClick = { onNavigate(SettingsRoute.LongPress) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingCategoryCard(
            iconRes = R.drawable.ic_spring,
            title = "弹簧动画",
            subtitle = springSubtitle,
            onClick = { onNavigate(SettingsRoute.Spring) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingCategoryCard(
            iconRes = R.drawable.ic_info,
            title = "关于",
            subtitle = "版本 1.0",
            onClick = { onNavigate(SettingsRoute.About) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingCategoryCard(
    iconRes: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.primaryContainer,
        animationSpec = tween(durationMillis = 300),
        label = "categoryCardColor"
    )
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "categoryCardScale"
    )

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale },
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }
        }
    }
}

// ========== 长按设置 ==========
@Composable
private fun LongPressSettings(
    settingsDataStore: SettingsDataStore,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val positionTopLeft by settingsDataStore.positionTopLeft.collectAsState(initial = true)
    val positionTopRight by settingsDataStore.positionTopRight.collectAsState(initial = true)
    val positionBottomLeft by settingsDataStore.positionBottomLeft.collectAsState(initial = false)
    val positionBottomRight by settingsDataStore.positionBottomRight.collectAsState(initial = false)
    val longPressDuration by settingsDataStore.longPressDurationMs.collectAsState(
        initial = SettingsDataStore.DEFAULT_LONG_PRESS_DURATION_MS
    )

    val enabledCount = listOf(
        positionTopLeft, positionTopRight,
        positionBottomLeft, positionBottomRight
    ).count { it }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        TextButton(onClick = onBack) {
            Text("< 返回")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "长按设置",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "选择1-2个角落作为解锁位置（需同时长按）",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        PositionCard(
            title = "左上角",
            description = "屏幕左上角区域",
            isEnabled = positionTopLeft,
            onToggle = { enabled ->
                scope.launch { settingsDataStore.setPosition(LongPressPosition.TOP_LEFT, enabled) }
            },
            isSwitchEnabled = when {
                enabledCount >= 2 && !positionTopLeft -> false
                enabledCount == 1 && positionTopLeft -> false
                else -> true
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        PositionCard(
            title = "右上角",
            description = "屏幕右上角区域",
            isEnabled = positionTopRight,
            onToggle = { enabled ->
                scope.launch { settingsDataStore.setPosition(LongPressPosition.TOP_RIGHT, enabled) }
            },
            isSwitchEnabled = when {
                enabledCount >= 2 && !positionTopRight -> false
                enabledCount == 1 && positionTopRight -> false
                else -> true
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        PositionCard(
            title = "左下角",
            description = "屏幕左下角区域",
            isEnabled = positionBottomLeft,
            onToggle = { enabled ->
                scope.launch { settingsDataStore.setPosition(LongPressPosition.BOTTOM_LEFT, enabled) }
            },
            isSwitchEnabled = when {
                enabledCount >= 2 && !positionBottomLeft -> false
                enabledCount == 1 && positionBottomLeft -> false
                else -> true
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        PositionCard(
            title = "右下角",
            description = "屏幕右下角区域",
            isEnabled = positionBottomRight,
            onToggle = { enabled ->
                scope.launch { settingsDataStore.setPosition(LongPressPosition.BOTTOM_RIGHT, enabled) }
            },
            isSwitchEnabled = when {
                enabledCount >= 2 && !positionBottomRight -> false
                enabledCount == 1 && positionBottomRight -> false
                else -> true
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "长按时间",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "设置解锁需要长按的时间（秒）",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        var durationText by remember(longPressDuration) {
            mutableStateOf((longPressDuration / 1000).toString())
        }

        BouncyOutlinedTextField(
            value = durationText,
            onValueChange = { newValue ->
                if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                    durationText = newValue
                    newValue.toFloatOrNull()?.let { seconds ->
                        if (seconds > 0) {
                            scope.launch {
                                settingsDataStore.setLongPressDurationMs(seconds * 1000)
                            }
                        }
                    }
                }
            },
            labelText = "长按时间（秒）",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

// ========== 弹簧动画设置 ==========
@Composable
private fun SpringSettings(
    settingsDataStore: SettingsDataStore,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val springDamping by settingsDataStore.springDampingRatio.collectAsState(
        initial = SettingsDataStore.DEFAULT_SPRING_DAMPING_RATIO
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("< 返回")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "弹簧动画",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "调整全局弹簧回弹力度",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "回弹力度",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("强", style = MaterialTheme.typography.bodySmall)
            Slider(
                value = springDamping,
                onValueChange = {
                    scope.launch { settingsDataStore.setSpringDampingRatio(it) }
                },
                valueRange = 0.1f..1.0f,
                steps = 8,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )
            Text("弱", style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "当前: ${springDampingLabel(springDamping)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 预览卡片
        Text(
            text = "预览",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        var previewSelected by remember { mutableStateOf(false) }
        val previewScale by animateFloatAsState(
            targetValue = if (previewSelected) 1.05f else 1f,
            animationSpec = spring(
                dampingRatio = springDamping,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "previewScale"
        )

        Card(
            onClick = { previewSelected = !previewSelected },
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { scaleX = previewScale; scaleY = previewScale },
            colors = CardDefaults.cardColors(
                containerColor = if (previewSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                }
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "点击预览弹簧效果",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

// ========== 关于页面 ==========
@Composable
private fun AboutPage(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.Start)
        ) {
            Text("< 返回")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(96.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "屏幕锁",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "版本 1.0",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 检查更新卡片
        Card(
            onClick = { /* 暂不执行动作 */ },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_info),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "检查更新",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 仓库按钮
        BouncyOutlinedTextField(
            value = "仓库",
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            labelText = "仓库"
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 作者信息
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "作者",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "timome",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// ========== 通用组件 ==========
@Composable
private fun PositionCard(
    title: String,
    description: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    isSwitchEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (isEnabled) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = tween(durationMillis = 300),
        label = "positionCardColor"
    )
    val titleColor by animateColorAsState(
        targetValue = if (isEnabled) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(durationMillis = 300),
        label = "positionTitleColor"
    )
    val descColor by animateColorAsState(
        targetValue = if (isEnabled) {
            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 300),
        label = "positionDescColor"
    )
    val scale by animateFloatAsState(
        targetValue = if (isEnabled) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "positionCardScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale },
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = titleColor
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = descColor
                )
            }

            // Switch with spring bounce on toggle
            var switchBounceTarget by remember(isEnabled) { mutableStateOf(1.15f) }
            val switchBounceScale by animateFloatAsState(
                targetValue = switchBounceTarget,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "switchBounceScale"
            )
            LaunchedEffect(switchBounceTarget) {
                if (switchBounceTarget > 1f) {
                    switchBounceTarget = 1f
                }
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                enabled = isSwitchEnabled,
                modifier = Modifier.graphicsLayer {
                    scaleX = switchBounceScale
                    scaleY = switchBounceScale
                }
            )
        }
    }
}

private fun springDampingLabel(ratio: Float): String = when {
    ratio <= 0.2f -> "极强"
    ratio <= 0.4f -> "强"
    ratio <= 0.6f -> "中"
    ratio <= 0.8f -> "弱"
    else -> "无"
}

/**
 * OutlinedTextField with a spring-bounce animation on the floating label.
 * When the field gains focus or the label floats up, the label "bounces" with a spring effect.
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
    supportingText: @Composable (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    // Bounce the label when focus changes
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
