package uz.kodava.studio.data

import kotlinx.serialization.Serializable

/** Bitta aktyor: ismi, tashqi ko'rinish tavsifi va yuz etaloni bo'lgan rasmlar. */
@Serializable
data class Actor(
    val id: String,
    var name: String = "",
    /** Rol/xarakter haqida qisqacha izoh (yosh, kasb, kiyim uslubi). */
    var note: String = "",
    /** Model uchun ingliz tilidagi yuz "pasporti" — har bir sahnada takrorlanadi. */
    var appearance: String = "",
    /** filesDir ichidagi nisbiy yo'llar: actors/<id>/ref_0.jpg ... */
    var refs: MutableList<String> = mutableListOf(),
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class Line(
    val speaker: String = "",
    val text: String = ""
)

@Serializable
data class Scene(
    val n: Int,
    var title: String = "",
    /** O'zbekcha sahna tavsifi — foydalanuvchi o'qishi uchun. */
    var summary: String = "",
    var location: String = "",
    var timeOfDay: String = "",
    /** Sahnada ishtirok etadigan aktyor id'lari. */
    var actorIds: MutableList<String> = mutableListOf(),
    var dialogue: MutableList<Line> = mutableListOf(),
    var camera: String = "",
    var durationSec: Int = 5,
    /** Rasm generatsiyasi uchun inglizcha prompt (tahrirlash mumkin). */
    var imagePrompt: String = "",
    /** projects/<id>/scene_<n>.jpg — yaratilgan bo'lsa. */
    var imagePath: String? = null
)

@Serializable
data class Project(
    val id: String,
    var idea: String = "",
    var title: String = "",
    var logline: String = "",
    var style: String = Styles.CINEMATIC,
    var aspect: String = "16:9",
    var sceneCount: Int = 6,
    var actorIds: MutableList<String> = mutableListOf(),
    var scenes: MutableList<Scene> = mutableListOf(),
    var videoPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
)

object Styles {
    const val CINEMATIC = "Kinematografik (realistik)"
    val all = listOf(
        CINEMATIC,
        "Hujjatli film",
        "Anime",
        "3D animatsiya (Pixar uslubi)",
        "Komiks / grafik roman",
        "Retro film (80-yillar)",
        "Reklama roligi",
        "Qora-oq noir"
    )

    /** Model uchun inglizcha uslub yo'riqnomasi. */
    fun toPrompt(style: String): String = when (style) {
        "Hujjatli film" -> "documentary photography look, natural available light, handheld feel, realistic skin texture"
        "Anime" -> "modern anime key visual, clean line art, cel shading, expressive eyes, detailed background"
        "3D animatsiya (Pixar uslubi)" -> "high quality 3D animated feature film render, soft global illumination, stylized but believable characters"
        "Komiks / grafik roman" -> "graphic novel illustration, bold inked outlines, halftone shading, dramatic panel composition"
        "Retro film (80-yillar)" -> "1980s film still, 35mm grain, warm analog colors, slight halation, vintage lenses"
        "Reklama roligi" -> "premium commercial advertising frame, glossy lighting, shallow depth of field, product-hero composition"
        "Qora-oq noir" -> "black and white film noir still, hard key light, deep shadows, smoke, high contrast"
        else -> "cinematic film still, shot on 35mm, shallow depth of field, natural cinematic lighting, film grain, color graded"
    }
}

object Aspects {
    val all = listOf("16:9", "9:16", "1:1", "4:3", "3:4")

    /** Slayd-shou video uchun o'lcham. */
    fun videoSize(aspect: String): Pair<Int, Int> = when (aspect) {
        "9:16" -> 720 to 1280
        "1:1" -> 1024 to 1024
        "4:3" -> 1280 to 960
        "3:4" -> 960 to 1280
        else -> 1280 to 720
    }
}
