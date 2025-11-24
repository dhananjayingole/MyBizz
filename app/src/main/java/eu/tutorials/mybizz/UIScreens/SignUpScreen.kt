package eu.tutorials.mybizz.UIScreens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import eu.tutorials.mybizz.Logic.Auth.AuthRepository
import eu.tutorials.mybizz.Navigation.Routes
import androidx.compose.runtime.collectAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    navController: NavController,
    authRepo: AuthRepository = AuthRepository.getInstance(LocalContext.current) // Use singleton
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("user") }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Create Account",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    placeholder = { Text("Enter your email") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                // Password
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    placeholder = { Text("Enter your password") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.Face else Icons.Filled.Lock,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    }
                )

                // Confirm Password
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password") },
                    placeholder = { Text("Confirm your password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) Icons.Filled.Face else Icons.Filled.Lock,
                                contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                            )
                        }
                    }
                )

                // Role selection
                Text(
                    text = "Select Role:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp)
                )

                Row(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .selectable(
                                selected = role == "admin",
                                onClick = { role = "admin" }
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = role == "admin",
                            onClick = { role = "admin" }
                        )
                        Text("Admin", modifier = Modifier.padding(start = 4.dp))
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .selectable(
                                selected = role == "user",
                                onClick = { role = "user" }
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = role == "user",
                            onClick = { role = "user" }
                        )
                        Text("User", modifier = Modifier.padding(start = 4.dp))
                    }
                }

                // Sign Up Button - SIMPLIFIED NAVIGATION
                Button(
                    onClick = {
                        // Validation
                        when {
                            email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() -> {
                                error = "Please fill all fields"
                                return@Button
                            }
                            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                                error = "Please enter a valid email"
                                return@Button
                            }
                            password != confirmPassword -> {
                                error = "Passwords do not match"
                                return@Button
                            }
                            password.length < 6 -> {
                                error = "Password must be at least 6 characters"
                                return@Button
                            }
                            else -> {
                                isLoading = true
                                error = null

                                // Store the role for navigation
                                val selectedRole = role

                                authRepo.signUp(email, password, role) { success, err, user ->
                                    isLoading = false

                                    if (success) {
                                        Toast.makeText(context, "Account Created Successfully!", Toast.LENGTH_SHORT).show()

                                        // NAVIGATE IMMEDIATELY using the selected role
                                        // Don't rely on state updates, navigate directly
                                        when (selectedRole) {
                                            "admin" -> {
                                                Log.d("SIGNUP", "🔄 Navigating to Admin Dashboard")
                                                navController.navigate(Routes.AdminDashboardScreen) {
                                                    popUpTo(Routes.SplashScreen) { inclusive = true }
                                                }
                                            }
                                            else -> {
                                                Log.d("SIGNUP", "🔄 Navigating to User Dashboard")
                                                navController.navigate(Routes.UserDashboardScreen) {
                                                    popUpTo(Routes.SplashScreen) { inclusive = true }
                                                }
                                            }
                                        }
                                    } else {
                                        error = err ?: "Signup failed. Please try again."
                                        Log.e("SIGNUP", "❌ Signup failed: $error")
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(top = 24.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("SIGN UP", style = MaterialTheme.typography.labelLarge)
                    }
                }

                // Login text
                TextButton(
                    onClick = {
                        navController.navigate(Routes.LoginScreen) {
                            popUpTo(Routes.SignUpScreen) { inclusive = true }
                        }
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Already have an account? Login")
                }

                // Error message
                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .background(
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    )
                }
            }
        }
    }
}
