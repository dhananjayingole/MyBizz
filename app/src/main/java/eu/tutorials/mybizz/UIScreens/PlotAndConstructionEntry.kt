package eu.tutorials.mybizz.UIScreens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import eu.tutorials.mybizz.Logic.Construction.ConstructionSheetsRepository
import eu.tutorials.mybizz.Logic.plot.PlotSheetsRepository
import eu.tutorials.mybizz.Navigation.Routes

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PlotAndConstructionEntry(
    navController: NavHostController,
    sheetsRepo: PlotSheetsRepository,
    constructionSheetsRepo: ConstructionSheetsRepository
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Plot, 1 = Construction

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 18.dp)
    ) {

        // -------- Centered Toggle Buttons with Beautiful UI ------------
        Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {

            Row(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(5.dp),
                horizontalArrangement = Arrangement.Center
            ) {

                ToggleChip(
                    text = "Plot",
                    isSelected = selectedTab == 0,
                    modifier = Modifier.weight(1f)
                ) {
                    selectedTab = 0
                }

                ToggleChip(
                    text = "Construction",
                    isSelected = selectedTab == 1,
                    modifier = Modifier.weight(1f)
                ) {
                    selectedTab = 1
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // ---------- Animated Screen Switch ------------
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                slideInHorizontally(initialOffsetX = { if (targetState > initialState) it else -it }) +
                        fadeIn() togetherWith
                        slideOutHorizontally(targetOffsetX = { if (targetState > initialState) -it else it }) +
                        fadeOut()
            },
            label = "plot-construction-tabs"
        ) { tab ->

            if (tab == 0) {
                PlotListScreen(
                    navController = navController,
                    sheetsRepo = sheetsRepo,
                    onAddClicked = { navController.navigate(Routes.AddPlotScreen) },
                    onBack = { navController.popBackStack() }
                )
            } else {
                ConstructionListScreen(
                    sheetsRepo = constructionSheetsRepo,
                    navController = navController,
                    onAddClicked = { navController.navigate(Routes.AddConstructionScreen) },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun ToggleChip(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.surfaceVariant

    val textColor = if (isSelected)
        MaterialTheme.colorScheme.onPrimary
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = textColor,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

