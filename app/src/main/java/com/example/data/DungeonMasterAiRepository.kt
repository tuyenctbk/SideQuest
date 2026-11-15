package com.example.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
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
import com.example.util.AiModelConfig
import com.example.util.AiModelConfig.AiCapability
import com.example.util.AiModelConfig.AiExecutionTier
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "DungeonMasterAI"

data class GeneratedQuestNarrative(
    val questTitle: String,
    val questIntro: String,
    val enrichedLandmarks: List<RealLandmark>,
    val executionTier: AiExecutionTier = AiExecutionTier.TIER_1_PRIMARY,
    val usedCloudQuota: Boolean = false
)

data class StreamReplyChunk(
    val text: String,
    val usedCloudQuota: Boolean = false,
    val executionTier: AiExecutionTier = AiExecutionTier.TIER_1_PRIMARY
)

data class RelicBitmapResult(
    val bitmap: Bitmap?,
    val usedCloudQuota: Boolean = false,
    val isFallback: Boolean = false,
    val hitRateLimit429: Boolean = false,
    val executionTier: AiExecutionTier = AiExecutionTier.TIER_1_PRIMARY,
    val errorMessage: String? = null
)

data class FingerprintCacheItem<T>(
    val timestampMs: Long,
    val data: T
)

class DungeonMasterAiRepository {

    // 45s Sliding Fingerprint Deduplication Cache (Playbook Section 1.C)
    // Prevents duplicate token spend when users double-tap generate/verify
    private val fingerprintCache = ConcurrentHashMap<String, FingerprintCacheItem<Any>>()

    // Smart Zero-Cost In-Memory Response Caches
    private val questCache = ConcurrentHashMap<String, GeneratedQuestNarrative>()
    private val factDossierCache = ConcurrentHashMap<String, LandmarkFactDossier>()
    private val riddleCache = ConcurrentHashMap<String, AiLandmarkRiddle>()

    private val _freeTierCallsUsed = MutableStateFlow(0)
    val freeTierCallsUsed: StateFlow<Int> = _freeTierCallsUsed.asStateFlow()

    private val _cacheHitsSaved = MutableStateFlow(0)
    val cacheHitsSaved: StateFlow<Int> = _cacheHitsSaved.asStateFlow()

    private val _paidImageCallsUsed = MutableStateFlow(0)
    val paidImageCallsUsed: StateFlow<Int> = _paidImageCallsUsed.asStateFlow()

    private val _autoCascadeCount = MutableStateFlow(0)
    val autoCascadeCount: StateFlow<Int> = _autoCascadeCount.asStateFlow()

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

    // Tier 1 Primary Standard Models
    private val structuredQuestModelPrimary by lazy {
        Firebase.ai.generativeModel(
            modelName = AiModelConfig.PRIMARY_REASONING_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = questJsonSchema
            }
        )
    }

    private val visionScanModelPrimary by lazy {
        Firebase.ai.generativeModel(
            modelName = AiModelConfig.PRIMARY_REASONING_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = visionScanSchema
            }
        )
    }

    private val factDossierModelPrimary by lazy {
        Firebase.ai.generativeModel(
            modelName = AiModelConfig.PRIMARY_REASONING_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = factDossierSchema
            }
        )
    }

    private val riddleModelPrimary by lazy {
        Firebase.ai.generativeModel(
            modelName = AiModelConfig.PRIMARY_REASONING_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = riddleSchema
            }
        )
    }

    private val fieldLogModelPrimary by lazy {
        Firebase.ai.generativeModel(
            modelName = AiModelConfig.PRIMARY_REASONING_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = fieldLogSchema
            }
        )
    }

    private val chatModelPrimary by lazy {
        Firebase.ai.generativeModel(modelName = AiModelConfig.PRIMARY_REASONING_MODEL)
    }

    // Tier 2 High-Efficiency Lite Models (Higher RPM, 3x-5x cheaper token consumption)
    private val structuredQuestModelLite by lazy {
        Firebase.ai.generativeModel(
            modelName = AiModelConfig.ECONOMY_LITE_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = questJsonSchema
            }
        )
    }

    private val visionScanModelLite by lazy {
        Firebase.ai.generativeModel(
            modelName = AiModelConfig.ECONOMY_LITE_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = visionScanSchema
            }
        )
    }

    private val factDossierModelLite by lazy {
        Firebase.ai.generativeModel(
            modelName = AiModelConfig.ECONOMY_LITE_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = factDossierSchema
            }
        )
    }

    private val riddleModelLite by lazy {
        Firebase.ai.generativeModel(
            modelName = AiModelConfig.ECONOMY_LITE_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = riddleSchema
            }
        )
    }

    private val fieldLogModelLite by lazy {
        Firebase.ai.generativeModel(
            modelName = AiModelConfig.ECONOMY_LITE_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = fieldLogSchema
            }
        )
    }

    private val chatModelLite by lazy {
        Firebase.ai.generativeModel(modelName = AiModelConfig.ECONOMY_LITE_MODEL)
    }

    // Image Models
    private val imageGenModelPrimary by lazy {
        Firebase.ai.generativeModel(
            modelName = AiModelConfig.PRIMARY_IMAGE_MODEL,
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
            }
        )
    }

    private val imageGenModelLite by lazy {
        Firebase.ai.generativeModel(
            modelName = AiModelConfig.ECONOMY_IMAGE_MODEL,
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
            }
        )
    }

    /**
     * Checks if an exception represents a 429 Resource Exhausted or Quota limit.
     */
    fun isQuotaOrRateLimitException(e: Throwable): Boolean {
        val msg = e.message.orEmpty().lowercase()
        return e is QuotaExceededException ||
            msg.contains("429") ||
            msg.contains("resource_exhausted") ||
            msg.contains("quota") ||
            msg.contains("rate limit")
    }

    private fun generateFingerprint(capability: AiCapability, input: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        val hash = digest.joinToString("") { "%02x".format(it) }
        return "${capability.name}::$hash"
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> checkFingerprintCache(fingerprint: String): T? {
        val item = fingerprintCache[fingerprint] ?: return null
        if (System.currentTimeMillis() - item.timestampMs < AiModelConfig.DEDUPLICATION_CACHE_TTL_MS) {
            _cacheHitsSaved.value += 1
            return item.data as? T
        }
        fingerprintCache.remove(fingerprint)
        return null
    }

    private fun putFingerprintCache(fingerprint: String, data: Any) {
        fingerprintCache[fingerprint] = FingerprintCacheItem(System.currentTimeMillis(), data)
    }

    /**
     * Generates an interactive walking RPG quest with automatic Tier 1 -> Tier 2 cascade & Tier 3 fallback.
     */
    suspend fun generateWalkingQuestWithGemini(
        style: CharacterStyle,
        regionName: String,
        landmarks: List<RealLandmark>,
        preferredTier: AiExecutionTier = AiExecutionTier.TIER_1_PRIMARY
    ): Result<GeneratedQuestNarrative> {
        val cacheKey = "${style.id}::${regionName.lowercase()}::${landmarks.joinToString(",") { it.name }}"
        questCache[cacheKey]?.let { cached ->
            _cacheHitsSaved.value += 1
            return Result.success(cached.copy(usedCloudQuota = false))
        }

        val fingerprint = generateFingerprint(AiCapability.QUEST_NARRATIVE, cacheKey)
        checkFingerprintCache<GeneratedQuestNarrative>(fingerprint)?.let {
            return Result.success(it.copy(usedCloudQuota = false))
        }

        if (preferredTier == AiExecutionTier.TIER_3_LOCAL_FALLBACK) {
            val localNarrative = generateWalkingQuestFallback(style, regionName, landmarks)
            return Result.success(localNarrative)
        }

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

        // Attempt Tier 1 or preferred Tier
        var currentTier = preferredTier
        var lastError: Throwable? = null

        for (attempt in 1..2) {
            val model = if (currentTier == AiExecutionTier.TIER_2_ECONOMY_LITE) structuredQuestModelLite else structuredQuestModelPrimary
            try {
                _freeTierCallsUsed.value += 1
                val response = withTimeout(45_000L) {
                    model.generateContent(prompt)
                }

                val jsonString = response.text
                    ?: throw IllegalStateException("Gemini returned an empty quest response.")

                val root = JSONObject(jsonString)
                val questTitle = root.optString("questTitle").takeIf { it.isNotBlank() }
                    ?: "${style.title}: The $regionName Chronicle"
                val questIntro = root.optString("questIntro").takeIf { it.isNotBlank() }
                    ?: "Greetings, wanderer. Your sensors have locked onto ${landmarks.size} real-world waypoints across $regionName."

                val stopsArray = root.optJSONArray("stops")
                val enriched = landmarks.mapIndexed { index, original ->
                    val stopObj = if (stopsArray != null && index < stopsArray.length()) stopsArray.optJSONObject(index) else null
                    if (stopObj == null) original else original.copy(
                        rpgNodeTitle = stopObj.optString("rpgNodeTitle").ifBlank { original.rpgNodeTitle },
                        loreSnippet = stopObj.optString("loreSnippet").ifBlank { original.loreSnippet },
                        questChallenge = stopObj.optString("questChallenge").ifBlank { original.questChallenge },
                        rewardItemName = stopObj.optString("rewardItemName").ifBlank { original.rewardItemName }
                    )
                }

                val narrative = GeneratedQuestNarrative(
                    questTitle = questTitle,
                    questIntro = questIntro,
                    enrichedLandmarks = enriched,
                    executionTier = currentTier,
                    usedCloudQuota = true
                )
                questCache[cacheKey] = narrative.copy(usedCloudQuota = false)
                putFingerprintCache(fingerprint, narrative.copy(usedCloudQuota = false))
                return Result.success(narrative)
            } catch (e: Exception) {
                lastError = e
                if (isQuotaOrRateLimitException(e) && currentTier == AiExecutionTier.TIER_1_PRIMARY) {
                    Log.w(TAG, "Tier 1 Quota exhausted/rate-limited on quest gen, auto-cascading to Economy Lite Tier 2")
                    _autoCascadeCount.value += 1
                    currentTier = AiExecutionTier.TIER_2_ECONOMY_LITE
                    continue
                }
                break
            }
        }

        // Graceful degradation: Tier 3 Zero-Cost Local Quest Generation
        Log.w(TAG, "Cloud models exhausted or failed, generating Tier 3 Local Quest Fallback", lastError)
        val fallback = generateWalkingQuestFallback(style, regionName, landmarks)
        return Result.success(fallback)
    }

    /**
     * Tier 3 Zero-Cost Local Quest Narrative Fallback (Playbook Section 2).
     */
    fun generateWalkingQuestFallback(
        style: CharacterStyle,
        regionName: String,
        landmarks: List<RealLandmark>
    ): GeneratedQuestNarrative {
        val title = when (style) {
            CharacterStyle.CYBERPUNK_SCOUT -> "Sector Grid: $regionName Data Recon"
            CharacterStyle.VICTORIAN_HISTORIAN -> "Antiquarian Survey of $regionName"
            CharacterStyle.ARCANE_CARTOGRAPHER -> "The Ley-Lines of $regionName"
            CharacterStyle.NOIR_DETECTIVE -> "Casefile #404: The $regionName Trail"
        }
        val intro = "Welcome, explorer. Offline Tactical Mode is active. Your sensors have identified ${landmarks.size} real-world waypoints in $regionName. Explore them to uncover lore and claim relics."

        val enriched = landmarks.mapIndexed { idx, original ->
            original.copy(
                rpgNodeTitle = original.category.rpgLabelFor(style),
                loreSnippet = "${original.name} stands as an authentic anchor of $regionName. ${original.realFactOrAddress}",
                questChallenge = "Stand within proximity of ${original.name} and observe its unique heritage features.",
                rewardItemName = "${original.name} Field Curio"
            )
        }
        return GeneratedQuestNarrative(title, intro, enriched, AiExecutionTier.TIER_3_LOCAL_FALLBACK)
    }

    /**
     * Analyzes landmark photo with Gemini Vision with automatic downscaling, Tier 1 -> Tier 2 cascade,
     * and Tier 3 Honor Pledge Check-In (Playbook Section 2).
     */
    suspend fun analyzeLandmarkPhotoWithGemini(
        bitmap: Bitmap,
        style: CharacterStyle,
        landmark: RealLandmark?,
        regionName: String,
        preferredTier: AiExecutionTier = AiExecutionTier.TIER_1_PRIMARY
    ): Result<VisionScanResult> {
        val cacheKey = "vision::${landmark?.name}::${regionName}::${bitmap.width}x${bitmap.height}"
        val fingerprint = generateFingerprint(AiCapability.VISION_PROOF, cacheKey)
        checkFingerprintCache<VisionScanResult>(fingerprint)?.let {
            return Result.success(it.copy(usedCloudQuota = false))
        }

        if (preferredTier == AiExecutionTier.TIER_3_LOCAL_FALLBACK) {
            return Result.success(generateHonorPledgeScanResult(style, landmark, regionName))
        }

        var currentTier = preferredTier
        var lastError: Throwable? = null

        for (attempt in 1..2) {
            val maxDim = if (currentTier == AiExecutionTier.TIER_2_ECONOMY_LITE) {
                AiModelConfig.LITE_IMAGE_DIMENSION_PX
            } else {
                AiModelConfig.MAX_IMAGE_DIMENSION_PX
            }
            val scaledBitmap = scaleBitmapForVision(bitmap, maxDimension = maxDim)
            val model = if (currentTier == AiExecutionTier.TIER_2_ECONOMY_LITE) visionScanModelLite else visionScanModelPrimary

            val landmarkInfo = if (landmark != null) {
                "Target Waypoint: ${landmark.name} (${landmark.category.label}) in $regionName. Known fact: ${landmark.realFactOrAddress}"
            } else {
                "Exploring $regionName on foot."
            }

            val prompt = """
                ${style.systemPersonaPrompt}
                
                The explorer has just captured this landmark image on foot.
                $landmarkInfo
                
                Tasks:
                1. Inspect the image (plaque, statue, park, facade, or coffee house).
                2. Narrate a 2-3 sentence in-character analysis as ${style.narratorTitle} (${style.title}).
                3. Provide 1-2 sentences of real-world historical/architectural context.
                4. Mint a unique RPG Relic name.
            """.trimIndent()

            try {
                _freeTierCallsUsed.value += 1
                val response = withTimeout(45_000L) {
                    model.generateContent(
                        content {
                            image(scaledBitmap)
                            text(prompt)
                        }
                    )
                }

                val jsonString = response.text
                    ?: throw IllegalStateException("Gemini Vision returned an empty result.")
                val root = JSONObject(jsonString)

                val result = VisionScanResult(
                    landmarkTitle = root.optString("landmarkTitle").ifBlank { landmark?.name ?: "Verified Waypoint" },
                    inCharacterAnalysis = root.optString("inCharacterAnalysis").ifBlank { "Optical scan complete. Waypoint resonance verified." },
                    historicalVerification = root.optString("historicalVerification").ifBlank { landmark?.realFactOrAddress ?: "Verified on-site." },
                    awardedRelicName = root.optString("awardedRelicName").ifBlank { "Codex Inscription of ${landmark?.name?.take(20) ?: "Discovery"}" },
                    bonusXp = 175,
                    usedCloudQuota = true
                )
                putFingerprintCache(fingerprint, result.copy(usedCloudQuota = false))
                return Result.success(result)
            } catch (e: Exception) {
                lastError = e
                if (isQuotaOrRateLimitException(e) && currentTier == AiExecutionTier.TIER_1_PRIMARY) {
                    Log.w(TAG, "Vision Tier 1 Quota exceeded, auto-cascading to Economy Lite Tier 2")
                    _autoCascadeCount.value += 1
                    currentTier = AiExecutionTier.TIER_2_ECONOMY_LITE
                    continue
                }
                break
            }
        }

        // Graceful Tier 3 Fallback: Honor Pledge Check-In
        Log.w(TAG, "Vision cloud models exhausted, activating Tier 3 Honor Pledge Check-In", lastError)
        return Result.success(generateHonorPledgeScanResult(style, landmark, regionName))
    }

    /**
     * Tier 3 Zero-Quota Honor Pledge Check-In (Playbook Section 2).
     * Protects daily walking streaks and badge progression without cloud tokens.
     */
    fun generateHonorPledgeScanResult(
        style: CharacterStyle,
        landmark: RealLandmark?,
        regionName: String
    ): VisionScanResult {
        val lmName = landmark?.name ?: "Local Landmark"
        val analysis = when (style) {
            CharacterStyle.CYBERPUNK_SCOUT -> "Tactical Honor-Pledge confirmed at $lmName. Proximity sensors logged street-level presence. Cloud quota conserved."
            CharacterStyle.VICTORIAN_HISTORIAN -> "Honorary field rubbing authenticated for $lmName. The scholar's word is duly recorded in the Guild ledger."
            CharacterStyle.ARCANE_CARTOGRAPHER -> "The ley-stone of $lmName resonates with your footstep. Honor-Pledge crest bound to your cartographer's tome."
            CharacterStyle.NOIR_DETECTIVE -> "Informant confirms you were on the scene at $lmName. Case log marked verified without extra expense."
        }

        return VisionScanResult(
            landmarkTitle = "$lmName [Honor Pledge]",
            inCharacterAnalysis = analysis,
            historicalVerification = landmark?.realFactOrAddress ?: "Verified local waypoint along walking path.",
            awardedRelicName = "Honor Rubbing of ${lmName.take(20)}",
            bonusXp = 125
        )
    }

    /**
     * Streams Dungeon Master dialogue with sliding context window (last 6 turns),
     * Tier 1 -> Tier 2 cascade, and Tier 3 Local Reflex Engine (Playbook Section 2).
     */
    fun streamDungeonMasterReplyEvents(
        style: CharacterStyle,
        regionName: String,
        activeLandmark: RealLandmark?,
        recentMessages: List<ChatMessage>,
        userMessage: String,
        preferredTier: AiExecutionTier = AiExecutionTier.TIER_1_PRIMARY,
        explorerRank: String = "Urban Pathfinder",
        totalXp: Long = 250L
    ): Flow<StreamReplyChunk> = flow {
        if (preferredTier == AiExecutionTier.TIER_3_LOCAL_FALLBACK) {
            val localReply = generateLocalReflexReply(style, regionName, activeLandmark, userMessage, explorerRank, totalXp)
            emit(StreamReplyChunk(text = localReply, usedCloudQuota = false, executionTier = AiExecutionTier.TIER_3_LOCAL_FALLBACK))
            return@flow
        }

        // Sliding context window: send only last 6 turns to conserve tokens (Playbook Section 1.C)
        val historyContext = recentMessages.takeLast(AiModelConfig.MAX_CHAT_HISTORY_TURNS).joinToString("\n") { msg ->
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

        var currentTier = preferredTier
        var streamedSuccessfully = false

        for (attempt in 1..2) {
            val model = if (currentTier == AiExecutionTier.TIER_2_ECONOMY_LITE) chatModelLite else chatModelPrimary
            val accumulated = StringBuilder()

            try {
                _freeTierCallsUsed.value += 1
                withTimeout(45_000L) {
                    model.generateContentStream(content { text(fullPrompt) }).collect { chunk ->
                        val part = chunk.text.orEmpty()
                        if (part.isNotEmpty()) {
                            accumulated.append(part)
                            emit(StreamReplyChunk(text = accumulated.toString(), usedCloudQuota = true, executionTier = currentTier))
                        }
                    }
                }
                if (accumulated.isNotBlank()) {
                    streamedSuccessfully = true
                    break
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (isQuotaOrRateLimitException(e) && currentTier == AiExecutionTier.TIER_1_PRIMARY) {
                    Log.w(TAG, "DM Chat Tier 1 Quota exceeded, auto-cascading to Economy Lite Tier 2")
                    _autoCascadeCount.value += 1
                    currentTier = AiExecutionTier.TIER_2_ECONOMY_LITE
                    continue
                }
                break
            }
        }

        if (!streamedSuccessfully) {
            Log.w(TAG, "Cloud models unavailable for DM chat, emitting Tier 3 Local Reflex Engine reply")
            val localReflex = generateLocalReflexReply(style, regionName, activeLandmark, userMessage, explorerRank, totalXp)
            emit(StreamReplyChunk(text = localReflex, usedCloudQuota = false, executionTier = AiExecutionTier.TIER_3_LOCAL_FALLBACK))
        }
    }

    /**
     * Tier 3 Instant Local Reflex Engine (Playbook Section 2).
     * Deterministic, context-aware reply using the user's real stats, rank, active waypoint, and archetype templates.
     * Consumes 0 cloud tokens and executes instantaneously offline.
     */
    fun generateLocalReflexReply(
        style: CharacterStyle,
        regionName: String,
        landmark: RealLandmark?,
        userMessage: String = "Status report",
        explorerRank: String = "Explorer",
        totalXp: Long = 100L
    ): String {
        val lmName = landmark?.name ?: regionName
        val fact = landmark?.realFactOrAddress ?: "This landmark holds deep architectural significance in the neighborhood."

        return when (style) {
            CharacterStyle.CYBERPUNK_SCOUT -> {
                "[$explorerRank // Tactical Node] Netrunner signal locked on $lmName. $fact. Local reflex protocols advise surveying the primary perimeter before initiating extraction. Keep your pace steady!"
            }
            CharacterStyle.VICTORIAN_HISTORIAN -> {
                "Splendid observation, esteemed $explorerRank. In my historical archives, $lmName is noted as follows: $fact. Let us proceed with dignified curiosity along the promenade."
            }
            CharacterStyle.ARCANE_CARTOGRAPHER -> {
                "Greetings, $explorerRank of the Astral Order. The ley-lines hum around $lmName ($totalXp XP recorded). $fact. Heed the whispers in the cobblestones and walk forward with purpose."
            }
            CharacterStyle.NOIR_DETECTIVE -> {
                "Case notes updated for $lmName. Here is what the wire says: $fact. It pays to keep your eyes open in this district, $explorerRank. Let us see where the trail leads next."
            }
        }
    }

    /**
     * Runtime Image Generation with Tier 1 -> Tier 2 (Economy Image) cascade & Tier 3 On-Device Canvas Emblem fallback.
     */
    suspend fun generateRelicArtworkWithGemini(
        promptText: String,
        style: CharacterStyle = CharacterStyle.CYBERPUNK_SCOUT,
        landmarkName: String = "Civic Waypoint",
        preferredTier: AiExecutionTier = AiExecutionTier.TIER_1_PRIMARY
    ): RelicBitmapResult {
        if (preferredTier == AiExecutionTier.TIER_3_LOCAL_FALLBACK) {
            return RelicBitmapResult(
                bitmap = generateLocalCanvasEmblem(promptText, style, landmarkName),
                usedCloudQuota = false,
                isFallback = true,
                executionTier = AiExecutionTier.TIER_3_LOCAL_FALLBACK
            )
        }

        var currentTier = preferredTier
        var hit429 = false
        var lastError: Throwable? = null

        for (attempt in 1..2) {
            val model = if (currentTier == AiExecutionTier.TIER_2_ECONOMY_LITE) imageGenModelLite else imageGenModelPrimary
            try {
                _paidImageCallsUsed.value += 1
                val response = withTimeout(45_000L) {
                    model.generateContent(promptText)
                }
                val imagePart = response.candidates.firstOrNull()
                    ?.content?.parts?.filterIsInstance<ImagePart>()
                    ?.firstOrNull()
                val generated = imagePart?.image
                if (generated != null) {
                    return RelicBitmapResult(
                        bitmap = generated,
                        usedCloudQuota = true,
                        isFallback = false,
                        executionTier = currentTier
                    )
                }
                lastError = IllegalStateException("No image was returned by the model.")
                break
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                if (isQuotaOrRateLimitException(e)) {
                    hit429 = true
                    if (currentTier == AiExecutionTier.TIER_1_PRIMARY) {
                        Log.w(TAG, "Image Gen Tier 1 rate-limited, auto-cascading to Economy Image Tier 2")
                        _autoCascadeCount.value += 1
                        currentTier = AiExecutionTier.TIER_2_ECONOMY_LITE
                        continue
                    }
                }
                break
            }
        }

        Log.w(TAG, "Cloud Image generation failed or rate-limited, creating Tier 3 Canvas Emblem", lastError)
        return RelicBitmapResult(
            bitmap = generateLocalCanvasEmblem(promptText, style, landmarkName),
            usedCloudQuota = false,
            isFallback = true,
            hitRateLimit429 = hit429,
            executionTier = AiExecutionTier.TIER_3_LOCAL_FALLBACK,
            errorMessage = lastError?.localizedMessage
        )
    }

    /**
     * Runtime Image Editing with Tier 1 -> Tier 2 (Economy Image) cascade and honest failure reporting.
     */
    suspend fun editRelicArtworkWithGemini(
        inputBitmap: Bitmap,
        editInstruction: String,
        style: CharacterStyle = CharacterStyle.CYBERPUNK_SCOUT,
        landmarkName: String = "Civic Waypoint",
        preferredTier: AiExecutionTier = AiExecutionTier.TIER_1_PRIMARY
    ): RelicBitmapResult {
        if (preferredTier == AiExecutionTier.TIER_3_LOCAL_FALLBACK) {
            return RelicBitmapResult(
                bitmap = null,
                usedCloudQuota = false,
                isFallback = true,
                executionTier = AiExecutionTier.TIER_3_LOCAL_FALLBACK,
                errorMessage = "Cloud quota depleted — local remixing paused."
            )
        }

        var currentTier = preferredTier
        var hit429 = false
        var lastError: Throwable? = null

        for (attempt in 1..2) {
            val maxDim = if (currentTier == AiExecutionTier.TIER_2_ECONOMY_LITE) {
                AiModelConfig.LITE_IMAGE_DIMENSION_PX
            } else {
                AiModelConfig.MAX_IMAGE_DIMENSION_PX
            }
            val model = if (currentTier == AiExecutionTier.TIER_2_ECONOMY_LITE) imageGenModelLite else imageGenModelPrimary
            try {
                _paidImageCallsUsed.value += 1
                val scaledInput = scaleBitmapForVision(inputBitmap, maxDimension = maxDim)
                val response = withTimeout(45_000L) {
                    model.generateContent(
                        content {
                            image(scaledInput)
                            text(editInstruction)
                        }
                    )
                }
                val imagePart = response.candidates.firstOrNull()
                    ?.content?.parts?.filterIsInstance<ImagePart>()
                    ?.firstOrNull()
                val edited = imagePart?.image
                if (edited != null) {
                    return RelicBitmapResult(
                        bitmap = edited,
                        usedCloudQuota = true,
                        isFallback = false,
                        executionTier = currentTier
                    )
                }
                lastError = IllegalStateException("No edited image was returned by the model.")
                break
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                if (isQuotaOrRateLimitException(e)) {
                    hit429 = true
                    if (currentTier == AiExecutionTier.TIER_1_PRIMARY) {
                        Log.w(TAG, "Image Edit Tier 1 rate-limited, auto-cascading to Economy Image Tier 2")
                        _autoCascadeCount.value += 1
                        currentTier = AiExecutionTier.TIER_2_ECONOMY_LITE
                        continue
                    }
                }
                break
            }
        }

        Log.w(TAG, "Image editing failed", lastError)
        return RelicBitmapResult(
            bitmap = null,
            usedCloudQuota = false,
            isFallback = true,
            hitRateLimit429 = hit429,
            executionTier = AiExecutionTier.TIER_3_LOCAL_FALLBACK,
            errorMessage = lastError?.localizedMessage
        )
    }

    /**
     * Tier 3 On-Device Canvas Emblem Generator (Playbook Section 2).
     * Renders a stylized, high-resolution collectible card / emblem locally on Android Canvas (0 cloud credits).
     */
    fun generateLocalCanvasEmblem(
        style: CharacterStyle,
        landmarkName: String,
        regionName: String
    ): Bitmap = generateLocalCanvasEmblem(
        curioTitle = "$landmarkName Emblem",
        style = style,
        landmarkName = landmarkName
    )

    fun generateLocalCanvasEmblem(
        curioTitle: String,
        style: CharacterStyle,
        landmarkName: String
    ): Bitmap {
        val size = 512
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // Palette per archetype style
        val (bgStart, bgEnd, accentColor, ringColor) = when (style) {
            CharacterStyle.CYBERPUNK_SCOUT -> listOf(
                Color.rgb(10, 16, 26), Color.rgb(20, 36, 52), Color.rgb(0, 240, 255), Color.rgb(16, 185, 129)
            )
            CharacterStyle.VICTORIAN_HISTORIAN -> listOf(
                Color.rgb(36, 28, 20), Color.rgb(58, 44, 30), Color.rgb(218, 175, 85), Color.rgb(235, 195, 105)
            )
            CharacterStyle.ARCANE_CARTOGRAPHER -> listOf(
                Color.rgb(18, 14, 32), Color.rgb(34, 24, 60), Color.rgb(167, 139, 250), Color.rgb(52, 211, 153)
            )
            CharacterStyle.NOIR_DETECTIVE -> listOf(
                Color.rgb(24, 24, 26), Color.rgb(40, 42, 46), Color.rgb(220, 220, 225), Color.rgb(218, 175, 85)
            )
        }

        // Background Gradient
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, size.toFloat(), size.toFloat(), bgStart, bgEnd, Shader.TileMode.CLAMP)
        }
        canvas.drawRoundRect(RectF(12f, 12f, size - 12f, size - 12f), 32f, 32f, bgPaint)

        // Gold Filigree Border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE
            strokeWidth = 6f
            color = accentColor
        }
        canvas.drawRoundRect(RectF(24f, 24f, size - 24f, size - 24f), 24f, 24f, borderPaint)

        // Center Medallion
        val center = size / 2f
        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(40, Color.red(ringColor), Color.green(ringColor), Color.blue(ringColor))
        }
        canvas.drawCircle(center, center - 20f, 110f, circlePaint)

        val ringStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE
            strokeWidth = 4f
            color = ringColor
        }
        canvas.drawCircle(center, center - 20f, 110f, ringStroke)

        // Geometric Emblem Glyph
        val glyphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE
            strokeWidth = 5f
            color = accentColor
        }
        val path = Path().apply {
            moveTo(center, center - 90f)
            lineTo(center + 60f, center - 20f)
            lineTo(center, center + 50f)
            lineTo(center - 60f, center - 20f)
            close()
        }
        canvas.drawPath(path, glyphPaint)

        // Header Label
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ringColor
            textSize = 20f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(style.title.uppercase(), center, 65f, headerPaint)

        // Emblem Title
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 24f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(curioTitle.take(28), center, size - 70f, titlePaint)

        // Footer Landmark Imprint
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(200, 200, 200)
            textSize = 17f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Forged at ${landmarkName.take(30)}", center, size - 40f, subPaint)

        return bmp
    }

    /**
     * Synthesizes a Fact-Check Dossier with Tier 1 -> Tier 2 cascade & Tier 3 Local fallback.
     */
    suspend fun generateLandmarkFactDossier(
        style: CharacterStyle,
        regionName: String,
        landmark: RealLandmark,
        preferredTier: AiExecutionTier = AiExecutionTier.TIER_1_PRIMARY
    ): Result<LandmarkFactDossier> {
        val cacheKey = "${style.id}::${landmark.name.lowercase()}"
        factDossierCache[cacheKey]?.let { cached ->
            _cacheHitsSaved.value += 1
            return Result.success(cached.copy(usedCloudQuota = false))
        }

        val fingerprint = generateFingerprint(AiCapability.FACT_DOSSIER, cacheKey)
        checkFingerprintCache<LandmarkFactDossier>(fingerprint)?.let {
            return Result.success(it.copy(usedCloudQuota = false))
        }

        if (preferredTier == AiExecutionTier.TIER_3_LOCAL_FALLBACK) {
            return Result.success(generateLocalFactDossierFallback(style, regionName, landmark))
        }

        var currentTier = preferredTier
        var lastError: Throwable? = null

        val prompt = """
            ${style.systemPersonaPrompt}
            
            Synthesize a verified Real-World Historical & Architectural Fact-Check Dossier for:
            Landmark: ${landmark.name} (${landmark.category.label})
            Region: $regionName
            Coordinates: ${landmark.latitude}, ${landmark.longitude}
            Known Grounding Context: ${landmark.realFactOrAddress}
            
            Provide accurate historical/geographical facts, era, architectural style, an on-site walker observation tip, and an RPG hook in the voice of ${style.narratorTitle}.
        """.trimIndent()

        for (attempt in 1..2) {
            val model = if (currentTier == AiExecutionTier.TIER_2_ECONOMY_LITE) factDossierModelLite else factDossierModelPrimary
            try {
                _freeTierCallsUsed.value += 1
                val response = withTimeout(45_000L) {
                    model.generateContent(prompt)
                }
                val jsonString = response.text ?: throw IllegalStateException("Empty fact dossier response.")
                val root = JSONObject(jsonString)

                val factsArray = root.optJSONArray("verifiedHistoricalFacts")
                val facts = mutableListOf<String>()
                if (factsArray != null) {
                    for (i in 0 until factsArray.length()) {
                        val f = factsArray.optString(i).trim()
                        if (f.isNotBlank()) facts.add(f)
                    }
                }
                if (facts.isEmpty()) facts.add(landmark.realFactOrAddress)

                val dossier = LandmarkFactDossier(
                    landmarkName = landmark.name,
                    verifiedEra = root.optString("verifiedEra").ifBlank { "Historic Civic Era" },
                    architecturalStyle = root.optString("architecturalStyle").ifBlank { landmark.category.label },
                    verifiedHistoricalFacts = facts,
                    localInsiderTip = root.optString("localInsiderTip").ifBlank { "Inspect the facade and historical inscription near the entry." },
                    rpgEncounterHook = root.optString("rpgEncounterHook").ifBlank { landmark.loreSnippet },
                    usedCloudQuota = true
                )
                factDossierCache[cacheKey] = dossier.copy(usedCloudQuota = false)
                putFingerprintCache(fingerprint, dossier.copy(usedCloudQuota = false))
                return Result.success(dossier)
            } catch (e: Exception) {
                lastError = e
                if (isQuotaOrRateLimitException(e) && currentTier == AiExecutionTier.TIER_1_PRIMARY) {
                    Log.w(TAG, "Fact Dossier Tier 1 Quota exceeded, auto-cascading to Economy Lite Tier 2")
                    _autoCascadeCount.value += 1
                    currentTier = AiExecutionTier.TIER_2_ECONOMY_LITE
                    continue
                }
                break
            }
        }

        Log.w(TAG, "Cloud models unavailable for fact dossier, using Tier 3 Local Dossier", lastError)
        return Result.success(generateLocalFactDossierFallback(style, regionName, landmark))
    }

    fun generateLocalFactDossierFallback(
        style: CharacterStyle,
        regionName: String,
        landmark: RealLandmark
    ): LandmarkFactDossier {
        return LandmarkFactDossier(
            landmarkName = landmark.name,
            verifiedEra = "Recorded Municipal Landmark",
            architecturalStyle = landmark.category.label,
            verifiedHistoricalFacts = listOf(
                landmark.realFactOrAddress,
                "Located at coordinates ${landmark.latitude}, ${landmark.longitude} in $regionName.",
                "Cataloged in public geographic records as an official ${landmark.category.label}."
            ),
            localInsiderTip = "Notice the surrounding pedestrian access points and architectural markers.",
            rpgEncounterHook = "${style.narratorTitle} notes: Observe ${landmark.name} carefully to claim the local explorer bounty."
        )
    }

    /**
     * Generates an interactive 3-option riddle with Tier 1 -> Tier 2 cascade & Tier 3 Local fallback.
     */
    suspend fun generateLandmarkRiddleWithGemini(
        style: CharacterStyle,
        regionName: String,
        landmark: RealLandmark,
        preferredTier: AiExecutionTier = AiExecutionTier.TIER_1_PRIMARY
    ): Result<AiLandmarkRiddle> {
        val cacheKey = "${style.id}::${landmark.name.lowercase()}"
        riddleCache[cacheKey]?.let { cached ->
            _cacheHitsSaved.value += 1
            return Result.success(cached.copy(usedCloudQuota = false))
        }

        val fingerprint = generateFingerprint(AiCapability.RIDDLE_CIPHER, cacheKey)
        checkFingerprintCache<AiLandmarkRiddle>(fingerprint)?.let {
            return Result.success(it.copy(usedCloudQuota = false))
        }

        if (preferredTier == AiExecutionTier.TIER_3_LOCAL_FALLBACK) {
            return Result.success(generateLocalRiddleFallback(style, regionName, landmark))
        }

        var currentTier = preferredTier
        var lastError: Throwable? = null

        val prompt = """
            ${style.systemPersonaPrompt}
            
            Create an interactive 3-option riddle/trivia challenge for a walker standing at:
            Landmark: ${landmark.name} (${landmark.category.label}) in $regionName
            Real-World Fact: ${landmark.realFactOrAddress}
            
            Requirements:
            - Frame the question in-character as ${style.narratorTitle}.
            - Provide exactly 3 distinct answer options.
            - Set correctIndex to 0, 1, or 2.
        """.trimIndent()

        for (attempt in 1..2) {
            val model = if (currentTier == AiExecutionTier.TIER_2_ECONOMY_LITE) riddleModelLite else riddleModelPrimary
            try {
                _freeTierCallsUsed.value += 1
                val response = withTimeout(45_000L) {
                    model.generateContent(prompt)
                }
                val jsonString = response.text ?: throw IllegalStateException("Empty riddle response.")
                val root = JSONObject(jsonString)

                val optsArray = root.optJSONArray("options")
                val options = mutableListOf<String>()
                if (optsArray != null) {
                    for (i in 0 until optsArray.length()) {
                        val opt = optsArray.optString(i).trim()
                        if (opt.isNotBlank()) options.add(opt)
                    }
                }
                while (options.size < 3) options.add("Observation Point ${options.size + 1}")
                val finalOptions = options.take(3)
                val correctIdx = root.optInt("correctIndex", 0).coerceIn(0, 2)

                val riddle = AiLandmarkRiddle(
                    landmarkName = landmark.name,
                    question = root.optString("question").ifBlank { "What historic secret is preserved at ${landmark.name}?" },
                    options = finalOptions,
                    correctIndex = correctIdx,
                    hint = root.optString("hint").ifBlank { landmark.realFactOrAddress.take(80) },
                    explanationLore = root.optString("explanationLore").ifBlank { landmark.realFactOrAddress },
                    xpReward = 175,
                    usedCloudQuota = true
                )
                riddleCache[cacheKey] = riddle.copy(usedCloudQuota = false)
                putFingerprintCache(fingerprint, riddle.copy(usedCloudQuota = false))
                return Result.success(riddle)
            } catch (e: Exception) {
                lastError = e
                if (isQuotaOrRateLimitException(e) && currentTier == AiExecutionTier.TIER_1_PRIMARY) {
                    Log.w(TAG, "Riddle Tier 1 Quota exceeded, auto-cascading to Economy Lite Tier 2")
                    _autoCascadeCount.value += 1
                    currentTier = AiExecutionTier.TIER_2_ECONOMY_LITE
                    continue
                }
                break
            }
        }

        Log.w(TAG, "Cloud models unavailable for riddle, using Tier 3 Local Riddle", lastError)
        return Result.success(generateLocalRiddleFallback(style, regionName, landmark))
    }

    fun generateLocalRiddleFallback(
        style: CharacterStyle,
        regionName: String,
        landmark: RealLandmark
    ): AiLandmarkRiddle {
        return AiLandmarkRiddle(
            landmarkName = landmark.name,
            question = "Which notable attribute defines ${landmark.name} in $regionName?",
            options = listOf(
                landmark.realFactOrAddress,
                "A fictional tower erected in 2088",
                "An unmarked railway junction"
            ),
            correctIndex = 0,
            hint = "Look for the primary historical plaque or architectural marker on-site.",
            explanationLore = "Verified: ${landmark.realFactOrAddress}.",
            xpReward = 150
        )
    }

    /**
     * Polishes a raw spoken/transcribed field note with Tier 1 -> Tier 2 cascade & Tier 3 Local fallback (Playbook Section 2).
     * Never drops or loses the explorer's audio memo!
     */
    suspend fun polishSpokenFieldNoteWithGemini(
        rawTranscript: String,
        style: CharacterStyle,
        regionName: String,
        landmarkName: String,
        preferredTier: AiExecutionTier = AiExecutionTier.TIER_1_PRIMARY
    ): Result<PolishedFieldLogResult> {
        if (preferredTier == AiExecutionTier.TIER_3_LOCAL_FALLBACK) {
            return Result.success(generateLocalFieldLogFallback(rawTranscript, style, regionName, landmarkName))
        }

        val prompt = """
            ${style.systemPersonaPrompt}
            
            The explorer just dictated this voice field note near "$landmarkName" in "$regionName":
            "$rawTranscript"
            
            Transform their spoken observation into:
            1. curioTitle: A memorable RPG Artifact / Field Chronicle title.
            2. polishedChronicle: A vivid 2-3 sentence RPG chronicle entry in the tone of ${style.title}.
            3. historicalConnection: 1 sentence tying their observation to $landmarkName in $regionName.
        """.trimIndent()

        var currentTier = preferredTier
        var lastError: Throwable? = null

        for (attempt in 1..2) {
            val model = if (currentTier == AiExecutionTier.TIER_2_ECONOMY_LITE) fieldLogModelLite else fieldLogModelPrimary
            try {
                _freeTierCallsUsed.value += 1
                val response = withTimeout(45_000L) {
                    model.generateContent(prompt)
                }
                val jsonString = response.text ?: throw IllegalStateException("Empty field log response.")
                val root = JSONObject(jsonString)

                return Result.success(
                    PolishedFieldLogResult(
                        curioTitle = root.optString("curioTitle").ifBlank { "Chronicle of $landmarkName" },
                        polishedChronicle = root.optString("polishedChronicle").ifBlank { rawTranscript },
                        historicalConnection = root.optString("historicalConnection").ifBlank {
                            "Recorded on foot at $landmarkName in $regionName."
                        },
                        usedCloudQuota = true
                    )
                )
            } catch (e: Exception) {
                lastError = e
                if (isQuotaOrRateLimitException(e) && currentTier == AiExecutionTier.TIER_1_PRIMARY) {
                    Log.w(TAG, "Field Log Tier 1 Quota exceeded, auto-cascading to Economy Lite Tier 2")
                    _autoCascadeCount.value += 1
                    currentTier = AiExecutionTier.TIER_2_ECONOMY_LITE
                    continue
                }
                break
            }
        }

        // Graceful Tier 3 Fallback: preserves raw transcript so user never loses their field note
        Log.w(TAG, "Cloud models unavailable for field log, using Tier 3 Raw Transcript Preservation", lastError)
        return Result.success(generateLocalFieldLogFallback(rawTranscript, style, regionName, landmarkName))
    }

    /**
     * Tier 3 Zero-Cost Local Field Log Fallback.
     */
    fun generateLocalFieldLogFallback(
        rawTranscript: String,
        style: CharacterStyle,
        regionName: String,
        landmarkName: String
    ): PolishedFieldLogResult {
        return PolishedFieldLogResult(
            curioTitle = "Field Note: $landmarkName [Raw Spoken Log]",
            polishedChronicle = "Explorer Observation: \"$rawTranscript\". (Saved in offline zero-quota mode — AI styling paused until quota resets).",
            historicalConnection = "Recorded on foot at $landmarkName ($regionName) by ${style.narratorTitle}."
        )
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
                    intArrayOf(Color.rgb(26, 22, 18), Color.rgb(48, 38, 24), Color.rgb(22, 18, 14)),
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
