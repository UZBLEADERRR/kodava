package uz.kodava.studio.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import uz.kodava.studio.ui.AppViewModel
import uz.kodava.studio.ui.Avatar
import uz.kodava.studio.ui.Banner
import uz.kodava.studio.ui.EmptyState
import uz.kodava.studio.ui.GradientButton
import uz.kodava.studio.ui.KodavaCard
import uz.kodava.studio.ui.KodavaScreen
import uz.kodava.studio.ui.TopRow
import uz.kodava.studio.ui.theme.Kodava

@Composable
fun ActorsScreen(vm: AppViewModel, nav: NavController) {
    KodavaScreen(vm) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(0.dp, 0.dp, 0.dp, 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                TopRow(
                    title = "Aktyorlar",
                    subtitle = "Yuzlar shu suratlardan olinadi",
                    onBack = { nav.popBackStack() }
                )
            }

            item {
                Column(
                    Modifier.padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Banner(
                        text = "Har bir aktyorga yuzi aniq ko'ringan 1–3 ta surat qo'shing. " +
                            "Ilova ulardan \"yuz pasporti\" yozadi va har bir kadrda aynan shu yuzni chizadi."
                    )
                    GradientButton(
                        text = "Aktyor qo'shish",
                        icon = Icons.Default.Add,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val actor = vm.newActor()
                        nav.navigate("actor/${actor.id}")
                    }
                }
            }

            if (vm.actors.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "👤",
                        title = "Aktyor yo'q",
                        text = "Aktyor qo'shib surat yuklang — shundan keyin barcha sahnalarda yuz o'zgarmaydi."
                    )
                }
            }

            items(vm.actors, key = { it.id }) { actor ->
                KodavaCard(
                    Modifier.padding(horizontal = 18.dp),
                    onClick = { nav.navigate("actor/${actor.id}") }
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(
                            file = actor.refs.firstOrNull()?.let { vm.store.file(it) },
                            name = actor.name,
                            size = 62.dp
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                actor.name.ifBlank { "Nomsiz aktyor" },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                if (actor.refs.isEmpty()) "surat yo'q"
                                else "${actor.refs.size} ta surat",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (actor.refs.isEmpty()) Kodava.Amber else Kodava.TextMid
                            )
                            Text(
                                if (actor.appearance.isBlank()) "yuz pasporti yozilmagan" else "yuz pasporti tayyor ✓",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (actor.appearance.isBlank()) Kodava.TextMid else Kodava.Mint
                            )
                        }
                    }
                }
            }
        }
    }
}
