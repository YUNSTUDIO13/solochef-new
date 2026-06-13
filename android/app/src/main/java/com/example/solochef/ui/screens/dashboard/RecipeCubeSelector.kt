package com.example.solochef.ui.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.example.solochef.model.*
import com.example.solochef.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun RecipeCubeSelector(
    recipes: List<Recipe>,
    onClose: () -> Unit,
    onViewRecipe: (Recipe) -> Unit,
    onCookRecipe: (Recipe) -> Unit
) {
    val cutoff = System.currentTimeMillis() - 72L * 3600 * 1000
    val pool = remember(recipes) { recipes.filter { (it.last_cooked_at?.toLongOrNull() ?: 0) < cutoff } }

    var shuffleKey by remember { mutableIntStateOf(0) }
    val gridItems = remember(pool, shuffleKey) {
        if (pool.isEmpty()) emptyList<Recipe>()
        else List(9) { pool[it % pool.size] }.shuffled()
    }
    if (gridItems.size < 9) { LaunchedEffect(Unit) { onClose() }; return }

    var isScanning by remember { mutableStateOf(false) }
    var highlightIdx by remember { mutableIntStateOf(-1) }
    var finalIdx by remember { mutableIntStateOf(-1) }
    var showDialog by remember { mutableStateOf(false) }
    var selectedRecipe by remember { mutableStateOf<Recipe?>(null) }

    fun startScan() { if (!isScanning) { isScanning = true; highlightIdx = -1; finalIdx = -1; showDialog = false; selectedRecipe = null } }

    LaunchedEffect(isScanning) {
        if (!isScanning) return@LaunchedEffect
        val target = (0..8).random()
        val trace = intArrayOf(0, 1, 2, 5, 8, 7, 6, 3)
        val steps = 20 + (0..6).random()
        var pathIdx = 0; var speed = 40L
        for (i in 0 until steps) {
            highlightIdx = trace[pathIdx]; pathIdx = (pathIdx + 1) % trace.size
            speed = when { i >= steps - 4 -> 500L; i >= steps - 8 -> 250L; i >= steps - 14 -> 100L; else -> 40L }
            delay(speed)
        }
        finalIdx = target; highlightIdx = target
        delay(200)
        selectedRecipe = gridItems[target]; showDialog = true
        isScanning = false
    }

    var triggerReshuffle by remember { mutableStateOf(false) }
    LaunchedEffect(triggerReshuffle) { if (triggerReshuffle) { delay(350); triggerReshuffle = false; shuffleKey++ } }
    LaunchedEffect(shuffleKey) { if (shuffleKey > 0) { delay(200); startScan() } }

    val cell = 100.dp; val g = 8.dp

    Box(Modifier.fillMaxSize().background(Color.White).clickable(MutableInteractionSource(), null) { }) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(top = 24.dp, bottom = 100.dp)) {
            Text("今天到底吃点啥？", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Sage900, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Text("让食运做出最好的安排", fontSize = 11.sp, color = Sage400, letterSpacing = 2.sp, modifier = Modifier.fillMaxWidth().padding(top = 4.dp), textAlign = TextAlign.Center)

            Spacer(Modifier.height(24.dp))

            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(g)) {
                    for (row in 0..2) {
                        Row(horizontalArrangement = Arrangement.spacedBy(g)) {
                            for (col in 0..2) {
                                val i = row * 3 + col; val r = gridItems[i]; val hl = highlightIdx == i; val fin = finalIdx == i
                                Box(Modifier.size(cell).then(if (fin) Modifier.border(2.5.dp, Color(0xFFFF7043), RoundedCornerShape(16.dp)) else if (hl) Modifier.border(2.5.dp, GreenPlay, RoundedCornerShape(16.dp)) else Modifier.border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))).clip(RoundedCornerShape(16.dp)).background(Color.White)) {
                                    AsyncImage(r.cover_image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)))))
                                    Text(r.name, Modifier.align(Alignment.BottomStart).padding(8.dp), fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (hl && !fin) Box(Modifier.fillMaxSize().background(GreenPlay.copy(alpha = 0.12f)))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(onClick = { startScan() }, enabled = !isScanning, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(50), color = if (isScanning) Color(0xFFCBD5E1) else Color(0xFF2D4A3A)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(if (isScanning) "转动中..." else "食来运转", fontSize = 14.sp, fontWeight = FontWeight.Black, color = if (isScanning) Color(0xFF64748B) else Color.White) }
                }
                Surface(onClick = onClose, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(50), color = Color.Transparent, border = BorderStroke(1.5.dp, Sage200)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("返回首页", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Sage500) }
                }
            }
        }

        if (showDialog && selectedRecipe != null) {
            val r = selectedRecipe!!
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.65f)).clickable(MutableInteractionSource(), null) { }, contentAlignment = Alignment.Center) {
                Surface(modifier = Modifier.padding(32.dp).fillMaxWidth(), shape = RoundedCornerShape(32.dp), color = Color(0xFF1C1917), shadowElevation = 24.dp) {
                    Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("命运的食光之轮选中了【${r.name}】，今日犒劳就它了！", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFCD34D), textAlign = TextAlign.Center)
                        Spacer(Modifier.height(14.dp))
                        Surface(modifier = Modifier.size(120.dp), shape = RoundedCornerShape(24.dp), shadowElevation = 8.dp) { AsyncImage(r.cover_image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                        Spacer(Modifier.height(12.dp))
                        Text(r.name, fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center)
                        if (r.tags.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                r.tags.take(3).forEach { t ->
                                    Surface(shape = RoundedCornerShape(8.dp), color = Color.White.copy(alpha = 0.08f)) {
                                        Text("#$t", Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 9.sp, color = Color.White.copy(alpha = 0.6f))
                                    }
                                }
                            }
                        }
                        if (r.cost > 0) {
                            Spacer(Modifier.height(4.dp))
                            Text("¥${"%.1f".format(r.cost)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA8A29E))
                        }
                        Spacer(Modifier.height(20.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(onClick = { showDialog = false; selectedRecipe = null; triggerReshuffle = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.08f)) { Box(Modifier.padding(vertical = 14.dp), contentAlignment = Alignment.Center) { Text("再来一次", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.6f)) } }
                            Surface(onClick = { onCookRecipe(r) }, modifier = Modifier.weight(1.2f), shape = RoundedCornerShape(50), color = GreenPlay) { Box(Modifier.padding(vertical = 14.dp), contentAlignment = Alignment.Center) { Text("就吃这个", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White) } }
                        }
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { showDialog = false }) { Text("关闭", fontSize = 12.sp, color = Color.White.copy(alpha = 0.35f)) }
                    }
                }
            }
        }
    }
}
