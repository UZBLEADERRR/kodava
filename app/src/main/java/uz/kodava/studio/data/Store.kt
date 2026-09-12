package uz.kodava.studio.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/**
 * Barcha ma'lumotlar telefon ichidagi ilova papkasida JSON + JPG ko'rinishida saqlanadi.
 * Hech qanday server yoki hisob kerak emas.
 */
class Store(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    private val root: File get() = context.filesDir
    private val actorsFile: File get() = File(root, "actors.json")
    private val projectsDir: File get() = File(root, "projects").apply { mkdirs() }

    fun file(relative: String): File = File(root, relative)

    // ---------- Aktyorlar ----------

    fun loadActors(): MutableList<Actor> = runCatching {
        if (!actorsFile.exists()) return@runCatching mutableListOf<Actor>()
        json.decodeFromString<MutableList<Actor>>(actorsFile.readText())
    }.getOrElse { mutableListOf() }

    fun saveActors(actors: List<Actor>) {
        actorsFile.writeText(json.encodeToString(actors))
    }

    fun upsertActor(actor: Actor) {
        val list = loadActors()
        val i = list.indexOfFirst { it.id == actor.id }
        if (i >= 0) list[i] = actor else list.add(actor)
        saveActors(list)
    }

    fun deleteActor(id: String) {
        saveActors(loadActors().filterNot { it.id == id })
        File(root, "actors/$id").deleteRecursively()
    }

    /** Galereyadan tanlangan rasmni aktyor papkasiga nusxalaydi va nisbiy yo'lni qaytaradi. */
    fun importActorPhoto(actorId: String, uri: Uri): String? = runCatching {
        val dir = File(root, "actors/$actorId").apply { mkdirs() }
        val bmp = decodeUri(uri, 1280) ?: return null
        val name = "ref_${System.currentTimeMillis()}.jpg"
        val out = File(dir, name)
        out.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        bmp.recycle()
        "actors/$actorId/$name"
    }.getOrNull()

    // ---------- Loyihalar ----------

    private fun projectFile(id: String) = File(projectsDir, "$id/project.json")

    fun loadProjects(): List<Project> =
        (projectsDir.listFiles()?.toList() ?: emptyList())
            .mapNotNull { dir -> runCatching { json.decodeFromString<Project>(File(dir, "project.json").readText()) }.getOrNull() }
            .sortedByDescending { it.updatedAt }

    fun loadProject(id: String): Project? =
        runCatching { json.decodeFromString<Project>(projectFile(id).readText()) }.getOrNull()

    fun saveProject(project: Project) {
        project.updatedAt = System.currentTimeMillis()
        val f = projectFile(project.id)
        f.parentFile?.mkdirs()
        f.writeText(json.encodeToString(project))
    }

    fun deleteProject(id: String) {
        File(projectsDir, id).deleteRecursively()
    }

    fun newProject(idea: String): Project = Project(id = UUID.randomUUID().toString(), idea = idea)

    /** Sahna rasmini saqlaydi, nisbiy yo'lni qaytaradi. */
    fun saveSceneImage(projectId: String, sceneNumber: Int, bytes: ByteArray): String {
        val dir = File(projectsDir, projectId).apply { mkdirs() }
        val out = File(dir, "scene_${sceneNumber}_${System.currentTimeMillis()}.jpg")
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        if (bmp != null) {
            out.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 95, it) }
            bmp.recycle()
        } else {
            out.writeBytes(bytes)
        }
        return "projects/$projectId/${out.name}"
    }

    fun readBytes(relative: String): ByteArray? =
        runCatching { File(root, relative).readBytes() }.getOrNull()

    fun loadBitmap(relative: String, maxSide: Int = 1600): Bitmap? =
        runCatching { decodeFile(File(root, relative), maxSide) }.getOrNull()

    // ---------- Rasm o'qish yordamchilari ----------

    private fun decodeUri(uri: Uri, maxSide: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxSide) }
        val bmp = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
        val rotation = context.contentResolver.openInputStream(uri)?.use { readRotation(it) } ?: 0
        return scaleAndRotate(bmp, maxSide, rotation)
    }

    private fun decodeFile(file: File, maxSide: Int): Bitmap? {
        if (!file.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxSide) }
        val bmp = BitmapFactory.decodeFile(file.absolutePath, opts) ?: return null
        return scaleAndRotate(bmp, maxSide, 0)
    }

    private fun readRotation(stream: java.io.InputStream): Int =
        when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }

    private fun sampleSize(w: Int, h: Int, maxSide: Int): Int {
        var sample = 1
        var max = maxOf(w, h)
        while (max / sample > maxSide * 2) sample *= 2
        return sample
    }

    private fun scaleAndRotate(src: Bitmap, maxSide: Int, rotation: Int): Bitmap {
        val longest = maxOf(src.width, src.height)
        var bmp = src
        if (longest > maxSide) {
            val ratio = maxSide.toFloat() / longest
            val scaled = Bitmap.createScaledBitmap(bmp, (bmp.width * ratio).toInt().coerceAtLeast(1), (bmp.height * ratio).toInt().coerceAtLeast(1), true)
            if (scaled != bmp) bmp.recycle()
            bmp = scaled
        }
        if (rotation != 0) {
            val m = Matrix().apply { postRotate(rotation.toFloat()) }
            val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
            if (rotated != bmp) bmp.recycle()
            bmp = rotated
        }
        return bmp
    }
}
