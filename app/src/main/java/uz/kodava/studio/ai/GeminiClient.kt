package uz.kodava.studio.ai

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/** Rasm yoki boshqa ikkilik ma'lumot — modelga yuboriladigan qism. */
data class InlineImage(val bytes: ByteArray, val mimeType: String = "image/jpeg") {
    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = System.identityHashCode(this)
}

class GeminiException(message: String) : Exception(message)

/**
 * Google Gemini API bilan ishlovchi minimal klient.
 * Matn (senariy) va rasm (sahna kadri) generatsiyasi uchun ishlatiladi.
 */
class GeminiClient(
    private val apiKey: String,
    private val textModel: String,
    private val imageModel: String
) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    /** Matn so'rovi. [asJson] true bo'lsa model faqat JSON qaytaradi. */
    suspend fun generateText(
        prompt: String,
        images: List<InlineImage> = emptyList(),
        asJson: Boolean = false,
        temperature: Double = 0.9
    ): String = withContext(Dispatchers.IO) {
        val body = buildJsonObject {
            putJsonArray("contents") {
                add(buildJsonObject {
                    put("role", "user")
                    putJsonArray("parts") {
                        images.forEach { img ->
                            add(buildJsonObject {
                                putJsonObject("inline_data") {
                                    put("mime_type", img.mimeType)
                                    put("data", img.bytes.toBase64())
                                }
                            })
                        }
                        add(buildJsonObject { put("text", prompt) })
                    }
                })
            }
            putJsonObject("generationConfig") {
                put("temperature", temperature)
                if (asJson) put("responseMimeType", "application/json")
            }
        }
        val response = post(textModel, body)
        val text = response.parts().mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.contentOrNullSafe() }
            .joinToString("\n")
            .trim()
        if (text.isBlank()) throw GeminiException("Model bo'sh javob qaytardi. Qayta urinib ko'ring.")
        text
    }

    /**
     * Rasm generatsiyasi. [references] — aktyorlarning etalon suratlari; model shu yuzlarni saqlab qoladi.
     * Ba'zi model versiyalari `responseModalities` yoki `imageConfig` ni qo'llamaydi — shuning uchun
     * so'rov bosqichma-bosqich soddalashtirilib qayta yuboriladi.
     */
    suspend fun generateImage(
        prompt: String,
        references: List<InlineImage> = emptyList(),
        aspectRatio: String? = null
    ): ByteArray = withContext(Dispatchers.IO) {
        val variants = listOf(
            buildImageBody(prompt, references, aspectRatio, modalities = true),
            buildImageBody(prompt, references, null, modalities = true),
            buildImageBody(prompt, references, null, modalities = false)
        )
        var lastError: Exception? = null
        for (body in variants) {
            try {
                val response = post(imageModel, body)
                val data = response.parts().firstNotNullOfOrNull { part ->
                    val inline = part.jsonObject["inlineData"]?.jsonObject ?: part.jsonObject["inline_data"]?.jsonObject
                    inline?.get("data")?.jsonPrimitive?.contentOrNullSafe()
                } ?: throw GeminiException(
                    "Model rasm qaytarmadi. Sabab matni: " +
                        response.parts().mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.contentOrNullSafe() }
                            .joinToString(" ").take(300).ifBlank { "noma'lum" }
                )
                return@withContext Base64.decode(data, Base64.DEFAULT)
            } catch (e: GeminiException) {
                lastError = e
                // 400 — so'rov formati qo'llanmaydi, keyingi (soddaroq) variantni sinaymiz.
                if (e.message?.contains("HTTP 400") != true) throw e
            }
        }
        throw lastError ?: GeminiException("Rasm yaratib bo'lmadi.")
    }

    private fun buildImageBody(
        prompt: String,
        references: List<InlineImage>,
        aspectRatio: String?,
        modalities: Boolean
    ): JsonObject = buildJsonObject {
        putJsonArray("contents") {
            add(buildJsonObject {
                put("role", "user")
                putJsonArray("parts") {
                    references.forEach { img ->
                        add(buildJsonObject {
                            putJsonObject("inline_data") {
                                put("mime_type", img.mimeType)
                                put("data", img.bytes.toBase64())
                            }
                        })
                    }
                    add(buildJsonObject { put("text", prompt) })
                }
            })
        }
        putJsonObject("generationConfig") {
            if (modalities) putJsonArray("responseModalities") { add("TEXT"); add("IMAGE") }
            if (aspectRatio != null) putJsonObject("imageConfig") { put("aspectRatio", aspectRatio) }
        }
    }

    private fun post(model: String, body: JsonObject): JsonObject {
        if (apiKey.isBlank()) throw GeminiException("API kalit kiritilmagan. Sozlamalar bo'limiga kiring.")
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"
        val request = Request.Builder()
            .url(url)
            .addHeader("x-goog-api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody(JSON_MEDIA))
            .build()

        http.newCall(request).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw GeminiException("HTTP ${resp.code}: ${extractApiError(text)}")
            }
            val parsed = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull()
                ?: throw GeminiException("Javobni o'qib bo'lmadi.")
            parsed["promptFeedback"]?.jsonObject?.get("blockReason")?.jsonPrimitive?.contentOrNullSafe()?.let {
                throw GeminiException("So'rov xavfsizlik filtri tomonidan bloklandi ($it). Matnni yumshatib qayta urining.")
            }
            return parsed
        }
    }

    private fun extractApiError(raw: String): String = runCatching {
        json.parseToJsonElement(raw).jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNullSafe()
            ?: raw.take(300)
    }.getOrElse { raw.take(300) }

    private fun JsonObject.parts(): List<kotlinx.serialization.json.JsonElement> {
        val candidates = this["candidates"]?.jsonArray ?: JsonArray(emptyList())
        val first = candidates.firstOrNull()?.jsonObject ?: return emptyList()
        return first["content"]?.jsonObject?.get("parts")?.jsonArray?.toList() ?: emptyList()
    }

    private fun kotlinx.serialization.json.JsonPrimitive.contentOrNullSafe(): String? =
        if (this.content == "null") null else this.content

    private fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)

    companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }
}
