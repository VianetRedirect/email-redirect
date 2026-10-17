package com.vianet.workoutlog.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.vianet.workoutlog.domain.Milestone
import com.vianet.workoutlog.domain.formatMilestoneValue
import kotlin.random.Random

/** Full-screen "milestone unlocked" moment with falling confetti. Pages through several. */
@Composable
fun CelebrationDialog(
    milestones: List<Milestone>,
    onShare: (Milestone) -> Unit,
    onDismiss: () -> Unit,
) {
    if (milestones.isEmpty()) return
    var index by remember { mutableIntStateOf(0) }
    val milestone = milestones[index.coerceIn(0, milestones.lastIndex)]
    val isLast = index >= milestones.lastIndex

    Dialog(onDismissRequest = onDismiss) {
        Box {
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "Milestone unlocked",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(milestone.emoji, fontSize = 72.sp, textAlign = TextAlign.Center)
                    Text(milestone.title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                    Text(
                        milestone.detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "${milestone.type.label}: ${formatMilestoneValue(milestone.type, milestone.threshold)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (milestones.size > 1) {
                        Text(
                            "${index + 1} of ${milestones.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onShare(milestone) }, modifier = Modifier.weight(1f)) { Text("Share") }
                        Button(
                            onClick = { if (isLast) onDismiss() else index++ },
                            modifier = Modifier.weight(1f),
                        ) { Text(if (isLast) "Nice" else "Next") }
                    }
                }
            }
            Confetti(modifier = Modifier.matchParentSize())
        }
    }
}

private val confettiColors = listOf(
    Color(0xFF2EC4B6), Color(0xFFFFB74D), Color(0xFFFF6B6B), Color(0xFF8E7CFF), Color(0xFF7CFF8E), Color(0xFFFFE066),
)

private class Flake(val x: Float, val phase: Float, val speed: Float, val size: Float, val color: Color, val wobble: Float)

@Composable
private fun Confetti(modifier: Modifier = Modifier) {
    val flakes = remember {
        val random = Random(42)
        List(70) {
            Flake(
                x = random.nextFloat(),
                phase = random.nextFloat(),
                speed = 0.6f + random.nextFloat() * 0.8f,
                size = 6f + random.nextFloat() * 10f,
                color = confettiColors[random.nextInt(confettiColors.size)],
                wobble = 10f + random.nextFloat() * 30f,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "confetti")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 4000, easing = LinearEasing)),
        label = "fall",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        flakes.forEach { f ->
            val progress = ((t * f.speed) + f.phase) % 1f
            val y = progress * (h + 40f) - 20f
            val x = f.x * w + kotlin.math.sin((progress * 6.28f * 2f) + f.phase * 6.28f) * f.wobble
            drawRect(
                color = f.color.copy(alpha = 0.9f),
                topLeft = Offset(x, y),
                size = androidx.compose.ui.geometry.Size(f.size, f.size * 0.6f),
            )
        }
    }
}
