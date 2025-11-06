// UIScreens/UserManagementScreen.kt
package eu.tutorials.mybizz.UIScreens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import eu.tutorials.mybizz.Logic.Auth.AuthRepository
import eu.tutorials.mybizz.Model.User
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    navController: NavHostController,
    authRepo: AuthRepository = AuthRepository(LocalContext.current)
) {
    val context = LocalContext.current
    var userList by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    // Fetch users from Firebase Auth and Firestore
    LaunchedEffect(Unit) {
        try {
            val users = fetchAllUsersFromFirebase()
            userList = users
            isLoading = false
        } catch (e: Exception) {
            Toast.makeText(context, "Error loading users: ${e.message}", Toast.LENGTH_SHORT).show()
            isLoading = false
        }
    }

    val filteredUsers = userList.filter { user ->
        user.email.contains(searchQuery, ignoreCase = true) ||
                user.role.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("User Management") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Users") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Search") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredUsers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (searchQuery.isNotEmpty()) "No users found for '$searchQuery'"
                        else "No users found",
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredUsers) { user ->
                        UserCard(
                            user = user,
                            onBlockToggle = { block ->
                                // Handle block/unblock user
                                toggleUserBlockStatus(user.uid, block, context) { success ->
                                    if (success) {
                                        // Update local state
                                        userList = userList.map {
                                            if (it.uid == user.uid) it.copy(isBlocked = block)
                                            else it
                                        }
                                        Toast.makeText(
                                            context,
                                            if (block) "${user.email} blocked" else "${user.email} unblocked",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        Toast.makeText(context, "Failed to update status", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserCard(
    user: User,
    onBlockToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "User",
                    tint = if (user.isBlocked) Color.Gray else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = user.email,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Role: ${user.role}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = if (user.isBlocked) "🔴 Blocked" else "🟢 Active",
                        color = if (user.isBlocked) Color.Red else Color.Green,
                        fontSize = 14.sp
                    )
                }
            }

            IconButton(
                onClick = { onBlockToggle(!user.isBlocked) }
            ) {
                Icon(
                    imageVector = if (user.isBlocked) Icons.Default.CheckCircle else Icons.Default.Close,
                    contentDescription = if (user.isBlocked) "Unblock" else "Block",
                    tint = if (user.isBlocked) Color.Green else Color.Red
                )
            }
        }
    }
}

// Function to fetch all users from Firebase Auth and Firestore
private suspend fun fetchAllUsersFromFirebase(): List<User> {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    // Note: This requires Firebase Admin SDK on backend for production
    // For demo purposes, we'll use the current approach with Firestore

    // Get all users from Firestore users collection
    val snapshot = firestore.collection("users").get().await()

    return snapshot.documents.mapNotNull { document ->
        try {
            val uid = document.id
            val email = document.getString("email") ?: ""
            val role = document.getString("role") ?: "user"
            val isBlocked = document.getBoolean("isBlocked") ?: false

            User(uid, email, role, isBlocked)
        } catch (e: Exception) {
            null
        }
    }
}

// Function to toggle user block status
private fun toggleUserBlockStatus(
    uid: String,
    block: Boolean,
    context: android.content.Context,
    onResult: (Boolean) -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()

    firestore.collection("users").document(uid)
        .update("isBlocked", block)
        .addOnSuccessListener {
            onResult(true)
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            onResult(false)
        }
}