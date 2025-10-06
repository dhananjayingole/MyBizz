// User.kt - User Model
package eu.tutorials.mybizz.Model

data class User(
    val uid: String = "",
    val email: String = "",
    val role: String = "user" // default role
)
