package eu.tutorials.mybizz.UIScreens

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    authRepo: AuthRepository = AuthRepository.getInstance(LocalContext.current) // Use singleton
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("user") }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

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
                    text = "Login",
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
                    modifier = Modifier.fillMaxWidth(),
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

                // Forget password?
                TextButton(
                    onClick = { /* TODO: implement forget password */ },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Forgot password?")
                }

                // Login button
                Button(
                    onClick = {
                        if (email.isEmpty() || password.isEmpty()) {
                            error = "Please fill all fields"
                            return@Button
                        }

                        isLoading = true
                        error = null

                        authRepo.login(email, password) { success, role, err, user ->
                            isLoading = false
                            if (success && user != null) {
                                if (role == selectedRole) {
                                    Toast.makeText(context, "Login Successful!", Toast.LENGTH_SHORT).show()
                                    when (role) {
                                        "admin" -> {
                                            navController.navigate(Routes.AdminDashboardScreen) {
                                                popUpTo(Routes.SplashScreen) { inclusive = true }
                                            }
                                        }
                                        else -> {
                                            navController.navigate(Routes.UserDashboardScreen) {
                                                popUpTo(Routes.SplashScreen) { inclusive = true }
                                            }
                                        }
                                    }
                                } else {
                                    error = "Role mismatch! You are registered as $role"
                                    authRepo.logout()
                                }
                            } else {
                                error = err ?: "Login failed"
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(top = 8.dp),
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
                        Text("LOGIN", style = MaterialTheme.typography.labelLarge)
                    }
                }

                // Role selection
                Text(
                    text = "Login as:",
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
                                selected = selectedRole == "admin",
                                onClick = { selectedRole = "admin" }
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedRole == "admin",
                            onClick = { selectedRole = "admin" }
                        )
                        Text("Admin", modifier = Modifier.padding(start = 4.dp))
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .selectable(
                                selected = selectedRole == "user",
                                onClick = { selectedRole = "user" }
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedRole == "user",
                            onClick = { selectedRole = "user" }
                        )
                        Text("User", modifier = Modifier.padding(start = 4.dp))
                    }
                }

                // Sign Up
                TextButton(
                    onClick = {
                        navController.navigate(Routes.SignUpScreen) {
                            popUpTo(Routes.LoginScreen) { inclusive = true }
                        }
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Don't have an account? Sign Up")
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

