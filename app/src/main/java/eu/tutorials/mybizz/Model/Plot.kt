package eu.tutorials.mybizz.Model

data class Plot(
    val id: String,
    val plotName: String,
    val plotId: String,
    val location: String,
    val visitorName: String,
    val visitorNumber: String,
    val visitorAddress: String,
    val askingAmount: String,
    val attendedBy: String,
    val initialPrice: String,
    val plotSize: String,
    val visitDate: String = "",
    val notes: String = ""
)
