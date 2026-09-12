package uz.kodava.studio.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import uz.kodava.studio.data.Aspects
import uz.kodava.studio.data.Styles
import uz.kodava.studio.ui.AppViewModel
import uz.kodava.studio.ui.InfoCard
import uz.kodava.studio.ui.KodavaScaffold

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewProjectScreen(vm: AppViewModel, nav: NavController) {
    var idea by remember { mutableStateOf("") }
    var style by remember { mutableStateOf(Styles.CINEMATIC) }
    var aspect by remember { mutableStateOf("16:9") }
    var sceneCount by remember { mutableStateOf(6f) }
    val selectedActors = remember { mutableStateListOf<String>() }

    KodavaScaffold(title = "Yangi loyiha", vm = vm, onBack = { nav.popBackStack() }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(PaddingValues(16.dp, 8.dp, 16.dp, 32.dp)),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text("G'oyangiz", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = idea,
                onValueChange = { idea = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp),
                placeholder = {
                    Text("Masalan: Toshkentlik yosh dasturchi eski chemodan ichidan otasining maktubini topadi va uni izlab Samarqandga yo'l oladi…")
                }
            )

            Text("Uslub", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Styles.all.forEach { option ->
                    FilterChip(
                        selected = style == option,
                        onClick = { style = option },
                        label = { Text(option) }
                    )
                }
            }

            Text("Kadr nisbati", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Aspects.all.forEach { option ->
                    FilterChip(
                        selected = aspect == option,
                        onClick = { aspect = option },
                        label = { Text(option) }
                    )
                }
            }

            Text("Sahnalar soni: ${sceneCount.toInt()}", style = MaterialTheme.typography.titleMedium)
            Slider(
                value = sceneCount,
                onValueChange = { sceneCount = it },
                valueRange = 3f..16f,
                steps = 12
            )

            Text("Aktyorlar", style = MaterialTheme.typography.titleMedium)
            if (vm.actors.isEmpty()) {
                InfoCard("Aktyor qo'shilmagan. Yuzlar bir xil chiqishi uchun avval aktyor va uning suratini qo'shing.")
                TextButton(onClick = { nav.navigate("actors") }) { Text("Aktyor qo'shish") }
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    vm.actors.forEach { actor ->
                        val selected = selectedActors.contains(actor.id)
                        FilterChip(
                            selected = selected,
                            onClick = {
                                if (selected) selectedActors.remove(actor.id) else selectedActors.add(actor.id)
                            },
                            label = { Text(actor.name.ifBlank { "Nomsiz" }) }
                        )
                    }
                }
            }

            Button(
                onClick = {
                    vm.createProject(
                        idea = idea.trim(),
                        style = style,
                        aspect = aspect,
                        sceneCount = sceneCount.toInt(),
                        actorIds = selectedActors.toList()
                    ) { id ->
                        nav.popBackStack()
                        nav.navigate("project/$id")
                    }
                },
                enabled = idea.trim().length >= 10 && vm.busy == null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Senariy yaratish")
            }

            if (!vm.hasKey()) {
                InfoCard("Diqqat: API kalit kiritilmagan. Sozlamalar bo'limiga kirib kalitni qo'shing.")
            }
        }
    }
}
