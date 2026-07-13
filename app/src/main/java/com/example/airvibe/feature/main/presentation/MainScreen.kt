package com.example.airvibe.feature.main.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight

enum class MainTab(val title: String, val icon: ImageVector) {
    Radar("Radar", Icons.Rounded.Home),
    Services("Servicios", Icons.Rounded.Search),
    Groups("Grupos", Icons.Rounded.Groups),
    Profile("Perfil", Icons.Rounded.Person)
}

@Composable
fun MainScreen(
    radarContent: @Composable () -> Unit,
    servicesContent: @Composable () -> Unit,
    groupsContent: @Composable () -> Unit,
    profileContent: @Composable () -> Unit,
) {
    var currentTab by remember { mutableStateOf(MainTab.Radar) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                MainTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.title) },
                        label = { Text(text = tab.title, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentTab) {
                MainTab.Radar -> radarContent()
                MainTab.Services -> servicesContent()
                MainTab.Groups -> groupsContent()
                MainTab.Profile -> profileContent()
            }
        }
    }
}
