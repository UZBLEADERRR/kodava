package uz.kodava.studio.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import uz.kodava.studio.ui.AppViewModel
import uz.kodava.studio.ui.InfoCard
import uz.kodava.studio.ui.KodavaScaffold

@Composable
fun ActorEditScreen(vm: AppViewModel, nav: NavController, actorId: String) {
    val actor = vm.actors.firstOrNull { it.id == actorId }
    var name by remember(actorId) { mutableStateOf(actor?.name.orEmpty()) }
    var note by remember(actorId) { mutableStateOf(actor?.note.orEmpty()) }
    var confirmDelete by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.addActorPhoto(actorId, uri)
    }

    LaunchedEffect(actor?.name) { if (name.isBlank() && actor != null) name = actor.name }

    if (actor == null) {
        KodavaScaffold(title = "Aktyor", vm = vm, onBack = { nav.popBackStack() }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Aktyor topilmadi")
            }
        }
        return
    }

    fun save() {
        vm.updateActor(actor.copy(name = name.trim(), note = note.trim()))
    }

    KodavaScaffold(
        title = "Aktyor",
        vm = vm,
        onBack = {
            save()
            nav.popBackStack()
        },
        actions = {
            IconButton(onClick = { confirmDelete = true }) {
                Icon(Icons.Default.Delete, contentDescription = "O'chirish")
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(PaddingValues(16.dp, 8.dp, 16.dp, 32.dp)),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Ismi (senariyda shu ism ishlatiladi)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Izoh: yoshi, xarakteri, kiyim uslubi") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Yuz etaloni suratlari", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(actor.refs, key = { it }) { path ->
                    Box(Modifier.size(110.dp)) {
                        AsyncImage(
                            model = vm.store.file(path),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(14.dp))
                        )
                        IconButton(
                            onClick = { vm.removeActorPhoto(actorId, path) },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "O'chirish")
                        }
                    }
                }
                item {
                    Box(
                        Modifier
                            .size(110.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                            .clickable {
                                picker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null)
                            Text("Surat", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            InfoCard(
                "Eng yaxshi natija uchun: yuz yaqindan, yorug' joyda, ko'zoynak/soyasiz. " +
                    "Turli rakursdagi 2–3 ta surat tavsifni aniqroq qiladi."
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        save()
                        vm.generateAppearance(actorId)
                    },
                    enabled = actor.refs.isNotEmpty() && vm.busy == null
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Text("  Yuz tavsifini yaratish")
                }
                OutlinedButton(onClick = { save() }) { Text("Saqlash") }
            }

            if (actor.appearance.isNotBlank()) {
                Text("Yuz pasporti (AI yozgan)", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = actor.appearance,
                    onValueChange = { vm.updateActor(actor.copy(appearance = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Ingliz tilida — tahrirlash mumkin") }
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Aktyorni o'chirish") },
            text = { Text("${actor.name.ifBlank { "Bu aktyor" }} va uning suratlari o'chiriladi.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteActor(actorId)
                    confirmDelete = false
                    nav.popBackStack()
                }) { Text("O'chirish") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Bekor") } }
        )
    }
}
