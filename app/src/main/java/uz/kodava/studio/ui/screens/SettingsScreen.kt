package uz.kodava.studio.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import uz.kodava.studio.data.Prefs
import uz.kodava.studio.ui.AppViewModel
import uz.kodava.studio.ui.Banner
import uz.kodava.studio.ui.GradientButton
import uz.kodava.studio.ui.KodavaScreen
import uz.kodava.studio.ui.OutlineButton
import uz.kodava.studio.ui.SectionTitle
import uz.kodava.studio.ui.TopRow
import uz.kodava.studio.ui.theme.Kodava

@Composable
fun SettingsScreen(vm: AppViewModel, nav: NavController) {
    var key by remember { mutableStateOf(vm.apiKey) }
    var textModel by remember { mutableStateOf(vm.textModel) }
    var imageModel by remember { mutableStateOf(vm.imageModel) }
    var visible by remember { mutableStateOf(false) }

    KodavaScreen(vm) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            TopRow(title = "Sozlamalar", onBack = { nav.popBackStack() })

            Column(
                Modifier.padding(PaddingValues(18.dp, 10.dp, 18.dp, 40.dp)),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                SectionTitle("Google AI Studio API kaliti")
                Banner(
                    text = "aistudio.google.com → \"Get API key\" → kalitni nusxalab shu yerga qo'ying. " +
                        "Kalit faqat shu telefonda saqlanadi va to'g'ridan-to'g'ri Google API'siga yuboriladi."
                )
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("API kalit") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = fieldColors(),
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlineButton(text = if (visible) "Kalitni yashirish" else "Kalitni ko'rsatish") {
                    visible = !visible
                }

                SectionTitle("Modellar")
                OutlinedTextField(
                    value = textModel,
                    onValueChange = { textModel = it },
                    label = { Text("Matn modeli (senariy)") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = imageModel,
                    onValueChange = { imageModel = it },
                    label = { Text("Rasm modeli (kadrlar)") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Standart: ${Prefs.DEFAULT_TEXT_MODEL} va ${Prefs.DEFAULT_IMAGE_MODEL}. " +
                        "Rasm modeli etalon suratdagi yuzni saqlab qolish uchun tanlangan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Kodava.TextMid
                )

                GradientButton(
                    text = "Saqlash",
                    modifier = Modifier.fillMaxWidth()
                ) { vm.saveSettings(key, textModel, imageModel) }
            }
        }
    }
}
