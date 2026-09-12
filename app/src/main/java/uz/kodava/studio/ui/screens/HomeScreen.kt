package uz.kodava.studio.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import uz.kodava.studio.ui.InfoCard
import uz.kodava.studio.ui.KodavaScaffold

@Composable
fun HomeScreen(vm: AppViewModel, nav: NavController) {
    var deleting by remember { mutableStateOf<Project?>(null) }

    LaunchedEffect(Unit) { vm.reload() }

    KodavaScaffold(
        title = "Kodava Studio",
        vm = vm,
        actions = {
            IconButton(onClick = { nav.navigate("actors") }) {
                Icon(Icons.Default.Group, contentDescription = "Aktyorlar")
            }
            IconButton(onClick = { nav.navigate("settings") }) {
                Icon(Icons.Default.Settings, contentDescription = "Sozlamalar")
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { nav.navigate("new") },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Yangi loyiha") }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!vm.hasKey()) {
                item {
                    ElevatedCard(
                        Modifier
                            .fillMaxWidth()
                            .clickable { nav.navigate("settings") },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("API kalit kiritilmagan", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Senariy va rasm yaratish uchun Google AI Studio'dan bepul olingan kalitni kiriting. Bosing →",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            item {
                InfoCard(
                    "1) Aktyor qo'shing (surat) → 2) g'oyangizni yozing → 3) AI senariy va sahnalarni chizadi → " +
                        "4) rasmlarni yuklab oling yoki video yig'ing."
                )
            }

            if (vm.projects.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "Hozircha loyiha yo'q.\n\"Yangi loyiha\" tugmasini bosing.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(vm.projects, key = { it.id }) { project ->
                ProjectRow(
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
            title = { Text("Loyihani o'chirish") },
            text = { Text("\"${target.title.ifBlank { target.idea.take(40) }}\" butunlay o'chiriladi.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteProject(target.id)
                    deleting = null
                }) { Text("O'chirish") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Bekor") } }
        )
    }
}

@Composable
private fun ProjectRow(vm: AppViewModel, project: Project, onOpen: () -> Unit, onDelete: () -> Unit) {
    val cover = project.scenes.firstOrNull { it.imagePath != null }?.imagePath
    ElevatedCard(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (cover != null) {
                    AsyncImage(
                        model = vm.store.file(cover),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Text(
                            "🎬",
                            Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    project.title.ifBlank { project.idea.take(40) },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2
                )
                Text(
                    "${project.scenes.size} sahna · ${project.scenes.count { it.imagePath != null }} rasm tayyor",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    project.style,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "O'chirish")
            }
        }
    }
}
