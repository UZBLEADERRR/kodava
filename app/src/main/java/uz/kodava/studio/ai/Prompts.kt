package uz.kodava.studio.ai

import uz.kodava.studio.data.Actor
import uz.kodava.studio.data.Project
import uz.kodava.studio.data.Scene
import uz.kodava.studio.data.Styles

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
            "- id: ${a.id} | ism: ${a.name} | izoh: ${a.note.ifBlank { "-" }} | tashqi ko'rinish: ${a.appearance.ifBlank { "-" }}"
        }
        return """
            Sen tajribali kino ssenariynavis va rejissyorsan. Foydalanuvchi g'oyasidan qisqa video uchun
            senariy yoz va uni sahnalarga bo'lib, storyboard tayyorla.

            G'OYA:
            ${project.idea}

            USLUB: ${project.style}
            KADR NISBATI: ${project.aspect}
            SAHNALAR SONI: aniq ${project.sceneCount} ta

            AKTYORLAR RO'YXATI (faqat shu id'lardan foydalan):
            $cast

            QOIDALAR:
            1. "title", "logline", "summary", "dialogue" — O'ZBEK TILIDA (lotin alifbosi).
            2. "image_prompt" — INGLIZ TILIDA, juda batafsil: kim kadrda, nima qilyapti, qayerda,
               yorug'lik, kamera rakursi, plan (wide/medium/close-up), rang palitrasi, kayfiyat.
               image_prompt ichida personajni FAQAT ismi bilan atа (masalan "Aziz"), yuzini tasvirlama.
            3. Har bir sahnada ishtirok etayotgan aktyorlarning id'larini "actor_ids" ga yoz.
            4. Sahnalar mantiqiy ketma-ketlikda bo'lsin: boshlanish, rivoj, kulminatsiya, yakun.
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

    /** Bitta sahna kadri uchun rasm prompti — yuz o'zgarmasligi shu yerda ta'minlanadi. */
    fun sceneImage(project: Project, scene: Scene, actors: List<Actor>): String {
        val identity = if (actors.isEmpty()) "" else buildString {
            appendLine()
            appendLine("IDENTITY LOCK — the attached reference photos define real people who MUST appear with the exact same face:")
            actors.forEachIndexed { index, actor ->
                appendLine("- Person ${index + 1} is \"${actor.name}\". Reference photo(s) #${index + 1}. ${actor.appearance.ifBlank { actor.note }}")
            }
            appendLine(
                "Reproduce each person's facial identity exactly as in their reference photo: same face shape, " +
                    "eyes, nose, mouth, skin tone, hair and distinctive marks. Do not beautify, do not age, " +
                    "do not change ethnicity, do not swap or blend faces between people. " +
                    "Clothing, pose, expression, lighting and background must follow the scene description."
            )
        }

        return buildString {
            appendLine("Generate a single storyboard frame for a short film. Photorealistic quality, no text, no watermark, no collage, no split screen.")
            appendLine("Visual style: ${Styles.toPrompt(project.style)}.")
            appendLine("Aspect ratio: ${project.aspect}.")
            appendLine()
            appendLine("FILM: ${project.title.ifBlank { project.idea.take(80) }}")
            appendLine("SCENE ${scene.n}: ${scene.title}")
            appendLine("Location: ${scene.location.ifBlank { "-" }} | Time: ${scene.timeOfDay.ifBlank { "-" }} | Camera: ${scene.camera.ifBlank { "medium shot" }}")
            appendLine()
            appendLine("SHOT DESCRIPTION: ${scene.imagePrompt}")
            append(identity)
        }.trim()
    }

    /** Sahna promptini AI yordamida qayta yozish (kuchaytirish). */
    fun improvePrompt(current: String, style: String): String = """
        Rewrite the following storyboard shot description into one richer English image prompt.
        Keep the same action, characters (by name) and location, but add camera angle, lens, lighting,
        color palette, composition and mood. Never describe the characters' faces. Max 120 words.
        Answer with the prompt only, no preamble.

        Visual style: ${Styles.toPrompt(style)}
        Current description: $current
    """.trimIndent()
}
