package eu.tutorials.mybizz.UIScreens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    authRepo: AuthRepository = AuthRepository(LocalContext.current)
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("user") }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Login to MyBiz",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Lock else Icons.Filled.Face
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = "Toggle password visibility")
                }
            }
        )

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
                    ),
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
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedRole == "user",
                    onClick = { selectedRole = "user" }
                )
                Text("User", modifier = Modifier.padding(start = 4.dp))
            }
        }

        Button(
            onClick = {
                if (email.isEmpty() || password.isEmpty()) {
                    error = "Please fill all fields"
                    return@Button
                }

                isLoading = true
                error = null

                authRepo.login(email, password) { success, role, err ->
                    isLoading = false
                    if (success) {
                        if (role == selectedRole) {
                            Toast.makeText(context, "Login Successful!", Toast.LENGTH_SHORT).show()
                            // Navigate based on role
                            if (role == "admin") {
                                navController.navigate(Routes.AdminDashboardScreen) {
                                    popUpTo(Routes.SplashScreen) { inclusive = true }
                                }
                            } else {
                                navController.navigate(Routes.UserDashboardScreen) {
                                    popUpTo(Routes.SplashScreen) { inclusive = true }
                                }
                            }
                        } else {
                            error = "Role mismatch! You are registered as $role"
                            authRepo.logout() // Logout since role doesn't match
                        }
                    } else {
                        error = err ?: "Login failed"
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Login")
            }
        }

        TextButton(
            onClick = { navController.navigate(Routes.SignUpScreen) },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Don't have an account? Sign Up")
        }

        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}
