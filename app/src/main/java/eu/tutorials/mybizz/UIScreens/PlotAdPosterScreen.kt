package eu.tutorials.mybizz.UIScreens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.view.View
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import eu.tutorials.mybizz.Logic.plot.PlotRepository
import eu.tutorials.mybizz.Logic.plot.PlotSheetsRepository
import eu.tutorials.mybizz.Model.Plot
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

// ─────────────────────────────────────────────────────────────────────────────
//  PlotAdBannerSection
//  Drop this anywhere in UserDashboardScreen.
//  It fetches the most-recently-added plot and shows an ad poster card.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PlotAdBannerSection(
    sheetsRepo: PlotSheetsRepository,
    onViewAllPlots: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val plotRepository = remember { PlotRepository() }

    var latestPlot by remember { mutableStateOf<Plot?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            val allPlots = plotRepository.getAllPlots(sheetsRepo)
            // Most recently added = last in sheet (highest index)
            latestPlot = allPlots.lastOrNull()
            isLoading = false
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp))
        }
        return
    }

    val plot = latestPlot ?: return  // nothing to show if no plots exist

    Column(modifier = Modifier.fillMaxWidth()) {

        // Section label
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🏡  Latest Plot Available",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            TextButton(onClick = onViewAllPlots) {
                Text("View All", style = MaterialTheme.typography.labelMedium)
            }
        }

        PlotAdPosterCard(
            plot = plot,
            onShare = { sharePlotPoster(context, plot) }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PlotAdPosterCard  – the visual poster card itself
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PlotAdPosterCard(
    plot: Plot,
    onShare: () -> Unit
) {
    val goldStart  = Color(0xFFFFD700)
    val goldEnd    = Color(0xFFFFA500)
    val darkBg     = Color(0xFF1A1A2E)
    val accentBlue = Color(0xFF4FC3F7)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(darkBg, Color(0xFF16213E))
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(listOf(goldStart, goldEnd)),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // ── Top badge row ─────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.horizontalGradient(listOf(goldStart, goldEnd))
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "🔥  FOR SALE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black,
                            letterSpacing = 1.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0D3B66))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Plot #${plot.plotId.ifEmpty { "N/A" }}",
                            fontSize = 11.sp,
                            color = accentBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // ── Plot name ─────────────────────────────────────────────
                Text(
                    text = plot.plotName.ifEmpty { "Unnamed Plot" },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    lineHeight = 28.sp
                )

                // ── Divider ───────────────────────────────────────────────
                Divider(
                    color = goldStart.copy(alpha = 0.35f),
                    thickness = 1.dp
                )

                // ── Info grid ─────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    PosterInfoChip(
                        label = "📐 Size",
                        value = if (plot.plotSize.isNotEmpty()) "${plot.plotSize} sq.ft" else "—"
                    )
                    PosterInfoChip(
                        label = "💰 Price",
                        value = if (plot.initialPrice.isNotEmpty()) "₹${formatAmount(plot.initialPrice)}" else "—",
                        valueColor = goldStart
                    )
                    PosterInfoChip(
                        label = "🤝 Asking",
                        value = if (plot.askingAmount.isNotEmpty()) "₹${formatAmount(plot.askingAmount)}" else "—",
                        valueColor = Color(0xFF81C784)
                    )
                }

                // ── Location ──────────────────────────────────────────────
                if (plot.location.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = accentBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = plot.location,
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                // ── Notes preview ─────────────────────────────────────────
                if (plot.notes.isNotEmpty()) {
                    Text(
                        text = "\"${plot.notes}\"",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.55f),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        maxLines = 2
                    )
                }

                // ── Bottom row: contact + share ───────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (plot.attendedBy.isNotEmpty()) {
                        Column {
                            Text(
                                text = "Contact Agent",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                            Text(
                                text = plot.attendedBy,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = accentBlue
                            )
                            if (plot.visitorNumber.isNotEmpty()) {
                                Text(
                                    text = plot.visitorNumber,
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onShare,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        border = ButtonDefaults.outlinedButtonBorder,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                brush = Brush.horizontalGradient(listOf(goldStart, goldEnd)),
                                shape = RoundedCornerShape(50.dp)
                            )
                            .clip(RoundedCornerShape(50.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = goldStart,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Share",
                            color = goldStart,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Small helper composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PosterInfoChip(
    label: String,
    value: String,
    valueColor: Color = Color.White
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            textAlign = TextAlign.Center
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Share helper – builds a plain-text share intent with plot details
// ─────────────────────────────────────────────────────────────────────────────

fun sharePlotPoster(context: Context, plot: Plot) {
    val text = buildString {
        appendLine("🏡 *${plot.plotName}* — AVAILABLE FOR SALE!")
        appendLine()
        if (plot.plotId.isNotEmpty())     appendLine("📋 Plot ID   : ${plot.plotId}")
        if (plot.location.isNotEmpty())   appendLine("📍 Location  : ${plot.location}")
        if (plot.plotSize.isNotEmpty())   appendLine("📐 Size      : ${plot.plotSize} sq.ft")
        if (plot.initialPrice.isNotEmpty())appendLine("💰 Price     : ₹${plot.initialPrice}")
        if (plot.askingAmount.isNotEmpty())appendLine("🤝 Asking    : ₹${plot.askingAmount}")
        if (plot.attendedBy.isNotEmpty()) appendLine("👤 Agent     : ${plot.attendedBy}")
        if (plot.visitorNumber.isNotEmpty())appendLine("📞 Contact   : ${plot.visitorNumber}")
        if (plot.notes.isNotEmpty()) {
            appendLine()
            appendLine("📝 ${plot.notes}")
        }
        appendLine()
        appendLine("Interested? Contact us today! 🙌")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_SUBJECT, "Plot Available: ${plot.plotName}")
    }
    context.startActivity(Intent.createChooser(intent, "Share via"))
}

// ─────────────────────────────────────────────────────────────────────────────
//  Utility
// ─────────────────────────────────────────────────────────────────────────────

private fun formatAmount(raw: String): String {
    return try {
        val value = raw.toDouble().toLong()
        when {
            value >= 10_00_000 -> "%.2f L".format(value / 1_00_000.0)
            value >= 1_000     -> "%,d".format(value)
            else               -> value.toString()
        }
    } catch (e: Exception) {
        raw
    }
}