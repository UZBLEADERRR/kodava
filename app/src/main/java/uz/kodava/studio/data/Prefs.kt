package uz.kodava.studio.data

import android.content.Context

/** API kalit va model sozlamalari (faqat shu ilova ichida saqlanadi). */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("kodava_prefs", Context.MODE_PRIVATE)

    var apiKey: String
        get() = sp.getString(K_API, "").orEmpty()
        set(v) = sp.edit().putString(K_API, v.trim()).apply()

    var textModel: String
        get() = sp.getString(K_TEXT, DEFAULT_TEXT_MODEL).orEmpty().ifBlank { DEFAULT_TEXT_MODEL }
        set(v) = sp.edit().putString(K_TEXT, v.trim()).apply()

    var imageModel: String
        get() = sp.getString(K_IMAGE, DEFAULT_IMAGE_MODEL).orEmpty().ifBlank { DEFAULT_IMAGE_MODEL }
        set(v) = sp.edit().putString(K_IMAGE, v.trim()).apply()

    fun hasKey(): Boolean = apiKey.isNotBlank()

    companion object {
        const val DEFAULT_TEXT_MODEL = "gemini-2.5-flash"
        const val DEFAULT_IMAGE_MODEL = "gemini-2.5-flash-image"
        private const val K_API = "api_key"
        private const val K_TEXT = "text_model"
        private const val K_IMAGE = "image_model"
    }
}
