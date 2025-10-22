package eu.tutorials.mybizz.Model

import androidx.compose.ui.graphics.vector.ImageVector

data class MenuItem(
    val id: String,
    val title: String,
    val route: String,
    val icon: Int,
    val adminOnly: Boolean = false
)