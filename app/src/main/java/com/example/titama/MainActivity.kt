package com.example.titama

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import com.example.titama.ui.theme.TITAMATheme

data class NavItem(val label: String, val icon: ImageVector)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TITAMATheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navItems = listOf(
        NavItem("Dashboard", Icons.Filled.Dashboard),
        NavItem("Tasks", Icons.Filled.CheckCircle),
        NavItem("Pomodoro", Icons.Filled.Timer),
        NavItem("Tracker", Icons.Filled.WatchLater),
    )

    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (selectedTab) {
                0 -> DashboardScreen()
                1 -> PlaceholderScreen("Tasks")
                2 -> PlaceholderScreen("Pomodoro")
                3 -> PlaceholderScreen("Tracker")
            }
        }
    }
}

@Composable
fun PlaceholderScreen(name: String) {
    Text(text = name, fontSize = 24.sp)
}
