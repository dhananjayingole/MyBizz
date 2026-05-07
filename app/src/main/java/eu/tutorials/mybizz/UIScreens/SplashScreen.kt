package eu.tutorials.mybizz.UIScreens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import eu.tutorials.mybizz.Logic.Auth.AuthRepository
import eu.tutorials.mybizz.Navigation.Routes
import eu.tutorials.mybizz.R
import kotlinx.coroutines.delay

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SplashScreen(navController: NavController) {
    val context = LocalContext.current

    // ✅ FIX: Use singleton getInstance() so cached role/auth state is shared correctly.
    // Previously `AuthRepository(context)` created a NEW instance, losing all cached data.
    val authRepo = remember { AuthRepository.getInstance(context) }

    val isAuthenticated by authRepo.isAuthenticated.collectAsState()
    val currentUser by authRepo.currentUser.collectAsState()
    val isLoading by authRepo.isLoading.collectAsState()

    var hasNavigated by remember { mutableStateOf(false) }

    LaunchedEffect(isLoading, isAuthenticated, currentUser) {
        if (!isLoading && !hasNavigated) {
            delay(1500)

            if (isAuthenticated && currentUser != null) {
                val role = currentUser!!.role

                // ✅ FIX: Compare role string directly — avoid getString() mismatch.
                // If R.string.admin is "admin" this is fine, but direct compare is safer.
                val destination = if (role == "admin") {
                    Routes.AdminDashboardScreen
                } else {
                    Routes.UserDashboardScreen
                }

                hasNavigated = true
                navController.navigate(destination) {
                    popUpTo(Routes.SplashScreen) { inclusive = true }
                }
            } else if (!isAuthenticated) {
                hasNavigated = true
                navController.navigate(Routes.LoginScreen) {
                    popUpTo(Routes.SplashScreen) { inclusive = true }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + scaleIn()
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img),
                    contentDescription = "MyBiz Logo",
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(delayMillis = 300))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.my_bizz_name),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.app_tagline),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isLoading) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.loading),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}