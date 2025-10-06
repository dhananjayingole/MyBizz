// Logic/Auth/AuthRepository.kt - Optimized for fast navigation
package eu.tutorials.mybizz.Logic.Auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import eu.tutorials.mybizz.Model.User

class AuthRepository(private val context: Context? = null) {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val prefs: SharedPreferences? = context?.getSharedPreferences("MyBizPrefs", Context.MODE_PRIVATE)

    private val TAG = "AuthRepository"

    // Track authentication state
    val currentUser: MutableState<User?> = mutableStateOf(null)
    val isAuthenticated: MutableState<Boolean> = mutableStateOf(false)
    val isLoading: MutableState<Boolean> = mutableStateOf(true)

    init {
        // Check if user is already logged in when repository is created
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        val firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            // First check cached role for fast navigation
            val cachedRole = prefs?.getString("user_role", null)
            val cachedEmail = prefs?.getString("user_email", null)

            if (cachedRole != null && cachedEmail != null) {
                // Use cached data for immediate navigation
                currentUser.value = User(firebaseUser.uid, cachedEmail, cachedRole)
                isAuthenticated.value = true
                isLoading.value = false

                // Verify with Firestore in background
                fetchUserRoleInBackground(firebaseUser.uid)
            } else {
                // Fetch from Firestore
                fetchUserRole(firebaseUser.uid) { user ->
                    currentUser.value = user
                    isAuthenticated.value = true
                    isLoading.value = false

                    // Cache for next time
                    cacheUserData(user)
                }
            }
        } else {
            currentUser.value = null
            isAuthenticated.value = false
            isLoading.value = false
        }
    }

    private fun cacheUserData(user: User) {
        prefs?.edit()?.apply {
            putString("user_role", user.role)
            putString("user_email", user.email)
            apply()
        }
    }

    private fun clearCachedData() {
        prefs?.edit()?.apply {
            remove("user_role")
            remove("user_email")
            apply()
        }
    }

    private fun fetchUserRoleInBackground(uid: String) {
        // Verify cached data is correct
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val role = doc.getString("role") ?: "user"
                    val email = doc.getString("email") ?: ""

                    // Update if different from cached
                    val cachedRole = prefs?.getString("user_role", null)
                    if (role != cachedRole) {
                        val updatedUser = User(uid, email, role)
                        currentUser.value = updatedUser
                        cacheUserData(updatedUser)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error verifying cached user data: ${e.message}")
            }
    }

    fun signUp(
        email: String,
        password: String,
        role: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        Log.d(TAG, "SignUp started for: $email")
        isLoading.value = true

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Firebase auth successful")
                    val user = task.result?.user
                    val uid = user?.uid ?: ""

                    // Create user document in Firestore
                    val userData = User(uid, email, role)
                    firestore.collection("users").document(uid).set(userData)
                        .addOnSuccessListener {
                            Log.d(TAG, "Firestore user document created successfully")

                            // Update auth state immediately
                            currentUser.value = userData
                            isAuthenticated.value = true
                            isLoading.value = false

                            // Cache for fast future access
                            cacheUserData(userData)

                            onResult(true, null)
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Firestore error: ${e.message}")
                            isLoading.value = false
                            onResult(false, e.message)
                        }
                } else {
                    val errorMessage = task.exception?.message ?: "Unknown error occurred"
                    Log.e(TAG, "Firebase auth error: $errorMessage")
                    isLoading.value = false
                    onResult(false, errorMessage)
                }
            }
    }

    fun login(
        email: String,
        password: String,
        onResult: (Boolean, String?, String?) -> Unit
    ) {
        isLoading.value = true

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: ""
                fetchUserRole(uid) { user ->
                    currentUser.value = user
                    isAuthenticated.value = true
                    isLoading.value = false

                    // Cache for fast future access
                    cacheUserData(user)

                    onResult(true, user.role, null)
                }
            }
            .addOnFailureListener { e ->
                isLoading.value = false
                onResult(false, null, e.message)
            }
    }

    private fun fetchUserRole(uid: String, onComplete: (User) -> Unit) {
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val email = doc.getString("email") ?: ""
                    val role = doc.getString("role") ?: "user"
                    onComplete(User(uid, email, role))
                } else {
                    // Create default user document if not exists
                    val defaultUser = User(uid, auth.currentUser?.email ?: "", "user")
                    firestore.collection("users").document(uid).set(defaultUser)
                    onComplete(defaultUser)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching user role: ${e.message}")
                // Fallback to default user
                val defaultUser = User(uid, auth.currentUser?.email ?: "", "user")
                onComplete(defaultUser)
            }
    }

    fun logout() {
        auth.signOut()
        currentUser.value = null
        isAuthenticated.value = false
        isLoading.value = false
        clearCachedData()
    }

    fun getCurrentUserRole(): String {
        return currentUser.value?.role ?: "user"
    }
    // Add this to your AuthRepository class
    fun getCurrentUserId(): String? {
        return currentUser.value?.uid ?: FirebaseAuth.getInstance().currentUser?.uid
    }
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null && isAuthenticated.value
    }
}