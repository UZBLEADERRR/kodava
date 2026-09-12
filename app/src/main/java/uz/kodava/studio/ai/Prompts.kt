package uz.kodava.studio.ai

import uz.kodava.studio.data.Actor
import uz.kodava.studio.data.Project
import uz.kodava.studio.data.Scene
import uz.kodava.studio.data.Styles

/** Bitta aktyor uchun yuborilgan etalon suratlar diapazoni. */
data class RefBlock(
    val name: String,
    val appearance: String,
    val firstImage: Int,
    val lastImage: Int
) {
    val label: String
        get() = if (firstImage == lastImage) "image #$firstImage" else "images #$firstImage-#$lastImage"
}

/** Barcha AI so'rovlari matni shu yerda yig'iladi. */
object Prompts {

    /** Aktyor suratidan "yuz pasporti" — har bir sahnada takrorlanib, yuzni bir xil ushlab turadi. */
    fun actorAppearance(actor: Actor): String = """
        You are a casting director writing a character reference sheet for an image model.
        Look at the attached photo(s) of one person and describe ONLY their permanent visual identity
        in one dense English paragraph (max 90 words): approximate age, gender presentation, skin tone,
        face shape, eye shape and color, eyebrows, nose, lips, cheekbones, jawline, facial hair,
        hairstyle, hair color and length, and any distinctive marks (moles, scars, glasses).
        Do NOT describe clothing, background, lighting, pose, mood or emotions.
        Do not add any preamble. Answer with the paragraph only.
        Extra note from the user about this character: ${actor.note.ifBlank { "-" }}
    """.trimIndent()

    /** G'oyadan senariy + sahnalar (JSON). */
    fun script(project: Project, actors: List<Actor>): String {
        val cast = if (actors.isEmpty()) "Aktyorlar ko'rsatilmagan — kerak bo'lsa personajlarni o'zingiz o'ylab toping."
        else actors.joinToString("\n") { a ->
            "- id: ${a.id} | ism: ${a.name} | izoh: ${a.note.ifBlank { "-" }}"
        }
        return """
            Sen tajribali kino ssenariynavis va rejissyorsan. Foydalanuvchi g'oyasidan qisqa video uchun
            senariy yoz va uni sahnalarga bo'lib, storyboard tayyorla.

            G'OYA:
            ${project.idea}

            USLUB: ${project.style}
            KADR NISBATI: ${project.aspect}
            SAHNALAR SONI: aniq ${project.sceneCount} ta

            AKTYORLAR RO'YXATI (faqat shu id'lardan foydalan, yangi personaj o'ylab topma):
            $cast

            QOIDALAR:
            1. "title", "logline", "summary", "dialogue" — O'ZBEK TILIDA (lotin alifbosi).
            2. "image_prompt" — INGLIZ TILIDA, batafsil: kim kadrda, nima qilyapti, qayerda,
               yorug'lik, kamera rakursi, plan (wide/medium/close-up), rang palitrasi, kayfiyat.
               image_prompt ichida personajni FAQAT ismi bilan atab o't (masalan "Aziz") —
               uning yuzini, sochini yoki yoshini TASVIRLAMA, chunki yuz etalon surat orqali beriladi.
            3. Har bir sahnada ishtirok etayotgan aktyorlarning id'larini "actor_ids" ga yoz.
               Deyarli har bir sahnada kamida bitta aktyor bo'lsin.
            4. Sahnalar mantiqiy ketma-ketlikda: boshlanish, rivoj, kulminatsiya, yakun.
            5. Har bir sahna uchun "duration_sec" 3 dan 8 gacha butun son.
            6. Hech qanday izoh yozma — FAQAT quyidagi JSON.

            JSON sxemasi:
            {
              "title": "string",
              "logline": "string",
              "scenes": [
                {
                  "n": 1,
                  "title": "string",
                  "summary": "string",
                  "location": "string",
                  "time_of_day": "string",
                  "actor_ids": ["string"],
                  "camera": "string",
                  "duration_sec": 5,
                  "dialogue": [{"speaker": "string", "text": "string"}],
                  "image_prompt": "string"
                }
              ]
            }
        """.trimIndent()
    }

    /**
     * Bitta sahna kadri uchun rasm prompti.
     * Etalon suratlar so'rovning boshida turadi, shuning uchun yo'riqnoma ham
     * ana shu suratlarga ochiq havola qiladi — yuz o'zgarmasligining asosiy sharti.
     */
    fun sceneImage(project: Project, scene: Scene, blocks: List<RefBlock>): String = buildString {
        if (blocks.isNotEmpty()) {
            appendLine("PHOTO REFERENCES OF REAL PEOPLE ARE ATTACHED ABOVE. READ THIS FIRST.")
            blocks.forEach { block ->
                appendLine("- Attached ${block.label} = \"${block.name}\". ${block.appearance}")
            }
            appendLine()
            appendLine(
                "Your task: draw these EXACT people in a new film frame. Every face you draw must be " +
                    "recognisably the same person as in their attached photo — identical face shape, eyes, " +
                    "eyebrows, nose, mouth, jawline, skin tone, hair and distinctive marks. " +
                    "Treat the result as a new photograph of the same person taken on a film set. " +
                    "Never invent a different face, never replace them with a generic model, never change " +
                    "their age, gender or ethnicity, and never mix two people's features. " +
                    "Only clothing, pose, expression, lighting and background follow the scene below."
            )
            appendLine()
        }

        appendLine("SCENE ${scene.n} — ${scene.title}")
        appendLine("Action: ${scene.imagePrompt}")
        appendLine("Location: ${scene.location.ifBlank { "-" }} | Time: ${scene.timeOfDay.ifBlank { "-" }} | Camera: ${scene.camera.ifBlank { "medium shot" }}")
        if (blocks.isNotEmpty()) {
            appendLine("People in this frame: ${blocks.joinToString(", ") { it.name }}.")
        }
        appendLine()
        appendLine("Style: ${Styles.toPrompt(project.style)}.")
        appendLine("Framing: ${project.aspect} aspect ratio, single frame, high detail, sharp focus on faces, professional colour grading.")
        append("Do not render any text, letters, logos, watermark, collage, split screen or border.")
    }

    /** Sahna promptini AI yordamida qayta yozish (kuchaytirish). */
    fun improvePrompt(current: String, style: String): String = """
        Rewrite the following storyboard shot description into one richer English image prompt.
        Keep the same action, characters (by name) and location, but add camera angle, lens, lighting,
        color palette, composition and mood. Never describe the characters' faces or hair.
        Max 120 words. Answer with the prompt only, no preamble.

        Visual style: ${Styles.toPrompt(style)}
        Current description: $current
    """.trimIndent()
}
