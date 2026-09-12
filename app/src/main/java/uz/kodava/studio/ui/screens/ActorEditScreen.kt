package uz.kodava.studio.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import kotlinx.coroutines.delay
import uz.kodava.studio.ui.AppViewModel
import uz.kodava.studio.ui.Avatar
import uz.kodava.studio.ui.Banner
import uz.kodava.studio.ui.CircleButton
import uz.kodava.studio.ui.GradientButton
import uz.kodava.studio.ui.KodavaScreen
import uz.kodava.studio.ui.SectionTitle
import uz.kodava.studio.ui.TopRow
import uz.kodava.studio.ui.theme.Kodava

@Composable
fun ActorEditScreen(vm: AppViewModel, nav: NavController, actorId: String) {
    val actor = vm.actors.firstOrNull { it.id == actorId }
    var confirmDelete by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.addActorPhoto(actorId, uri)
    }

    if (actor == null) {
        KodavaScreen(vm) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Aktyor topilmadi", color = Kodava.TextMid)
            }
        }
        return
    }

    // Tahrirlar avtomatik saqlanadi.
    var name by remember(actorId) { mutableStateOf(actor.name) }
    var note by remember(actorId) { mutableStateOf(actor.note) }
    LaunchedEffect(name, note) {
        delay(500)
        val fresh = vm.actor(actorId) ?: return@LaunchedEffect
        if (fresh.name != name || fresh.note != note) {
            vm.updateActor(fresh.copy(name = name, note = note))
        }
    }

    KodavaScreen(vm) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            TopRow(
                title = name.ifBlank { "Yangi aktyor" },
                subtitle = if (actor.refs.isEmpty()) "surat yo'q" else "${actor.refs.size} ta surat",
                onBack = { nav.popBackStack() }
            ) {
                CircleButton(Icons.Default.Delete, "O'chirish") { confirmDelete = true }
            }

            Column(
                Modifier.padding(PaddingValues(18.dp, 10.dp, 18.dp, 40.dp)),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(
                        file = actor.refs.firstOrNull()?.let { vm.store.file(it) },
                        name = name,
                        size = 84.dp
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            if (actor.appearance.isBlank()) "Yuz pasporti yozilmagan" else "Yuz pasporti tayyor ✓",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (actor.appearance.isBlank()) Kodava.Amber else Kodava.Mint
                        )
                        Text(
                            "Surat qo'shilganda ilova yuz tavsifini o'zi yozadi.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Kodava.TextMid
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Ismi (senariyda shu ism ishlatiladi)") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Izoh: yoshi, xarakteri, kiyim uslubi") },
                    shape = RoundedCornerShape(18.dp),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                SectionTitle("Etalon suratlar")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(actor.refs, key = { it }) { path ->
                        Box(Modifier.size(118.dp)) {
                            AsyncImage(
                                model = vm.store.file(path),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(18.dp))
                            )
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(Kodava.Ink.copy(alpha = 0.8f))
                                    .clickable { vm.removeActorPhoto(actorId, path) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "O'chirish",
                                    tint = Kodava.TextHigh,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                    item {
                        Box(
                            Modifier
                                .size(118.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Kodava.Surface)
                                .border(1.dp, Kodava.Line, RoundedCornerShape(18.dp))
                                .clickable {
                                    picker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = Kodava.TextMid)
                                Spacer(Modifier.size(6.dp))
                                Text("Surat", style = MaterialTheme.typography.bodySmall, color = Kodava.TextMid)
                            }
                        }
                    }
                }

                Banner(
                    text = "Eng yaxshi natija: yuz yaqindan, yorug' joyda, boshqa odamsiz. " +
                        "Turli rakursdagi 2–3 ta surat yuzni ancha barqaror qiladi."
                )

                GradientButton(
                    text = if (actor.appearance.isBlank()) "Yuz pasportini yaratish" else "Yuz pasportini yangilash",
                    icon = Icons.Default.AutoAwesome,
                    enabled = actor.refs.isNotEmpty() && vm.busy == null,
                    modifier = Modifier.fillMaxWidth()
                ) { vm.generateAppearance(actorId) }

                if (actor.appearance.isNotBlank()) {
                    OutlinedTextField(
                        value = actor.appearance,
                        onValueChange = { vm.updateActor(actor.copy(appearance = it)) },
                        label = { Text("Yuz pasporti (inglizcha, tahrirlash mumkin)") },
                        shape = RoundedCornerShape(18.dp),
                        colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = Kodava.Surface,
            title = { Text("Aktyorni o'chirish") },
            text = { Text("${name.ifBlank { "Bu aktyor" }} va uning suratlari o'chiriladi.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteActor(actorId)
                    confirmDelete = false
                    nav.popBackStack()
                }) { Text("O'chirish", color = Kodava.Danger) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Bekor") } }
        )
    }
}
