// Navigation/NavGraph.kt
package eu.tutorials.mybizz.Navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import eu.tutorials.mybizz.Logic.Auth.AuthRepository
import eu.tutorials.mybizz.UIScreens.*

@Composable
fun NavGraph(
    navController: NavHostController,
    authRepository: AuthRepository
) {
    NavHost(
        navController = navController,
        startDestination = Routes.SplashScreen
    ) {
        composable(Routes.SplashScreen) {
            SplashScreen(navController = navController)
        }

        composable(Routes.LoginScreen) {
            LoginScreen(
                navController = navController,
                authRepo = authRepository
            )
        }

        composable(Routes.SignUpScreen) {
            SignupScreen(
                navController = navController,
                authRepo = authRepository
            )
        }

        composable(Routes.AdminDashboardScreen) {
            AdminDashboardScreen(
                navController = navController,
                authRepo = authRepository
            )
        }

        composable(Routes.UserDashboardScreen) {
            UserDashboardScreen(
                navController = navController,
                authRepo = authRepository
            )
        }

        composable(Routes.BillsListScreen) {
            BillsListScreen(
                navController = navController,
                authRepo = authRepository
            )
        }

        composable(Routes.AddBillScreen) {
            AddBillScreen(
                navController = navController,
                authRepo = authRepository
            )
        }

        composable(
            route = Routes.EditBillScreen,
            arguments = listOf(navArgument("billId") { type = NavType.StringType })
        ) { backStackEntry ->
            val billId = backStackEntry.arguments?.getString("billId") ?: ""
            EditBillScreen(
                navController = navController,
                billId = billId,
                authRepo = authRepository
            )
        }

        composable(
            route = Routes.BillDetailsScreen,
            arguments = listOf(navArgument("billId") { type = NavType.StringType })
        ) { backStackEntry ->
            val billId = backStackEntry.arguments?.getString("billId") ?: ""
            BillDetailsScreen(
                navController = navController,
                billId = billId,
                authRepo = authRepository
            )
        }
    }
}