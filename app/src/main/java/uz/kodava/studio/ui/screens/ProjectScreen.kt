package uz.kodava.studio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import uz.kodava.studio.data.Scene
import uz.kodava.studio.media.Exporter
import uz.kodava.studio.ui.AppViewModel
import uz.kodava.studio.ui.Avatar
import uz.kodava.studio.ui.Banner
import uz.kodava.studio.ui.GradientButton
import uz.kodava.studio.ui.KodavaCard
import uz.kodava.studio.ui.KodavaScreen
import uz.kodava.studio.ui.OutlineButton
import uz.kodava.studio.ui.ProgressLine
import uz.kodava.studio.ui.SectionTitle
import uz.kodava.studio.ui.TopRow
import uz.kodava.studio.ui.theme.Kodava

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectScreen(vm: AppViewModel, nav: NavController, projectId: String) {
    val context = LocalContext.current
    LaunchedEffect(projectId) { if (vm.project?.id != projectId) vm.openProject(projectId) }

    var castDialog by remember { mutableStateOf(false) }
    val project = vm.project

    if (project == null) {
        KodavaScreen(vm) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Kodava.Magenta)
            }
        }
        return
    }

    val done = project.scenes.count { it.imagePath != null }
    val cast = vm.actors.filter { it.id in project.actorIds }

    KodavaScreen(vm) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(0.dp, 0.dp, 0.dp, 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                TopRow(
                    title = project.title.ifBlank { "Loyiha" },
                    subtitle = "${project.style} · ${project.aspect}",
                    onBack = { nav.popBackStack() }
                )
            }

            item {
                Column(
                    Modifier.padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (project.logline.isNotBlank()) {
                        Text(project.logline, style = MaterialTheme.typography.bodyMedium)
                    }
                    ProgressLine(done, project.scenes.size)

                    if (cast.isEmpty()) {
                        Banner(
                            text = "Bu loyihaga aktyor biriktirilmagan — AI yuzlarni o'zi o'ylab topadi.",
                            tone = Kodava.Amber,
                            actionLabel = "Aktyorlarni biriktirish",
                            onAction = { castDialog = true }
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                                cast.take(5).forEach { actor ->
                                    Avatar(
                                        file = actor.refs.firstOrNull()?.let { vm.store.file(it) },
                                        name = actor.name,
                                        size = 38.dp
                                    )
                                }
                            }
                            OutlineButton(text = "O'zgartirish", icon = Icons.Default.Group) { castDialog = true }
                        }
                    }

                    GradientButton(
                        text = if (done == 0) "Barcha kadrlarni chizish" else "Qolgan kadrlarni chizish",
                        icon = Icons.Default.AutoAwesome,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = vm.busy == null,
                        onClick = { vm.generateAllImages(onlyMissing = true) }
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlineButton(text = "Video yig'ish", icon = Icons.Default.Movie) {
                            vm.buildVideo { uri -> Exporter.openVideo(context, uri) }
                        }
                        OutlineButton(text = "Rasmlarni saqlash", icon = Icons.Default.SaveAlt) { vm.exportImages() }
                        OutlineButton(text = "Senariy .txt", icon = Icons.Default.Description) { vm.exportScript() }
                        OutlineButton(text = "Senariyni qayta yozish", icon = Icons.Default.Refresh) { vm.regenerateScript() }
                    }
                }
            }

            item {
                SectionTitle("Sahnalar", Modifier.padding(horizontal = 18.dp))
            }

            items(project.scenes, key = { it.n }) { scene ->
                SceneCard(
                    vm = vm,
                    scene = scene,
                    modifier = Modifier.padding(horizontal = 18.dp),
                    onOpen = { nav.navigate("scene/${scene.n}") },
                    onGenerate = { vm.generateSceneImage(scene.n) }
                )
            }
        }
    }

    if (castDialog) {
        val picked = remember { mutableStateListOf<String>().apply { addAll(project.actorIds) } }
        AlertDialog(
            onDismissRequest = { castDialog = false },
            containerColor = Kodava.Surface,
            title = { Text("Aktyorlarni biriktirish") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (vm.actors.isEmpty()) {
                        Text("Aktyorlar ro'yxati bo'sh. Avval aktyor qo'shing.")
                    }
                    vm.actors.forEach { actor ->
                        val isOn = picked.contains(actor.id)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { if (isOn) picked.remove(actor.id) else picked.add(actor.id) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Avatar(
                                file = actor.refs.firstOrNull()?.let { vm.store.file(it) },
                                name = actor.name,
                                size = 36.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(actor.name.ifBlank { "Nomsiz" }, Modifier.weight(1f))
                            Text(if (isOn) "✓" else "", color = Kodava.Mint, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.setProjectActors(picked.toList())
                    castDialog = false
                }) { Text("Saqlash") }
            },
            dismissButton = {
                TextButton(onClick = { castDialog = false }) { Text("Bekor") }
            }
        )
    }
}

@Composable
private fun SceneCard(
    vm: AppViewModel,
    scene: Scene,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
    onGenerate: () -> Unit
) {
    val busy = vm.busyScenes.contains(scene.n)
    val sceneCast = vm.actors.filter { it.id in scene.actorIds }

    KodavaCard(modifier, onClick = onOpen) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Kodava.SurfaceHigh),
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
                    Box(Modifier.fillMaxSize().background(Kodava.scrim))
                }
                when {
                    busy -> CircularProgressIndicator(color = Kodava.Magenta)
                    path == null -> GradientButton(
                        text = "Kadrni chizish",
                        icon = Icons.Default.AutoAwesome,
                        onClick = onGenerate
                    )
                }
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Kodava.Ink.copy(alpha = 0.75f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${scene.n}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                if (sceneCast.isNotEmpty()) {
                    Row(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        sceneCast.take(3).forEach { actor ->
                            Avatar(
                                file = actor.refs.firstOrNull()?.let { vm.store.file(it) },
                                name = actor.name,
                                size = 26.dp
                            )
                        }
                    }
                }
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(scene.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                if (scene.summary.isNotBlank()) {
                    Text(
                        scene.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = Kodava.TextMid,
                        maxLines = 3
                    )
                }
                Text(
                    "${scene.location.ifBlank { "—" }} · ${scene.camera.ifBlank { "—" }} · ${scene.durationSec}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = Kodava.TextMid
                )
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}
