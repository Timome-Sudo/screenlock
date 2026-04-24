package com.timome.screenlock.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import com.timome.screenlock.data.SettingsDataStore
import com.timome.screenlock.ui.theme.ScreenlockTheme
import kotlinx.coroutines.launch

sealed class MainScreenTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    data object Launch : MainScreenTab("启动", Icons.Default.PlayArrow)
    data object Settings : MainScreenTab("调整", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    settingsDataStore: SettingsDataStore,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(MainScreenTab.Launch, MainScreenTab.Settings)
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    // 设置图标旋转动画
    var settingsRotationTarget by remember { mutableStateOf(0f) }
    val settingsRotation by animateFloatAsState(
        targetValue = settingsRotationTarget,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "settingsRotation"
    )

    // 是否在子菜单中（用于禁用 Pager 滑动）
    var settingsSubmenuActive by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "屏幕锁")
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        icon = {
                            if (tab is MainScreenTab.Settings) {
                                Icon(
                                    tab.icon,
                                    contentDescription = tab.title,
                                    modifier = Modifier.graphicsLayer {
                                        rotationZ = settingsRotation
                                    }
                                )
                            } else {
                                Icon(tab.icon, contentDescription = tab.title)
                            }
                        },
                        label = { Text(tab.title) },
                        selected = pagerState.currentPage == index,
                        onClick = {
                            if (tab is MainScreenTab.Settings) {
                                settingsRotationTarget += 360f
                            }
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !settingsSubmenuActive,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            // 页面切换过渡动画
            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    fadeIn(animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )) togetherWith fadeOut()
                },
                label = "pageTransition"
            ) { targetPage ->
                when (targetPage) {
                    0 -> LaunchScreen(settingsDataStore = settingsDataStore)
                    1 -> SettingsScreen(
                        settingsDataStore = settingsDataStore,
                        onSubmenuActive = { settingsSubmenuActive = it }
                    )
                    else -> LaunchScreen(settingsDataStore = settingsDataStore)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    ScreenlockTheme {
        // Preview
    }
}
