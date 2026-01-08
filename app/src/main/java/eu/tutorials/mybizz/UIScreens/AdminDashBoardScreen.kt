package eu.tutorials.mybizz.UIScreens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import eu.tutorials.mybizz.Logic.Auth.AuthRepository
import eu.tutorials.mybizz.Model.MenuItem
import eu.tutorials.mybizz.Navigation.Routes
import eu.tutorials.mybizz.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    navController: NavController,
    authRepo: AuthRepository
) {
    val context = LocalContext.current

    val menuItems = listOf(
        MenuItem("bills", "Bills Management", Routes.BillsListScreen, R.drawable.img_13), // Replace with your drawable
        MenuItem("users", "User Management", Routes.UserManagementScreen, R.drawable.img_14), // Replace with your drawable
        MenuItem("RentalManagement", "RentalManagement", Routes.RentalListScreen, R.drawable.img_12), // Replace with your drawable
        MenuItem("Task", "Task", Routes.TaskListScreen, R.drawable.img_15), // Replace with your drawable
        MenuItem("Construction", "plots & Construction", Routes.PlotAndConstructionEntry, R.drawable.img_10), // Replace with your drawable
        MenuItem("Profile", "Profile", Routes.ProfileScreen, R.drawable.img_11),
        MenuItem("Setting", "Setting", Routes.SettingScreen, R.drawable.img_16)// Replace with your drawable
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Welcome Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Welcome, Admin!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Manage your business operations from here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(menuItems) { item ->
                    MenuItemCard(
                        menuItem = item,
                        onClick = {
                            // ✅ Handle all navigation properly
                            when {
                                item.route.isNotEmpty() -> navController.navigate(item.route)
                                else -> {
                                    // Handle empty or invalid routes
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MenuItemCard(
    menuItem: MenuItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = menuItem.icon),
                contentDescription = menuItem.title,
                modifier = Modifier.size(32.dp),
                tint = androidx.compose.ui.graphics.Color.Unspecified // This removes the tint
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = menuItem.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}