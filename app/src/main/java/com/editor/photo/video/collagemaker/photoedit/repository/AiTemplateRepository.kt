package com.editor.photo.video.collagemaker.photoedit.repository

import com.editor.photo.video.collagemaker.photoedit.R
import com.editor.photo.video.collagemaker.photoedit.models.AiTemplate
import com.editor.photo.video.collagemaker.photoedit.models.ToolType

/**
 * AiTemplateRepository — Single Source of Truth for all AI templates.
 *
 * Architecture role: Data Layer
 * Pattern: Kotlin Singleton (object)
 *
 * Fragments and ViewModels must NEVER hardcode template data.
 * All template access goes through this repository exclusively.
 *
 * Usage:
 *   AiTemplateRepository.getGroupTemplates()
 *   AiTemplateRepository.getSingleTemplates()
 *   AiTemplateRepository.getTemplateById(101)
 *   AiTemplateRepository.getAllAsPhotoTools()
 */
object AiTemplateRepository {

    // ── Public API ────────────────────────────────────────────────────────────

    fun getGroupTemplates(): List<AiTemplate> = groupTemplates

    fun getSingleTemplates(): List<AiTemplate> {
        val priorityIds = listOf(107, 108, 109,110,111)
        val priority = priorityIds.mapNotNull { id -> singleTemplates.find { it.id == id } }
        val remaining = singleTemplates.filterNot { it.id in priorityIds }
        return priority + remaining
    }

    fun getTemplateById(id: Int): AiTemplate? =
        (groupTemplates + singleTemplates).firstOrNull { it.id == id }

    fun getTemplatesByType(type: ToolType): List<AiTemplate> = when (type) {
        ToolType.AI_GROUP_PHOTO  -> groupTemplates
        ToolType.AI_SINGLE_PHOTO -> singleTemplates
        else                     -> emptyList()
    }

    // ── Group Photo Templates (IDs 1–18) ──────────────────────────────────────

    private val groupTemplates: List<AiTemplate> = listOf(

        AiTemplate(
            id = 1,
            imageResId = R.drawable.template_1,
            title = "Stadium Couple",
            prompt = "A photorealistic, cinematic photo of person1_image and person2_image sitting together in a crowded stadium. " +
                    "POSE — CRITICAL: Generate completely NEW body poses. DO NOT use the original body poses from the uploaded photos. " +
                    "The man (person1_image) is seated in a stadium seat, his body facing forward, both hands resting on his knees or lap, torso slightly leaning toward the woman on his right, head turned slightly toward the camera. " +
                    "The woman (person2_image) is seated in the stadium seat directly to the man's right, her body facing forward, hands in her lap, torso slightly leaning toward the man, head facing the camera with a smile. " +
                    "Both are clearly sitting down with bent knees, thighs visible on the seats, upper bodies visible from waist up. " +
                    "CRITICAL INSTRUCTION FOR IDENTITY: Strictly map the exact facial features, skin tones, and identities of person1_image and person2_image onto these NEW poses. Do not blend their faces. " +
                    "CLOTHING: Both person1_image and person2_image MUST be wearing the exact clothes from their uploaded photos, adjusted to fit the seated stadium pose. " +
                    "BACKGROUND: A packed crowded stadium with blurred audience, stadium seats, and bright stadium floodlights in the background. " +
                    "Television broadcast aesthetic, shallow depth of field, blurred stadium background, highly detailed faces, 8k resolution, ultra-realistic."
        ),

        AiTemplate(
            id = 2,
            imageResId = R.drawable.template_2,
            title = "Intimate Hug",
            prompt = "A photorealistic, intimate portrait of person1_image and person2_image against a solid olive green studio background. " +
                    "POSE — CRITICAL: Generate completely NEW body poses. DO NOT use the original body poses from the uploaded photos. " +
                    "The man (person1_image) is standing upright in the center of the frame, his body facing directly toward the camera, both arms relaxed down at his sides, head facing the camera. " +
                    "The woman (person2_image) is standing directly behind the man, her chest pressed gently against his back, both of her arms wrapped forward around the man's chest and stomach in an affectionate hug from behind, her chin resting near his right shoulder, her head leaning forward so her face is visible over his shoulder, smiling at the camera. " +
                    "The man's full upper body is visible from the front. The woman's face and both arms are visible from behind him. " +
                    "CRITICAL INSTRUCTION FOR IDENTITY: Strictly map the exact distinct facial features, skin tones, and hairstyles of person1_image and person2_image onto these NEW poses. Do not mix or blend their faces. " +
                    "CLOTHING: Both person1_image and person2_image MUST be wearing the exact clothes from their uploaded photos, naturally fitted and adjusted to this hugging pose. " +
                    "BACKGROUND: Solid flat olive green studio background, no patterns, no gradients. " +
                    "Soft even studio lighting, highly detailed faces, 8k resolution, ultra-realistic."
        ),

        AiTemplate(
            id = 3,
            imageResId = R.drawable.template_3,
            title = "Mahjong Tiles",
            prompt = "A photorealistic, top-down overhead photo taken from directly above, looking straight down. " +
                    "POSE — CRITICAL: Generate completely NEW body poses. DO NOT use the original body poses from the uploaded photos. " +
                    "The man (person1_image) is lying flat on his back on a dark green surface, his entire body horizontal and parallel to the ground, arms resting at his sides, his face looking straight up toward the camera. He is holding one small white Mahjong tile up with one hand raised near his face, tilted so the tile faces the camera. " +
                    "The woman (person2_image) is lying flat on her back on the same dark green surface beside the man, her entire body horizontal, arms resting at her sides, face looking straight up toward the camera. She is also holding one small white Mahjong tile up with one hand raised near her face. " +
                    "Both bodies are fully flat and horizontal as seen from directly overhead. Their faces are upward facing the camera. Scattered white Mahjong tiles are spread all around both of them on the dark green surface. " +
                    "CRITICAL INSTRUCTION FOR IDENTITY: Strictly map the exact facial features, skin tones, and identities of person1_image and person2_image onto these NEW flat lying poses. Do not blend their faces. " +
                    "CLOTHING: Both person1_image and person2_image MUST be wearing the exact clothes from their uploaded photos, adjusted to fit the lying flat on back pose. " +
                    "BACKGROUND: Dark green flat surface fully covered with scattered white Mahjong tiles around the two people. " +
                    "Directly overhead top-down camera angle, bright flash photography, casual and fun aesthetic, highly detailed faces, 8k resolution, ultra-realistic."
        ),

        AiTemplate(
            id = 4,
            imageResId = R.drawable.template_4,
            title = "Retro Lights",
            prompt = "A photorealistic, cinematic close-up selfie-style photo of person1_image and person2_image together in a retro 1980s styled room. " +
                    "POSE — CRITICAL: Generate completely NEW body poses. DO NOT use the original body poses from the uploaded photos. " +
                    "The man (person1_image) is on the left, holding the camera with one arm extended taking the selfie, his face close to the camera, looking directly into the lens with a relaxed smile. " +
                    "The woman (person2_image) is on the right, standing close beside the man, leaning her head toward him, smiling at the camera. " +
                    "Both faces are close together filling the upper portion of the frame in a natural selfie crop. " +
                    "CRITICAL INSTRUCTION FOR IDENTITY: Strictly map the exact facial features, skin tones, hairstyles, and identities of person1_image and person2_image onto these NEW poses. Do not blend their faces. " +
                    "CLOTHING: Both person1_image and person2_image MUST be wearing the exact clothes from their uploaded photos. Do NOT change or replace their clothing. " +
                    "BACKGROUND: A retro 1980s styled room interior visible behind them with vintage floral patterned wallpaper on the walls and colorful Christmas string lights strung across the background glowing warmly. The background is slightly blurred from the selfie depth of field. " +
                    "Warm cinematic golden lighting from the string lights, shallow depth of field, selfie camera angle, slightly grainy vintage film texture, highly detailed faces, 8k resolution, ultra-realistic."
        ),

        AiTemplate(
            id = 5,
            imageResId = R.drawable.template_5,
            title = "Beach Mirror",
            prompt = "A photorealistic photo of a close-up hand holding a round mirror. Inside the mirror's reflection, person1_image and person2_image are posing closely together. The man (person1_image) is holding a smartphone, taking a mirror selfie. The woman (person2_image) has a white and yellow plumeria flower tucked in her hair. The background outside the mirror is a blurred tropical beach with palm trees and a blue sky. " +
                    "CRITICAL INSTRUCTION: You must strictly maintain 100% of the original identities from the uploaded images. " +
                    "Both person1_image and person2_image MUST be wearing their exact original clothes, shirts, and outfits from their uploaded photos. " +
                    "Strictly preserve their distinct facial features, hairstyles, skin tones, and overall body appearances exactly as they look in the original pictures. Do not mix or blend their faces. " +
                    "Bright tropical sunlight, highly detailed faces, 8k resolution, ultra-realistic."
        ),

        AiTemplate(
            id = 6,
            imageResId = R.drawable.template_6,
            title = "Red Carpet",
            prompt = "A photorealistic, cinematic photo of person1_image and person2_image standing together on a glamorous red carpet event. " +
                    "POSE: The woman (person2_image) is standing slightly in front on the left, facing the camera and smiling confidently. The man (person1_image) is standing just behind her on the right, with his hand gently placed on her waist, also facing the camera and smiling. Both are standing upright in a classic red carpet couple pose. " +
                    "CRITICAL INSTRUCTION FOR IDENTITY: Strictly map the exact facial features, skin tones, hairstyles, and identities of person1_image and person2_image. Do not blend their faces. " +
                    "CLOTHING: Both person1_image and person2_image MUST be wearing the exact clothes from their uploaded photos, adjusted to fit the standing pose. " +
                    "BACKGROUND: A busy red carpet event background with paparazzi photographers holding cameras with bright camera flashes, dark surroundings, red rope barriers with silver stanchions visible on the sides. " +
                    "Editorial fashion photography aesthetic, dramatic paparazzi flash lighting from behind, highly detailed faces, 8k resolution, ultra-realistic."
        ),

        AiTemplate(
            id = 7,
            imageResId = R.drawable.template_7,
            title = "Golden Signature",
            prompt = "A photorealistic, close-up cinematic photo of person1_image and person2_image leaning their heads together toward the camera in an intimate and glamorous setting. " +
                    "POSE: The woman (person2_image) is on the left and the man (person1_image) is on the right. Both are leaning their heads together so they are nearly touching, facing directly into the camera. Each is holding a gold metallic pen extended forward toward the camera in the lower foreground. Both have direct eye contact with the camera. " +
                    "CRITICAL INSTRUCTION FOR IDENTITY: Strictly map the exact facial features, skin tones, hairstyles, and identities of person1_image and person2_image. Do not blend their faces. " +
                    "CLOTHING: Both person1_image and person2_image MUST be wearing the exact clothes from their uploaded photos, adjusted to fit the pose. " +
                    "BACKGROUND: A dark, deep red/crimson bokeh background with warm moody nightclub or event ambiance lighting. Golden glowing cursive signature text overlaid across the image in the foreground and background, partially transparent. " +
                    "ACCESSORIES: The woman (person2_image) is wearing a multi-strand diamond/crystal choker necklace and small stud earrings. Do NOT add these accessories to person1_image. " +
                    "Close-up editorial portrait style, warm red and gold tones, dramatic moody lighting, highly detailed faces, 8k resolution, ultra-realistic."
        ),

        AiTemplate(
            id = 8,
            imageResId = R.drawable.template_8,
            title = "Elevator",
            prompt = "A photorealistic, high-angle overhead photo of person1_image and person2_image standing together inside a modern elevator, both looking up directly into the camera with a slight smile. " +
                    "POSE: The woman (person2_image) is standing on the left with arms relaxed at her sides, looking up at the camera with a subtle smile. The man (person1_image) is standing on the right, slightly leaning, with one hand holding a structured woven tote bag at his side, also looking up at the camera smiling. The camera is positioned directly above them looking straight down. " +
                    "CRITICAL INSTRUCTION FOR IDENTITY: Strictly map the exact facial features, skin tones, hairstyles, and identities of person1_image and person2_image. Do not blend their faces. " +
                    "CLOTHING: Both person1_image and person2_image MUST be wearing the exact clothes from their uploaded photos, adjusted to fit the standing pose. " +
                    "BACKGROUND: The interior of a sleek modern elevator with polished brushed stainless steel metallic walls and doors, bright even lighting from above, light grey floor visible at the bottom. " +
                    "PROPS: The man (person1_image) is holding a structured woven straw tote bag with brown leather handles at his side. " +
                    "High-angle top-down camera perspective, clean editorial fashion aesthetic, bright natural lighting, highly detailed faces, 8k resolution, ultra-realistic."
        ),

        AiTemplate(
            id = 9,
            imageResId = R.drawable.template_9,
            title = "Garden Close-Up",
            prompt = "A photorealistic, intimate close-up portrait of person1_image and person2_image with their faces pressed closely together, both looking directly into the camera. " +
                    "POSE: The woman (person2_image) is on the left, leaning her head gently sideways so her cheek rests against the man's cheek/temple, with a soft subtle smile. The man (person1_image) is on the right, pressing his face close to hers, looking straight into the camera with a calm confident expression. Both faces are at the same level, filling the entire frame in a very tight close-up portrait shot. " +
                    "CRITICAL INSTRUCTION FOR IDENTITY: Strictly map the exact facial features, skin tones, hairstyles, and identities of person1_image and person2_image. Do not blend their faces. " +
                    "CLOTHING: Both person1_image and person2_image MUST be wearing the exact clothes from their uploaded photos, adjusted to fit the pose. " +
                    "BACKGROUND: A lush soft-focus outdoor garden background with blurred pink and yellow flowering roses and green trees/foliage visible behind them. Warm natural daylight. " +
                    "FILM AESTHETIC: Slightly grainy film photography texture, warm muted tones, soft natural colors, vintage film grain overlay. " +
                    "Tight close-up portrait framing, warm golden hour natural lighting, highly detailed faces, 8k resolution, ultra-realistic."
        ),

        AiTemplate(
            id = 10,
            imageResId = R.drawable.template_10,
            title = "Lipstick Marks",
            prompt = "A photorealistic, fun and playful close-up portrait of person1_image and person2_image standing together against a deep dark crimson/burgundy red velvet draped curtain background, both smiling and looking at the camera. " +
                    "POSE: The woman (person2_image) is on the left, standing slightly in front, leaning her head toward the man with a playful smile at the camera, reaching one hand forward to grab a black satin necktie hanging from the man's collar. The man (person1_image) is on the right, standing upright and smiling broadly at the camera. " +
                    "CRITICAL INSTRUCTION FOR IDENTITY: Strictly map the exact facial features, skin tones, hairstyles, and identities of person1_image and person2_image. Do not blend their faces. " +
                    "CLOTHING: Both person1_image and person2_image MUST be wearing the exact clothes from their uploaded photos. The man (person1_image) additionally has a black satin necktie added over his existing clothing. " +
                    "BACKGROUND: Deep dark crimson/burgundy red velvet draped curtain, dimly lit with soft warm light falling on the subjects faces only. " +
                    "FACE DETAIL — CRITICAL: The man (person1_image) has exactly 5 to 6 red lipstick kiss mark prints on his face and neck. Place two kiss marks on his cheeks, one on his forehead, one on his chin, and one on his neck just above the collar. These are red lipstick imprints only — do NOT change his facial structure or skin tone. " +
                    "ACCESSORIES: The woman (person2_image) is wearing a camouflage pattern baseball cap on her head only. Do NOT change her face, hair color, or any facial feature because of this cap. " +
                    "Candid party photo booth aesthetic, warm dim lighting, slightly grainy film texture, highly detailed faces, 8k resolution, ultra-realistic."
        ),

        AiTemplate(
            id = 11,
            imageResId = R.drawable.template_11,
            title = "Escalator Selfie",
            prompt = "A photorealistic, high-angle overhead selfie-style photo of person1_image and person2_image standing together on a moving escalator, both looking up directly into the camera with serious, confident expressions. " +
                    "POSE: The man (person1_image) is on the left, standing on an escalator step, extending one arm upward holding a smartphone with a clear case to take an overhead selfie, looking directly up into the camera with a calm intense expression. The woman (person2_image) is standing on the right, one step beside the man, arms relaxed at her sides, also looking directly up into the camera with a serious confident expression. The camera angle is from directly above looking down at both of them. " +
                    "CRITICAL INSTRUCTION FOR IDENTITY: Strictly map the exact facial features, skin tones, hairstyles, and identities of person1_image and person2_image. Do not blend their faces. " +
                    "CLOTHING: Both person1_image and person2_image MUST be wearing the exact clothes from their uploaded photos, adjusted to fit the standing escalator pose. " +
                    "BACKGROUND: A modern escalator with grey ribbed metal steps, yellow safety edge strips on each step, and brushed stainless steel side panels on both sides. Cool neutral ambient lighting. " +
                    "PROPS: The man (person1_image) is holding a smartphone with a clear transparent case in his hand, pointed upward taking the selfie. " +
                    "ACCESSORIES: The man (person1_image) is wearing a small silver hoop earring and a silver cross pendant necklace. The woman (person2_image) is wearing large silver hoop earrings. " +
                    "Overhead selfie camera angle, cool toned moody editorial aesthetic, sharp even lighting, highly detailed faces, 8k resolution, ultra-realistic."
        ),

        AiTemplate(
            id = 12,
            imageResId = R.drawable.template_12,
            title = "New Year 2026",
            prompt = "A photorealistic, warm and celebratory photo of person1_image and person2_image leaning out of an open window together, holding champagne glasses and toasting, both looking at the camera. " +
                    "POSE: The man (person1_image) is on the left, leaning his arms on the window sill, smiling at the camera while holding a tall champagne flute with rosé pink champagne in one hand. The woman (person2_image) is on the right, leaning her head on the man's shoulder, raising one arm upward to hold the top edge of the open window frame, holding a champagne flute in her other hand. Both are leaning out from inside through an open window, framed by white lace curtains on both sides. " +
                    "CRITICAL INSTRUCTION FOR IDENTITY: Strictly map the exact facial features, skin tones, hairstyles, and identities of person1_image and person2_image. Do not blend their faces. " +
                    "CLOTHING: Both person1_image and person2_image MUST be wearing the exact clothes from their uploaded photos, adjusted to fit the pose. " +
                    "BACKGROUND: Outside the window is a dark night sky filled with spectacular colorful fireworks exploding in the distance. The window ledge has snow settled on it. White lace curtains frame both sides of the window interior. " +
                    "PROPS: Both person1_image and person2_image are each holding a tall champagne flute filled with rosé pink champagne. " +
                    "FOREGROUND: Large shiny gold foil number balloons spelling '2026' are floating above the window at the top of the frame, partially in front of the window. " +
                    "ACCESSORIES: The woman (person2_image) is wearing a chunky pearl statement necklace with a large black bow accent. Do NOT add these accessories to person1_image. " +
                    "Warm golden indoor lighting contrasting with cool dark night sky outside, festive New Year celebration aesthetic, highly detailed faces, 8k resolution, ultra-realistic."
        ),

        AiTemplate(
            id = 13,
            imageResId = R.drawable.template_13,
            title = "Red Room",
            prompt = "A photorealistic, high-angle overhead photo of person1_image and person2_image standing together inside a bold bright red room, both looking up directly into the camera with warm smiling expressions. " +
                    "POSE: The woman (person2_image) is on the left, standing upright and tilting her head slightly upward toward the camera, with a soft confident smile. The man (person1_image) is on the right, standing close beside her, leaning slightly toward her, smiling broadly up at the camera. Both are standing side by side, bodies angled slightly inward toward each other, looking directly up into the overhead camera. " +
                    "CRITICAL INSTRUCTION FOR IDENTITY: Strictly map the exact facial features, skin tones, hairstyles, and identities of person1_image and person2_image onto these poses. Do not blend their faces. " +
                    "CLOTHING: Both person1_image and person2_image MUST be wearing the exact clothes from their uploaded photos, adjusted to fit the standing pose. " +
                    "BACKGROUND: All walls and the floor are the same vivid bold solid red color — a bright red corner room where two red walls meet, creating a striking monochromatic red environment. Bright even lighting with no shadows. " +
                    "ACCESSORIES: The woman (person2_image) is wearing a blue paisley print bandana/headband tied as a bow on top of her head holding her hair up. Do NOT add this accessory to person1_image. " +
                    "High-angle top-down camera perspective, bold minimalist aesthetic, bright even studio lighting, highly detailed faces, 8k resolution, ultra-realistic."
        ),

        AiTemplate(
            id = 14,
            imageResId = R.drawable.template_14,
            title = "Crosswalk",
            prompt = "A photorealistic, high-angle overhead photo of person1_image and person2_image standing together on a street pedestrian crosswalk, both looking up directly into the camera. " +
                    "POSE: The man (person1_image) is on the right side, standing closer to the camera so he appears larger in the frame, leaning his upper body slightly forward toward the camera, looking directly up into the lens with a calm serious intense expression, one hand relaxed open at his side. The woman (person2_image) is on the left side, standing slightly further from the camera so she appears smaller/further away, standing upright, holding the man's hand, looking up at the camera with a soft subtle smile. Their hands are clasped together between them. The camera is positioned from directly above at a steep downward angle. " +
                    "CRITICAL INSTRUCTION FOR IDENTITY: Strictly map the exact facial features, skin tones, hairstyles, and identities of person1_image and person2_image. Do not blend their faces. " +
                    "CLOTHING: Both person1_image and person2_image MUST be wearing the exact clothes from their uploaded photos, adjusted to fit the standing pose. " +
                    "BACKGROUND: A dark grey asphalt street surface with bold wide white painted zebra crosswalk stripes running diagonally across the frame. The stripes are thick and evenly spaced. " +
                    "ACCESSORIES: The woman (person2_image) is wearing a thick silver chain-link necklace and silver hoop earrings. Do NOT add these accessories to person1_image. " +
                    "High-angle steep top-down street photography perspective, urban editorial aesthetic, cool natural daylight, highly detailed faces, 8k resolution, ultra-realistic."
        ),

        AiTemplate(
            id = 15,
            imageResId = R.drawable.template_15,
            title = "Retro Convertible",
            prompt = "A photorealistic, cinematic night photography photo of person1_image and person2_image sitting together inside a classic vintage red convertible sports car parked on a busy city street at night, both looking directly at the camera with calm serious expressions. " +
                    "POSE: The woman (person2_image) is in the passenger seat on the left, sitting upright and turning her body slightly toward the camera, resting one arm on the car door, looking directly into the camera with a cool serious expression. The man (person1_image) is in the driver's seat on the right, sitting upright with both hands on the classic white steering wheel, turning his face toward the camera with a calm intense expression. Both are at the same eye level inside the open-top convertible. " +
                    "CRITICAL INSTRUCTION FOR IDENTITY: Strictly map the exact facial features, skin tones, hairstyles, and identities of person1_image and person2_image. Do not blend their faces. " +
                    "CLOTHING: Both person1_image and person2_image MUST be wearing the exact clothes from their uploaded photos, adjusted to fit the seated car pose. " +
                    "BACKGROUND: A glamorous busy nighttime city street setting, resembling New York City or Times Square, with bright neon signs, glowing streetlights, bokeh city lights, and yellow taxi cabs visible and blurred in the background. Dark night sky above. " +
                    "CAR: A classic vintage 1960s red convertible sports car with red leather interior seats, a white steering wheel, chrome silver details, and the convertible top fully down. The car body is coral/tomato red and highly polished. " +
                    "ACCESSORIES: The woman (person2_image) is wearing large ornate silver drop earrings. The man (person1_image) is wearing a small silver cross pendant necklace. " +
                    "Cinematic night editorial photography, warm bokeh city lights, dramatic cool-toned lighting on faces, highly detailed faces, 8k resolution, ultra-realistic."
        ),

        AiTemplate(
            id = 16,
            imageResId = R.drawable.template_16,
            title = "Luxury Night Drive",
            prompt = "A photorealistic, cinematic close-up interior shot of person1_image and person2_image sitting inside a modern luxury black car at night, photographed from outside the car through the passenger side window. " +
                    "POSE: The woman (person2_image) is in the passenger seat on the left, sitting upright facing slightly forward/away from the camera, with a distant serious expression, not looking at the camera. The man (person1_image) is in the driver seat on the right, sitting upright with his right hand gripping the top of the dark leather steering wheel, turning his face toward the woman/slightly toward camera with an intense focused expression. Both are seated with seatbelts on, visible black leather headrests behind them. " +
                    "CRITICAL INSTRUCTION FOR IDENTITY: Strictly map the exact facial features, skin tones, hairstyles, and identities of person1_image and person2_image. Do not blend their faces. " +
                    "CLOTHING: Both person1_image and person2_image MUST be wearing the exact clothes from their uploaded photos, adjusted to fit the seated car interior pose. " +
                    "CAR INTERIOR: A sleek modern luxury car interior with black leather seats and headrests, dark dashboard, black leather steering wheel, dark roof lining, and black door panels. The car windows and frame are visible around the subjects. " +
                    "BACKGROUND: A dark moody nighttime city street visible through the car windows, with soft blurred ambient street lights and building lights glowing in the darkness outside. " +
                    "ACCESSORIES: The woman (person2_image) is wearing large ornate silver floral cluster earrings. The man (person1_image) is wearing a small silver diamond-shaped stud earring and a silver cross pendant necklace. " +
                    "Shot from outside through passenger window, cinematic moody night lighting, cool dark tones with soft warm face lighting, highly detailed faces, 8k resolution, ultra-realistic."
        ),

        AiTemplate(
            id = 17,
            imageResId = R.drawable.template_17,
            title = "Hotel Hallway",
            prompt = "A photorealistic, cinematic photo of person1_image and person2_image walking together side by side down a grand luxury hotel hallway, both facing the camera and smiling warmly. " +
                    "POSE: The woman (person2_image) is on the left, walking toward the camera, arms relaxed at her sides, smiling broadly and confidently at the camera. The man (person1_image) is on the right, walking beside her slightly behind, arms relaxed at his sides, smiling broadly at the camera. Both are mid-stride walking toward the camera in a natural relaxed gait, bodies slightly angled toward each other. " +
                    "CRITICAL INSTRUCTION FOR IDENTITY: Strictly map the exact facial features, skin tones, hairstyles, and identities of person1_image and person2_image. Do not blend their faces. " +
                    "CLOTHING: Both person1_image and person2_image MUST be wearing the exact clothes from their uploaded photos, adjusted to fit the walking pose. " +
                    "BACKGROUND: A long narrow luxury hotel corridor/hallway stretching far into the background, with dark wood paneled walls, multiple closed dark wooden doors on both sides, warm glowing wall sconce lights mounted on the walls, and a single ceiling flush mount light fixture glowing warmly above. The hallway floor is visible and the corridor creates a strong vanishing point perspective deep into the background. Dark moody elegant atmosphere. " +
                    "Cinematic warm golden hallway lighting, shallow depth of field with blurred corridor background, film grain texture, highly detailed faces, 8k resolution, ultra-realistic."
        ),

        AiTemplate(
            id = 18,
            imageResId = R.drawable.template_18,
            title = "Airport Selfie",
            prompt = "A photorealistic, close-up selfie-style photo of person1_image and person2_image together inside a bright modern international airport. " +
                    "POSE — CRITICAL: Generate completely NEW body poses. DO NOT use the original body poses from the uploaded photos. " +
                    "The man (person1_image) is on the left, extending one arm forward holding the camera to take the selfie, his face close to the camera, looking directly into the lens with a calm serious expression. His other hand is holding a clear plastic iced coffee cup with a straw and two airline boarding passes tucked beside the cup. " +
                    "The woman (person2_image) is on the right, leaning her head sideways against the man's head, her face close to his, looking directly into the camera with a relaxed subtle expression. She is holding a clear plastic iced coffee cup with dark iced coffee and a clear straw in one hand at chest level. " +
                    "Both faces are close together filling the upper frame in a tight selfie crop. " +
                    "CRITICAL INSTRUCTION FOR IDENTITY: Strictly map the exact facial features, skin tones, hairstyles, and identities of person1_image and person2_image onto these NEW poses. Do not blend their faces. " +
                    "CLOTHING: Both person1_image and person2_image MUST be wearing the exact clothes from their uploaded photos. Do NOT change or replace their clothing. " +
                    "BACKGROUND: Bright modern airport interior, large floor-to-ceiling glass windows with bright natural daylight, clean white structural columns visible behind them, slightly blurred from selfie depth of field. " +
                    "PROPS: Both are holding clear plastic iced coffee cups with dark iced coffee and clear straws. The man additionally has two blue and white airline boarding passes visible tucked in the same hand as his coffee cup. " +
                    "Bright natural daylight, candid travel couple selfie aesthetic, shallow depth of field, highly detailed faces, 8k resolution, ultra-realistic."
        )
    )

    // ── Single Photo Templates (IDs 101–118) ─────────────────────────────────

    private val singleTemplates: List<AiTemplate> = listOf(

        AiTemplate(
            id = 101,
            imageResId = R.drawable.single_template_1,
            title = "Cyberpunk Neon",
            prompt = "A photorealistic, full-body studio portrait of person_image standing against a clean light grey background. " +
                    "POSE: person_image is standing upright, facing directly at the camera with a warm, confident smile. Both arms are tucked behind the back. The legs are relaxed, with one leg slightly crossed in front of the other at the ankles. " +
                    "CRITICAL INSTRUCTION FOR IDENTITY AND CLOTHING: Strictly map the exact facial features, skin tone, hairstyle, and identity of person_image. person_image MUST be wearing the EXACT SAME clothes, top, and bottom from their uploaded original photo, adjusted naturally to fit this standing pose. Do NOT generate the green shirt or white pants from the reference image. " +
                    "BACKGROUND: A simple, seamless minimalist light grey studio backdrop. Soft, even, professional studio lighting with no harsh shadows. " +
                    "Full-body shot, clean modern editorial aesthetic, highly detailed face, 8k resolution, ultra-realistic."
        ),

        AiTemplate(
            id = 102,
            imageResId = R.drawable.single_template_2,
            title = "Business Suit",
            prompt = "A photorealistic, full-body studio portrait of person_image standing against a solid warm beige background. " +
                    "POSE: person_image is standing with their body angled slightly sideways, but turning their head to face the camera directly with a bright, natural smile. Both arms are crossed casually over the chest. One leg is straight and bearing weight, while the other leg is bent at the knee and playfully crossed behind the straight leg, resting lightly on the toe. " +
                    "CRITICAL INSTRUCTION FOR IDENTITY AND CLOTHING: Strictly map the exact facial features, skin tone, hairstyle, and identity of person_image. person_image MUST be wearing the EXACT SAME clothes, top, bottom, and shoes from their uploaded original photo, adjusted naturally to fit this standing, arms-crossed pose. Do NOT generate the pink sweater, blue jeans, or white sneakers from the reference image. " +
                    "BACKGROUND: A simple, seamless warm beige/peach studio backdrop. Soft, bright, even studio lighting. " +
                    "Full-body shot, casual fashion catalog aesthetic, highly detailed face, 8k resolution, ultra-realistic."
        ),

        AiTemplate(
            id = 103,
            imageResId = R.drawable.single_template_3,
            title = "Fantasy Warrior",
            prompt = "A photorealistic, medium waist-up portrait of person_image standing indoors in a bright, cozy living space. " +
                    "POSE: person_image is standing comfortably and relaxed, facing the camera directly with a very soft, gentle, and warm smile. The posture is natural, with shoulders slightly dropped and arms resting down out of frame. " +
                    "CRITICAL INSTRUCTION FOR IDENTITY AND CLOTHING: Strictly map the exact facial features, skin tone, hairstyle, and identity of person_image. person_image MUST be wearing the EXACT SAME clothes, shirt, and outfit from their uploaded original photo, naturally fitted to this relaxed posture. Do NOT generate the oversized light beige knit sweater or dark grey jeans from the reference image. " +
                    "BACKGROUND: A softly blurred (bokeh) modern, warm home interior. Bright, soft natural daylight is streaming in from a window on the right side, casting a beautiful flattering light on the subject's face. The defocused background features a white wall with a wooden picture frame, a hint of green indoor house plants, and warm wooden furniture tones. " +
                    "Lifestyle photography aesthetic, shallow depth of field, warm cozy color palette, highly detailed face, 8k resolution, ultra-realistic."
        ),

        AiTemplate(
            id = 104,
            imageResId = R.drawable.single_template_4,
            title = "Vintage Film",
            prompt = "A photorealistic, full-body studio portrait of person_image standing against a solid, vibrant yellow background. " +
                    "POSE: person_image is standing with their body angled slightly to the side, but turning their head to face the camera directly with a confident, cheerful smile. Both arms are crossed comfortably over the chest. One leg is straight and bearing weight, while the other leg is bent at the knee and playfully crossed in front of the straight leg, resting lightly on the toe. " +
                    "CRITICAL INSTRUCTION FOR IDENTITY AND CLOTHING: Strictly map the exact facial features, skin tone, hairstyle, and identity of person_image. person_image MUST be wearing the EXACT SAME clothes, top, bottom, and shoes from their uploaded original photo, adjusted naturally to fit this standing, arms-crossed pose. Do NOT generate the black-and-white vertical striped shirt, light blue ripped distressed jeans, or white canvas sneakers from the reference image. " +
                    "BACKGROUND: A simple, seamless bright vibrant yellow studio backdrop. High-key, bright, and even studio lighting with soft natural shadows on the floor. " +
                    "Full-body shot, fun and cheerful fashion catalog aesthetic, highly detailed face, 8k resolution, ultra-realistic."
        ),

        AiTemplate(
            id = 105,
            imageResId = R.drawable.single_template_5,
            title = "Snowy Mountain",
            prompt = "A photorealistic, full-body portrait of person_image sitting comfortably on a black metal patio chair. " +
                    "POSE: person_image is seated, facing the camera directly with a warm, natural, and relaxed smile. Their legs are crossed, with one leg resting over the knee of the other. One arm rests horizontally across the lap, while the other arm is bent with the elbow resting on the horizontal arm, and the hand lightly touching the chin/jawline in a gentle, elegant pose. " +
                    "CRITICAL INSTRUCTION FOR IDENTITY AND CLOTHING: Strictly map the exact facial features, skin tone, hairstyle, and identity of person_image. person_image MUST be wearing the EXACT SAME clothes, top, bottom, and shoes from their uploaded original photo, naturally fitted to this seated, legs-crossed posture. Do NOT generate the rust orange mock-neck top, light blue cuffed denim jeans, or black leather ankle boots from the reference image. " +
                    "BACKGROUND: An outdoor balcony setting with a thin black metal railing directly behind the subject. The background is a beautifully blurred (bokeh) view of lush, vibrant green trees and foliage. Soft, natural, diffused outdoor daylight. " +
                    "Professional portrait photography aesthetic, shallow depth of field, highly detailed face, 8k resolution, ultra-realistic."
        ),

        AiTemplate(
            id = 106,
            imageResId = R.drawable.single_template_6,
            title = "Purple Playful",
            prompt = "A photorealistic, full-body studio portrait of person_image standing playfully against a solid muted purple background. " +
                    "EXACT POSE INSTRUCTION: person_image is balancing playfully on one leg. The right leg is straight and firmly planted on the ground. The left leg is lifted and bent outward at the knee, with the left foot tucked behind the right knee. The upper body leans slightly forward with the shoulders playfully shrugged up. Both arms are hanging straight down in front of the body, with the hands loosely clasped together resting just above the knees. " +
                    "HEAD AND FACE INSTRUCTION: The head is facing straight forward, looking directly into the camera lens with direct eye contact, making a big, joyful, laughing smile. The hair is slightly windswept. " +
                    "CRITICAL INSTRUCTION FOR IDENTITY AND CLOTHING: Strictly map the exact facial features, skin tone, hairstyle, and identity of person_image. person_image MUST be wearing the EXACT SAME clothes, top, bottom, and shoes from their uploaded original photo, fitted naturally to this dynamic one-legged balancing pose. " +
                    "STRICT NEGATIVE CONSTRAINTS: Do NOT generate glasses, sunglasses, or eyewear of any kind; the eyes must be perfectly clear and fully visible. Do NOT generate the coral/peach sweatshirt, baggy wide-leg blue jeans, or red high-top canvas sneakers from the reference image. " +
                    "BACKGROUND: A simple, seamless solid muted purple (lilac/lavender) studio backdrop. Soft, even, vibrant studio lighting with a subtle drop shadow on the floor. " +
                    "Full-body shot, fun and energetic fashion editorial aesthetic, highly detailed face, 8k resolution, ultra-realistic."
        ),

        AiTemplate(
            id = 107,
            imageResId = R.drawable.single_template_7,
            title = "Stone Wall Sit",
            prompt = "A photorealistic portrait of person_image sitting on a low outdoor stone wall. " +
                    "EXACT POSE: person_image is seated on a low stone wall, body leaning slightly backward with the left hand placed flat on the wall behind for support. The right hand rests loosely on the right thigh. The head is tilted upward and to the right, eyes gazing softly upward away from the camera. Expression is a calm, gentle closed-mouth smile. Shoulders are relaxed with a natural casual backward lean. " +
                    "CRITICAL INSTRUCTION FOR IDENTITY AND CLOTHING: Strictly map the exact facial features, skin tone, hairstyle, and identity of person_image. person_image MUST wear the EXACT SAME clothes from their uploaded original photo, fitted naturally to this seated pose. " +
                    "STRICT NEGATIVE CONSTRAINTS: Do NOT replicate any clothing, glasses, or accessories from the reference template image. " +
                    "BACKGROUND: Softly blurred outdoor bokeh, green trees, paved pathway, warm golden-hour side lighting with soft rim light on hair. " +
                    "Three-quarter body shot, natural editorial aesthetic, highly detailed face, 8k resolution, ultra-realistic."
        ),

        AiTemplate(
            id = 108,
            imageResId = R.drawable.single_template_8,
            title = "Stone Sit",
            prompt = "A photorealistic portrait of person_image sitting casually on a stone surface outdoors. " +
                    "EXACT POSE: person_image is seated on a rock or stone surface, body leaning slightly forward. The right elbow is resting on the right knee with the right hand gently cupping the right cheek. The left arm is relaxed and resting down. Head is tilted slightly to the right, eyes looking directly into the camera with a soft confident closed-mouth smile. " +
                    "CRITICAL INSTRUCTION FOR IDENTITY AND CLOTHING: Strictly map the exact facial features, skin tone, hairstyle, and identity of person_image. person_image MUST wear the EXACT SAME clothes from their uploaded original photo, fitted naturally to this seated pose. " +
                    "STRICT NEGATIVE CONSTRAINTS: Do NOT replicate the red plaid flannel shirt, colorful wrist bracelets, or any clothing from the reference template image. " +
                    "BACKGROUND: Blurred dark outdoor stone wall and archway background, moody natural lighting with soft bokeh. " +
                    "Upper-body three-quarter shot, moody editorial aesthetic, highly detailed face, 8k resolution, ultra-realistic."
        ),

        AiTemplate(
            id = 109,
            imageResId = R.drawable.single_template_9,
            title = "Park Dreamy",
            prompt = "A photorealistic full-body portrait of person_image standing outdoors on green grass in a tree-lined park. " +
                    "EXACT POSE: person_image is standing upright, body facing slightly left, both hands loosely clasped together at waist level. Head is turned upward and to the right, gazing softly away from the camera with a calm dreamy expression and neutral slightly parted lips. Weight evenly distributed on both feet, posture relaxed and still. " +
                    "CRITICAL INSTRUCTION FOR IDENTITY AND CLOTHING: Strictly map the exact facial features, skin tone, hairstyle, and identity of person_image. person_image MUST wear the EXACT SAME clothes from their uploaded original photo, fitted naturally to this standing pose. " +
                    "STRICT NEGATIVE CONSTRAINTS: Do NOT replicate the royal blue long maxi dress, black choker necklace, or any clothing from the reference template image. " +
                    "BACKGROUND: Tree-lined park with lush green grass, tall trees with leafy canopy on both sides, warm golden-hour soft natural lighting filtering through trees. " +
                    "Full-body shot, dreamy natural outdoor editorial aesthetic, highly detailed face, 8k resolution, ultra-realistic."
        ),

        AiTemplate(
            id = 110,
            imageResId = R.drawable.single_template_10,
            title = "Urban Pole Lean",
            prompt = "A photorealistic three-quarter body portrait of person_image standing outdoors leaning against a white pole or pillar. " +
                    "EXACT POSE: person_image is standing with back and right shoulder leaning casually against a white vertical pole on their right side. Both hands are tucked into front trouser pockets. Body leans slightly toward the pole, weight resting against it. Head is turned to the left, eyes gazing far left away from the camera with a calm, serious, thoughtful expression and neutral closed lips. " +
                    "CRITICAL INSTRUCTION FOR IDENTITY AND CLOTHING: Strictly map the exact facial features, skin tone, hairstyle, and identity of person_image. person_image MUST wear the EXACT SAME clothes from their uploaded original photo, fitted naturally to this standing pose. " +
                    "STRICT NEGATIVE CONSTRAINTS: Do NOT replicate the black crew-neck sweatshirt, dark trousers, or any clothing from the reference template image. " +
                    "BACKGROUND: Blurred outdoor background with a palm tree, white wall/stairs, and bright daylight. Harsh natural sunlight creating strong shadows. " +
                    "Three-quarter body shot, urban editorial aesthetic, highly detailed face, 8k resolution, ultra-realistic."
        ),

        AiTemplate(
            id = 111,
            imageResId = R.drawable.single_template_11,
            title = "Dark Wall Sit",
            prompt = "A photorealistic full-body portrait of person_image sitting on the ground against a dark wall outdoors. " +
                    "EXACT POSE: person_image is sitting on the ground with back leaning against a dark paneled wall. The left knee is bent with left foot flat on the ground close to the body. The right leg is extended forward and outward toward the camera with the right foot raised and the sole of the right shoe directly facing the camera. The right hand is raised up, fingers gripping through the hair on top of the head. The left arm rests across the left knee with hand hanging loosely. Head faces slightly downward, eyes looking directly into the camera with a cool serious expression. " +
                    "CRITICAL INSTRUCTION FOR IDENTITY AND CLOTHING: Strictly map the exact facial features, skin tone, hairstyle, and identity of person_image. person_image MUST wear the EXACT SAME clothes from their uploaded original photo, fitted naturally to this seated ground pose. " +
                    "STRICT NEGATIVE CONSTRAINTS: Do NOT replicate the navy blue jacket, white raglan t-shirt, dark jeans, white sneakers, aviator sunglasses, or any clothing from the reference template image. " +
                    "BACKGROUND: Dark charcoal/black large panel wall directly behind, brick/paved ground floor. Moody urban lighting, slightly low camera angle. " +
                    "Full-body ground-level shot, dark urban editorial aesthetic, highly detailed face, 8k resolution, ultra-realistic."
        ),

        AiTemplate(
            id = 112,
            imageResId = R.drawable.single_template_12,
            title = "Street Suit",
            prompt = "A photorealistic upper-body portrait of person_image standing outdoors on a European city street. " +
                    "EXACT POSE: person_image is standing with body turned slightly to the right but face turned back toward the camera. The right hand is tucked into the right trouser pocket. The left hand is gripping and slightly pulling open the left lapel of the outer coat. Head is tilted very slightly downward, eyes looking directly into the camera with a cold, serious, confident expression and neutral closed lips. Slightly low camera angle looking upward. " +
                    "CRITICAL INSTRUCTION FOR IDENTITY AND CLOTHING: Strictly map the exact facial features, skin tone, hairstyle, and identity of person_image. person_image MUST wear the EXACT SAME clothes from their uploaded original photo, fitted naturally to this standing pose. " +
                    "STRICT NEGATIVE CONSTRAINTS: Do NOT replicate the navy blue overcoat, dark waistcoat, white dress shirt, black tie, dark sunglasses, or any clothing from the reference template image. " +
                    "BACKGROUND: A European urban street with a large terracotta/brown brick apartment building with balconies and windows directly behind. A silver-grey parked car on the left and a light blue-white parked car on the right. A glowing warm street lamp visible on the left. Cool muted overcast dusk lighting with no harsh shadows. " +
                    "Upper-body three-quarter shot, slightly low angle, dark cinematic European street editorial aesthetic, highly detailed face, 8k resolution, ultra-realistic."
        ),

        AiTemplate(
            id = 113,
            imageResId = R.drawable.single_template_13,
            title = "Staircase Sit",
            prompt = "A photorealistic full-body portrait of person_image sitting casually on outdoor metal/concrete stairs. " +
                    "EXACT POSE: person_image is seated on a mid-level stair step. The right foot is placed on a lower step with the right knee bent, right hand resting on right thigh. The left foot is on an even lower step with left leg more extended downward, left hand resting loosely on left thigh. Body leans slightly forward, facing slightly left. Head faces directly into the camera with a big confident smile. " +
                    "CRITICAL INSTRUCTION FOR IDENTITY AND CLOTHING: Strictly map the exact facial features, skin tone, hairstyle, and identity of person_image. person_image MUST wear the EXACT SAME clothes from their uploaded original photo, fitted naturally to this seated stair pose. " +
                    "STRICT NEGATIVE CONSTRAINTS: Do NOT replicate the navy blue button-up shirt, white chinos, brown sneakers, black aviator sunglasses, or any clothing from the reference template image. " +
                    "BACKGROUND: Outdoor industrial staircase with metal grid steps, a blue metal handrail on the left, a corrugated metal door and brick wall behind. Bright overcast natural daylight. " +
                    "Full-body shot, casual urban editorial aesthetic, highly detailed face, 8k resolution, ultra-realistic."
        ),

        AiTemplate(
            id = 114,
            imageResId = R.drawable.single_template_14,
            title = "Bollard Sit",
            prompt = "A photorealistic full-body portrait of person_image sitting on a concrete bollard/post outdoors. " +
                    "EXACT POSE: person_image is seated on a concrete bollard. The right leg is crossed over the left with the right ankle resting on the left knee, and the right hand holding the right ankle. The left foot is flat on the ground, left hand resting on left thigh. Body faces slightly right but the head is turned sharply to the left, eyes gazing far left away from the camera with a serious cool confident expression and neutral lips. " +
                    "CRITICAL INSTRUCTION FOR IDENTITY AND CLOTHING: Strictly map the exact facial features, skin tone, hairstyle, and identity of person_image. person_image MUST wear the EXACT SAME clothes from their uploaded original photo, fitted naturally to this seated pose. " +
                    "STRICT NEGATIVE CONSTRAINTS: Do NOT replicate the black and grey colorblock knit sweater, acid wash light blue jeans, white sneakers, yellow tinted glasses, or any clothing from the reference template image. " +
                    "BACKGROUND: Outdoor park with a shallow decorative pond/water feature behind, white metal chain fence on both sides of the bollard, tall green trees and warm golden-hour light in the background. " +
                    "Full-body shot, warm golden-hour outdoor editorial aesthetic, highly detailed face, 8k resolution, ultra-realistic."
        ),

        AiTemplate(
            id = 115,
            imageResId = R.drawable.single_template_15,
            title = "Railing Lean",
            prompt = "A photorealistic close-up upper-body portrait of person_image leaning forward over a white ornate surface or railing. " +
                    "EXACT POSE: person_image is leaning forward with the left forearm resting flat on a white ornate ledge/railing, elbow planted down. The right hand is raised up near the right cheek and temple with fingers loosely curled. Body leans forward and slightly left. Head is turned toward the camera with a calm intense serious expression and neutral closed lips. " +
                    "CAMERA ANGLE: Camera is positioned slightly above and to the right, shooting slightly downward at the subject who is leaning forward toward the surface. Tight close-up crop showing only head, shoulders, and upper chest. " +
                    "CRITICAL INSTRUCTION FOR IDENTITY AND CLOTHING: Strictly map the exact facial features, skin tone, hairstyle, and identity of person_image. person_image MUST wear the EXACT SAME clothes from their uploaded original photo, fitted naturally to this leaning pose. " +
                    "STRICT NEGATIVE CONSTRAINTS: Do NOT replicate the dark wash denim jacket with white sherpa collar, or any clothing from the reference template image. " +
                    "BACKGROUND: Soft blurred indoor setting with white ornate architectural elements, cool muted blue-grey ambient lighting. " +
                    "Close-up portrait, slightly overhead angle shooting down, moody cool-toned indoor editorial aesthetic, highly detailed face, 8k resolution, ultra-realistic."
        ),

        AiTemplate(
            id = 116,
            imageResId = R.drawable.single_template_16,
            title = "Tunnel Crouch",
            prompt = "A photorealistic full-body portrait of person_image in a low streetwear crouch on an indoor tiled floor. " +
                    "EXACT POSE: person_image is in a low asymmetric crouch. The left foot is flat on the ground with left knee bent forward. The right knee is lowered close to the ground with the right foot on toes with heel raised. Body weight shifts to the right side. Both arms rest forward on the knees with hands loosely clasped together. Body leans slightly forward. Head faces directly into the camera with a calm cool intense expression and neutral lips. This is NOT a squat — it is a low one-knee-down streetwear crouch. " +
                    "CAMERA ANGLE: Camera positioned at ground level shooting upward with a wide-angle fisheye perspective. Subject centered in frame. " +
                    "CRITICAL INSTRUCTION FOR IDENTITY AND CLOTHING: Strictly map the exact facial features, skin tone, hairstyle, and identity of person_image. person_image MUST wear the EXACT SAME clothes from their uploaded original photo, fitted naturally to this crouch pose. " +
                    "STRICT NEGATIVE CONSTRAINTS: Do NOT replicate the light pink hoodie, dark green joggers, black sneakers, black snapback cap, or any clothing from the reference template image. " +
                    "BACKGROUND: Long futuristic indoor tunnel corridor with repeating white geometric arch frames glowing with bright white light receding into the distance, warm orange glow at the far end, dark tiled floor. Cool blue-white ambient lighting. " +
                    "Full-body ground-level wide-angle shot, futuristic urban editorial aesthetic, highly detailed face, 8k resolution, ultra-realistic."
        ),

        AiTemplate(
            id = 117,
            imageResId = R.drawable.single_template_17,
            title = "Barrel Sit",
            prompt = "A photorealistic three-quarter body portrait of person_image perched on top of a dark metal barrel indoors. " +
                    "EXACT POSE: person_image is perched high on top of a dark metal barrel with knees spread wide apart and both feet dangling off the ground not touching the floor. The right hand grips the right edge of the barrel with arm straight down. The left hand rests flat on the left thigh. Body is fully upright with chest open and shoulders relaxed slightly back. Head faces straight into the camera with a soft calm slight smile. This is NOT a normal chair sit — the person is perched high on a barrel with legs wide apart and feet hanging. " +
                    "CAMERA ANGLE: Camera at eye level, straight-on, no tilt. " +
                    "CRITICAL INSTRUCTION FOR IDENTITY AND CLOTHING: Strictly map the exact facial features, skin tone, hairstyle, and identity of person_image. person_image MUST wear the EXACT SAME clothes from their uploaded original photo, fitted naturally to this seated pose. " +
                    "STRICT NEGATIVE CONSTRAINTS: Do NOT replicate the beige/tan t-shirt, mustard yellow corduroy wide-leg pants, or any clothing from the reference template image. " +
                    "BACKGROUND: Colorful vibrant graffiti mural wall directly behind with orange, grey, red, white street art characters and graffiti lettering. Warm dramatic sunlight casting diagonal shadow across the subject from the left. " +
                    "Three-quarter body shot, eye-level straight-on, urban street art editorial aesthetic, highly detailed face, 8k resolution, ultra-realistic."
        ),

        AiTemplate(
            id = 118,
            imageResId = R.drawable.single_template_18,
            title = "Wall Lean",
            prompt = "A photorealistic full-body portrait of person_image leaning sideways against the edge of a tall white wall outdoors. " +
                    "EXACT POSE: person_image is leaning with the left shoulder and left side of the body against the left edge/corner of a white wall. The body is turned at roughly 45 degrees facing toward the right open space, NOT facing the camera directly. The left leg is straight. The right leg is crossed loosely in front of the left with the right foot heel on the ground. The left hand hangs relaxed at the side. The right hand holds sunglasses loosely at waist level. Head is turned to the right gazing into the open space away from the camera with a serious thoughtful expression. " +
                    "CAMERA ANGLE: Camera positioned slightly to the right of the subject capturing the full body from a slight side angle. Eye level, full body shot. " +
                    "CRITICAL INSTRUCTION FOR IDENTITY AND CLOTHING: Strictly map the exact facial features, skin tone, hairstyle, and identity of person_image. person_image MUST wear the EXACT SAME clothes from their uploaded original photo, fitted naturally to this pose. " +
                    "STRICT NEGATIVE CONSTRAINTS: Do NOT replicate the olive/khaki sherpa jacket, grey button-up shirt, cream/white trousers, white sneakers, or any clothing from the reference template image. " +
                    "BACKGROUND: European city waterfront promenade, large classical stone government building with columns on the right, river/canal behind, overcast cloudy sky, large stone paved walkway. Cool muted desaturated daylight. " +
                    "Full-body slight side angle shot, cool muted European street editorial aesthetic, highly detailed face, 8k resolution, ultra-realistic."
        )
    )
}