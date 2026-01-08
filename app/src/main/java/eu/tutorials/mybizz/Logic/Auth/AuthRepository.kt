package eu.tutorials.mybizz.Logic.Auth

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import eu.tutorials.mybizz.Model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthRepository(private val context: Context? = null) {
    @SuppressLint("StaticFieldLeak")
    companion object {
        @Volatile
        private var INSTANCE: AuthRepository? = null

        fun getInstance(context: Context? = null): AuthRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthRepository(context).also { INSTANCE = it }
            }
        }
    }

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val prefs: SharedPreferences? = context?.getSharedPreferences("MyBizPrefs", Context.MODE_PRIVATE)

    private val TAG = "AuthRepository"

    private val _currentUser = MutableStateFlow<User?>(null)
    private val _isAuthenticated = MutableStateFlow(false)
    private val _isLoading = MutableStateFlow(true)

    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        val firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            val cachedRole = prefs?.getString("user_role", null)
            val cachedEmail = prefs?.getString("user_email", null)

            if (cachedRole != null && cachedEmail != null) {
                _currentUser.value = User(firebaseUser.uid, cachedEmail, cachedRole)
                _isAuthenticated.value = true
                _isLoading.value = false
                fetchUserRoleInBackground(firebaseUser.uid)
            } else {
                fetchUserRole(firebaseUser.uid) { user ->
                    _currentUser.value = user
                    _isAuthenticated.value = true
                    _isLoading.value = false
                    cacheUserData(user)
                }
            }
        } else {
            _currentUser.value = null
            _isAuthenticated.value = false
            _isLoading.value = false
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
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val role = doc.getString("role") ?: "user"
                    val email = doc.getString("email") ?: ""

                    val cachedRole = prefs?.getString("user_role", null)
                    if (role != cachedRole) {
                        val updatedUser = User(uid, email, role)
                        _currentUser.value = updatedUser
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
        onResult: (Boolean, String?, User?) -> Unit
    ) {
        Log.d(TAG, "🚀 SIGNUP STARTED: $email, role: $role")
        _isLoading.value = true

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "✅ Firebase auth successful")
                    val user = task.result?.user
                    val uid = user?.uid ?: ""

                    val userData = User(uid, email, role)

                    // ✅ UPDATE STATE IMMEDIATELY - Don't wait for Firestore
                    _currentUser.value = userData
                    _isAuthenticated.value = true
                    _isLoading.value = false

                    // ✅ CACHE IMMEDIATELY for fast access
                    cacheUserData(userData)

                    // ✅ CALL CALLBACK IMMEDIATELY - Navigate now!
                    Log.d(TAG, "🎯 Calling onResult with success BEFORE Firestore")
                    onResult(true, null, userData)

                    // ✅ SAVE TO FIRESTORE IN BACKGROUND (non-blocking)
                    firestore.collection("users").document(uid).set(userData)
                        .addOnSuccessListener {
                            Log.d(TAG, "✅ Firestore user document created successfully (background)")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "⚠️ Firestore error (background): ${e.message}")
                            // User is still authenticated via Firebase Auth
                            // We can retry later or handle this gracefully
                        }
                } else {
                    val errorMessage = task.exception?.message ?: "Unknown error occurred"
                    Log.e(TAG, "❌ Firebase auth error: $errorMessage")
                    _isLoading.value = false
                    onResult(false, errorMessage, null)
                }
            }
    }

    fun login(
        email: String,
        password: String,
        onResult: (Boolean, String?, String?, User?) -> Unit
    ) {
        _isLoading.value = true

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: ""

                // Check cache first for instant login
                val cachedRole = prefs?.getString("user_role", null)
                val cachedEmail = prefs?.getString("user_email", null)

                if (cachedRole != null && cachedEmail != null) {
                    // ✅ INSTANT LOGIN from cache
                    val user = User(uid, cachedEmail, cachedRole)
                    _currentUser.value = user
                    _isAuthenticated.value = true
                    _isLoading.value = false

                    Log.d(TAG, "⚡ Fast login from cache")
                    onResult(true, cachedRole, null, user)

                    // Verify in background
                    fetchUserRoleInBackground(uid)
                } else {
                    // Fetch from Firestore
                    fetchUserRole(uid) { user ->
                        _currentUser.value = user
                        _isAuthenticated.value = true
                        _isLoading.value = false
                        cacheUserData(user)
                        onResult(true, user.role, null, user)
                    }
                }
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                onResult(false, null, e.message, null)
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
                    val defaultUser = User(uid, auth.currentUser?.email ?: "", "user")
                    firestore.collection("users").document(uid).set(defaultUser)
                    onComplete(defaultUser)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching user role: ${e.message}")
                val defaultUser = User(uid, auth.currentUser?.email ?: "", "user")
                onComplete(defaultUser)
            }
    }

    fun logout() {
        auth.signOut()
        _currentUser.value = null
        _isAuthenticated.value = false
        _isLoading.value = false
        clearCachedData()
    }

    fun getCurrentUserRole(): String {
        return _currentUser.value?.role ?: "user"
    }

    fun getCurrentUserId(): String? {
        return _currentUser.value?.uid ?: FirebaseAuth.getInstance().currentUser?.uid
    }

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null && _isAuthenticated.value
    }
}
