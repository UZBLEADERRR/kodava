package uz.kodava.studio.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import uz.kodava.studio.data.Prefs
import uz.kodava.studio.ui.AppViewModel
import uz.kodava.studio.ui.InfoCard
import uz.kodava.studio.ui.KodavaScaffold

@Composable
fun SettingsScreen(vm: AppViewModel, nav: NavController) {
    var key by remember { mutableStateOf(vm.apiKey) }
    var textModel by remember { mutableStateOf(vm.textModel) }
    var imageModel by remember { mutableStateOf(vm.imageModel) }
    var visible by remember { mutableStateOf(false) }

    KodavaScaffold(title = "Sozlamalar", vm = vm, onBack = { nav.popBackStack() }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(PaddingValues(16.dp, 8.dp, 16.dp, 32.dp)),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Google AI Studio API kaliti", style = MaterialTheme.typography.titleMedium)
            InfoCard(
                "aistudio.google.com saytiga kiring → \"Get API key\" → kalitni nusxalab shu yerga qo'ying. " +
                    "Kalit faqat shu telefonda saqlanadi va to'g'ridan-to'g'ri Google API'siga yuboriladi."
            )
            OutlinedTextField(
                value = key,
                onValueChange = { key = it },
                label = { Text("API kalit") },
                singleLine = true,
                visualTransformation = if (visible) androidx.compose.ui.text.input.VisualTransformation.None
                else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            TextButton(onClick = { visible = !visible }) {
                Text(if (visible) "Kalitni yashirish" else "Kalitni ko'rsatish")
            }

            Text("Modellar", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = textModel,
                onValueChange = { textModel = it },
                label = { Text("Matn modeli (senariy)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = imageModel,
                onValueChange = { imageModel = it },
                label = { Text("Rasm modeli (sahna kadrlari)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            InfoCard(
                "Standart qiymatlar: ${Prefs.DEFAULT_TEXT_MODEL} va ${Prefs.DEFAULT_IMAGE_MODEL}. " +
                    "Rasm modeli etalon suratlardagi yuzni saqlab qolish uchun tanlangan."
            )

            Button(
                onClick = { vm.saveSettings(key, textModel, imageModel) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Saqlash")
            }
        }
    }
}
