package eu.tutorials.mybizz.Navigation

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
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
import eu.tutorials.mybizz.Logic.Rental.RentalSharedViewModel
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

    val billSheetsRepo = remember { BillSheetsRepository(context) }
    val billRepo = remember { BillRepository() }
    val paymentSheetsRepo = remember { PaymentSheetsRepository(context) }

    val plotSheetsRepo = remember { PlotSheetsRepository(context) }
    val plotRepository = remember { PlotRepository() }
    val taskRepo = remember { TaskRepository() }

    // ── Shared ViewModel for tenant → properties flow ─────────────────────────
    // viewModel() scopes it to the NavGraph's lifecycle so it survives
    // navigation between rental screens without being recreated.
    val rentalSharedViewModel: RentalSharedViewModel = viewModel()

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

        // Payment Screen for Bills
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
                            navController.navigate(Routes.BillsListScreen) {
                                popUpTo(Routes.BillsListScreen) { inclusive = true }
                            }
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }

        // ── RENTAL SCREENS ────────────────────────────────────────────────────

        // ① Rental List — ONE card per unique tenant name
        composable(Routes.RentalListScreen) {
            RentalListScreen(
                sheetsRepo = rentalSheetsRepo,
                onTenantSelected = { tenantName, tenantRentals ->
                    // Store in shared VM, then navigate to the properties screen
                    rentalSharedViewModel.selectTenant(tenantName, tenantRentals)
                    navController.navigate(Routes.TenantPropertiesScreen)
                },
                onAddRental = { navController.navigate(Routes.AddRentalScreen) },
                onBack = { navController.popBackStack() }
            )
        }

        // ② Tenant Properties — all properties for the selected tenant
        composable(Routes.TenantPropertiesScreen) {
            TenantPropertiesScreen(
                viewModel = rentalSharedViewModel,
                onPropertySelected = { rental ->
                    // Navigate to the existing detail screen, passing rental id
                    navController.navigate("${Routes.RentalDetailScreen}/${rental.id}")
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ③ Add Rental — unchanged
        composable(Routes.AddRentalScreen) {
            AddRentalScreen(
                sheetsRepo = rentalSheetsRepo,
                onRentalAdded = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        // ④ Rental Detail — unchanged structure, but now also updates the shared VM
        //    so the properties screen reflects edits / deletes / mark-paid on back press.
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
                    onEdit = {
                        navController.navigate("${Routes.EditRentalScreen}/${r.id}")
                    },
                    onDelete = {
                        scope.launch {
                            rentalRepo.deleteRental(r.id, rentalSheetsRepo)
                            rentalSharedViewModel.removeRental(r.id)   // keep VM in sync
                            navController.popBackStack()               // back to TenantPropertiesScreen
                        }
                    },
                    onMarkPaid = {
                        scope.launch {
                            rentalRepo.markRentalAsPaid(r.id, rentalSheetsRepo)
                            // Build updated copy and refresh VM so list reflects new status
                            val updated = r.copy(
                                status = Rental.STATUS_PAID,
                                paymentDate = java.text.SimpleDateFormat(
                                    "yyyy-MM-dd", java.util.Locale.getDefault()
                                ).format(java.util.Date())
                            )
                            rentalSharedViewModel.refreshRental(updated) // keep VM in sync
                            navController.popBackStack()                 // back to TenantPropertiesScreen
                        }
                    },
                    onBack = { navController.popBackStack() },
                    navController = navController
                )
            } ?: run {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        // ⑤ Edit Rental — unchanged
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
                    onRentalUpdated = {
                        // Refresh the VM entry so TenantPropertiesScreen shows new data
                        scope.launch {
                            val refreshed = rentalSheetsRepo.getAllRentals().find { it.id == r.id }
                            refreshed?.let { rentalSharedViewModel.refreshRental(it) }
                        }
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            } ?: run {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        // ⑥ Payment Screen for Rentals — unchanged
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
                            navController.navigate(Routes.RentalListScreen) {
                                popUpTo(Routes.RentalListScreen) { inclusive = true }
                            }
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }

        // ── CONSTRUCTION SCREENS ──────────────────────────────────────────────

        composable(Routes.ConstructionListScreen) {
            ConstructionListScreen(
                sheetsRepo = constructionSheetsRepo,
                onAddClicked = { navController.navigate(Routes.AddConstructionScreen) },
                navController = navController,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PlotAndConstructionEntry) {
            PlotAndConstructionEntry(
                navController = navController,
                sheetsRepo = plotSheetsRepo,
                constructionSheetsRepo = constructionSheetsRepo
            )
        }

        composable(Routes.AddConstructionScreen) {
            AddConstructionScreen(
                sheetsRepo = constructionSheetsRepo,
                onBack = { navController.popBackStack() }
            )
        }

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

        // ── TASK SCREENS ──────────────────────────────────────────────────────

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

        composable(Routes.UserManagementScreen) {
            UserManagementScreen(
                navController = navController,
                authRepository
            )
        }

        // ── PLOT SCREENS ──────────────────────────────────────────────────────

        composable(Routes.PlotListScreen) {
            PlotListScreen(
                navController = navController,
                sheetsRepo = plotSheetsRepo,
                onAddClicked = { navController.navigate(Routes.AddPlotScreen) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.AddPlotScreen) {
            AddPlotScreen(
                sheetsRepo = plotSheetsRepo,
                onBack = { navController.popBackStack() }
            )
        }

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

        // ── REPORTING ─────────────────────────────────────────────────────────

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

        // ── CHATBOT / SMS ─────────────────────────────────────────────────────

        composable(Routes.ChatScreen) {
            ChatScreen(navController = navController)
        }

        composable(Routes.BankSMSScreen) {
            BankSMSScreen({ navController.popBackStack() })
        }
    }
}