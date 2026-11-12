package com.example.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.Log
import com.example.model.AiLandmarkRiddle
import com.example.model.CharacterStyle
import com.example.model.ChatMessage
import com.example.model.LandmarkFactDossier
import com.example.model.PolishedFieldLogResult
import com.example.model.RealLandmark
import com.example.model.VisionScanResult
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "DungeonMasterAI"

data class GeneratedQuestNarrative(
    val questTitle: String,
    val questIntro: String,
    val enrichedLandmarks: List<RealLandmark>
)

class DungeonMasterAiRepository {

    private val questJsonSchema = Schema.obj(
        properties = mapOf(
            "questTitle" to Schema.string(
                description = "Evocative RPG quest title matching the character style and real neighborhood"
            ),
            "questIntro" to Schema.string(
                description = "2-3 sentence atmospheric briefing from the Dungeon Master setting up the walking quest"
            ),
            "stops" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "landmarkName" to Schema.string(description = "Exact name of the real landmark"),
                        "rpgNodeTitle" to Schema.string(description = "In-character RPG waypoint title"),
                        "loreSnippet" to Schema.string(
                            description = "2-3 sentences blending real history/geography of this landmark with the RPG persona"
                        ),
                        "questChallenge" to Schema.string(
                            description = "A short real-world observation or roleplay action for the walker at this stop"
                        ),
                        "rewardItemName" to Schema.string(
                            description = "Name of the RPG relic, chronicle, or elixir awarded at this landmark"
                        )
                    )
                ),
                description = "Enriched RPG stops corresponding to the real-world landmarks"
            )
        )
    )

    private val visionScanSchema = Schema.obj(
        properties = mapOf(
            "landmarkTitle" to Schema.string(
                description = "Title of the scanned landmark, plaque, statue, or architectural feature"
            ),
            "inCharacterAnalysis" to Schema.string(
                description = "2-3 vivid sentences from the Dungeon Master analyzing the visual details or plaque text in-character"
            ),
            "historicalVerification" to Schema.string(
                description = "1-2 sentences of grounded real-world historical or architectural context verified from the scan"
            ),
            "awardedRelicName" to Schema.string(
                description = "Unique RPG relic or chronicle name minted from this visual scan"
            )
        )
    )

    private val factDossierSchema = Schema.obj(
        properties = mapOf(
            "verifiedEra" to Schema.string(
                description = "Historical era, founding year, or architectural period of the landmark"
            ),
            "architecturalStyle" to Schema.string(
                description = "Architectural, botanical, or cultural classification of the place"
            ),
            "verifiedHistoricalFacts" to Schema.array(
                items = Schema.string(description = "A concise, verified real-world historical or geographical fact"),
                description = "3 grounded real-world facts about the landmark or district"
            ),
            "localInsiderTip" to Schema.string(
                description = "Practical walker observation tip (what detail to spot on-site)"
            ),
            "rpgEncounterHook" to Schema.string(
                description = "In-character RPG hook connecting the real facts to the active persona"
            )
        )
    )

    private val riddleSchema = Schema.obj(
        properties = mapOf(
            "question" to Schema.string(
                description = "An engaging in-character riddle or trivia question grounded in the landmark's real history or architecture"
            ),
            "options" to Schema.array(
                items = Schema.string(description = "A plausible answer choice"),
                description = "Exactly 3 answer choices"
            ),
            "correctIndex" to Schema.integer(
                description = "0-based index (0, 1, or 2) of the correct option"
            ),
            "hint" to Schema.string(
                description = "A short clue pointing to the landmark's real history"
            ),
            "explanationLore" to Schema.string(
                description = "1-2 sentences explaining why the answer is historically true in character"
            )
        )
    )

    private val fieldLogSchema = Schema.obj(
        properties = mapOf(
            "curioTitle" to Schema.string(
                description = "Evocative RPG Artifact or Field Chronicle title minted from the spoken observation"
            ),
            "polishedChronicle" to Schema.string(
                description = "2-3 sentence eloquent in-character RPG field log entry expanding the walker's spoken note"
            ),
            "historicalConnection" to Schema.string(
                description = "1 sentence connecting the observation to the real landmark and neighborhood"
            )
        )
    )

    private val structuredQuestModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = questJsonSchema
            }
        )
    }

    private val visionScanModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = visionScanSchema
            }
        )
    }

    private val factDossierModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = factDossierSchema
            }
        )
    }

    private val riddleModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = riddleSchema
            }
        )
    }

    private val fieldLogModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = fieldLogSchema
            }
        )
    }

    private val chatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash"
        )
    }

    private val imageGenModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
            }
        )
    }

    suspend fun generateWalkingQuestWithGemini(
        style: CharacterStyle,
        regionName: String,
        landmarks: List<RealLandmark>
    ): Result<GeneratedQuestNarrative> {
        return try {
            val landmarksBlock = landmarks.mapIndexed { idx, lm ->
                "${idx + 1}. ${lm.name} [${lm.category.label}] (Lat ${lm.latitude}, Lng ${lm.longitude}) — Fact: ${lm.realFactOrAddress}"
            }.joinToString("\n")

            val prompt = """
                ${style.systemPersonaPrompt}
                
                Generate an interactive walking RPG quest in "$regionName" using these real-world landmarks:
                $landmarksBlock
                
                Requirements:
                - Keep each stop grounded in the real landmark's identity, architecture, or history while narrating in the voice of ${style.narratorTitle} (${style.title}).
                - Provide exactly ${landmarks.size} stops in the same order.
                - Make the lore vivid and ready to be read aloud via Text-To-Speech on a walk.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                structuredQuestModel.generateContent(prompt)
            }

            val jsonString = response.text
                ?: return Result.failure(IllegalStateException("Gemini returned an empty quest response."))

            val root = JSONObject(jsonString)
            val questTitle = root.optString("questTitle").takeIf { it.isNotBlank() }
                ?: "${style.title}: The $regionName Chronicle"
            val questIntro = root.optString("questIntro").takeIf { it.isNotBlank() }
                ?: "Greetings, wanderer. Your sensors have locked onto ${landmarks.size} real-world waypoints across $regionName."

            val stopsArray = root.optJSONArray("stops")
            val enriched = landmarks.mapIndexed { index, original ->
                val stopObj = if (stopsArray != null && index < stopsArray.length()) {
                    stopsArray.optJSONObject(index)
                } else {
                    null
                }
                if (stopObj == null) {
                    original
                } else {
                    original.copy(
                        rpgNodeTitle = stopObj.optString("rpgNodeTitle").takeIf { it.isNotBlank() }
                            ?: original.rpgNodeTitle,
                        loreSnippet = stopObj.optString("loreSnippet").takeIf { it.isNotBlank() }
                            ?: original.loreSnippet,
                        questChallenge = stopObj.optString("questChallenge").takeIf { it.isNotBlank() }
                            ?: original.questChallenge,
                        rewardItemName = stopObj.optString("rewardItemName").takeIf { it.isNotBlank() }
                            ?: original.rewardItemName
                    )
                }
            }

            Result.success(
                GeneratedQuestNarrative(
                    questTitle = questTitle,
                    questIntro = questIntro,
                    enrichedLandmarks = enriched
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Quest generation timed out", e)
            Result.failure(Exception("Quest generation timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate quest with Gemini", e)
            Result.failure(e)
        }
    }

    suspend fun analyzeLandmarkPhotoWithGemini(
        bitmap: Bitmap,
        style: CharacterStyle,
        landmark: RealLandmark?,
        regionName: String
    ): Result<VisionScanResult> {
        return try {
            val scaledBitmap = scaleBitmapForVision(bitmap, maxDimension = 1024)
            val landmarkInfo = if (landmark != null) {
                "Target Waypoint: ${landmark.name} (${landmark.category.label}) in $regionName. Known fact: ${landmark.realFactOrAddress}"
            } else {
                "Exploring $regionName on foot."
            }

            val prompt = """
                ${style.systemPersonaPrompt}
                
                The explorer has just used their optical scanner / camera to capture this image during a SideQuest walk.
                $landmarkInfo
                
                Tasks:
                1. Inspect the image (whether it is a commemorative plaque, monument, park scene, cafe, or architectural detail).
                2. Narrate an exciting 2-3 sentence in-character analysis as ${style.narratorTitle} (${style.title}).
                3. Provide a 1-2 sentence historical/architectural verification connecting what is visible to the real-world landmark.
                4. Mint a unique, evocative RPG Relic name inspired by the scan.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                visionScanModel.generateContent(
                    content {
                        image(scaledBitmap)
                        text(prompt)
                    }
                )
            }

            val jsonString = response.text
                ?: return Result.failure(IllegalStateException("Gemini Vision returned an empty scan result."))
            val root = JSONObject(jsonString)

            val title = root.optString("landmarkTitle").takeIf { it.isNotBlank() }
                ?: (landmark?.name ?: "Verified Waypoint Scan")
            val analysis = root.optString("inCharacterAnalysis").takeIf { it.isNotBlank() }
                ?: "Optical scan complete. Energy signatures around ${landmark?.name ?: regionName} confirmed."
            val verification = root.optString("historicalVerification").takeIf { it.isNotBlank() }
                ?: (landmark?.realFactOrAddress ?: "Verified on-site architectural marker.")
            val relicName = root.optString("awardedRelicName").takeIf { it.isNotBlank() }
                ?: "Codex Lens of ${landmark?.name?.take(20) ?: "Discovery"}"

            Result.success(
                VisionScanResult(
                    landmarkTitle = title,
                    inCharacterAnalysis = analysis,
                    historicalVerification = verification,
                    awardedRelicName = relicName,
                    bonusXp = 175
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Vision scan timed out", e)
            Result.failure(Exception("Relic Vision Scan timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Vision scan failed", e)
            Result.failure(e)
        }
    }

    fun streamDungeonMasterReply(
        style: CharacterStyle,
        regionName: String,
        activeLandmark: RealLandmark?,
        recentMessages: List<ChatMessage>,
        userMessage: String
    ): Flow<String> = flow {
        val historyContext = recentMessages.takeLast(6).joinToString("\n") { msg ->
            val speaker = if (msg.isUser) "Explorer" else style.narratorTitle
            "$speaker: ${msg.text}"
        }

        val landmarkContext = if (activeLandmark != null) {
            """
            Current Real-World Waypoint: ${activeLandmark.name} (${activeLandmark.category.label})
            Coordinates: ${activeLandmark.latitude}, ${activeLandmark.longitude}
            Real Fact / Context: ${activeLandmark.realFactOrAddress}
            RPG Node Title: ${activeLandmark.rpgNodeTitle}
            Current Challenge: ${activeLandmark.questChallenge}
            """.trimIndent()
        } else {
            "Exploring $regionName on foot."
        }

        val fullPrompt = """
            ${style.systemPersonaPrompt}
            
            You are acting as the live audio RPG Dungeon Master & real-world tour guide for a walker in $regionName.
            $landmarkContext
            
            Recent Dialogue:
            $historyContext
            
            Explorer's Action or Question: "$userMessage"
            
            Respond in character as ${style.narratorTitle} in 2 to 4 vivid, spoken-friendly sentences (around 50-90 words).
            Weave real historical, architectural, or cultural facts about ${activeLandmark?.name ?: regionName} into your RPG response, and end with an engaging hook or observation prompt for their walk!
        """.trimIndent()

        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            chatModel.generateContentStream(content { text(fullPrompt) }).collect { chunk ->
                val part = chunk.text.orEmpty()
                if (part.isNotEmpty()) {
                    accumulated.append(part)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("Dungeon Master returned an empty response.")
        }
    }

    /**
     * Runtime Text-to-Image Generation via Firebase AI Logic (`gemini-nano-banana-2.1`).
     */
    suspend fun generateRelicArtworkWithGemini(promptText: String): Bitmap? {
        val response = imageGenModel.generateContent(promptText)
        val imagePart = response.candidates.firstOrNull()
            ?.content?.parts?.filterIsInstance<ImagePart>()
            ?.firstOrNull()
        return imagePart?.image
    }

    /**
     * Runtime Image-to-Image Editing via Firebase AI Logic (`gemini-nano-banana-2.1`).
     */
    suspend fun editRelicArtworkWithGemini(
        inputBitmap: Bitmap,
        editInstruction: String
    ): Bitmap? {
        val scaledInput = scaleBitmapForVision(inputBitmap, maxDimension = 1024)
        val response = imageGenModel.generateContent(
            content {
                image(scaledInput)
                text(editInstruction)
            }
        )
        val imagePart = response.candidates.firstOrNull()
            ?.content?.parts?.filterIsInstance<ImagePart>()
            ?.firstOrNull()
        return imagePart?.image
    }

    /**
     * Generates a Grounded Real-World Historical & Architectural Fact-Check Dossier (`gemini-3.8-flash`).
     */
    suspend fun generateLandmarkFactDossier(
        style: CharacterStyle,
        regionName: String,
        landmark: RealLandmark
    ): Result<LandmarkFactDossier> {
        return try {
            val prompt = """
                ${style.systemPersonaPrompt}
                
                Synthesize a verified Real-World Historical & Architectural Fact-Check Dossier for:
                Landmark: ${landmark.name} (${landmark.category.label})
                Region: $regionName
                Coordinates: ${landmark.latitude}, ${landmark.longitude}
                Known Grounding Context: ${landmark.realFactOrAddress}
                
                Provide accurate historical/geographical facts, era, architectural style, an on-site walker observation tip, and an RPG hook in the voice of ${style.narratorTitle}.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                factDossierModel.generateContent(prompt)
            }
            val jsonString = response.text
                ?: return Result.failure(IllegalStateException("Empty fact dossier response."))
            val root = JSONObject(jsonString)

            val factsArray = root.optJSONArray("verifiedHistoricalFacts")
            val facts = mutableListOf<String>()
            if (factsArray != null) {
                for (i in 0 until factsArray.length()) {
                    val f = factsArray.optString(i).trim()
                    if (f.isNotBlank()) facts.add(f)
                }
            }
            if (facts.isEmpty()) {
                facts.add(landmark.realFactOrAddress)
            }

            Result.success(
                LandmarkFactDossier(
                    landmarkName = landmark.name,
                    verifiedEra = root.optString("verifiedEra").ifBlank { "Historic Civic Era" },
                    architecturalStyle = root.optString("architecturalStyle").ifBlank { landmark.category.label },
                    verifiedHistoricalFacts = facts,
                    localInsiderTip = root.optString("localInsiderTip").ifBlank {
                        "Inspect the primary plaque or facade details near the entrance."
                    },
                    rpgEncounterHook = root.optString("rpgEncounterHook").ifBlank { landmark.loreSnippet }
                )
            )
        } catch (e: TimeoutCancellationException) {
            Result.failure(Exception("Fact-Check Dossier timed out after 60 seconds."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Fact dossier generation failed", e)
            Result.failure(e)
        }
    }

    /**
     * Generates an interactive 3-option AI Landmark Riddle & Cipher Duel (`gemini-3.8-flash`).
     */
    suspend fun generateLandmarkRiddleWithGemini(
        style: CharacterStyle,
        regionName: String,
        landmark: RealLandmark
    ): Result<AiLandmarkRiddle> {
        return try {
            val prompt = """
                ${style.systemPersonaPrompt}
                
                Create an interactive 3-option riddle/trivia challenge for a walker standing at:
                Landmark: ${landmark.name} (${landmark.category.label}) in $regionName
                Real-World Fact: ${landmark.realFactOrAddress}
                
                Requirements:
                - Frame the question in-character as ${style.narratorTitle}, testing real knowledge or keen observation of ${landmark.name}.
                - Provide exactly 3 distinct answer options.
                - Set correctIndex to 0, 1, or 2.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                riddleModel.generateContent(prompt)
            }
            val jsonString = response.text
                ?: return Result.failure(IllegalStateException("Empty riddle response."))
            val root = JSONObject(jsonString)

            val optsArray = root.optJSONArray("options")
            val options = mutableListOf<String>()
            if (optsArray != null) {
                for (i in 0 until optsArray.length()) {
                    val opt = optsArray.optString(i).trim()
                    if (opt.isNotBlank()) options.add(opt)
                }
            }
            while (options.size < 3) {
                options.add("Option ${options.size + 1}")
            }
            val finalOptions = options.take(3)
            val correctIdx = root.optInt("correctIndex", 0).coerceIn(0, 2)

            Result.success(
                AiLandmarkRiddle(
                    landmarkName = landmark.name,
                    question = root.optString("question").ifBlank {
                        "What secret lies within the history of ${landmark.name}?"
                    },
                    options = finalOptions,
                    correctIndex = correctIdx,
                    hint = root.optString("hint").ifBlank { landmark.realFactOrAddress.take(90) },
                    explanationLore = root.optString("explanationLore").ifBlank { landmark.realFactOrAddress },
                    xpReward = 175
                )
            )
        } catch (e: TimeoutCancellationException) {
            Result.failure(Exception("AI Riddle generation timed out after 60 seconds."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI Riddle generation failed", e)
            Result.failure(e)
        }
    }

    /**
     * Polishes a raw spoken/transcribed voice field note into an eloquent RPG Chronicle (`gemini-3.8-flash`).
     */
    suspend fun polishSpokenFieldNoteWithGemini(
        rawTranscript: String,
        style: CharacterStyle,
        regionName: String,
        landmarkName: String
    ): Result<PolishedFieldLogResult> {
        return try {
            val prompt = """
                ${style.systemPersonaPrompt}
                
                The explorer just dictated this live voice field note while walking near "$landmarkName" in "$regionName":
                "$rawTranscript"
                
                Transform their spoken observation into:
                1. curioTitle: A memorable RPG Artifact / Field Chronicle title.
                2. polishedChronicle: A vivid 2-3 sentence RPG chronicle entry in the tone of ${style.title}.
                3. historicalConnection: 1 sentence tying their observation to $landmarkName in $regionName.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                fieldLogModel.generateContent(prompt)
            }
            val jsonString = response.text
                ?: return Result.failure(IllegalStateException("Empty field log response."))
            val root = JSONObject(jsonString)

            Result.success(
                PolishedFieldLogResult(
                    curioTitle = root.optString("curioTitle").ifBlank { "Voice Chronicle of $landmarkName" },
                    polishedChronicle = root.optString("polishedChronicle").ifBlank { rawTranscript },
                    historicalConnection = root.optString("historicalConnection").ifBlank {
                        "Recorded on foot at $landmarkName ($regionName)."
                    }
                )
            )
        } catch (e: TimeoutCancellationException) {
            Result.failure(Exception("Voice Field Log synthesis timed out after 60 seconds."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Voice Field Log synthesis failed", e)
            Result.failure(e)
        }
    }

    private fun scaleBitmapForVision(source: Bitmap, maxDimension: Int): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= maxDimension && height <= maxDimension) return source
        val ratio = width.toFloat() / height.toFloat()
        val targetWidth: Int
        val targetHeight: Int
        if (width >= height) {
            targetWidth = maxDimension
            targetHeight = (maxDimension / ratio).toInt().coerceAtLeast(1)
        } else {
            targetHeight = maxDimension
            targetWidth = (maxDimension * ratio).toInt().coerceAtLeast(1)
        }
        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }

    companion object {
        /**
         * Renders an authentic heritage plaque / landmark inscription bitmap for on-device /
         * emulator multimodal Gemini Vision verification when a hardware camera isn't attached.
         */
        fun createLandmarkPlaqueBitmap(
            landmarkName: String,
            categoryLabel: String,
            realFact: String,
            coordinatesText: String
        ): Bitmap {
            val width = 720
            val height = 480
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)

            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    intArrayOf(
                        Color.rgb(26, 22, 18),
                        Color.rgb(48, 38, 24),
                        Color.rgb(22, 18, 14)
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 8f
                color = Color.rgb(218, 175, 85)
            }
            canvas.drawRoundRect(RectF(24f, 24f, width - 24f, height - 24f), 24f, 24f, borderPaint)

            val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(235, 195, 105)
                textSize = 22f
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("OFFICIAL HERITAGE LANDMARK PLAQUE • $categoryLabel", width / 2f, 80f, headerPaint)

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 34f
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(landmarkName.take(32), width / 2f, 145f, titlePaint)

            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(230, 222, 205)
                textSize = 22f
                typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
                textAlign = Paint.Align.CENTER
            }
            val words = realFact.split(" ")
            val lines = mutableListOf<String>()
            var currentLine = ""
            for (w in words) {
                val test = if (currentLine.isEmpty()) w else "$currentLine $w"
                if (test.length > 44) {
                    lines.add(currentLine)
                    currentLine = w
                } else {
                    currentLine = test
                }
            }
            if (currentLine.isNotEmpty()) lines.add(currentLine)

            lines.take(5).forEachIndexed { idx, line ->
                canvas.drawText(line, width / 2f, 215f + (idx * 36f), bodyPaint)
            }

            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(180, 155, 95)
                textSize = 18f
                typeface = Typeface.MONOSPACE
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(coordinatesText, width / 2f, height - 50f, footerPaint)

            return bmp
        }
    }
}
