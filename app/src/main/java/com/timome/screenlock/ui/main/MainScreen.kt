package com.timome.screenlock.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

// 设置页面路由，提升到 MainScreen 层级
sealed class SettingsRoute(val title: String, val index: Int) {
    data object Main : SettingsRoute("", 0)
    data object LongPress : SettingsRoute("长按设置", 1)
    data object ThemeManagement : SettingsRoute("主题色管理", 2)
    data object Permissions : SettingsRoute("权限管理", 3)
    data object About : SettingsRoute("关于", 4)
}

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

    // 设置页面路由状态（提升到 MainScreen）
    var settingsRoute by remember { mutableStateOf<SettingsRoute>(SettingsRoute.Main) }

    // 是否在子菜单中（用于禁用 Pager 滑动 & 隐藏导航栏）
    val settingsSubmenuActive = settingsRoute !is SettingsRoute.Main

    // 导航到权限管理页面
    val navigateToPermissions: () -> Unit = {
        settingsRoute = SettingsRoute.Permissions
        // 确保在设置页面
        scope.launch {
            pagerState.animateScrollToPage(1)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (settingsSubmenuActive) settingsRoute.title else "屏幕锁"
                    )
                },
                navigationIcon = {
                    AnimatedVisibility(visible = settingsSubmenuActive) {
                        IconButton(onClick = {
                            // 返回设置主界面
                            settingsRoute = SettingsRoute.Main
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            AnimatedVisibility(visible = !settingsSubmenuActive) {
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
                                    // 切换到设置页时重置到主设置界面
                                    settingsRoute = SettingsRoute.Main
                                }
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                        )
                    }
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
                    val targetOffset = if (targetState > initialState) 1 else -1
                    val initialOffset = if (targetState > initialState) -1 else 1
                    slideInHorizontally(
                        animationSpec = tween(
                            durationMillis = 300,
                            easing = LinearOutSlowInEasing
                        ),
                        initialOffsetX = { fullWidth -> fullWidth * initialOffset }
                    ) togetherWith slideOutHorizontally(
                        animationSpec = tween(
                            durationMillis = 300,
                            easing = FastOutLinearInEasing
                        ),
                        targetOffsetX = { fullWidth -> fullWidth * targetOffset }
                    )
                },
                label = "pageTransition"
            ) { targetPage ->
                when (targetPage) {
                    0 -> LaunchScreen(
                        settingsDataStore = settingsDataStore,
                        onNavigateToPermissions = navigateToPermissions
                    )
                    1 -> SettingsScreen(
                        settingsDataStore = settingsDataStore,
                        currentRoute = settingsRoute,
                        onNavigate = { newRoute -> settingsRoute = newRoute }
                    )
                    else -> LaunchScreen(
                        settingsDataStore = settingsDataStore,
                        onNavigateToPermissions = navigateToPermissions
                    )
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
