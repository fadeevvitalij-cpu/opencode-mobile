package com.opencode.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.opencode.mobile.ui.screens.chat.ChatScreen
import com.opencode.mobile.ui.screens.connections.ConnectionsScreen
import com.opencode.mobile.ui.screens.files.FileBrowserScreen
import com.opencode.mobile.ui.screens.settings.SettingsScreen
import com.opencode.mobile.ui.screens.terminal.TerminalScreen
import com.opencode.mobile.ui.theme.ThemedApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ThemedApp {
                AppScaffold()
            }
        }
    }
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun AppScaffold() {
    val navController = rememberNavController()
    var selectedConnectionId by remember { mutableStateOf<Long?>(null) }

    val items = listOf(
        BottomNavItem("connections", "Подключения", Icons.Outlined.Link),
        BottomNavItem("chat/{connectionId}", "Чат", Icons.Outlined.Forum),
        BottomNavItem("files/{connectionId}", "Файлы", Icons.Outlined.Folder),
        BottomNavItem("terminal/{connectionId}", "Терминал", Icons.Outlined.Terminal),
        BottomNavItem("settings", "Настройки", Icons.Outlined.Settings)
    )

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            NavigationBar {
                items.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        selected = selected,
                        enabled = item.route == "connections" || item.route == "settings" || selectedConnectionId != null,
                        onClick = {
                            val target = when (item.route) {
                                "chat/{connectionId}" -> "chat/${selectedConnectionId ?: return@NavigationBarItem}"
                                "files/{connectionId}" -> "files/${selectedConnectionId ?: return@NavigationBarItem}"
                                "terminal/{connectionId}" -> "terminal/${selectedConnectionId ?: return@NavigationBarItem}"
                                else -> item.route
                            }
                            navController.navigate(target) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "connections",
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            composable("connections") {
                ConnectionsScreen(
                    onNavigateToChat = { connectionId ->
                        selectedConnectionId = connectionId
                        navController.navigate("chat/$connectionId")
                    }
                )
            }
            composable(
                route = "chat/{connectionId}",
                arguments = listOf(navArgument("connectionId") { type = NavType.LongType })
            ) { backStackEntry ->
                val cid = backStackEntry.arguments?.getLong("connectionId") ?: 0L
                selectedConnectionId = cid
                ChatScreen(connectionId = cid)
            }
            composable(
                route = "files/{connectionId}",
                arguments = listOf(navArgument("connectionId") { type = NavType.LongType })
            ) { backStackEntry ->
                val cid = backStackEntry.arguments?.getLong("connectionId") ?: 0L
                selectedConnectionId = cid
                FileBrowserScreen(connectionId = cid)
            }
            composable(
                route = "terminal/{connectionId}",
                arguments = listOf(navArgument("connectionId") { type = NavType.LongType })
            ) { backStackEntry ->
                val cid = backStackEntry.arguments?.getLong("connectionId") ?: 0L
                selectedConnectionId = cid
                TerminalScreen(connectionId = cid)
            }
            composable("settings") {
                SettingsScreen()
            }
        }
    }
}