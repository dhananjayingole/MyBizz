package eu.tutorials.mybizz.UIScreens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import eu.tutorials.mybizz.Logic.Auth.AuthRepository
import eu.tutorials.mybizz.Navigation.Routes
import eu.tutorials.mybizz.R

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, authRepo: AuthRepository) {
    var showAboutUs by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showAboutUs) {
        AboutUsScreen(onBackClick = { showAboutUs = false })
        return
    }

    if (showPrivacyPolicy) {
        PrivacyPolicyScreen(onBackClick = { showPrivacyPolicy = false })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "App Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SettingItem(
                        icon = painterResource(R.drawable.img_21),
                        title = "About Us",
                        subtitle = "Learn more about MyBiz",
                        onClick = { showAboutUs = true }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingItem(
                        icon = painterResource(R.drawable.img_24),
                        title = "Privacy Policy",
                        subtitle = "How we handle your data",
                        onClick = { showPrivacyPolicy = true }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingItem(
                        icon = painterResource(R.drawable.img_19),
                        title = "Email Support",
                        subtitle = "Contact our support team",
                        onClick = {
                            sendSupportEmail(context)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingItem(
                        icon = painterResource(R.drawable.img_23),
                        title = "Share App",
                        subtitle = "Share MyBiz with others",
                        onClick = {
                            shareApp(context)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingItem(
    icon: Painter,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    isDestructive: Boolean = false
) {
    Card(
        onClick = { onClick?.invoke() },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = androidx.compose.ui.graphics.Color.Unspecified // show original icon color
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )

                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            if (onClick != null) {
                Icon(
                    painter = painterResource(R.drawable.img),
                    contentDescription = "Navigate",
                    tint = androidx.compose.ui.graphics.Color.Unspecified, // show original arrow color
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}


fun sendSupportEmail(context: android.content.Context) {
    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:support@mybiz.com")
        putExtra(Intent.EXTRA_SUBJECT, "MyBiz App Support Request")
        putExtra(
            Intent.EXTRA_TEXT,
            "Hello MyBiz Support Team,\n\nI need assistance with:\n\n[Please describe your issue here]\n\nDevice Information:\n- App Version: 1.0.0\n- Android Version: ${Build.VERSION.RELEASE}\n\nThank you!"
        )
    }

    try {
        context.startActivity(Intent.createChooser(emailIntent, "Send email using"))
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "No email app found. Please contact support@mybiz.com",
            Toast.LENGTH_LONG
        ).show()
    }
}

/* -------------------------------------------------------------------------- */
/*                             SHARE APP FUNCTION                             */
/* -------------------------------------------------------------------------- */
fun shareApp(context: android.content.Context) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Check out MyBiz App")
        putExtra(
            Intent.EXTRA_TEXT,
            "Discover MyBiz - Your comprehensive business management solution! " +
                    "Manage bills, rentals, tasks, and construction projects all in one place.\n\n" +
                    "Download now and streamline your business operations!\n\n" +
                    "Download link: https://play.google.com/store/apps/details?id=eu.tutorials.mybizz"
        )
    }

    try {
        context.startActivity(Intent.createChooser(shareIntent, "Share MyBiz App"))
    } catch (e: Exception) {
        Toast.makeText(context, "Unable to share app", Toast.LENGTH_SHORT).show()
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutUsScreen(onBackClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About Us") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "MyBiz - Personal Business Management System",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Welcome to MyBiz, your comprehensive solution for managing business operations efficiently. Our application is designed specifically for property owners and construction project managers to streamline administrative tasks and maintain complete control over their business activities.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Text(
                        text = "Key Features:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    FeatureItem("📊 Bills Management - Track and manage all your payments")
                    FeatureItem("🏢 Rentals Management - Monitor rental properties and payments")
                    FeatureItem("✅ Task Management - Assign and track worker tasks")
                    FeatureItem("🏗️ Construction & Plots - Monitor projects and plot information")
                    FeatureItem("🔐 Secure Access - Role-based authentication system")
                    FeatureItem("📱 Mobile First - Optimized for Android devices")

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Our Mission",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "To provide business owners with a simple yet powerful tool that eliminates administrative headaches and lets them focus on growing their business. With real-time data synchronization and secure cloud storage, your business information is always accessible and protected.",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Version 1.0.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBackClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Privacy Policy",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Last updated: ${java.time.LocalDate.now()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    PrivacySection(
                        title = "Data Collection and Usage",
                        content = "MyBiz collects and stores your business data including bills, rental information, task details, and construction project data. This information is used solely to provide you with the application's core functionality and is never shared with third parties for marketing purposes."
                    )

                    PrivacySection(
                        title = "Data Storage",
                        content = "Your operational data is stored securely in Google Sheets, while historical and audit data is maintained in Firebase Firestore. Both platforms provide enterprise-level security and data protection."
                    )

                    PrivacySection(
                        title = "Authentication",
                        content = "We use Firebase Authentication to securely manage user accounts. Your login credentials are protected using industry-standard encryption methods."
                    )

                    PrivacySection(
                        title = "User Roles and Access",
                        content = "The application implements role-based access control. Administrators have full access to CRUD operations, while regular users have limited read-only access as defined by the administrator."
                    )

                    PrivacySection(
                        title = "Data Ownership",
                        content = "You retain complete ownership of all your business data. MyBiz acts as a service provider to help you manage and organize your information more efficiently."
                    )

                    PrivacySection(
                        title = "Contact Information",
                        content = "For any privacy-related concerns or questions, please contact our support team through the Help & Support section in the app."
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "By using MyBiz, you agree to the terms outlined in this privacy policy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun PrivacySection(title: String, content: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun FeatureItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "•",
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
