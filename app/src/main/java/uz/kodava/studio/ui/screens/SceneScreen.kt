package uz.kodava.studio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import uz.kodava.studio.media.Exporter
import uz.kodava.studio.ui.AppViewModel
import uz.kodava.studio.ui.InfoCard
import uz.kodava.studio.ui.KodavaScaffold

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SceneScreen(vm: AppViewModel, nav: NavController, sceneNumber: Int) {
    val context = LocalContext.current
    val project = vm.project
    val scene = project?.scenes?.firstOrNull { it.n == sceneNumber }

    if (project == null || scene == null) {
        KodavaScaffold(title = "Sahna", vm = vm, onBack = { nav.popBackStack() }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Sahna topilmadi")
            }
        }
        return
    }

    var prompt by remember(sceneNumber, scene.imagePrompt) { mutableStateOf(scene.imagePrompt) }
    var title by remember(sceneNumber) { mutableStateOf(scene.title) }
    var summary by remember(sceneNumber) { mutableStateOf(scene.summary) }
    var duration by remember(sceneNumber) { mutableStateOf(scene.durationSec.toFloat()) }
    val busy = vm.busyScenes.contains(sceneNumber)

    fun persist() {
        vm.updateScene(sceneNumber) {
            it.copy(
                title = title.trim(),
                summary = summary.trim(),
                imagePrompt = prompt.trim(),
                durationSec = duration.toInt()
            )
        }
    }

    KodavaScaffold(
        title = "Sahna $sceneNumber",
        vm = vm,
        onBack = {
            persist()
            nav.popBackStack()
        },
        actions = {
            scene.imagePath?.let { path ->
                IconButton(onClick = { Exporter.share(context, vm.store.file(path), "image/jpeg") }) {
                    Icon(Icons.Default.Share, contentDescription = "Ulashish")
                }
            }
            IconButton(onClick = {
                vm.deleteScene(sceneNumber)
                nav.popBackStack()
            }) {
                Icon(Icons.Default.Delete, contentDescription = "O'chirish")
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(PaddingValues(16.dp, 8.dp, 16.dp, 40.dp)),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (project.aspect == "9:16" || project.aspect == "3:4") 3f / 4f else 16f / 9f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val path = scene.imagePath
                if (path != null) {
                    AsyncImage(
                        model = vm.store.file(path),
                        contentDescription = scene.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text("Kadr hali chizilmagan", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (busy) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        persist()
                        vm.generateSceneImage(sceneNumber)
                    },
                    enabled = !busy
                ) {
                    Icon(
                        if (scene.imagePath == null) Icons.Default.AutoAwesome else Icons.Default.Refresh,
                        contentDescription = null
                    )
                    Text(if (scene.imagePath == null) "  Kadrni chizish" else "  Qayta chizish")
                }
                AssistChip(
                    onClick = {
                        persist()
                        vm.improvePrompt(sceneNumber)
                    },
                    label = { Text("Promptni yaxshilash") }
                )
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Sahna nomi") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = summary,
                onValueChange = { summary = it },
                label = { Text("Sahna tavsifi (o'zbekcha)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("Rasm prompti (inglizcha)") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Davomiyligi: ${duration.toInt()} soniya", style = MaterialTheme.typography.titleMedium)
            Slider(
                value = duration,
                onValueChange = { duration = it },
                onValueChangeFinished = { persist() },
                valueRange = 2f..12f,
                steps = 9
            )

            Text("Kadrdagi aktyorlar", style = MaterialTheme.typography.titleMedium)
            if (vm.actors.isEmpty()) {
                InfoCard("Aktyorlar ro'yxati bo'sh. Bosh sahifadagi \"Aktyorlar\" bo'limidan qo'shing.")
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    vm.actors.forEach { actor ->
                        val selected = scene.actorIds.contains(actor.id)
                        FilterChip(
                            selected = selected,
                            onClick = {
                                vm.updateScene(sceneNumber) { s ->
                                    val ids = s.actorIds.toMutableList()
                                    if (selected) ids.remove(actor.id) else ids.add(actor.id)
                                    s.copy(actorIds = ids)
                                }
                            },
                            label = { Text(actor.name.ifBlank { "Nomsiz" }) }
                        )
                    }
                }
            }

            if (scene.dialogue.isNotEmpty()) {
                Text("Dialog", style = MaterialTheme.typography.titleMedium)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    scene.dialogue.forEach { line ->
                        Text(
                            "${line.speaker.uppercase()}: ${line.text}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            InfoCard(
                "Yuz o'zgarib ketsa: aktyorning suratini aniqroqiga almashtiring yoki \"Yuz tavsifini yaratish\" " +
                    "tugmasini bosib pasportni yangilang, so'ng kadrni qayta chizing."
            )
        }
    }
}
