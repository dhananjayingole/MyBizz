package eu.tutorials.mybizz.Navigation

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import eu.tutorials.mybizz.Chatbot.ChatScreen
import eu.tutorials.mybizz.Logic.Auth.AuthRepository
import eu.tutorials.mybizz.Logic.Bill.BillRepository
import eu.tutorials.mybizz.Logic.Construction.ConstructionRepository
import eu.tutorials.mybizz.Logic.Construction.ConstructionSheetsRepository
import eu.tutorials.mybizz.Logic.Rental.RentalRepository
import eu.tutorials.mybizz.Logic.Rental.RentalSheetsRepository
import eu.tutorials.mybizz.Logic.Task.TaskRepository
import eu.tutorials.mybizz.Logic.Task.TaskSheetsRepository
import eu.tutorials.mybizz.Logic.plot.PlotRepository
import eu.tutorials.mybizz.Logic.plot.PlotSheetsRepository
import eu.tutorials.mybizz.Model.Construction
import eu.tutorials.mybizz.Model.Plot
import eu.tutorials.mybizz.Model.Rental
import eu.tutorials.mybizz.Model.Bill
import eu.tutorials.mybizz.Navigation.Routes.PaymentScreen
import eu.tutorials.mybizz.Payments.PaymentScreen
import eu.tutorials.mybizz.Payments.PaymentSheetsRepository
import eu.tutorials.mybizz.Reporting.MonthlyReportScreen
import eu.tutorials.mybizz.Reporting.MonthlyReportViewModel
import eu.tutorials.mybizz.Repository.BillSheetsRepository
import eu.tutorials.mybizz.UIScreens.*
import eu.tutorials.mybizz.bankingSms.BankSMSScreen
import kotlinx.coroutines.launch

@SuppressLint("ViewModelConstructorInComposable")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NavGraph(
    navController: NavHostController,
    context: Context
) {
    val authRepository = remember { AuthRepository.getInstance(context) }

    val rentalSheetsRepo = remember { RentalSheetsRepository(context) }
    val rentalRepo = remember { RentalRepository() }
    val scope = rememberCoroutineScope()
    val constructionSheetsRepo = remember { ConstructionSheetsRepository(context) }
    val constructionRepository = remember { ConstructionRepository() }
    val taskSheetsRepo = remember { TaskSheetsRepository(context) }

    // NEW: Add Bill and Payment repositories
    val billSheetsRepo = remember { BillSheetsRepository(context) }
    val billRepo = remember { BillRepository() }
    val paymentSheetsRepo = remember { PaymentSheetsRepository(context) }
    // plot sheet Repos.
    val plotSheetsRepo = remember { PlotSheetsRepository(context) }
    val plotRepository = remember { PlotRepository() }
    val taskRepo = remember { TaskRepository() }

    NavHost(
        navController = navController,
        startDestination = Routes.SplashScreen
    ) {
        composable(Routes.SplashScreen) { SplashScreen(navController) }
        composable(Routes.LoginScreen) { LoginScreen(navController, authRepository) }
        composable(Routes.SignUpScreen) { SignupScreen(navController, authRepository) }
        composable(Routes.AdminDashboardScreen) { AdminDashboardScreen(navController, authRepository) }
        composable(Routes.UserDashboardScreen) { UserDashboardScreen(navController, authRepository) }
        composable(Routes.ProfileScreen) { ProfileScreen(navController, authRepository) }
        composable(Routes.BillsListScreen) { BillsListScreen(navController, authRepository) }
        composable(Routes.AddBillScreen) { AddBillScreen(navController, authRepository) }
        composable(Routes.SettingScreen) { SettingsScreen(navController, authRepository) }

        composable(
            route = Routes.EditBillScreen,
            arguments = listOf(navArgument("billId") { type = NavType.StringType })
        ) { backStackEntry ->
            val billId = backStackEntry.arguments?.getString("billId") ?: ""
            EditBillScreen(navController, billId, authRepository)
        }

        composable(
            route = Routes.BillDetailsScreen,
            arguments = listOf(navArgument("billId") { type = NavType.StringType })
        ) { backStackEntry ->
            val billId = backStackEntry.arguments?.getString("billId") ?: ""
            BillDetailsScreen(navController, billId, authRepository)
        }

        // NEW: Payment Screen for Bills
        composable(
            route = "payment_bill/{billId}",
            arguments = listOf(navArgument("billId") { type = NavType.StringType })
        ) { backStackEntry ->
            val billId = backStackEntry.arguments?.getString("billId") ?: ""
            var bill by remember { mutableStateOf<Bill?>(null) }
            var isLoading by remember { mutableStateOf(true) }

            LaunchedEffect(billId) {
                bill = billSheetsRepo.getBillById(billId)
                isLoading = false
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                bill?.let { b ->
                    PaymentScreen(
                        bill = b,
                        rental = null,
                        onPaymentSuccess = {
                            // Navigate back to bill details with refresh flag
                            navController.navigate(Routes.BillsListScreen) {
                                popUpTo(Routes.BillsListScreen) { inclusive = true }
                            }
                        },
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }

        // ------------------ Rental Screens ------------------

        // Rental List
        composable(Routes.RentalListScreen) {
            var rentals by remember { mutableStateOf<List<Rental>>(emptyList()) }
            var isLoading by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                rentals = rentalSheetsRepo.getAllRentals()
                isLoading = false
            }

            RentalListScreen(
                sheetsRepo = rentalSheetsRepo,
                onRentalSelected = { rental ->
                    navController.navigate("${Routes.RentalDetailScreen}/${rental.id}")
                },
                onAddRental = { navController.navigate(Routes.AddRentalScreen) },
                onBack = {navController.popBackStack()}
            )
        }

        // Add Rental
        composable(Routes.AddRentalScreen) {
            AddRentalScreen(
                sheetsRepo = rentalSheetsRepo,
                onRentalAdded = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        // Rental Detail
        composable(
            route = "${Routes.RentalDetailScreen}/{rentalId}",
            arguments = listOf(navArgument("rentalId") { type = NavType.StringType })
        ) { backStackEntry ->
            val rentalId = backStackEntry.arguments?.getString("rentalId") ?: ""
            var rental by remember { mutableStateOf<Rental?>(null) }

            LaunchedEffect(rentalId) {
                rental = rentalSheetsRepo.getAllRentals().find { it.id == rentalId }
            }

            rental?.let { r ->
                RentalDetailScreen(
                    rental = r,
                    onEdit = { navController.navigate("${Routes.EditRentalScreen}/${r.id}") },
                    onDelete = {
                        scope.launch {
                            rentalRepo.deleteRental(r.id, rentalSheetsRepo)
                            navController.popBackStack()
                        }
                    },
                    onMarkPaid = {
                        scope.launch {
                            rentalRepo.markRentalAsPaid(r.id, rentalSheetsRepo)
                            navController.navigate(Routes.RentalListScreen) {
                                popUpTo(Routes.RentalListScreen) { inclusive = true }
                            }
                        }
                    },
                    onBack = { navController.popBackStack() },
                    navController = navController // NEW: Pass navController
                )
            } ?: run {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        // Edit Rental
        composable(
            route = "${Routes.EditRentalScreen}/{rentalId}",
            arguments = listOf(navArgument("rentalId") { type = NavType.StringType })
        ) { backStackEntry ->
            val rentalId = backStackEntry.arguments?.getString("rentalId") ?: ""
            var rental by remember { mutableStateOf<Rental?>(null) }

            LaunchedEffect(rentalId) {
                rental = rentalSheetsRepo.getAllRentals().find { it.id == rentalId }
            }

            rental?.let { r ->
                EditRentalScreen(
                    rental = r,
                    sheetsRepo = rentalSheetsRepo,
                    onRentalUpdated = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            } ?: run {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        // NEW: Payment Screen for Rentals
        composable(
            route = "payment_rental/{rentalId}",
            arguments = listOf(navArgument("rentalId") { type = NavType.StringType })
        ) { backStackEntry ->
            val rentalId = backStackEntry.arguments?.getString("rentalId") ?: ""
            var rental by remember { mutableStateOf<Rental?>(null) }
            var isLoading by remember { mutableStateOf(true) }

            LaunchedEffect(rentalId) {
                rental = rentalSheetsRepo.getAllRentals().find { it.id == rentalId }
                isLoading = false
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                rental?.let { r ->
                    PaymentScreen(
                        bill = null,
                        rental = r,
                        onPaymentSuccess = {
                            // Navigate back to rental list with refresh
                            navController.navigate(Routes.RentalListScreen) {
                                popUpTo(Routes.RentalListScreen) { inclusive = true }
                            }
                        },
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }

        // Construction List
        composable(Routes.ConstructionListScreen) {
            ConstructionListScreen(
                sheetsRepo = constructionSheetsRepo,
                onAddClicked = { navController.navigate(Routes.AddConstructionScreen) },
                navController = navController,
                onBack = {navController.popBackStack()}
            )
        }

        composable(Routes.PlotAndConstructionEntry) {
            PlotAndConstructionEntry(
                navController = navController,
                sheetsRepo = plotSheetsRepo,
                constructionSheetsRepo = constructionSheetsRepo
            )
        }

        // Add Construction
        composable(Routes.AddConstructionScreen) {
            AddConstructionScreen(
                sheetsRepo = constructionSheetsRepo,
                onBack = { navController.popBackStack() }
            )
        }

        // Construction Detail
        composable(
            route = "construction_detail/{constructionId}",
            arguments = listOf(navArgument("constructionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val constructionId = backStackEntry.arguments?.getString("constructionId") ?: ""
            var construction by remember { mutableStateOf<Construction?>(null) }

            LaunchedEffect(constructionId) {
                val allConstructions = constructionSheetsRepo.getAllConstructions()
                construction = allConstructions.find { it.id == constructionId }
            }

            construction?.let { constr ->
                ConstructionDetailScreen(
                    construction = constr,
                    onEdit = { project ->
                        navController.navigate("edit_construction/${project.id}")
                    },
                    onDelete = { project ->
                        scope.launch {
                            constructionRepository.deleteConstruction(project.id, constructionSheetsRepo)
                            navController.popBackStack()
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            } ?: run {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        // Edit Construction
        composable(
            route = "edit_construction/{constructionId}",
            arguments = listOf(navArgument("constructionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val constructionId = backStackEntry.arguments?.getString("constructionId") ?: ""
            var construction by remember { mutableStateOf<Construction?>(null) }

            LaunchedEffect(constructionId) {
                val allConstructions = constructionSheetsRepo.getAllConstructions()
                construction = allConstructions.find { it.id == constructionId }
            }

            construction?.let { constr ->
                EditConstructionScreen(
                    sheetsRepo = constructionSheetsRepo,
                    existing = constr,
                    onBack = { navController.popBackStack() }
                )
            } ?: run {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        // Task screens
        composable(Routes.TaskListScreen) {
            TaskListScreen(
                navController = navController,
                sheetsRepo = taskSheetsRepo
            )
        }

        composable(Routes.AddTaskScreen) {
            AddTaskScreen(
                navController = navController,
                sheetsRepo = taskSheetsRepo
            )
        }

        composable(
            route = Routes.TaskDetailScreen,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId") ?: ""
            TaskDetailScreen(
                navController = navController,
                taskId = taskId,
                sheetsRepo = taskSheetsRepo
            )
        }

        composable(
            route = Routes.EditTaskScreen,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId") ?: ""
            EditTaskScreen(
                navController = navController,
                taskId = taskId,
                sheetsRepo = taskSheetsRepo
            )
        }

        composable(Routes.UserManagementScreen){
            UserManagementScreen(
                navController = navController,
                authRepository
            )
        }

        // Plot List Screen
        composable(Routes.PlotListScreen) {
            PlotListScreen(
                navController = navController,
                sheetsRepo = plotSheetsRepo,
                onAddClicked = { navController.navigate(Routes.AddPlotScreen) },
                onBack = { navController.popBackStack() }
            )
        }

        // Add Plot Screen
        composable(Routes.AddPlotScreen) {
            AddPlotScreen(
                sheetsRepo = plotSheetsRepo,
                onBack = { navController.popBackStack() }
            )
        }

        // Plot Detail Screen
        composable(
            route = Routes.PlotDetailScreen,
            arguments = listOf(navArgument("plotId") { type = NavType.StringType })
        ) { backStackEntry ->
            val plotId = backStackEntry.arguments?.getString("plotId") ?: ""
            var plot by remember { mutableStateOf<Plot?>(null) }

            LaunchedEffect(plotId) {
                val allPlots = plotSheetsRepo.getAllPlots()
                plot = allPlots.find { it.id == plotId }
            }

            plot?.let { plotItem ->
                PlotDetailScreen(
                    plot = plotItem,
                    onEdit = { plot ->
                        navController.navigate("editplotscreen/${plot.id}")
                    },
                    onDelete = { plot ->
                        scope.launch {
                            plotRepository.deletePlot(plot.id, plotSheetsRepo)
                            navController.popBackStack()
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            } ?: run {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        // Edit Plot Screen
        composable(
            route = Routes.EditPlotScreen,
            arguments = listOf(navArgument("plotId") { type = NavType.StringType })
        ) { backStackEntry ->
            val plotId = backStackEntry.arguments?.getString("plotId") ?: ""
            var plot by remember { mutableStateOf<Plot?>(null) }

            LaunchedEffect(plotId) {
                val allPlots = plotSheetsRepo.getAllPlots()
                plot = allPlots.find { it.id == plotId }
            }

            plot?.let { plotItem ->
                EditPlotScreen(
                    sheetsRepo = plotSheetsRepo,
                    existing = plotItem,
                    onBack = { navController.popBackStack() }
                )
            } ?: run {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        // Monthly Report Screen
        composable(Routes.MonthlyReportScreen) {
            val viewModel: MonthlyReportViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        val application = context.applicationContext as Application
                        return MonthlyReportViewModel(application) as T
                    }
                }
            )
            MonthlyReportScreen(
                viewModel = viewModel,
                navController = navController
            )
        }

//        Chatbot Screen
        composable(Routes.ChatScreen){
            ChatScreen(navController = navController)
        }

        composable(Routes.BankSMSScreen){
            BankSMSScreen({navController.popBackStack()})
        }
    }
}
