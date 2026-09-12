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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import uz.kodava.studio.media.Exporter
import uz.kodava.studio.ui.AppViewModel
import uz.kodava.studio.ui.Avatar
import uz.kodava.studio.ui.Banner
import uz.kodava.studio.ui.CircleButton
import uz.kodava.studio.ui.GradientButton
import uz.kodava.studio.ui.KodavaScreen
import uz.kodava.studio.ui.OutlineButton
import uz.kodava.studio.ui.SectionTitle
import uz.kodava.studio.ui.TopRow
import uz.kodava.studio.ui.theme.Kodava

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SceneScreen(vm: AppViewModel, nav: NavController, sceneNumber: Int) {
    val context = LocalContext.current
    val project = vm.project
    val scene = project?.scenes?.firstOrNull { it.n == sceneNumber }

    if (project == null || scene == null) {
        KodavaScreen(vm) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Sahna topilmadi", color = Kodava.TextMid)
            }
        }
        return
    }

    // Tahrirlar 500 ms dan keyin avtomatik saqlanadi — orqaga qaytishda hech narsa yo'qolmaydi.
    var title by remember(sceneNumber) { mutableStateOf(scene.title) }
    var summary by remember(sceneNumber) { mutableStateOf(scene.summary) }
    var prompt by remember(sceneNumber) { mutableStateOf(scene.imagePrompt) }
    var duration by remember(sceneNumber) { mutableStateOf(scene.durationSec.toFloat()) }

    LaunchedEffect(scene.imagePrompt) {
        if (scene.imagePrompt != prompt && scene.imagePrompt.isNotBlank()) prompt = scene.imagePrompt
    }
    LaunchedEffect(title, summary, prompt, duration) {
        delay(500)
        if (title != scene.title || summary != scene.summary ||
            prompt != scene.imagePrompt || duration.toInt() != scene.durationSec
        ) {
            vm.updateScene(sceneNumber) {
                it.copy(
                    title = title,
                    summary = summary,
                    imagePrompt = prompt,
                    durationSec = duration.toInt()
                )
            }
        }
    }

    val busy = vm.busyScenes.contains(sceneNumber)
    val sceneCast = vm.actorsForScene(project, scene)

    KodavaScreen(vm) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            TopRow(
                title = "Sahna $sceneNumber",
                subtitle = project.title.ifBlank { null },
                onBack = { nav.popBackStack() }
            ) {
                scene.imagePath?.let { path ->
                    CircleButton(Icons.Default.Share, "Ulashish") {
                        Exporter.share(context, vm.store.file(path), "image/jpeg")
                    }
                    Spacer(Modifier.width(8.dp))
                }
                CircleButton(Icons.Default.Delete, "O'chirish") {
                    vm.deleteScene(sceneNumber)
                    nav.popBackStack()
                }
            }

            Column(
                Modifier.padding(PaddingValues(18.dp, 10.dp, 18.dp, 40.dp)),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(if (project.aspect == "9:16" || project.aspect == "3:4") 3f / 4f else 16f / 9f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Kodava.SurfaceHigh),
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
                        Text("Kadr hali chizilmagan", color = Kodava.TextMid, style = MaterialTheme.typography.bodySmall)
                    }
                    if (busy) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Kodava.Ink.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator(color = Kodava.Magenta) }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    GradientButton(
                        text = if (scene.imagePath == null) "Kadrni chizish" else "Qayta chizish",
                        icon = if (scene.imagePath == null) Icons.Default.AutoAwesome else Icons.Default.Refresh,
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                        onClick = { vm.generateSceneImage(sceneNumber) }
                    )
                    OutlineButton(text = "Prompt+", enabled = vm.busy == null) { vm.improvePrompt(sceneNumber) }
                }

                if (sceneCast.isEmpty()) {
                    Banner(
                        text = "Bu kadrga aktyor biriktirilmagan — yuz tasodifiy chiqadi. Quyidan aktyor tanlang.",
                        tone = Kodava.Amber
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionTitle("Kadrdagi yuzlar (etalon suratlar yuboriladi)")
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            sceneCast.forEach { actor ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Avatar(
                                        file = actor.refs.firstOrNull()?.let { vm.store.file(it) },
                                        name = actor.name,
                                        size = 52.dp
                                    )
                                    Text(
                                        actor.name.ifBlank { "Nomsiz" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Kodava.TextMid
                                    )
                                }
                            }
                        }
                    }
                }

                if (vm.actors.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionTitle("Aktyorlarni tanlash")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            vm.actors.forEach { actor ->
                                val selected = scene.actorIds.contains(actor.id)
                                Pill(
                                    text = actor.name.ifBlank { "Nomsiz" },
                                    selected = selected
                                ) {
                                    vm.updateScene(sceneNumber) { s ->
                                        val ids = s.actorIds.toMutableList()
                                        if (selected) ids.remove(actor.id) else ids.add(actor.id)
                                        s.copy(actorIds = ids)
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Sahna nomi") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("Sahna tavsifi (o'zbekcha)") },
                    shape = RoundedCornerShape(18.dp),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Rasm prompti (inglizcha)") },
                    shape = RoundedCornerShape(18.dp),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Column {
                    SectionTitle("Davomiyligi — ${duration.toInt()} soniya")
                    Slider(
                        value = duration,
                        onValueChange = { duration = it },
                        valueRange = 2f..12f,
                        steps = 9,
                        colors = SliderDefaults.colors(
                            thumbColor = Kodava.Magenta,
                            activeTrackColor = Kodava.Violet,
                            inactiveTrackColor = Kodava.SurfaceHigh
                        )
                    )
                }

                if (scene.dialogue.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionTitle("Dialog")
                        scene.dialogue.forEach { line ->
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Kodava.Surface)
                                    .padding(14.dp)
                            ) {
                                Text(
                                    line.speaker.uppercase(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Kodava.Mint,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(line.text, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}
