package uz.kodava.studio.ai

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import uz.kodava.studio.data.Actor
import uz.kodava.studio.data.Line
import uz.kodava.studio.data.Scene

data class ParsedScript(
    val title: String,
    val logline: String,
    val scenes: List<Scene>
)

/** Model qaytargan JSON matnini ilova modellariga aylantiradi (formatga nisbatan bardoshli). */
object ScriptParser {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parse(raw: String, actors: List<Actor>): ParsedScript {
        val cleaned = clean(raw)
        val root = runCatching { json.parseToJsonElement(cleaned).jsonObject }.getOrNull()
            ?: throw GeminiException("Senariyni o'qib bo'lmadi. Qayta urinib ko'ring.")

        val scenesArray = root["scenes"]?.let { it as? JsonArray } ?: JsonArray(emptyList())
        val scenes = scenesArray.mapIndexedNotNull { index, element ->
            val obj = element as? JsonObject ?: return@mapIndexedNotNull null
            Scene(
                n = obj.int("n") ?: (index + 1),
                title = obj.str("title").orEmpty().ifBlank { "Sahna ${index + 1}" },
                summary = obj.str("summary").orEmpty(),
                location = obj.str("location").orEmpty(),
                timeOfDay = obj.str("time_of_day") ?: obj.str("timeOfDay") ?: "",
                actorIds = resolveActors(obj, actors).toMutableList(),
                dialogue = parseDialogue(obj).toMutableList(),
                camera = obj.str("camera").orEmpty(),
                durationSec = (obj.int("duration_sec") ?: obj.int("durationSec") ?: 5).coerceIn(2, 15),
                imagePrompt = (obj.str("image_prompt") ?: obj.str("imagePrompt") ?: obj.str("summary")).orEmpty()
            )
        }
        if (scenes.isEmpty()) throw GeminiException("Model sahnalarni qaytarmadi. Qayta urinib ko'ring.")

        return ParsedScript(
            title = root.str("title").orEmpty().ifBlank { "Nomsiz loyiha" },
            logline = root.str("logline").orEmpty(),
            scenes = scenes.mapIndexed { i, s -> s.copy(n = i + 1) }
        )
    }

    private fun parseDialogue(obj: JsonObject): List<Line> {
        val arr = obj["dialogue"] as? JsonArray ?: return emptyList()
        return arr.mapNotNull { element ->
            val line = element as? JsonObject ?: return@mapNotNull null
            val text = line.str("text") ?: line.str("line") ?: return@mapNotNull null
            if (text.isBlank()) null else Line(speaker = line.str("speaker").orEmpty(), text = text)
        }
    }

    /** Model id yoki ism qaytarishi mumkin — ikkalasini ham qabul qilamiz. */
    private fun resolveActors(obj: JsonObject, actors: List<Actor>): List<String> {
        val arr = (obj["actor_ids"] ?: obj["actorIds"] ?: obj["characters"]) as? JsonArray ?: return emptyList()
        val values = arr.mapNotNull { runCatching { it.jsonPrimitive.content }.getOrNull() }
        return values.mapNotNull { value ->
            actors.firstOrNull { it.id == value }?.id
                ?: actors.firstOrNull { it.name.isNotBlank() && it.name.equals(value.trim(), ignoreCase = true) }?.id
                ?: actors.firstOrNull { it.name.isNotBlank() && value.contains(it.name, ignoreCase = true) }?.id
        }.distinct()
    }

    private fun clean(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("```")) {
            text = text.removePrefix("```json").removePrefix("```JSON").removePrefix("```").trim()
            val end = text.lastIndexOf("```")
            if (end >= 0) text = text.substring(0, end).trim()
        }
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        return if (start >= 0 && end > start) text.substring(start, end + 1) else text
    }

    private fun JsonObject.str(key: String): String? =
        runCatching { this[key]?.jsonPrimitive?.content?.takeIf { it != "null" } }.getOrNull()

    private fun JsonObject.int(key: String): Int? =
        runCatching { this[key]?.jsonPrimitive?.content?.trim()?.toDouble()?.toInt() }.getOrNull()
}
