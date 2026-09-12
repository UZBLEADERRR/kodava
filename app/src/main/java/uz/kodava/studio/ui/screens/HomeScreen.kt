package uz.kodava.studio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import uz.kodava.studio.data.Project
import uz.kodava.studio.ui.AppViewModel
import uz.kodava.studio.ui.Avatar
import uz.kodava.studio.ui.Banner
import uz.kodava.studio.ui.CircleButton
import uz.kodava.studio.ui.EmptyState
import uz.kodava.studio.ui.GradientButton
import uz.kodava.studio.ui.KodavaCard
import uz.kodava.studio.ui.KodavaScreen
import uz.kodava.studio.ui.ProgressLine
import uz.kodava.studio.ui.SectionTitle
import uz.kodava.studio.ui.theme.Kodava

@Composable
fun HomeScreen(vm: AppViewModel, nav: NavController) {
    var deleting by remember { mutableStateOf<Project?>(null) }

    KodavaScreen(vm) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(18.dp, 14.dp, 18.dp, 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Kodava", style = MaterialTheme.typography.displaySmall)
                        Text(
                            "G'oyadan storyboardgacha",
                            style = MaterialTheme.typography.bodySmall,
                            color = Kodava.TextMid
                        )
                    }
                    CircleButton(Icons.Default.Group, "Aktyorlar") { nav.navigate("actors") }
                    Spacer(Modifier.width(8.dp))
                    CircleButton(Icons.Default.Settings, "Sozlamalar") { nav.navigate("settings") }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile("Loyihalar", vm.projects.size.toString(), Modifier.weight(1f))
                    StatTile("Aktyorlar", vm.actors.size.toString(), Modifier.weight(1f))
                }
            }

            if (!vm.hasKey()) {
                item {
                    Banner(
                        text = "API kalit kiritilmagan. Senariy va kadrlar yaratilishi uchun Google AI Studio kalitini qo'shing.",
                        tone = Kodava.Amber,
                        actionLabel = "Sozlamalarni ochish",
                        onAction = { nav.navigate("settings") }
                    )
                }
            } else if (vm.actors.none { it.refs.isNotEmpty() }) {
                item {
                    Banner(
                        text = "Suratli aktyor yo'q. Aktyor qo'shib surat yuklamasangiz, AI yuzlarni o'zicha o'ylab topadi.",
                        tone = Kodava.Amber,
                        actionLabel = "Aktyor qo'shish",
                        onAction = { nav.navigate("actors") }
                    )
                }
            }

            if (vm.actors.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionTitle("Aktyorlar")
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            vm.actors.take(8).forEach { actor ->
                                Avatar(
                                    file = actor.refs.firstOrNull()?.let { vm.store.file(it) },
                                    name = actor.name,
                                    size = 48.dp,
                                    modifier = Modifier
                                )
                            }
                        }
                    }
                }
            }

            item {
                GradientButton(
                    text = "Yangi loyiha",
                    icon = Icons.Default.Add,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { nav.navigate("new") }
                )
            }

            item { SectionTitle("Loyihalar") }

            if (vm.projects.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "🎬",
                        title = "Hali loyiha yo'q",
                        text = "G'oyangizni yozing — AI senariy yozadi, sahnalarga bo'ladi va har bir kadrni chizadi."
                    )
                }
            }

            items(vm.projects, key = { it.id }) { project ->
                ProjectCard(
                    vm = vm,
                    project = project,
                    onOpen = {
                        vm.openProject(project.id)
                        nav.navigate("project/${project.id}")
                    },
                    onDelete = { deleting = project }
                )
            }
        }
    }

    deleting?.let { target ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            containerColor = Kodava.Surface,
            title = { Text("Loyihani o'chirish") },
            text = { Text("\"${target.title.ifBlank { target.idea.take(40) }}\" va uning barcha kadrlari o'chiriladi.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteProject(target.id)
                    deleting = null
                }) { Text("O'chirish", color = Kodava.Danger) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Bekor") } }
        )
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    KodavaCard(modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(value, style = MaterialTheme.typography.displaySmall)
            Text(label, style = MaterialTheme.typography.bodySmall, color = Kodava.TextMid)
        }
    }
}

@Composable
private fun ProjectCard(vm: AppViewModel, project: Project, onOpen: () -> Unit, onDelete: () -> Unit) {
    val cover = project.scenes.firstOrNull { it.imagePath != null }?.imagePath
    val done = project.scenes.count { it.imagePath != null }

    KodavaCard(onClick = onOpen) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Kodava.SurfaceHigh)
            ) {
                if (cover != null) {
                    AsyncImage(
                        model = vm.store.file(cover),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        "🎬",
                        Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.displaySmall
                    )
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Kodava.scrim)
                )
                Column(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        project.title.ifBlank { project.idea.take(40) },
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2
                    )
                    Text(
                        "${project.style} · ${project.aspect}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Kodava.TextMid
                    )
                }
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    CircleButton(Icons.Default.Delete, "O'chirish", onDelete)
                }
            }
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProgressLine(done, project.scenes.size, Modifier.weight(1f))
                Spacer(Modifier.width(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                    vm.actors.filter { it.id in project.actorIds }.take(3).forEach { actor ->
                        Avatar(
                            file = actor.refs.firstOrNull()?.let { vm.store.file(it) },
                            name = actor.name,
                            size = 30.dp,
                            modifier = Modifier.clip(RoundedCornerShape(15.dp))
                        )
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}
