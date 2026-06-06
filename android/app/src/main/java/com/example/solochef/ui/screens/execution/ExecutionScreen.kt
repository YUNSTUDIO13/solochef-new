package com.example.solochef.ui.screens.execution

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.solochef.model.Recipe
import com.example.solochef.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun ExecutionScreen(
    recipe: Recipe,
    isLazy: Boolean,
    onComplete: (Recipe) -> Unit
) {
    var activeStepIndex by remember { mutableStateOf(0) }
    var timeLeft by remember { mutableStateOf(recipe.timeline.firstOrNull()?.duration ?: 0) }
    var isPaused by remember { mutableStateOf(false) }
    var fullScreenImage by remember { mutableStateOf<String?>(null) }

    // WakeLock — keep screen on during cooking
    val context = androidx.compose.ui.platform.LocalContext.current
    DisposableEffect(Unit) {
        val pm = context.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
        val wakeLock = pm.newWakeLock(
            android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK or android.os.PowerManager.ON_AFTER_RELEASE,
            "SoloChef:Execution"
        )
        try { wakeLock.acquire(10 * 60 * 1000L) } catch (_: Exception) {}
        onDispose { try { if (wakeLock.isHeld) wakeLock.release() } catch (_: Exception) {} }
    }

    val currentStep = recipe.timeline.getOrNull(activeStepIndex)

    // Timer: countdown each second
    LaunchedEffect(activeStepIndex, isPaused) {
        if (currentStep == null) return@LaunchedEffect
        while (timeLeft > 0 && !isPaused) {
            delay(1000L)
            timeLeft--
        }
        if (timeLeft <= 0 && !isPaused) {
            if (activeStepIndex < recipe.timeline.size - 1) {
                activeStepIndex++  // next LaunchedEffect(activeStepIndex) will set timeLeft
            } else {
                onComplete(recipe)
            }
        }
    }

    // Reset timeLeft whenever step changes
    LaunchedEffect(activeStepIndex) {
        timeLeft = recipe.timeline.getOrNull(activeStepIndex)?.duration ?: 0
    }

    val totalRemaining = recipe.timeline.drop(activeStepIndex).sumOf { it.duration } - (currentStep?.duration ?: 0) + timeLeft

    fun formatTime(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return "$m:${s.toString().padStart(2, '0')}"
    }

    Box(Modifier.fillMaxSize().background(Sage100)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(24.dp))

            // Header — compact
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(recipe.name, fontSize = 26.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.05).sp, color = Sage900)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier, shape = RoundedCornerShape(4.dp), color = Sage900) { Text(if (isLazy) "懒人模式" else "标准模式", Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 2.sp) }
                        Spacer(Modifier.width(8.dp))
                        Text("${recipe.energy_level.name} 精力", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Sage500, letterSpacing = 2.sp)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(formatTime(totalRemaining), fontSize = 26.sp, fontWeight = FontWeight.Black, color = Sage900)
                    Text("剩余总时长", fontSize = 9.sp, color = Sage500, letterSpacing = 2.sp)
                }
            }

            // Steps
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                recipe.timeline.forEachIndexed { index, step ->
                    val isActive = index == activeStepIndex
                    val isPast = index < activeStepIndex
                    if (isPast) return@forEachIndexed

                    Surface(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        RoundedCornerShape(40.dp),
                        color = if (isActive) Color.White else Color.White.copy(0.6f),
                        border = BorderStroke(1.dp, if (step.type == "waiting") Sage400 else Sage900),
                        shadowElevation = if (isActive) 8.dp else 1.dp
                    ) {
                        Column(modifier = if (!isActive) Modifier.graphicsLayer(scaleX = 0.95f, scaleY = 0.95f, alpha = 0.4f) else Modifier.graphicsLayer(scaleX = 1f, scaleY = 1f, alpha = 1f)) {
                            // Step images — show all in horizontal scroll
                            if (!step.images.isNullOrEmpty()) {
                                LazyRow(Modifier.fillMaxWidth().height(180.dp)) {
                                    items(step.images) { img ->
                                        AsyncImage(img, null, Modifier.fillMaxWidth().height(180.dp).clickable { fullScreenImage = img }, contentScale = ContentScale.Crop)
                                    }
                                }
                            }
                            Column(Modifier.padding(24.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(modifier = Modifier.size(28.dp), shape = CircleShape, color = if (step.type == "waiting") Sage100 else Sage900) {
                                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(if (step.type == "waiting") Icons.Default.Timer else Icons.Default.Bolt, contentDescription = null, tint = if (step.type == "waiting") Sage500 else Color.White, modifier = Modifier.size(14.dp)) }
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Text("STEP ${index + 1} • ${if (step.type == "waiting") "后台等待" else "手动执行"}", fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage500)
                                    }
                                    if (isActive && step.duration > 0) {
                                        Text(formatTime(timeLeft), fontSize = 28.sp, fontWeight = FontWeight.Black, color = Sage900)
                                    }
                                }

                                Spacer(Modifier.height(8.dp))
                                Text(
                                    if (isLazy && isActive) "建议点外卖或备餐" else step.content,
                                    fontSize = if (isActive) 26.sp else 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Sage900,
                                    lineHeight = 32.sp
                                )

                                // Sub-tasks
                                if (isActive && step.sub_tasks.isNotEmpty()) {
                                    Spacer(Modifier.height(8.dp))
                                    Divider(color = Sage50)
                                    Text("后续并行任务", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage500)
                                    step.sub_tasks.forEach { sub ->
                                        Surface(Modifier.fillMaxWidth().padding(top = 6.dp), RoundedCornerShape(24.dp), Sage50) {
                                            Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Row {
                                                    Box(Modifier.size(6.dp).background(Sage900, CircleShape))
                                                    Spacer(Modifier.width(16.dp))
                                                    Text(sub.content, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Sage900)
                                                }
                                                Text(formatTime(sub.duration), fontSize = 18.sp, fontWeight = FontWeight.Black, color = Sage500)
                                            }
                                        }
                                    }
                                }

                                // Progress bar
                                if (isActive && step.duration > 0) {
                                    Spacer(Modifier.height(16.dp))
                                    Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Sage100)) {
                                        Box(Modifier.fillMaxHeight().fillMaxWidth(timeLeft.toFloat() / step.duration).background(Sage900))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Controls
        Surface(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp), color = Sage100) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Surface(onClick = { isPaused = !isPaused }, modifier = Modifier.weight(1f).height(96.dp), shape = RoundedCornerShape(32.dp), color = Color.White, border = BorderStroke(1.dp, Sage200)) {
                        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null, tint = Sage900, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.width(16.dp))
                            Text(if (isPaused) "继续" else "暂停", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Sage900)
                        }
                    }
                    Surface(onClick = {
                        if (activeStepIndex < recipe.timeline.size - 1) {
                            activeStepIndex++
                        } else {
                            onComplete(recipe)
                        }
                    }, modifier = Modifier.width(112.dp).height(96.dp), shape = RoundedCornerShape(32.dp), color = Sage900) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.SkipNext, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp)) }
                    }
                }
            }
        }

        // Full-screen image
        AnimatedVisibility(fullScreenImage != null) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(0.95f)).clickable { fullScreenImage = null }, contentAlignment = Alignment.Center) {
                fullScreenImage?.let { AsyncImage(it, null, Modifier.fillMaxWidth().padding(24.dp), contentScale = ContentScale.Fit) }
                Surface(onClick = { fullScreenImage = null }, Modifier.align(Alignment.TopEnd).padding(32.dp), color = Color.Transparent) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White.copy(0.5f), modifier = Modifier.size(32.dp))
                }
            }
        }

        // Near-completion pulse
        AnimatedVisibility(timeLeft in 1..3 && !isPaused) {
            Box(Modifier.fillMaxSize().border(20.dp, Color(0xFFEAB308).copy(0.2f)))
        }
    }
}
