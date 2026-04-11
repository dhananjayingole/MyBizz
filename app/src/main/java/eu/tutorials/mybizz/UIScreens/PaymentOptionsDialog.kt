package eu.tutorials.mybizz.UIScreens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import eu.tutorials.mybizz.Model.Rental
import eu.tutorials.mybizz.R
import java.io.File
import java.io.FileOutputStream

@Composable
fun PaymentOptionsDialog(
    rental: Rental,
    onDismiss: () -> Unit,
    onPayHere: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    stringResource(R.string.payment_options),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "₹${String.format("%.2f", rental.rentAmount)} for ${rental.tenantName}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                Divider()

                // WhatsApp Share Button
                Button(
                    onClick = {
                        sharePaymentViaWhatsApp(context, rental)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF25D366) // WhatsApp green
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.share_via_whatsapp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Pay Here Button
                Button(
                    onClick = {
                        onDismiss()
                        onPayHere()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        stringResource(R.string.pay_here_upi),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel), color = Color.Gray)
                }
            }
        }
    }
}

fun sharePaymentViaWhatsApp(context: android.content.Context, rental: Rental) {
    val paymentMessage = """
🏠 *Rent Payment Request*
━━━━━━━━━━━━━━━━━━
👤 Tenant: ${rental.tenantName}
🏠 Property: ${rental.property}
📅 Month: ${rental.month}
💰 Amount Due: ₹${String.format("%.2f", rental.rentAmount)}
━━━━━━━━━━━━━━━━━━

*Payment Gateway - Navtej Singh*
🏦 Bank: Axis Bank
📋 A/C: 41439039398
🔑 IFSC: SBIN00021047

📱 *Google Pay Only:* 7350337139
🔗 UPI ID: navtejsingh891323-1@okaxis

✅ After payment, please send screenshot as confirmation.

🙏 Thank you for your payment!
    """.trimIndent()

    val phoneNumber = rental.contactNo.replace("+91", "").replace(" ", "").trim()

    // Try to share QR image + message together
    try {
        val qrImageUri = getQrImageUri(context)

        if (qrImageUri != null) {
            // Share with QR image
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, qrImageUri)
                putExtra(Intent.EXTRA_TEXT, paymentMessage)
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Try direct WhatsApp chat with phone number
            if (phoneNumber.isNotBlank()) {
                try {
                    val directIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/*"
                        putExtra(Intent.EXTRA_STREAM, qrImageUri)
                        putExtra(Intent.EXTRA_TEXT, paymentMessage)
                        putExtra("jid", "91$phoneNumber@s.whatsapp.net")
                        setPackage("com.whatsapp")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(directIntent)
                    return
                } catch (e: Exception) {
                    context.startActivity(sendIntent)
                    return
                }
            }
            context.startActivity(sendIntent)
        } else {
            // Fallback: text only
            shareTextOnlyWhatsApp(context, phoneNumber, paymentMessage)
        }
    } catch (e: Exception) {
        // Fallback if WhatsApp not installed
        shareTextOnlyWhatsApp(context, phoneNumber, paymentMessage)
    }
}

fun getQrImageUri(context: android.content.Context): Uri? {
    return try {
        // Load QR from drawable resources
        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.img_28)
            ?: return null

        // Save to cache dir for sharing
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "payment_qr.png")
        val fos = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        fos.close()

        androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun shareTextOnlyWhatsApp(
    context: android.content.Context,
    phoneNumber: String,
    message: String
) {
    try {
        if (phoneNumber.isNotBlank()) {
            // Direct chat via URL
            val url = "https://api.whatsapp.com/send?phone=91$phoneNumber&text=${Uri.encode(message)}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } else {
            // Open WhatsApp chooser
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                setPackage("com.whatsapp")
            }
            context.startActivity(sendIntent)
        }
    } catch (e: Exception) {
        // WhatsApp not installed - open general share
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share via"))
    }
}