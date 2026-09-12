package uz.kodava.studio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import uz.kodava.studio.data.Aspects
import uz.kodava.studio.data.Styles
import uz.kodava.studio.ui.AppViewModel
import uz.kodava.studio.ui.Avatar
import uz.kodava.studio.ui.Banner
import uz.kodava.studio.ui.GradientButton
import uz.kodava.studio.ui.KodavaScreen
import uz.kodava.studio.ui.SectionTitle
import uz.kodava.studio.ui.TopRow
import uz.kodava.studio.ui.theme.Kodava

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewProjectScreen(vm: AppViewModel, nav: NavController) {
    var idea by rememberSaveable { mutableStateOf("") }
    var style by rememberSaveable { mutableStateOf(Styles.CINEMATIC) }
    var aspect by rememberSaveable { mutableStateOf("16:9") }
    var sceneCount by rememberSaveable { mutableStateOf(6f) }
    val selected = remember { mutableStateListOf<String>() }

    // Suratli aktyorlar avtomatik belgilanadi — yuzlar shulardan olinadi.
    LaunchedEffect(vm.actors.size) {
        if (selected.isEmpty()) {
            selected.addAll(vm.actors.filter { it.refs.isNotEmpty() }.map { it.id })
        }
    }

    KodavaScreen(vm) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            TopRow(title = "Yangi loyiha", subtitle = "3 qadamda storyboard", onBack = { nav.popBackStack() })

            Column(
                Modifier.padding(PaddingValues(18.dp, 10.dp, 18.dp, 40.dp)),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle("1 — G'oyangiz")
                    OutlinedTextField(
                        value = idea,
                        onValueChange = { idea = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 150.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = fieldColors(),
                        placeholder = {
                            Text(
                                "Masalan: Toshkentlik yosh dasturchi eski chemodandan otasining maktubini topadi va uni izlab Samarqandga yo'l oladi…",
                                style = MaterialTheme.typography.bodySmall,
                                color = Kodava.TextMid
                            )
                        }
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle("2 — Uslub")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Styles.all.forEach { option ->
                            Pill(text = option, selected = style == option) { style = option }
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    SectionTitle("Kadr nisbati")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Aspects.all.forEach { option ->
                            Pill(text = option, selected = aspect == option) { aspect = option }
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    SectionTitle("Sahnalar soni — ${sceneCount.toInt()}")
                    Slider(
                        value = sceneCount,
                        onValueChange = { sceneCount = it },
                        valueRange = 3f..16f,
                        steps = 12,
                        colors = SliderDefaults.colors(
                            thumbColor = Kodava.Magenta,
                            activeTrackColor = Kodava.Violet,
                            inactiveTrackColor = Kodava.SurfaceHigh
                        )
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle("3 — Aktyorlar")
                    if (vm.actors.isEmpty()) {
                        Banner(
                            text = "Aktyor qo'shilmagan. Yuzlar barcha sahnalarda bir xil chiqishi uchun avval aktyor va uning suratini qo'shing.",
                            tone = Kodava.Amber,
                            actionLabel = "Aktyor qo'shish",
                            onAction = { nav.navigate("actors") }
                        )
                    } else {
                        vm.actors.forEach { actor ->
                            val isOn = selected.contains(actor.id)
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if (isOn) Kodava.Violet.copy(alpha = 0.16f) else Kodava.Surface)
                                    .border(
                                        1.dp,
                                        if (isOn) Kodava.Violet.copy(alpha = 0.5f) else Kodava.Line,
                                        RoundedCornerShape(18.dp)
                                    )
                                    .clickable {
                                        if (isOn) selected.remove(actor.id) else selected.add(actor.id)
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Avatar(
                                    file = actor.refs.firstOrNull()?.let { vm.store.file(it) },
                                    name = actor.name,
                                    size = 44.dp
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        actor.name.ifBlank { "Nomsiz aktyor" },
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        if (actor.refs.isEmpty()) "surat yo'q — yuz tasodifiy chiqadi"
                                        else "${actor.refs.size} ta surat",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (actor.refs.isEmpty()) Kodava.Amber else Kodava.TextMid
                                    )
                                }
                                Text(
                                    if (isOn) "✓" else "",
                                    color = Kodava.Mint,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }

                GradientButton(
                    text = "Senariy yaratish",
                    modifier = Modifier.fillMaxWidth(),
                    enabled = idea.trim().length >= 10 && vm.busy == null,
                    onClick = {
                        vm.createProject(
                            idea = idea.trim(),
                            style = style,
                            aspect = aspect,
                            sceneCount = sceneCount.toInt(),
                            actorIds = selected.toList()
                        ) { id ->
                            nav.popBackStack()
                            nav.navigate("project/$id")
                        }
                    }
                )

                if (!vm.hasKey()) {
                    Banner(
                        text = "API kalit kiritilmagan — senariy yaratilmaydi.",
                        tone = Kodava.Danger,
                        actionLabel = "Sozlamalar",
                        onAction = { nav.navigate("settings") }
                    )
                }
            }
        }
    }
}

@Composable
fun Pill(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Kodava.Violet.copy(alpha = 0.22f) else Kodava.Surface)
            .border(
                1.dp,
                if (selected) Kodava.Violet else Kodava.Line,
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 10.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = if (selected) Kodava.TextHigh else Kodava.TextMid,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Kodava.Violet,
    unfocusedBorderColor = Kodava.Line,
    focusedContainerColor = Kodava.Surface,
    unfocusedContainerColor = Kodava.Surface,
    cursorColor = Kodava.Magenta,
    focusedTextColor = Kodava.TextHigh,
    unfocusedTextColor = Kodava.TextHigh,
    focusedLabelColor = Kodava.TextMid,
    unfocusedLabelColor = Kodava.TextMid
)
