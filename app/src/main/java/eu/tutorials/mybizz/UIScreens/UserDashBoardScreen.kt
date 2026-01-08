// UserDashboardScreen.kt - User Dashboard
package eu.tutorials.mybizz.UIScreens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import eu.tutorials.mybizz.Logic.Auth.AuthRepository
import eu.tutorials.mybizz.Model.MenuItem
import eu.tutorials.mybizz.Navigation.Routes
import eu.tutorials.mybizz.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDashboardScreen(
    navController: NavController,
    authRepo: AuthRepository
) {
    val menuItems = listOf(
        MenuItem("bills", "View Bills", Routes.BillsListScreen, R.drawable.img_13), // Replace with your drawable
        MenuItem("settings", "Settings", Routes.SettingScreen, R.drawable.img_16), // Replace with your drawable
        MenuItem("RentalManagement", "Rental Management", Routes.RentalListScreen, R.drawable.img_12), // Replace with your drawable
        MenuItem("Task", "Task", Routes.TaskListScreen, R.drawable.img_15), // Replace with your drawable
        MenuItem("Construction", "Plot & Constructions", Routes.PlotAndConstructionEntry, R.drawable.img_10), // Replace with your drawable
        MenuItem("Profile", "Profile", Routes.ProfileScreen, R.drawable.img_11), // Replace with your drawable
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Dashboard") }
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Welcome, User!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "View and track your bills",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Available Options",
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
                            navController.navigate(item.route)
                        }
                    )
                }
            }
        }
    }
}