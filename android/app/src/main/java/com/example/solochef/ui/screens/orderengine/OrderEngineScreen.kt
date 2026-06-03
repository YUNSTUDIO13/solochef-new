package com.example.solochef.ui.screens.orderengine

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.solochef.model.EnergyLevel
import com.example.solochef.model.Recipe
import com.example.solochef.storage.LocalFileManager
import com.example.solochef.ui.screens.library.*
import com.example.solochef.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OrderEngineViewModel(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {
    private val storage = LocalFileManager(application)
    private val _recipes = MutableStateFlow<List<Recipe>>(emptyList())
    val recipes: StateFlow<List<Recipe>> = _recipes.asStateFlow()
    private val _cart = MutableStateFlow<List<String>>(emptyList())
    val cart: StateFlow<List<String>> = _cart.asStateFlow()
    private val _category = MutableStateFlow("all")
    val category: StateFlow<String> = _category.asStateFlow()

    init { viewModelScope.launch { _recipes.value = storage.getAllRecipes() } }
    fun setCategory(cat: String) { _category.value = cat }
    fun addToCart(id: String) { _cart.value = _cart.value + id }
    fun removeFromCart(id: String) { val idx = _cart.value.indexOf(id); if (idx >= 0) _cart.value = _cart.value.toMutableList().also { it.removeAt(idx) } }
    fun getCount(id: String) = _cart.value.count { it == id }
}

val ALL_ORDER = COOKING_PROCESS_TAGS + CUISINE_TAGS

@Composable
fun OrderEngineScreen(
    onConfirm: (List<String>) -> Unit,
    onCancel: () -> Unit,
    activeBatch: com.example.solochef.model.OrderBatch? = null,
    viewModel: OrderEngineViewModel = viewModel()
) {
    val recipes by viewModel.recipes.collectAsStateWithLifecycle()
    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val activeCategory by viewModel.category.collectAsStateWithLifecycle()
    var showConflictDialog by remember { mutableStateOf(false) }

    fun onGeneratePlan() {
        if (cart.isEmpty()) return
        // Check for conflict: existing batch in processing state
        if (activeBatch != null && activeBatch.status != com.example.solochef.model.BatchStatus.Picking) {
            showConflictDialog = true
        } else {
            onConfirm(cart)
        }
    }

    val categories = listOf("all", "主推菜") + COOKING_PROCESS_TAGS + CUISINE_TAGS

    val filtered = remember(recipes, activeCategory) {
        recipes.filter { r ->
            when (activeCategory) { "all" -> true; "主推菜" -> r.is_featured; else -> r.tags.contains(activeCategory) }
        }.sortedBy { r ->
            if (activeCategory == "all") r.tags.mapNotNull { ALL_ORDER.indexOf(it) }.filter { it >= 0 }.minOrNull() ?: Int.MAX_VALUE else 0
        }
    }

    Box(Modifier.fillMaxSize().background(Color.White)) {
        // ── Header ──
        Surface(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().height(80.dp),
            color = Color.White.copy(0.85f), shadowElevation = 0.dp
        ) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCancel, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Sage400, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text("独厨点单", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Sage900)
                Spacer(Modifier.weight(1f))
                AnimatedVisibility(visible = cart.isNotEmpty()) {
                    Surface(modifier = Modifier.clip(RoundedCornerShape(16.dp)), color = Sage900, shadowElevation = 4.dp) {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("${cart.size}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }
                }
            }
        }

        // ── Body: sidebar + recipe list ──
        Row(Modifier.fillMaxSize().padding(top = 80.dp)) {
            // Sidebar
            Column(
                Modifier.width(80.dp).fillMaxHeight().background(Sage50).verticalScroll(rememberScrollState())
            ) {
                categories.forEach { cat ->
                    val sel = cat == activeCategory
                    Box(
                        Modifier.fillMaxWidth()
                            .then(if (sel) Modifier.background(Color.White) else Modifier)
                            .clickable { viewModel.setCategory(cat) }
                    ) {
                        if (sel) Box(Modifier.width(4.dp).fillMaxHeight().background(Sage900).align(Alignment.CenterStart))
                        Text(
                            if (cat == "all") "全部" else cat,
                            Modifier.fillMaxWidth().padding(vertical = 20.dp),
                            fontSize = 9.sp, fontWeight = FontWeight.Black,
                            color = if (sel) Sage900 else Sage400,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Recipe list
            LazyColumn(
                Modifier.fillMaxSize().padding(12.dp).padding(bottom = if (cart.isNotEmpty()) 120.dp else 0.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered, key = { it.id }) { recipe ->
                    RecipeCard(
                        recipe = recipe,
                        viewModel = viewModel
                    )
                }
                if (filtered.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(vertical = 80.dp), contentAlignment = Alignment.Center) {
                            Text("该分类下暂无菜谱", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage300)
                        }
                    }
                }
            }
        }

        // ── Footer: absolute bottom (like Web "absolute bottom-0") ──
        androidx.compose.animation.AnimatedVisibility(
            visible = cart.isNotEmpty(),
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) {
            Surface(modifier = Modifier.fillMaxWidth().padding(12.dp), shape = RoundedCornerShape(28.dp), color = Sage900, shadowElevation = 12.dp) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("待采订单 (Selected)", fontSize = 9.sp, fontWeight = FontWeight.Black, color = White40, letterSpacing = 2.sp)
                        Text("${cart.size} 份菜品", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                    Spacer(Modifier.weight(1f))
                    Surface(onClick = { onGeneratePlan() }, shape = RoundedCornerShape(50), color = Color.White) {
                        Row(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("生成今日计划", fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage900, maxLines = 1)
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Sage900, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        // ── Conflict Dialog ──
        if (showConflictDialog) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(0.4f)).clickable { }) {
                Surface(modifier = Modifier.align(Alignment.Center).padding(24.dp), shape = RoundedCornerShape(40.dp), color = Color.White, border = BorderStroke(1.dp, Sage200)) {
                    Column(Modifier.padding(32.dp)) {
                        Surface(modifier = Modifier.size(64.dp), shape = RoundedCornerShape(16.dp), color = Color(0xFFFEF3C7)) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(32.dp)) } }
                        Spacer(Modifier.height(24.dp))
                        Text("发现冲突计划", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Sage900)
                        Spacer(Modifier.height(8.dp))
                        Text("当前已有正在履约中的独厨订单，请确认是否覆盖现有计划？", fontSize = 13.sp, color = Sage500)
                        Spacer(Modifier.height(32.dp))
                        Surface(onClick = { showConflictDialog = false; onConfirm(cart) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Sage900) {
                            Text("确认覆盖", Modifier.fillMaxWidth().padding(16.dp), textAlign = TextAlign.Center, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Color.White)
                        }
                        Spacer(Modifier.height(12.dp))
                        Surface(onClick = { showConflictDialog = false }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Sage50) {
                            Text("取消", Modifier.fillMaxWidth().padding(16.dp), textAlign = TextAlign.Center, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage400)
                        }
                    }
                }
            }
        }
    }
}

// ── Recipe Card ──
@Composable
private fun RecipeCard(
    recipe: Recipe,
    viewModel: OrderEngineViewModel
) {
    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val count = cart.count { it == recipe.id }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Sage200),
        shadowElevation = 1.dp
    ) {
        Row(Modifier.padding(8.dp).padding(end = 12.dp)) {
            // Thumbnail — 80dp square
            Box(Modifier.size(80.dp).clip(RoundedCornerShape(12.dp))) {
                AsyncImage(recipe.cover_image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f).padding(vertical = 2.dp)) {
                // Name
                Text(recipe.name, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Sage900, maxLines = 1, overflow = TextOverflow.Ellipsis)

                // Energy dot + label
                Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(6.dp).background(
                            when (recipe.energy_level) {
                                EnergyLevel.High -> Red500; EnergyLevel.Mid -> Amber400; else -> Color(0xFF34D399)
                            }, CircleShape
                        )
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(recipe.energy_level.name, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Sage400)
                }

                Spacer(Modifier.weight(1f))

                // Bottom row: time + cart controls
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("~25 min", fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage400)

                    if (count > 0) {
                        // Selected state: pill counter
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Sage50,
                            border = BorderStroke(1.dp, Sage100)
                        ) {
                            Row(Modifier.padding(horizontal = 2.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    onClick = { viewModel.removeFromCart(recipe.id) },
                                    modifier = Modifier.size(28.dp),
                                    shape = CircleShape,
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Sage200)
                                ) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Remove, contentDescription = null, tint = Sage800, modifier = Modifier.size(14.dp))
                                    }
                                }
                                Text("$count", Modifier.padding(horizontal = 8.dp), fontSize = 12.sp, fontWeight = FontWeight.Black, color = Sage900)
                                Surface(
                                    onClick = { viewModel.addToCart(recipe.id) },
                                    modifier = Modifier.size(28.dp),
                                    shape = CircleShape,
                                    color = Sage900
                                ) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    } else {
                        // Unselected: "选购" button
                        Surface(
                            onClick = { viewModel.addToCart(recipe.id) },
                            shape = RoundedCornerShape(12.dp),
                            color = Sage900,
                            shadowElevation = 2.dp
                        ) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("选购", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
