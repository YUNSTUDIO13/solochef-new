package com.example.solochef.ui.screens.dashboard

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.solochef.model.*
import com.example.solochef.storage.LocalFileManager
import com.example.solochef.ui.screens.library.COOKING_PROCESS_TAGS
import com.example.solochef.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(app: android.app.Application) : androidx.lifecycle.AndroidViewModel(app) {
    private val s = LocalFileManager(app)
    private val _r = MutableStateFlow<List<Recipe>>(emptyList())
    val recipes: StateFlow<List<Recipe>> = _r.asStateFlow()
    private val _st = MutableStateFlow(UserStats())
    val stats: StateFlow<UserStats> = _st.asStateFlow()
    private val _ab = MutableStateFlow<OrderBatch?>(null)
    val activeBatch: StateFlow<OrderBatch?> = _ab.asStateFlow()
    private val _sp = MutableStateFlow("全部")
    val selProcess: StateFlow<String> = _sp.asStateFlow()
    init { viewModelScope.launch { _r.value = s.getAllRecipes(); _st.value = s.getStats(); _ab.value = s.getActiveBatch() } }
    fun select(p: String) { _sp.value = p }
}

@Composable
fun DashboardScreen(
    onPlaceOrder: () -> Unit,
    onOpenBatch: () -> Unit,
    onSelectRecipe: (Recipe) -> Unit,
    onRandomOrder: (String) -> Unit,
    activeBatchOverride: OrderBatch? = null,
    vm: DashboardViewModel = viewModel()
) {
    val recipes by vm.recipes.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val vmA = vm.activeBatch.collectAsStateWithLifecycle()
    val activeBatch = activeBatchOverride ?: vmA.value
    val selProcess by vm.selProcess.collectAsStateWithLifecycle()
    var randomResult by remember { mutableStateOf<Recipe?>(null) }
    val scope = rememberCoroutineScope()

    val dynProcesses = remember(recipes) {
        val s = recipes.flatMap { it.tags }.filter { it in COOKING_PROCESS_TAGS }.toSet()
        listOf("全部") + s.sortedBy { COOKING_PROCESS_TAGS.indexOf(it) }
    }
    val featured = remember(recipes, selProcess) {
        recipes.filter { r ->
            if (selProcess == "全部") true  // show all recipes, not just cooking-process-tagged
            else r.tags.contains(selProcess)
        }
            .sortedWith(compareByDescending<Recipe> { it.is_featured }.thenBy { it.name })
    }

    Column(Modifier.fillMaxSize().background(Sage100).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(24.dp))
        Text("独厨SoloChef", fontSize = 40.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.05).sp, color = Sage900)
        Text("你的精品线上厨房", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = Sage500, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(16.dp))

        // Task Module (no FireStatusHeader per web spec)

        // Task Module
        val batch = activeBatch
        if (batch != null) {
            Surface(modifier = Modifier.fillMaxWidth().height(160.dp), shape = RoundedCornerShape(32.dp), color = Sage900) {
                Box(Modifier.fillMaxSize()) {
                    // Decorative background element
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White.copy(0.03f), modifier = Modifier.size(160.dp).align(Alignment.BottomEnd).offset(x = 40.dp, y = 40.dp).graphicsLayer(rotationZ = -12f))
                Column(Modifier.padding(20.dp)) {
                    Row(Modifier.fillMaxWidth().clickable { onOpenBatch() }, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(modifier = Modifier.size(32.dp), shape = CircleShape, color = White10) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp)) } }
                            Spacer(Modifier.width(8.dp))
                            Text(if (batch.status == BatchStatus.Picking) "物料筹备中" else "烹饪履约中", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                        // Count badge — circle, right-aligned
                        Surface(modifier = Modifier.size(24.dp), shape = CircleShape, color = Amber400) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("${batch.recipeIds.size}", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Sage900) }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(batch.recipeIds.filter { it !in batch.completedRecipeIds }) { rid ->
                            val r = recipes.find { it.id == rid }; if (r != null) {
                                Box(Modifier.height(80.dp).aspectRatio(1f).clip(RoundedCornerShape(16.dp)).background(White10).border(1.dp, White10, RoundedCornerShape(16.dp)).clickable { onSelectRecipe(r) }) {
                                    AsyncImage(r.cover_image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Black60))))
                                    Text(r.name, Modifier.align(Alignment.BottomStart).padding(6.dp), fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
                } // close Box
            }
        } else {
            Row(Modifier.fillMaxWidth().height(160.dp)) {
                Box(modifier = Modifier.weight(2f).fillMaxHeight().clip(RoundedCornerShape(32.dp)).background(Color.White).dashedBorder(color = Sage200).clickable { onPlaceOrder() }, contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                        Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(14.dp), color = Sage50) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Sage300, modifier = Modifier.size(20.dp)) } }
                        Spacer(Modifier.height(6.dp))
                        Text("精品厨房空闲中", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Sage900)
                        Text("开启今日美味", fontSize = 8.sp, color = Sage400)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Surface(modifier = Modifier.weight(1f).fillMaxHeight().clickable {
                    val w = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
                    val c = recipes.filter { it.is_featured || (it.last_cooked_at?.toLongOrNull() ?: 0) < w }
                    if (c.isNotEmpty()) randomResult = c.random()
                }, shape = RoundedCornerShape(32.dp), color = if (randomResult != null) Color.White else Indigo500) {
                    randomResult?.let { rr ->
                        Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
                            AsyncImage(rr.cover_image, null, Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(rr.name, fontSize = 9.sp, fontWeight = FontWeight.Black, color = Sage900, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Surface(onClick = { onRandomOrder(rr.id); randomResult = null }, modifier = Modifier.padding(top = 6.dp), shape = RoundedCornerShape(50), color = Sage900) {
                                    Box(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) { Text("下单", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.White) }
                                }
                            }
                        }
                    } ?: Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Indigo500, Sage900), start = androidx.compose.ui.geometry.Offset(0f, 0f), end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY))), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White.copy(0.06f), modifier = Modifier.size(120.dp).align(Alignment.BottomEnd).offset(x = 30.dp, y = 30.dp).graphicsLayer(rotationZ = -15f))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White.copy(0.8f), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("今天吃啥？", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Text("随机匹配", fontSize = 7.sp, color = Color.White.copy(0.4f))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("推荐菜 (Featured)", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage500)
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Sage400, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(dynProcesses) { tag ->
                val sel = tag == selProcess
                Surface(onClick = { vm.select(tag) }, modifier = Modifier, shape = RoundedCornerShape(50), color = if (sel) Sage900 else Color.White, border = BorderStroke(1.dp, if (sel) Sage900 else Sage200)) {
                    Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) { Text(tag, fontSize = 10.sp, fontWeight = FontWeight.Black, color = if (sel) Color.White else Sage400) }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        if (featured.isEmpty()) {
            Surface(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), shape = RoundedCornerShape(32.dp), color = Color.White, border = BorderStroke(1.dp, Sage100)) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("暂无推荐菜谱", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage300) }
            }
        } else {
            val g = featured.take(4)
            for (i in g.indices step 2) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Card(recipe = g[i], onClick = onSelectRecipe, modifier = Modifier.weight(1f))
                    if (i + 1 < g.size) Card(recipe = g[i + 1], onClick = onSelectRecipe, modifier = Modifier.weight(1f))
                    else Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

private fun Modifier.dashedBorder(strokeWidth: Float = 2f, dashWidth: Float = 10f, dashGap: Float = 6f, cornerRadius: Float = 32f, color: Color = Sage800) = this.drawBehind {
    val sw = strokeWidth.dp.toPx()
    val cr = cornerRadius.dp.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(sw / 2, sw / 2),
        size = Size(size.width - sw, size.height - sw),
        cornerRadius = CornerRadius(cr, cr),
        style = Stroke(width = sw, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashWidth, dashGap), 0f))
    )
}

@Composable
private fun Card(recipe: Recipe, onClick: (Recipe) -> Unit, modifier: Modifier) {
    Box(modifier.aspectRatio(1f).clip(RoundedCornerShape(32.dp)).background(Color.White).border(1.dp, Sage200, RoundedCornerShape(32.dp)).clickable { onClick(recipe) }) {
        AsyncImage(recipe.cover_image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Black60))))
        Text(recipe.name, Modifier.align(Alignment.BottomStart).padding(12.dp), fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
    }
}

@Composable
fun FireStatusView(stats: UserStats) {
    val diffHours = (System.currentTimeMillis() - (stats.last_ignition.toLongOrNull() ?: 0L)) / 3600000.0
    val state = when { diffHours >= 24 -> "extinguished"; stats.streak >= 3 && diffHours < 12 -> "blazing"; diffHours < 12 -> "embers"; else -> "extinguished" }
    val c = when { stats.streak <= 3 -> "cyan"; stats.streak <= 10 -> "gold"; else -> "purple" }
    val label = when { stats.streak >= 100 -> "生活掌控大师"; stats.streak >= 30 -> "外卖行业死对头"; stats.streak >= 7 -> "烟火气守护者"; else -> "厨房新贵" }
    val bg = when (c) { "cyan" -> Color(0xFFE0F2FE); "gold" -> Color(0xFFFEF3C7); else -> Color(0xFFF3E8FF) }
    val fg = when (c) { "cyan" -> Color(0xFF0284C7); "gold" -> Color(0xFFB45309); else -> Color(0xFF7C3AED) }
    val ic = if (state == "extinguished") Sage300 else fg
    val inf = rememberInfiniteTransition()
    val scl by inf.animateFloat(1f, if (state == "blazing") 1.1f else 1.05f, infiniteRepeatable(tween(1000), RepeatMode.Reverse))

    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(32.dp), color = Sage200.copy(0.6f), border = BorderStroke(1.dp, Sage300)) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(64.dp).graphicsLayer(scaleX = scl, scaleY = scl), shape = RoundedCornerShape(16.dp), color = if (state == "extinguished") Sage300 else bg) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = ic, modifier = Modifier.size(32.dp)) }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("已连续点火 ${stats.streak} 天", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Sage900)
                Surface(modifier = Modifier.padding(top = 4.dp), shape = RoundedCornerShape(50), color = bg) { Text(label, Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Black, color = fg) }
                Spacer(Modifier.height(4.dp))
                Text(if (state == "extinguished") "重新开启生活掌控权" else "今晚准备做什么？", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = Sage800)
            }
        }
    }
}
