package eu.tutorials.mybizz.Model

data class Construction(
    val id: String,             // Unique ID
    val projectName: String,    // Project name
    val location: String,       // Location
    val startDate: String,      // Start date
    val endDate: String,        // End date
    val cost: String,           // Estimated cost
    val status: String,         // e.g., "In Progress", "Completed"
    val notes: String? = ""     // Optional notes
)

