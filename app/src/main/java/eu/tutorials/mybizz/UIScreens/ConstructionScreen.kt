package eu.tutorials.mybizz.UIScreens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.tutorials.mybizz.Model.Construction
import eu.tutorials.mybizz.Logic.Construction.ConstructionSheetsRepository
import eu.tutorials.mybizz.Logic.Construction.ConstructionRepository
import kotlinx.coroutines.launch
import java.util.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.navigation.NavController
import androidx.navigation.NavHostController

@Composable
fun AddConstructionScreen(
    sheetsRepo: ConstructionSheetsRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val repository = ConstructionRepository()

    var projectName by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("In Progress") }
    var notes by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Add Construction Project", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(value = projectName, onValueChange = { projectName = it }, label = { Text("Project Name") })
        OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") })
        OutlinedTextField(value = startDate, onValueChange = { startDate = it }, label = { Text("Start Date") })
        OutlinedTextField(value = endDate, onValueChange = { endDate = it }, label = { Text("End Date") })
        OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text("Cost") })
        OutlinedTextField(value = status, onValueChange = { status = it }, label = { Text("Status") })
        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") })

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            scope.launch {
                val construction = Construction(
                    id = UUID.randomUUID().toString(),
                    projectName = projectName,
                    location = location,
                    startDate = startDate,
                    endDate = endDate,
                    cost = cost,
                    status = status,
                    notes = notes
                )
                repository.addConstruction(construction, sheetsRepo)
                onBack()
            }
        }) {
            Text("Save")
        }
    }
}

@Composable
fun ConstructionListScreen(
    navController: NavHostController,
    sheetsRepo: ConstructionSheetsRepository,
    onAddClicked: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val repository = ConstructionRepository()

    var constructions by remember { mutableStateOf<List<Construction>>(emptyList()) }

    LaunchedEffect(true) {
        scope.launch {
            constructions = repository.getAllConstructions(sheetsRepo)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Construction Projects", style = MaterialTheme.typography.titleLarge)
            Button(onClick = onAddClicked) { Text("Add") }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn {
            items(constructions) { construction ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            navController.navigate("construction_detail/${construction.id}")
                        }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(construction.projectName, style = MaterialTheme.typography.titleMedium)
                        Text("Location: ${construction.location}")
                        Text("Status: ${construction.status}")
                    }
                }
            }
        }
    }
}

@Composable
fun ConstructionDetailScreen(
    construction: Construction,
    onEdit: (Construction) -> Unit,
    onDelete: (Construction) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(construction.projectName, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        Text("Location: ${construction.location}")
        Text("Start Date: ${construction.startDate}")
        Text("End Date: ${construction.endDate}")
        Text("Cost: ${construction.cost}")
        Text("Status: ${construction.status}")
        Text("Notes: ${construction.notes}")

        Spacer(modifier = Modifier.height(16.dp))

        Row {
            Button(onClick = { onEdit(construction) }) { Text("Edit") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { onDelete(construction) }) { Text("Delete") }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBack) { Text("Back") }
    }
}

@Composable
fun EditConstructionScreen(
    sheetsRepo: ConstructionSheetsRepository,
    existing: Construction,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val repository = ConstructionRepository()

    var projectName by remember { mutableStateOf(existing.projectName) }
    var location by remember { mutableStateOf(existing.location) }
    var startDate by remember { mutableStateOf(existing.startDate) }
    var endDate by remember { mutableStateOf(existing.endDate) }
    var cost by remember { mutableStateOf(existing.cost) }
    var status by remember { mutableStateOf(existing.status) }
    var notes by remember { mutableStateOf(existing.notes ?: "") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Edit Construction", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(value = projectName, onValueChange = { projectName = it }, label = { Text("Project Name") })
        OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") })
        OutlinedTextField(value = startDate, onValueChange = { startDate = it }, label = { Text("Start Date") })
        OutlinedTextField(value = endDate, onValueChange = { endDate = it }, label = { Text("End Date") })
        OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text("Cost") })
        OutlinedTextField(value = status, onValueChange = { status = it }, label = { Text("Status") })
        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") })

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            scope.launch {
                val updated = existing.copy(
                    projectName = projectName,
                    location = location,
                    startDate = startDate,
                    endDate = endDate,
                    cost = cost,
                    status = status,
                    notes = notes
                )
                repository.updateConstruction(updated, sheetsRepo)
                onBack()
            }
        }) {
            Text("Update")
        }
    }
}