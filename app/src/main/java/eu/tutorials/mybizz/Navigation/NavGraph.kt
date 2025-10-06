// Navigation/NavGraph.kt
package eu.tutorials.mybizz.Navigation

import android.content.Context
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
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import eu.tutorials.mybizz.Logic.Auth.AuthRepository
import eu.tutorials.mybizz.Logic.Construction.ConstructionRepository
import eu.tutorials.mybizz.Logic.Construction.ConstructionSheetsRepository
import eu.tutorials.mybizz.Logic.Rental.RentalRepository
import eu.tutorials.mybizz.Logic.Rental.RentalSheetsRepository
import eu.tutorials.mybizz.Logic.Task.TaskRepository
import eu.tutorials.mybizz.Logic.Task.TaskSheetsRepository
import eu.tutorials.mybizz.Model.Construction
import eu.tutorials.mybizz.Model.Rental
import eu.tutorials.mybizz.Model.Task
import eu.tutorials.mybizz.Navigation.Routes.EditTaskScreen
import eu.tutorials.mybizz.UIScreens.*
import kotlinx.coroutines.launch

@Composable
fun NavGraph(
    navController: NavHostController,
    authRepository: AuthRepository,
    context: Context
) {
    val rentalSheetsRepo = remember { RentalSheetsRepository(context) }
    val rentalRepo = remember { RentalRepository() }
    val scope = rememberCoroutineScope()
    val constructionSheetsRepo = remember { ConstructionSheetsRepository(context) }
    val constructionRepository = remember { ConstructionRepository() }
    val taskSheetsRepo = remember { TaskSheetsRepository(context) } // Add remember
    val taskRepo = remember { TaskRepository() } // Add remember

    NavHost(
        navController = navController,
        startDestination = Routes.SplashScreen
    ) {
        // ------------------ Auth & Main Screens ------------------
        composable(Routes.SplashScreen) { SplashScreen(navController) }
        composable(Routes.LoginScreen) { LoginScreen(navController, authRepository) }
        composable(Routes.SignUpScreen) { SignupScreen(navController, authRepository) }
        composable(Routes.AdminDashboardScreen) { AdminDashboardScreen(navController, authRepository) }
        composable(Routes.UserDashboardScreen) { UserDashboardScreen(navController, authRepository) }
        composable(Routes.ProfileScreen) { ProfileScreen(navController, authRepository) }
        composable(Routes.BillsListScreen) { BillsListScreen(navController, authRepository) }
        composable(Routes.AddBillScreen) { AddBillScreen(navController, authRepository) }
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
                onAddRental = { navController.navigate(Routes.AddRentalScreen) }
            )
        }

        // Add Rental
        composable(Routes.AddRentalScreen) {
            AddRentalScreen(
                sheetsRepo = rentalSheetsRepo,
                onRentalAdded = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
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
                        scope.launch { rentalRepo.markRentalAsPaid(r.id, rentalSheetsRepo) }
                    }
                )
            } ?: run {
                // Show loading while rental is fetched
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
                    onCancel = { navController.popBackStack() }
                )
            } ?: run {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
        // Construction List
        composable(Routes.ConstructionListScreen) {
            ConstructionListScreen(
                sheetsRepo = constructionSheetsRepo,
                onAddClicked = { navController.navigate(Routes.AddConstructionScreen) },
                navController = navController
            )
        }

// Add Construction
        composable(Routes.AddConstructionScreen) {
            AddConstructionScreen(
                sheetsRepo = constructionSheetsRepo,
                onBack = { navController.popBackStack() }
            )
        }

// Construction Detail - FIXED ROUTE
        composable(
            route = "construction_detail/{constructionId}",
            arguments = listOf(navArgument("constructionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val constructionId = backStackEntry.arguments?.getString("constructionId") ?: ""
            var construction by remember { mutableStateOf<Construction?>(null) }

            LaunchedEffect(constructionId) {
                // You need to implement this method in your repository
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

// Edit Construction - FIXED ROUTE
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

        // Navigation/NavGraph.kt - Update the task section
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
    }
}