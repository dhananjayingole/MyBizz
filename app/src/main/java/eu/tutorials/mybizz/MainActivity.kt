package eu.tutorials.mybizz

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.google.firebase.FirebaseApp
import eu.tutorials.mybizz.Navigation.NavGraph
import eu.tutorials.mybizz.Notification.NotificationHelper
import eu.tutorials.mybizz.Notification.NotificationPermissionHelper
import eu.tutorials.mybizz.Notification.ReminderScheduler
import eu.tutorials.mybizz.ui.theme.MyBizzTheme

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Create notification channel
        NotificationHelper.createNotificationChannel(this)

        // Request notification permission for Android 13+
        if (!NotificationPermissionHelper.hasNotificationPermission(this)) {
            NotificationPermissionHelper.requestNotificationPermission(this)
        }

        // Schedule daily reminders
        val scheduler = ReminderScheduler(this)
        scheduler.scheduleDailyReminders()

        // Optionally run an immediate check on app start (only if permission granted)
        if (NotificationPermissionHelper.hasNotificationPermission(this)) {
            scheduler.scheduleImmediateCheck()
        }

        setContent {
            MyBizzTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val context = LocalContext.current
                    NavGraph(navController, context)
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            NotificationPermissionHelper.NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
                    // Run immediate check after permission granted
                    ReminderScheduler(this).scheduleImmediateCheck()
                } else {
                    Toast.makeText(
                        this,
                        "Notification permission denied. You won't receive payment reminders.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}