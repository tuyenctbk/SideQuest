package com.example.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import com.example.model.CharacterStyle
import com.example.model.LandmarkCategory
import com.example.model.PresetCityRegion
import com.example.model.RealLandmark
import com.example.model.SeedLandmark
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class LandmarkDiscoveryRepository(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val presetRegions: List<PresetCityRegion> = listOf(
        PresetCityRegion(
            name = "San Francisco — Embarcadero & Telegraph Hill",
            subtitle = "Waterfront piers, Gold Rush plaques, Coit Tower & historic cafes",
            latitude = 37.7955,
            longitude = -122.3937,
            fallbackLandmarks = listOf(
                SeedLandmark(
                    name = "Ferry Building Marketplace Clocktower",
                    category = LandmarkCategory.MONUMENT,
                    latitude = 37.7955,
                    longitude = -122.3937,
                    realFact = "Opened in 1898 and modeled after the 12th-century Giralda bell tower in Seville, Spain; survived Both the 1906 and 1989 earthquakes."
                ),
                SeedLandmark(
                    name = "Sue Bierman Waterfront Park",
                    category = LandmarkCategory.PARK,
                    latitude = 37.7966,
                    longitude = -122.3976,
                    realFact = "Lush urban park built where the elevated Embarcadero Freeway once stood before its post-1989 earthquake demolition."
                ),
                SeedLandmark(
                    name = "Pony Express Western Terminus Plaque",
                    category = LandmarkCategory.PLAQUE,
                    latitude = 37.7945,
                    longitude = -122.4018,
                    realFact = "California Historical Landmark #696 marking the 1860-1861 headquarters of the legendary transcontinental Pony Express."
                ),
                SeedLandmark(
                    name = "Blue Bottle Coffee — Ferry Arcade",
                    category = LandmarkCategory.COFFEE_SHOP,
                    latitude = 37.7952,
                    longitude = -122.3931,
                    realFact = "Specialty Bay Area roaster nestled inside the historic 1898 nave of the San Francisco Ferry Building."
                ),
                SeedLandmark(
                    name = "Levi's Plaza Park & Fountain",
                    category = LandmarkCategory.PARK,
                    latitude = 37.8017,
                    longitude = -122.4023,
                    realFact = "Designed by landscape architect Lawrence Halprin with cascading granite waterblocks symbolizing the Sierra Nevada mountains."
                )
            )
        ),
        PresetCityRegion(
            name = "New York — Central Park South & Fifth Ave",
            subtitle = "Olmsted's scenic promenade, literary statues & Manhattan espresso bars",
            latitude = 40.7681,
            longitude = -73.9719,
            fallbackLandmarks = listOf(
                SeedLandmark(
                    name = "The Mall & Literary Walk",
                    category = LandmarkCategory.PARK,
                    latitude = 40.7712,
                    longitude = -73.9714,
                    realFact = "The only deliberate straight line in Central Park, shaded by one of North America's largest stands of American Elm trees."
                ),
                SeedLandmark(
                    name = "Bethesda Terrace & Angel of the Waters Plaque",
                    category = LandmarkCategory.PLAQUE,
                    latitude = 40.7738,
                    longitude = -73.9709,
                    realFact = "Unveiled in 1873 by sculptor Emma Stebbins to commemorate the opening of the Croton Aqueduct that brought pure water to NYC."
                ),
                SeedLandmark(
                    name = "Balto Bronze Memorial Statue",
                    category = LandmarkCategory.MONUMENT,
                    latitude = 40.7699,
                    longitude = -73.9710,
                    realFact = "Sculpted by Frederick Roth in 1925 honoring the sled dog who led the final leg of the Nome diphtheria serum run."
                ),
                SeedLandmark(
                    name = "Ralph's Coffee — Madison Promenade",
                    category = LandmarkCategory.COFFEE_SHOP,
                    latitude = 40.7676,
                    longitude = -73.9698,
                    realFact = "Classic mosaic-floored Manhattan coffee stop serving organic roast steps away from Central Park's eastern gates."
                )
            )
        ),
        PresetCityRegion(
            name = "London — Westminster & St James's Park",
            subtitle = "Royal parks, Victorian scientific plaques & historic Soho coffeehouses",
            latitude = 51.5033,
            longitude = -0.1276,
            fallbackLandmarks = listOf(
                SeedLandmark(
                    name = "St James's Park Duck Island Sanctuary",
                    category = LandmarkCategory.PARK,
                    latitude = 51.5028,
                    longitude = -0.1315,
                    realFact = "The oldest Royal Park in London, landscaped by John Nash in 1827 and home to resident pelicans since 1664."
                ),
                SeedLandmark(
                    name = "John Snow Broadwick Street Pump Plaque",
                    category = LandmarkCategory.PLAQUE,
                    latitude = 51.5133,
                    longitude = -0.1366,
                    realFact = "Marks the site where Dr. John Snow traced the 1854 cholera outbreak to a single water pump, founding modern epidemiology."
                ),
                SeedLandmark(
                    name = "Algerian Coffee Stores — Old Compton St",
                    category = LandmarkCategory.COFFEE_SHOP,
                    latitude = 51.5137,
                    longitude = -0.1322,
                    realFact = "Established in 1887 in the heart of Soho, roasting beans behind its original Victorian wooden storefront for over a century."
                ),
                SeedLandmark(
                    name = "Admiralty Arch Ceremonial Gateway",
                    category = LandmarkCategory.MONUMENT,
                    latitude = 51.5067,
                    longitude = -0.1287,
                    realFact = "Commissioned by King Edward VII in memory of Queen Victoria, linking Trafalgar Square with The Mall."
                )
            )
        ),
        PresetCityRegion(
            name = "Tokyo — Shibuya & Yoyogi Sanctuary",
            subtitle = "Neon crossings, loyal Hachiko monument & tranquil forest shrines",
            latitude = 35.6590,
            longitude = 139.7006,
            fallbackLandmarks = listOf(
                SeedLandmark(
                    name = "Hachiko Memorial Bronze Statue",
                    category = LandmarkCategory.MONUMENT,
                    latitude = 35.6590,
                    longitude = 139.7006,
                    realFact = "Dedicated in 1934 to the Akita dog Hachiko, who waited at Shibuya Station every day for nine years for his late professor."
                ),
                SeedLandmark(
                    name = "Miyashita Rooftop Park",
                    category = LandmarkCategory.PARK,
                    latitude = 35.6620,
                    longitude = 139.7018,
                    realFact = "Tokyo's first elevated park built atop a railway line in 1966 and reimagined as a lush multi-level sky garden."
                ),
                SeedLandmark(
                    name = "Fuglen Tokyo — Tomigaya Roastery",
                    category = LandmarkCategory.COFFEE_SHOP,
                    latitude = 35.6661,
                    longitude = 139.6926,
                    realFact = "Renowned Nordic-Japanese specialty coffee house tucked on the edge of Yoyogi Park."
                ),
                SeedLandmark(
                    name = "Meiji Jingu Minami-Sando Torii Gate",
                    category = LandmarkCategory.PLAQUE,
                    latitude = 35.6702,
                    longitude = 139.7026,
                    realFact = "Towering Japanese cypress torii marking the threshold into a 170-acre forest planted by 110,000 volunteers in 1920."
                )
            )
        ),
        PresetCityRegion(
            name = "Kyoto — Higashiyama & Gion",
            subtitle = "Lantern-lit stone lanes, Yasaka Pagoda & centuries-old matcha houses",
            latitude = 34.9987,
            longitude = 135.7788,
            fallbackLandmarks = listOf(
                SeedLandmark(
                    name = "Hokan-ji Temple (Yasaka Pagoda)",
                    category = LandmarkCategory.MONUMENT,
                    latitude = 34.9985,
                    longitude = 135.7792,
                    realFact = "Five-story wooden pagoda reconstructed in 1440 by Shogun Ashikaga Yoshinori, towering over the cobblestone slopes of Higashiyama."
                ),
                SeedLandmark(
                    name = "Maruyama Park Weeping Cherry Sanctuary",
                    category = LandmarkCategory.PARK,
                    latitude = 35.0037,
                    longitude = 135.7805,
                    realFact = "Kyoto's oldest public park, opened in 1886 and designed by master landscape gardener Ogawa Jihei VII."
                ),
                SeedLandmark(
                    name = "Ninenzaka Traditional Tatami Tea & Coffee House",
                    category = LandmarkCategory.COFFEE_SHOP,
                    latitude = 34.9979,
                    longitude = 135.7804,
                    realFact = "Housed inside a preserved 100-year-old two-story Japanese machiya townhouse along the historic Ninenzaka pilgrimage lane."
                ),
                SeedLandmark(
                    name = "Yasaka Shrine Nishiromon Gate Plaque",
                    category = LandmarkCategory.PLAQUE,
                    latitude = 35.0036,
                    longitude = 135.7785,
                    realFact = "Founded in 656 AD and home to the 1,150-year-old Gion Matsuri festival originally held to ward off epidemics."
                )
            )
        )
    )

    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentDeviceLocation(): Location? {
        return try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()
            withTimeoutOrNull(6_000L) {
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token).await()
            } ?: withTimeoutOrNull(3_000L) {
                client.lastLocation.await()
            }
        } catch (e: Exception) {
            Log.w("LandmarkDiscovery", "Device location lookup failed: ${e.message}")
            null
        }
    }

    suspend fun geocodePlaceQuery(query: String): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext null
        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val url = "https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=1"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "SideQuestAndroidApp/1.0")
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val arr = JSONArray(body)
                if (arr.length() == 0) return@withContext null
                val obj = arr.getJSONObject(0)
                val lat = obj.optString("lat").toDoubleOrNull() ?: return@withContext null
                val lon = obj.optString("lon").toDoubleOrNull() ?: return@withContext null
                Pair(lat, lon)
            }
        } catch (e: Exception) {
            Log.w("LandmarkDiscovery", "Geocoding failed for '$query': ${e.message}")
            null
        }
    }

    suspend fun discoverRealLandmarksNearby(
        latitude: Double,
        longitude: Double,
        style: CharacterStyle,
        fallbackRegion: PresetCityRegion = presetRegions.first()
    ): List<RealLandmark> = withContext(Dispatchers.IO) {
        val overpassSeeds = fetchFromOverpassApi(latitude, longitude)
        val rawSeeds = if (overpassSeeds.size >= 3) {
            overpassSeeds.take(5)
        } else {
            val nearestPreset = presetRegions.minByOrNull {
                calculateDistanceMeters(latitude, longitude, it.latitude, it.longitude)
            } ?: fallbackRegion
            (overpassSeeds + nearestPreset.fallbackLandmarks)
                .distinctBy { it.name.lowercase(Locale.US) }
                .take(5)
        }

        rawSeeds.mapIndexed { index, seed ->
            val distance = calculateDistanceMeters(
                latitude,
                longitude,
                seed.latitude,
                seed.longitude
            ).coerceAtLeast((index + 1) * 140)
            buildStyledLandmark(
                index = index,
                seed = seed,
                distanceMeters = distance,
                style = style
            )
        }
    }

    private fun fetchFromOverpassApi(lat: Double, lon: Double): List<SeedLandmark> {
        return try {
            val query = """
                [out:json][timeout:10];
                (
                  node["leisure"="park"]["name"](around:1600,$lat,$lon);
                  way["leisure"="park"]["name"](around:1600,$lat,$lon);
                  node["historic"~"plaque|memorial|monument"]["name"](around:1800,$lat,$lon);
                  node["amenity"="cafe"]["name"](around:1200,$lat,$lon);
                  node["tourism"~"museum|attraction"]["name"](around:1800,$lat,$lon);
                );
                out center 25;
            """.trimIndent()

            val requestBody = "data=${URLEncoder.encode(query, "UTF-8")}"
                .toRequestBody("application/x-www-form-urlencoded".toMediaType())

            val request = Request.Builder()
                .url("https://overpass-api.de/api/interpreter")
                .header("User-Agent", "SideQuestAndroidApp/1.0")
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val raw = response.body?.string() ?: return emptyList()
                val root = JSONObject(raw)
                val elements = root.optJSONArray("elements") ?: return emptyList()

                val parks = mutableListOf<SeedLandmark>()
                val plaquesAndMonuments = mutableListOf<SeedLandmark>()
                val cafes = mutableListOf<SeedLandmark>()

                for (i in 0 until elements.length()) {
                    val el = elements.getJSONObject(i)
                    val tags = el.optJSONObject("tags") ?: continue
                    val name = tags.optString("name").trim()
                    if (name.length < 3) continue

                    val elLat = if (el.has("lat")) {
                        el.optDouble("lat")
                    } else {
                        el.optJSONObject("center")?.optDouble("lat") ?: continue
                    }
                    val elLon = if (el.has("lon")) {
                        el.optDouble("lon")
                    } else {
                        el.optJSONObject("center")?.optDouble("lon") ?: continue
                    }

                    val historic = tags.optString("historic")
                    val leisure = tags.optString("leisure")
                    val amenity = tags.optString("amenity")
                    val tourism = tags.optString("tourism")
                    val inscription = tags.optString("inscription").takeIf { it.isNotBlank() }
                    val description = tags.optString("description").takeIf { it.isNotBlank() }
                    val street = tags.optString("addr:street").takeIf { it.isNotBlank() }
                    val wikipedia = tags.optString("wikipedia").takeIf { it.isNotBlank() }

                    val factBuilder = buildString {
                        when {
                            inscription != null -> append("Plaque inscription: \"$inscription\". ")
                            description != null -> append("$description. ")
                            wikipedia != null -> append("Documented landmark ($wikipedia). ")
                        }
                        if (street != null) append("Located along $street. ")
                        append(
                            String.format(
                                Locale.US,
                                "Verified coordinates %.4f, %.4f.",
                                elLat,
                                elLon
                            )
                        )
                    }

                    when {
                        historic == "plaque" || historic == "memorial" -> {
                            plaquesAndMonuments.add(
                                SeedLandmark(name, LandmarkCategory.PLAQUE, elLat, elLon, factBuilder)
                            )
                        }
                        historic.isNotBlank() || tourism == "attraction" -> {
                            plaquesAndMonuments.add(
                                SeedLandmark(name, LandmarkCategory.MONUMENT, elLat, elLon, factBuilder)
                            )
                        }
                        tourism == "museum" -> {
                            plaquesAndMonuments.add(
                                SeedLandmark(name, LandmarkCategory.MUSEUM, elLat, elLon, factBuilder)
                            )
                        }
                        leisure == "park" -> {
                            parks.add(
                                SeedLandmark(name, LandmarkCategory.PARK, elLat, elLon, factBuilder)
                            )
                        }
                        amenity == "cafe" -> {
                            cafes.add(
                                SeedLandmark(name, LandmarkCategory.COFFEE_SHOP, elLat, elLon, factBuilder)
                            )
                        }
                    }
                }

                // Curate a balanced RPG walking route: parks + historical plaques/monuments + coffee shops
                val combined = mutableListOf<SeedLandmark>()
                parks.distinctBy { it.name }.take(2).forEach { combined.add(it) }
                plaquesAndMonuments.distinctBy { it.name }.take(2).forEach { combined.add(it) }
                cafes.distinctBy { it.name }.take(2).forEach { combined.add(it) }
                combined.distinctBy { it.name }.take(5)
            }
        } catch (e: Exception) {
            Log.w("LandmarkDiscovery", "Overpass lookup fallback triggered: ${e.message}")
            emptyList()
        }
    }

    fun buildStyledLandmark(
        index: Int,
        seed: SeedLandmark,
        distanceMeters: Int,
        style: CharacterStyle
    ): RealLandmark {
        val catLabel = seed.category.rpgLabelFor(style)
        val rpgNodeTitle = "Waypoint ${index + 1}: ${seed.name} ($catLabel)"

        val defaultLore = when (style) {
            CharacterStyle.CYBERPUNK_SCOUT ->
                "Tactical scan locked on ${seed.name}. ${seed.realFact} Sub-surface telemetry indicates an old-world data cache embedded near this ${catLabel.lowercase(Locale.US)}."
            CharacterStyle.VICTORIAN_HISTORIAN ->
                "Observe ${seed.name}, my dear colleague. ${seed.realFact} Our Royal Society ledgers record remarkable civic gatherings at this very ${catLabel.lowercase(Locale.US)}."
            CharacterStyle.ARCANE_CARTOGRAPHER ->
                "The ley-lines shimmer brightly around ${seed.name}. ${seed.realFact} Ancient cartographers marked this ${catLabel.lowercase(Locale.US)} as a nexus of living memory."
            CharacterStyle.NOIR_DETECTIVE ->
                "Rain gleams off the pavement outside ${seed.name}. ${seed.realFact} If anyone in this precinct knows the next piece of the puzzle, it's waiting right here."
        }

        val challenge = when (seed.category) {
            LandmarkCategory.PARK -> "Survey the perimeter paths and attune your compass to the central grove."
            LandmarkCategory.PLAQUE -> "Inspect the inscription closely and transcribe the hidden historical cipher."
            LandmarkCategory.COFFEE_SHOP -> "Pause at the counter, sample the local roast, and gather street intel."
            LandmarkCategory.MONUMENT -> "Circle the monument base and align your sightline with its pinnacle."
            LandmarkCategory.MUSEUM -> "Cross-reference the exterior architecture with your field chronicle."
        }

        val choices = when (seed.category) {
            LandmarkCategory.PARK -> listOf(
                "Scan the canopy & pathways for hidden signals",
                "Ask the Dungeon Master about the park's origin",
                "Collect a botanical or ley-line sample"
            )
            LandmarkCategory.PLAQUE -> listOf(
                "Decipher the historical inscription",
                "Ask the Dungeon Master who is honored here",
                "Record a rubbing into your Chronicle"
            )
            LandmarkCategory.COFFEE_SHOP -> listOf(
                "Order a restorative espresso elixir",
                "Listen in on local rumors with the Dungeon Master",
                "Trade field notes with the barista informant"
            )
            LandmarkCategory.MONUMENT, LandmarkCategory.MUSEUM -> listOf(
                "Inspect the stonework & architectural details",
                "Ask the Dungeon Master for the deeper lore",
                "Claim the waypoint resonance keystone"
            )
        }

        val (rewardName, rewardType, rarity) = when (seed.category) {
            LandmarkCategory.PARK -> Triple("Verdant Resonance Shard (${seed.name.take(22)})", "RELIC", "RARE")
            LandmarkCategory.PLAQUE -> Triple("Cipher Rubbing of ${seed.name.take(24)}", "CHRONICLE", "EPIC")
            LandmarkCategory.COFFEE_SHOP -> Triple("Artisanal Espresso Elixir (${seed.name.take(20)})", "ELIXIR", "RARE")
            LandmarkCategory.MONUMENT -> Triple("Keystone of ${seed.name.take(24)}", "KEYSTONE", "EPIC")
            LandmarkCategory.MUSEUM -> Triple("Neural Archive Core (${seed.name.take(22)})", "CYBER_DECK", "LEGENDARY")
        }

        return RealLandmark(
            id = "lm_${index}_${seed.name.hashCode()}",
            name = seed.name,
            category = seed.category,
            latitude = seed.latitude,
            longitude = seed.longitude,
            realFactOrAddress = seed.realFact,
            distanceMeters = distanceMeters,
            rpgNodeTitle = rpgNodeTitle,
            loreSnippet = defaultLore,
            questChallenge = challenge,
            actionChoices = choices,
            rewardItemName = rewardName,
            rewardItemType = rewardType,
            rewardRarity = rarity,
            xpReward = 100 + (index * 25),
            isCleared = false
        )
    }

    fun calculateDistanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Int {
        val earthRadius = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (earthRadius * c).roundToInt()
    }
}
