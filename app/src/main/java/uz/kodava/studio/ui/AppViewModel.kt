package uz.kodava.studio.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.kodava.studio.ai.GeminiClient
import uz.kodava.studio.ai.InlineImage
import uz.kodava.studio.ai.Prompts
import uz.kodava.studio.ai.ScriptParser
import uz.kodava.studio.data.Actor
import uz.kodava.studio.data.Aspects
import uz.kodava.studio.data.Prefs
import uz.kodava.studio.data.Project
import uz.kodava.studio.data.Scene
import uz.kodava.studio.data.Store
import uz.kodava.studio.media.Exporter
import uz.kodava.studio.media.SlideFrame
import uz.kodava.studio.media.SlideshowEncoder
import java.io.File
import java.util.UUID

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx: android.content.Context get() = getApplication<Application>()
    val store = Store(app)
    private val prefs = Prefs(app)

    var apiKey by mutableStateOf(prefs.apiKey)
    var textModel by mutableStateOf(prefs.textModel)
    var imageModel by mutableStateOf(prefs.imageModel)

    val actors = mutableStateListOf<Actor>()
    val projects = mutableStateListOf<Project>()

    /** Ochiq loyiha. Har o'zgarishdan keyin [touch] chaqiriladi. */
    var project by mutableStateOf<Project?>(null)
        private set

    var busy by mutableStateOf<String?>(null)
        private set
    var progress by mutableStateOf(0f)
        private set
    var message by mutableStateOf<String?>(null)
    val busyScenes = mutableStateListOf<Int>()

    init {
        reload()
    }

    fun reload() {
        actors.clear()
        actors.addAll(store.loadActors())
        projects.clear()
        projects.addAll(store.loadProjects())
    }

    fun hasKey(): Boolean = apiKey.isNotBlank()

    private fun client() = GeminiClient(apiKey.trim(), textModel.trim(), imageModel.trim())

    fun saveSettings(key: String, text: String, image: String) {
        apiKey = key.trim()
        textModel = text.trim().ifBlank { Prefs.DEFAULT_TEXT_MODEL }
        imageModel = image.trim().ifBlank { Prefs.DEFAULT_IMAGE_MODEL }
        prefs.apiKey = apiKey
        prefs.textModel = textModel
        prefs.imageModel = imageModel
        message = "Sozlamalar saqlandi"
    }

    // ---------------- Aktyorlar ----------------

    fun newActor(): Actor {
        val actor = Actor(id = UUID.randomUUID().toString(), name = "")
        store.upsertActor(actor)
        actors.add(actor)
        return actor
    }

    fun actor(id: String): Actor? = actors.firstOrNull { it.id == id }

    fun updateActor(updated: Actor) {
        store.upsertActor(updated)
        val i = actors.indexOfFirst { it.id == updated.id }
        if (i >= 0) actors[i] = updated.copy() else actors.add(updated)
    }

    fun deleteActor(id: String) {
        store.deleteActor(id)
        actors.removeAll { it.id == id }
    }

    fun addActorPhoto(actorId: String, uri: Uri) = launchSafely(null) {
        val path = withContext(Dispatchers.IO) { store.importActorPhoto(actorId, uri) }
            ?: throw IllegalStateException("Rasmni o'qib bo'lmadi")
        val actor = actor(actorId) ?: return@launchSafely
        val refs = actor.refs.toMutableList().apply { add(path) }
        updateActor(actor.copy(refs = refs))
    }

    fun removeActorPhoto(actorId: String, path: String) {
        val actor = actor(actorId) ?: return
        runCatching { store.file(path).delete() }
        updateActor(actor.copy(refs = actor.refs.filterNot { it == path }.toMutableList()))
    }

    /** Suratlardan "yuz pasporti" yozadi — sahnalarda yuz o'zgarmasligi uchun asosiy vosita. */
    fun generateAppearance(actorId: String) = launchSafely("Aktyor tavsifi yozilmoqda…") {
        val actor = actor(actorId) ?: return@launchSafely
        if (actor.refs.isEmpty()) throw IllegalStateException("Avval kamida bitta surat qo'shing")
        val images = actor.refs.take(4).mapNotNull { store.readBytes(it)?.let { bytes -> InlineImage(bytes) } }
        val text = client().generateText(Prompts.actorAppearance(actor), images, temperature = 0.4)
        updateActor(actor.copy(appearance = text.trim()))
        message = "Tavsif tayyor"
    }

    // ---------------- Loyihalar ----------------

    fun openProject(id: String) {
        project = store.loadProject(id)
    }

    fun closeProject() {
        project = null
        reload()
    }

    private fun touch() {
        project = project?.copy()
    }

    private fun persist() {
        project?.let { store.saveProject(it) }
        touch()
    }

    fun deleteProject(id: String) {
        store.deleteProject(id)
        projects.removeAll { it.id == id }
        if (project?.id == id) project = null
    }

    /** Yangi loyiha yaratib, darhol senariy generatsiya qiladi. */
    fun createProject(
        idea: String,
        style: String,
        aspect: String,
        sceneCount: Int,
        actorIds: List<String>,
        onReady: (String) -> Unit
    ) = launchSafely("Senariy yozilmoqda…") {
        val fresh = store.newProject(idea).apply {
            this.style = style
            this.aspect = aspect
            this.sceneCount = sceneCount
            this.actorIds = actorIds.toMutableList()
        }
        store.saveProject(fresh)
        project = fresh
        generateScriptInternal()
        projects.add(0, project!!)
        onReady(fresh.id)
    }

    fun regenerateScript() = launchSafely("Senariy qayta yozilmoqda…") { generateScriptInternal() }

    private suspend fun generateScriptInternal() {
        val current = project ?: return
        val cast = actors.filter { it.id in current.actorIds }
        val raw = client().generateText(Prompts.script(current, cast), asJson = true, temperature = 1.0)
        val parsed = ScriptParser.parse(raw, cast)
        current.title = parsed.title
        current.logline = parsed.logline
        // Eski rasmlar yangi senariyga mos kelmaydi — fayllar tozalanadi.
        current.scenes.forEach { old -> old.imagePath?.let { runCatching { store.file(it).delete() } } }
        current.scenes = parsed.scenes.map { scene ->
            scene.copy(
                imagePath = null,
                actorIds = scene.actorIds.ifEmpty { current.actorIds }.toMutableList()
            )
        }.toMutableList()
        store.saveProject(current)
        touch()
    }

    fun updateScene(n: Int, transform: (Scene) -> Scene) {
        val current = project ?: return
        val i = current.scenes.indexOfFirst { it.n == n }
        if (i < 0) return
        current.scenes[i] = transform(current.scenes[i])
        persist()
    }

    fun addScene() {
        val current = project ?: return
        val n = (current.scenes.maxOfOrNull { it.n } ?: 0) + 1
        current.scenes.add(
            Scene(n = n, title = "Sahna $n", actorIds = current.actorIds.toMutableList())
        )
        persist()
    }

    fun deleteScene(n: Int) {
        val current = project ?: return
        current.scenes.removeAll { it.n == n }
        current.scenes = current.scenes.mapIndexed { i, s -> s.copy(n = i + 1) }.toMutableList()
        persist()
    }

    /** Sahna promptini AI yordamida boyitish. */
    fun improvePrompt(n: Int) = launchSafely("Prompt yaxshilanmoqda…") {
        val current = project ?: return@launchSafely
        val scene = current.scenes.firstOrNull { it.n == n } ?: return@launchSafely
        val better = client().generateText(Prompts.improvePrompt(scene.imagePrompt, current.style), temperature = 0.8)
        updateScene(n) { it.copy(imagePrompt = better.trim()) }
    }

    // ---------------- Rasm generatsiyasi ----------------

    fun generateSceneImage(n: Int) = viewModelScope.launch {
        if (busyScenes.contains(n)) return@launch
        busyScenes.add(n)
        try {
            generateSceneImageInternal(n)
        } catch (e: Exception) {
            message = e.message ?: "Rasm yaratishda xatolik"
        } finally {
            busyScenes.remove(n)
        }
    }

    fun generateAllImages(onlyMissing: Boolean = true) = launchSafely("Sahnalar chizilmoqda…") {
        val current = project ?: return@launchSafely
        val targets = current.scenes.filter { !onlyMissing || it.imagePath == null }.map { it.n }
        if (targets.isEmpty()) {
            message = "Barcha sahnalar allaqachon tayyor"
            return@launchSafely
        }
        targets.forEachIndexed { index, n ->
            busy = "Sahna $n chizilmoqda… (${index + 1}/${targets.size})"
            progress = index.toFloat() / targets.size
            runCatching { generateSceneImageInternal(n) }
                .onFailure { message = "Sahna $n: ${it.message}" }
        }
        progress = 1f
        message = "Tayyor"
    }

    private suspend fun generateSceneImageInternal(n: Int) {
        val current = project ?: return
        val scene = current.scenes.firstOrNull { it.n == n } ?: return
        if (scene.imagePrompt.isBlank()) throw IllegalStateException("Sahna tavsifi bo'sh")

        val sceneActors = actors.filter { it.id in scene.actorIds }
            .ifEmpty { actors.filter { it.id in current.actorIds } }
        // Har bir aktyordan bitta asosiy surat — promptdagi "Person 1, Person 2" tartibi bilan mos tushadi.
        val refs = sceneActors.mapNotNull { a ->
            a.refs.firstOrNull()?.let { path -> store.readBytes(path)?.let { InlineImage(it) } }
        }
        val bytes = client().generateImage(
            prompt = Prompts.sceneImage(current, scene, sceneActors),
            references = refs,
            aspectRatio = current.aspect
        )
        val old = scene.imagePath
        val path = withContext(Dispatchers.IO) { store.saveSceneImage(current.id, n, bytes) }
        old?.let { runCatching { store.file(it).delete() } }
        updateScene(n) { it.copy(imagePath = path) }
    }

    // ---------------- Eksport ----------------

    fun buildVideo(onDone: (Uri) -> Unit) = launchSafely("Video yig'ilmoqda…") {
        val current = project ?: return@launchSafely
        val ready = current.scenes.filter { it.imagePath != null }
        if (ready.isEmpty()) throw IllegalStateException("Avval kamida bitta sahna rasmini yarating")

        val (w, h) = Aspects.videoSize(current.aspect)
        val slides = withContext(Dispatchers.IO) {
            ready.mapNotNull { scene ->
                store.loadBitmap(scene.imagePath!!, maxOf(w, h))?.let { SlideFrame(it, scene.durationSec) }
            }
        }
        if (slides.isEmpty()) throw IllegalStateException("Rasmlarni o'qib bo'lmadi")

        val output = File(ctx.cacheDir, "kodava_${System.currentTimeMillis()}.mp4")
        SlideshowEncoder.encode(slides, w, h, output) { p ->
            progress = p
            busy = "Video yig'ilmoqda… ${(p * 100).toInt()}%"
        }
        slides.forEach { it.bitmap.recycle() }

        val uri = Exporter.saveVideo(
            ctx,
            output,
            "${current.title.ifBlank { "kodava" }}_${System.currentTimeMillis()}.mp4"
        )
        current.videoPath = output.absolutePath
        store.saveProject(current)
        touch()
        message = "Video Movies/Kodava papkasiga saqlandi"
        onDone(uri)
    }

    fun exportImages() = launchSafely("Galereyaga saqlanmoqda…") {
        val current = project ?: return@launchSafely
        val ready = current.scenes.filter { it.imagePath != null }
        if (ready.isEmpty()) throw IllegalStateException("Saqlash uchun rasm yo'q")
        ready.forEach { scene ->
            Exporter.saveImage(
                ctx,
                store.file(scene.imagePath!!),
                current.title.ifBlank { "Kodava" },
                "sahna_${scene.n}_${System.currentTimeMillis()}.jpg"
            )
        }
        message = "${ready.size} ta rasm Pictures/Kodava papkasiga saqlandi"
    }

    fun exportScript() = launchSafely("Senariy saqlanmoqda…") {
        val current = project ?: return@launchSafely
        Exporter.saveText(
            ctx,
            scriptAsText(current),
            "${current.title.ifBlank { "senariy" }}.txt"
        )
        message = "Senariy Documents/Kodava papkasiga saqlandi"
    }

    fun scriptAsText(p: Project): String = buildString {
        appendLine(p.title)
        appendLine("=".repeat(p.title.length.coerceAtLeast(3)))
        if (p.logline.isNotBlank()) {
            appendLine()
            appendLine(p.logline)
        }
        appendLine()
        appendLine("G'oya: ${p.idea}")
        appendLine("Uslub: ${p.style} | Kadr: ${p.aspect}")
        p.scenes.forEach { scene ->
            appendLine()
            appendLine("SAHNA ${scene.n} — ${scene.title}")
            appendLine("${scene.location} / ${scene.timeOfDay} / ${scene.camera} / ${scene.durationSec}s")
            if (scene.summary.isNotBlank()) appendLine(scene.summary)
            scene.dialogue.forEach { line ->
                appendLine("  ${line.speaker.uppercase()}: ${line.text}")
            }
            if (scene.imagePrompt.isNotBlank()) {
                appendLine("  [prompt] ${scene.imagePrompt}")
            }
        }
    }

    // ---------------- Yordamchi ----------------

    private fun launchSafely(busyMessage: String?, block: suspend () -> Unit) = viewModelScope.launch {
        if (busy != null) {
            message = "Avvalgi amal tugashini kuting"
            return@launch
        }
        busy = busyMessage
        progress = 0f
        try {
            block()
        } catch (e: Exception) {
            message = e.message ?: "Xatolik yuz berdi"
        } finally {
            busy = null
            progress = 0f
            projects.clear()
            projects.addAll(store.loadProjects())
        }
    }
}
