package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.ArcaneEmerald
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.GuildGold
import com.example.ui.theme.MidnightAbyss
import com.example.ui.theme.ObsidianSurface
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SideQuestSplashScreen(
    onSplashFinished: () -> Unit
) {
    val stages = listOf(
        "Attuning GPS Compass & Real-World Ley-Lines...",
        "Syncing OpenStreetMap Parks, Plaques & Cafes...",
        "Waking Gemini Audio Dungeon Master..."
    )
    var stageIndex by remember { mutableIntStateOf(0) }
    var targetProgress by remember { mutableFloatStateOf(0.25f) }

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
        label = "splash_progress"
    )

    LaunchedEffect(Unit) {
        delay(380L)
        stageIndex = 1
        targetProgress = 0.68f
        delay(380L)
        stageIndex = 2
        targetProgress = 1.0f
        delay(380L)
        onSplashFinished()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "splash_compass")
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_rotation"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        ObsidianSurface,
                        MidnightAbyss
                    )
                )
            )
            .clickable { onSplashFinished() }
            .testTag("splash_screen_root"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .widthIn(max = 420.dp)
                .padding(28.dp)
        ) {
            Box(
                modifier = Modifier.size(216.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.minDimension / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    // Outer degree ticks
                    for (deg in 0 until 360 step 15) {
                        val rad = Math.toRadians(deg.toDouble())
                        val isCardinal = deg % 90 == 0
                        val innerR = radius * (if (isCardinal) 0.88f else 0.93f)
                        val outerR = radius * 0.98f
                        drawLine(
                            color = if (isCardinal) GuildGold else GuildGold.copy(alpha = 0.35f),
                            start = Offset(
                                x = center.x + (innerR * cos(rad)).toFloat(),
                                y = center.y + (innerR * sin(rad)).toFloat()
                            ),
                            end = Offset(
                                x = center.x + (outerR * cos(rad)).toFloat(),
                                y = center.y + (outerR * sin(rad)).toFloat()
                            ),
                            strokeWidth = if (isCardinal) 4f else 2f
                        )
                    }

                    rotate(ringRotation) {
                        drawCircle(
                            color = GuildGold.copy(alpha = 0.5f),
                            radius = radius * 0.84f,
                            style = Stroke(
                                width = 3f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f), 0f)
                            )
                        )
                    }
                    rotate(-ringRotation * 0.75f) {
                        drawCircle(
                            color = CyberCyan.copy(alpha = 0.42f),
                            radius = radius * 0.72f,
                            style = Stroke(
                                width = 2.5f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 14f), 0f)
                            )
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .size(134.dp)
                        .scale(pulseScale)
                        .clip(CircleShape),
                    shape = CircleShape,
                    border = BorderStroke(3.dp, GuildGold),
                    color = MidnightAbyss
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_launcher_fg_1794444305032),
                        contentDescription = "SideQuest Compass Emblem",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                color = GuildGold.copy(alpha = 0.16f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, GuildGold.copy(alpha = 0.6f))
            ) {
                Text(
                    text = "SOLOPRONO • REAL-WORLD AUDIO RPG",
                    style = MaterialTheme.typography.labelMedium,
                    color = GuildGold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stages[stageIndex],
                style = MaterialTheme.typography.bodyMedium,
                color = CyberCyan,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = GuildGold,
                trackColor = ObsidianSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Tap anywhere to enter immediately",
                style = MaterialTheme.typography.labelMedium,
                color = ArcaneEmerald.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )
        }
    }
}
