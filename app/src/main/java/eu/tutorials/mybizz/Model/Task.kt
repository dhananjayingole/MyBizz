package eu.tutorials.mybizz.Model

data class Task(
    val id: String,
    val title: String,
    val description: String,
    val assignedTo: String,
    val dueDate: String,
    val status: String,
    val notes: String
)