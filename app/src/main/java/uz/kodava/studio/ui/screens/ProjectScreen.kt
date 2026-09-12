package uz.kodava.studio.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import uz.kodava.studio.data.Scene
import uz.kodava.studio.media.Exporter
import uz.kodava.studio.ui.AppViewModel
import uz.kodava.studio.ui.InfoCard
import uz.kodava.studio.ui.KodavaScaffold

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun ProjectScreen(vm: AppViewModel, nav: NavController, projectId: String) {
    val context = LocalContext.current
    LaunchedEffect(projectId) { if (vm.project?.id != projectId) vm.openProject(projectId) }

    val project = vm.project
    if (project == null) {
        KodavaScaffold(title = "Loyiha", vm = vm, onBack = { nav.popBackStack() }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Loyiha yuklanmoqda…")
            }
        }
        return
    }

    KodavaScaffold(
        title = project.title.ifBlank { "Loyiha" },
        vm = vm,
        onBack = {
            vm.closeProject()
            nav.popBackStack()
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (project.logline.isNotBlank()) {
                        Text(project.logline, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        "${project.style} · ${project.aspect} · ${project.scenes.size} sahna · " +
                            "${project.scenes.count { it.imagePath != null }} rasm",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = { vm.generateAllImages(onlyMissing = true) }, enabled = vm.busy == null) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Text("  Sahnalarni chizish")
                    }
                    AssistChip(
                        onClick = { vm.buildVideo { uri -> Exporter.openVideo(context, uri) } },
                        label = { Text("Video yig'ish") },
                        leadingIcon = { Icon(Icons.Default.Movie, contentDescription = null) }
                    )
                    AssistChip(
                        onClick = { vm.exportImages() },
                        label = { Text("Rasmlarni saqlash") },
                        leadingIcon = { Icon(Icons.Default.SaveAlt, contentDescription = null) }
                    )
                    AssistChip(
                        onClick = { vm.exportScript() },
                        label = { Text("Senariy .txt") },
                        leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) }
                    )
                    AssistChip(
                        onClick = { vm.regenerateScript() },
                        label = { Text("Senariyni qayta yozish") },
                        leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) }
                    )
                }
            }

            if (project.scenes.none { it.imagePath != null }) {
                item {
                    InfoCard(
                        "Sahnalar tayyor. \"Sahnalarni chizish\" tugmasi barcha kadrlarni ketma-ket yaratadi — " +
                            "bu bir necha daqiqa olishi mumkin. Alohida sahnani ochib, matnini tahrirlab ham chizdirish mumkin."
                    )
                }
            }

            items(project.scenes, key = { it.n }) { scene ->
                SceneCard(
                    vm = vm,
                    scene = scene,
                    onOpen = { nav.navigate("scene/${scene.n}") },
                    onGenerate = { vm.generateSceneImage(scene.n) }
                )
            }
        }
    }
}

@Composable
private fun SceneCard(vm: AppViewModel, scene: Scene, onOpen: () -> Unit, onGenerate: () -> Unit) {
    val busy = vm.busyScenes.contains(scene.n)
    ElevatedCard(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val path = scene.imagePath
                if (path != null) {
                    AsyncImage(
                        model = vm.store.file(path),
                        contentDescription = scene.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (busy) {
                    CircularProgressIndicator()
                } else {
                    Button(onClick = onGenerate) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Text("  Kadrni chizish")
                    }
                }
                if (busy && path != null) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                }
            }
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${scene.n}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Text(
                        "  ${scene.title}",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2
                    )
                }
                if (scene.summary.isNotBlank()) {
                    Text(scene.summary, style = MaterialTheme.typography.bodySmall, maxLines = 3)
                }
                Text(
                    "${scene.location.ifBlank { "-" }} · ${scene.camera.ifBlank { "-" }} · ${scene.durationSec}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
