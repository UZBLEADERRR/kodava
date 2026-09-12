package uz.kodava.studio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import uz.kodava.studio.ui.AppViewModel
import uz.kodava.studio.ui.screens.ActorEditScreen
import uz.kodava.studio.ui.screens.ActorsScreen
import uz.kodava.studio.ui.screens.HomeScreen
import uz.kodava.studio.ui.screens.NewProjectScreen
import uz.kodava.studio.ui.screens.ProjectScreen
import uz.kodava.studio.ui.screens.SceneScreen
import uz.kodava.studio.ui.screens.SettingsScreen
import uz.kodava.studio.ui.theme.Kodava
import uz.kodava.studio.ui.theme.KodavaTheme

class MainActivity : ComponentActivity() {

    // ViewModel Activity'ga bog'langan — ekranlar almashganda holat saqlanib qoladi.
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            KodavaTheme {
                Surface(Modifier.fillMaxSize(), color = Kodava.Ink) {
                    val nav = rememberNavController()
                    NavHost(navController = nav, startDestination = "home") {
                        composable("home") { HomeScreen(vm, nav) }
                        composable("new") { NewProjectScreen(vm, nav) }
                        composable("actors") { ActorsScreen(vm, nav) }
                        composable("actor/{id}") { entry ->
                            ActorEditScreen(vm, nav, entry.arguments?.getString("id").orEmpty())
                        }
                        composable("project/{id}") { entry ->
                            ProjectScreen(vm, nav, entry.arguments?.getString("id").orEmpty())
                        }
                        composable("scene/{n}") { entry ->
                            SceneScreen(vm, nav, entry.arguments?.getString("n")?.toIntOrNull() ?: 1)
                        }
                        composable("settings") { SettingsScreen(vm, nav) }
                    }
                }
            }
        }
    }
}
