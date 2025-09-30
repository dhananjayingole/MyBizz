// UIScreens/SplashScreen.kt - Optimized for fast navigation
package eu.tutorials.mybizz.UIScreens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import eu.tutorials.mybizz.Logic.Auth.AuthRepository
import eu.tutorials.mybizz.Navigation.Routes
import eu.tutorials.mybizz.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    val context = LocalContext.current
    val authRepo = remember { AuthRepository(context) }

    // Observe authentication state
    val isAuthenticated by authRepo.isAuthenticated
    val currentUser by authRepo.currentUser
    val isLoading by authRepo.isLoading

    LaunchedEffect(isLoading, isAuthenticated, currentUser) {
        if (!isLoading) {
            // Small delay for better UX (optional)
            delay(500)

            if (isAuthenticated && currentUser != null) {
                val role = currentUser!!.role
                // Navigate to appropriate dashboard based on role
                val destination = if (role == "admin") {
                    Routes.AdminDashboardScreen
                } else {
                    Routes.UserDashboardScreen
                }

                navController.navigate(destination) {
                    popUpTo(Routes.SplashScreen) { inclusive = true }
                }
            } else {
                // User not logged in, go to login screen
                navController.navigate(Routes.LoginScreen) {
                    popUpTo(Routes.SplashScreen) { inclusive = true }
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // App Icon with Rounded Corners
            Image(
                painter = painterResource(id = R.drawable.img),
                contentDescription = "MyBiz Logo",
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "MyBizz",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Business Management System",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Loading...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}