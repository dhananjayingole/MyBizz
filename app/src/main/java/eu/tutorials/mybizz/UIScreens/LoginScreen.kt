package eu.tutorials.mybizz.UIScreens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import eu.tutorials.mybizz.Logic.Auth.AuthRepository
import eu.tutorials.mybizz.Navigation.Routes

@Composable
fun LoginScreen(
    navController: NavController,
    authRepo: AuthRepository = AuthRepository.getInstance(LocalContext.current)
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("user") }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. REUSABLE ANIMATED BACKGROUND
        LoginBackgroundGradient()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Modern Header
            Text(
                text = "Welcome Back",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Sign in to continue your journey",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(), // Smooth resize for error messages
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Email Field
                    LoginTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email",
                        icon = Icons.Default.Email,
                        keyboardType = KeyboardType.Email
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Field
                    LoginTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        icon = Icons.Default.Lock,
                        isPassword = true,
                        passwordVisible = passwordVisible,
                        onVisibilityChange = { passwordVisible = !passwordVisible }
                    )

                    // Forgot Password
                    TextButton(
                        onClick = { /* Implement Forget Password */ },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Forgot password?", style = MaterialTheme.typography.labelMedium)
                    }

                    // Modern Role Selector (Chip style)
                    Text(
                        text = "Login as",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                    RoleSelectorRow(selectedRole) { selectedRole = it }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. ANIMATED LOGIN BUTTON
                    AnimatedLoginButton(
                        isLoading = isLoading,
                        onClick = {
                            if (email.isEmpty() || password.isEmpty()) {
                                error = "Please fill all fields"
                            } else {
                                isLoading = true
                                error = null
                                authRepo.login(email, password) { success, role, err, user ->
                                    isLoading = false
                                    if (success && user != null) {
                                        if (role == selectedRole) {
                                            Toast.makeText(context, "Welcome back!", Toast.LENGTH_SHORT).show()
                                            navController.navigate(
                                                if (role == "admin") Routes.AdminDashboardScreen
                                                else Routes.UserDashboardScreen
                                            ) {
                                                popUpTo(Routes.SplashScreen) { inclusive = true }
                                            }
                                        } else {
                                            error = "Role mismatch! You are a $role"
                                            authRepo.logout()
                                        }
                                    } else {
                                        error = err ?: "Login failed"
                                    }
                                }
                            }
                        }
                    )

                    // Error Message with Fade/Slide animation
                    AnimatedVisibility(
                        visible = error != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Text(
                            text = error ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .padding(top = 16.dp)
                                .background(MaterialTheme.colorScheme.error.copy(0.1f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        )
                    }
                }
            }

            // Navigate to Signup
            TextButton(
                onClick = {
                    navController.navigate(Routes.SignUpScreen) {
                        popUpTo(Routes.LoginScreen) { inclusive = true }
                    }
                },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Don't have an account? Sign Up", color = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}

// --- HELPER UI COMPONENTS ---

@Composable
fun LoginBackgroundGradient() {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val animOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing)),
        label = "offset"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFE0F7FA), Color(0xFFF3E5F5), Color(0xFFFFF9C4)),
                start = androidx.compose.ui.geometry.Offset(animOffset, animOffset),
                end = androidx.compose.ui.geometry.Offset(animOffset + size.width, animOffset + size.height)
            )
        )
    }
}

@Composable
fun AnimatedLoginButton(isLoading: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.94f else 1f, label = "scale")

    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale),
        shape = RoundedCornerShape(16.dp),
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Text("LOGIN", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }
}

@Composable
fun LoginTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onVisibilityChange: () -> Unit = {},
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        trailingIcon = {
            if (isPassword) {
                IconButton(onClick = onVisibilityChange) {
                    Icon(imageVector = if (passwordVisible) Icons.Default.Face else Icons.Default.Lock, contentDescription = null)
                }
            }
        },
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        singleLine = true
    )
}

@Composable
fun RoleSelectorRow(selectedRole: String, onRoleSelected: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        listOf("admin", "user").forEach { role ->
            val isSelected = selectedRole == role
            val bgColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            val textColor by animateColorAsState(if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor.copy(alpha = if (isSelected) 1f else 0.1f))
                    .clickable { onRoleSelected(role) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = role.uppercase(), color = textColor, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
        }
    }
}