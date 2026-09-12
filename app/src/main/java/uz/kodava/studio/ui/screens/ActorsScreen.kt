package uz.kodava.studio.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
fun ActorsScreen(vm: AppViewModel, nav: NavController) {
    KodavaScaffold(
        title = "Aktyorlar",
        vm = vm,
        onBack = { nav.popBackStack() },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    val actor = vm.newActor()
                    nav.navigate("actor/${actor.id}")
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Aktyor qo'shish") }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                InfoCard(
                    "Har bir aktyor uchun yuzi aniq ko'ringan 1–4 ta surat qo'shing. " +
                        "Ilova shu suratlardan \"yuz pasporti\" yozadi va har bir sahnada aynan shu yuzni chizadi."
                )
            }
            items(vm.actors, key = { it.id }) { actor ->
                ElevatedCard(
                    Modifier
                        .fillMaxWidth()
                        .clickable { nav.navigate("actor/${actor.id}") },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(64.dp)
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            val ref = actor.refs.firstOrNull()
                            if (ref != null) {
                                AsyncImage(
                                    model = vm.store.file(ref),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text("👤", style = MaterialTheme.typography.titleLarge)
                            }
                        }
                        Column(
                            Modifier
                                .weight(1f)
                                .padding(start = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(actor.name.ifBlank { "Nomsiz aktyor" }, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${actor.refs.size} ta surat · " +
                                    if (actor.appearance.isBlank()) "tavsif yo'q" else "tavsif tayyor",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
