// Navigation/Routes.kt
package eu.tutorials.mybizz.Navigation

object Routes {
    const val SplashScreen = "splash_screen"
    const val LoginScreen = "login_screen"
    const val SignUpScreen = "signup_screen"
    const val AdminDashboardScreen = "admin_dashboard"
    const val UserDashboardScreen = "user_dashboard"
    const val BillsListScreen = "bills_list"
    const val AddBillScreen = "add_bill"
    const val EditBillScreen = "edit_bill/{billId}"
    const val BillDetailsScreen = "bill_details/{billId}"
    const val RentalListScreen = "rental_list"
    const val AddRentalScreen = "add_rental"
    const val EditRentalScreen = "edit_rental/{rentalId}"
    const val RentalDetailScreen = "rental_detail/{rentalId}"
    const val ProfileScreen = "profilescreen"
    const val ConstructionListScreen = "construction_list"
    const val AddConstructionScreen = "add_construction"
    const val ConstructionDetailScreen = "construction_detail/{constructionId}"
    const val EditConstructionScreen = "edit_construction/{constructionId}"

    // Task Routes - CORRECTED
    const val TaskListScreen = "task_list"
    const val AddTaskScreen = "add_task"
    const val TaskDetailScreen = "task_detail/{taskId}"
    const val EditTaskScreen = "edit_task/{taskId}"

    // Helper functions to build routes with parameters
    fun constructionDetail(constructionId: String): String = "construction_detail/$constructionId"
    fun editConstruction(constructionId: String): String = "edit_construction/$constructionId"
}