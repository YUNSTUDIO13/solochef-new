package com.example.solochef.ui.screens.batchdetail

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import coil.compose.AsyncImage
import com.example.solochef.model.*
import com.example.solochef.ui.theme.*

@Composable
fun BatchDetailScreen(
    batch: OrderBatch,
    recipes: List<Recipe>,
    onUpdateStatus: (String, BatchStatus) -> Unit,
    onCompleteRecipe: (String) -> Unit,
    onClose: () -> Unit,
    onOpenSOP: (Recipe) -> Unit
) {
    var checkedMaterials by remember { mutableStateOf(setOf<String>()) }
    val batchRecipes = recipes.filter { batch.recipeIds.contains(it.id) }

    val groupedMaterials = remember(batchRecipes) {
        val agg = mutableMapOf<String, MutableMap<String, Material>>()
        listOf("meat", "vegetable", "seasoning").forEach { agg[it] = mutableMapOf() }
        // Count each recipe's frequency and multiply materials
        val freq = batch.recipeIds.groupingBy { it }.eachCount()
        batchRecipes.forEach { r ->
            val mult = freq[r.id] ?: 1  // multiply by number of times this recipe appears
            r.materials.forEach { (cat, items) ->
                items.forEach { m ->
                    val map = agg[cat] ?: mutableMapOf()
                    val key = "${m.item}_${m.unit}"
                    val existing = map[key]
                    val raw = m.amount.toDoubleOrNull() ?: 0.0
                    val total = if (existing != null) {
                        (existing.amount.toDoubleOrNull() ?: 0.0) + (raw * mult)
                    } else {
                        raw * mult
                    }
                    map[key] = m.copy(amount = if (total == total.toLong().toDouble()) total.toLong().toString() else "%.1f".format(total))
                    agg[cat] = map
                }
            }
        }
        agg
    }

    Column(Modifier.fillMaxSize().background(Sage50)) {
        Surface(modifier = Modifier.fillMaxWidth().background(Sage50.copy(0.8f)).border(1.dp, Sage200), color = Color.Transparent) {
            Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = null, tint = Sage400, modifier = Modifier.size(24.dp)) }
                Column(Modifier.padding(start = 8.dp)) {
                    Text("独厨履约 (Kitchen OMS)", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Sage900)
                    Text("批次编号: ${batch.id.takeLast(6)}", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage400)
                }
                Spacer(Modifier.weight(1f))
                Surface(modifier = Modifier.clip(RoundedCornerShape(50)), color = Sage900) {
                    Text(if (batch.status == BatchStatus.Picking) "物料筹备" else "待烹饪", Modifier.padding(horizontal = 12.dp, vertical = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Color.White, maxLines = 1)
                }
            }
        }

        Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { i ->
                    val filled = when (batch.status) { BatchStatus.Picking -> i == 0; BatchStatus.Processing -> i <= 1; BatchStatus.Finished -> true }
                    Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(if (filled) Sage900 else Sage200))
                }
            }

            Spacer(Modifier.height(32.dp))

            when (batch.status) {
                BatchStatus.Picking -> {
                    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(32.dp), color = Color.White, border = BorderStroke(1.dp, Sage200)) {
                        Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(modifier = Modifier.size(64.dp), shape = RoundedCornerShape(16.dp), color = Sage100) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.ShoppingBasket, contentDescription = null, tint = Sage900, modifier = Modifier.size(32.dp)) } }
                            Spacer(Modifier.width(16.dp))
                            Column { Text("批量采购清单 (Batch BOM)", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Sage900); Text("系统已按分区自动去重合并食材清单", fontSize = 12.sp, color = Sage500) }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    listOf("meat" to "肉禽 / 水产", "vegetable" to "蔬菜 / 蔬果", "seasoning" to "调味 / 干货").forEach { (cat, label) ->
                        val items = (groupedMaterials[cat] ?: emptyMap()).values.toList()
                        if (items.isEmpty()) return@forEach
                        val icon = when (cat) { "meat" -> Icons.Default.SetMeal; "vegetable" -> Icons.Default.Eco; else -> Icons.Default.Opacity }
                        Row(Modifier.padding(horizontal = 4.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(icon, contentDescription = null, tint = Sage400, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage400)
                        }
                        items.forEach { m ->
                            val checked = m.item in checkedMaterials
                            Surface(
                                onClick = { checkedMaterials = if (checked) checkedMaterials - m.item else checkedMaterials + m.item },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape = RoundedCornerShape(28.dp),
                                border = BorderStroke(1.dp, Sage200)
                            ) {
                                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(12.dp), color = if (checked) Color(0xFF10B981) else Color.Transparent) {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            if (checked) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                            else Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Sage300, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Text(m.item, Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.Black, color = if (checked) Sage400 else Sage900, textDecoration = if (checked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null)
                                    Text(m.amount, fontSize = 13.sp, fontWeight = FontWeight.Black, color = Sage900)
                                    Text(m.unit, fontSize = 10.sp, fontWeight = FontWeight.Black, color = Sage400, modifier = Modifier.padding(start = 2.dp))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Surface(onClick = { onUpdateStatus(batch.id, BatchStatus.Processing) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(32.dp), color = Sage900) {
                        Text("采购完成 (Confirm)", Modifier.fillMaxWidth().padding(24.dp), textAlign = TextAlign.Center, fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }

                BatchStatus.Processing -> {
                    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(32.dp), color = Color.White, border = BorderStroke(1.dp, Sage200)) {
                        Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(modifier = Modifier.size(64.dp), shape = RoundedCornerShape(16.dp), color = Sage800) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Restaurant, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp)) } }
                            Spacer(Modifier.width(16.dp))
                            Column { Text("今日待做 (Cook)", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Sage900); Text("点击进入标准图解或一键快速完成", fontSize = 12.sp, color = Sage500) }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    batchRecipes.forEach { recipe ->
                        val isDone = recipe.id in batch.completedRecipeIds
                        Surface(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(32.dp), color = Color.White, border = BorderStroke(1.dp, Sage200)) {
                            Row {
                                Box(Modifier.size(120.dp).clip(RoundedCornerShape(24.dp))) {
                                    AsyncImage(recipe.cover_image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    if (isDone) Box(Modifier.fillMaxSize().background(Color(0xFF10B981).copy(0.2f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(24.dp)) }
                                }
                                Column(Modifier.padding(12.dp).weight(1f)) {
                                    Text(recipe.name, fontSize = 15.sp, fontWeight = FontWeight.Black, color = Sage900, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        recipe.tags.filter { it in com.example.solochef.ui.screens.library.COOKING_PROCESS_TAGS || it in com.example.solochef.ui.screens.library.CUISINE_TAGS }.take(3).forEach { tag ->
                                            Surface(shape = RoundedCornerShape(50), color = Sage50, border = BorderStroke(1.dp, Sage100)) {
                                                Text(tag, Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Sage400, maxLines = 1)
                                            }
                                        }
                                    }
                                    Spacer(Modifier.weight(1f))
                                    if (!isDone) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Surface(onClick = { onOpenSOP(recipe) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), color = Sage800) {
                                                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                                    Text("开始加工", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Color.White)
                                                    Spacer(Modifier.width(4.dp))
                                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                }
                                            }
                                            Surface(onClick = { onCompleteRecipe(recipe.id) }, modifier = Modifier.size(48.dp), shape = RoundedCornerShape(12.dp), color = Sage100) {
                                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Sage500, modifier = Modifier.size(18.dp)) }
                                            }
                                        }
                                    } else {
                                        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = Color(0xFFECFDF5)) {
                                            Text("制作完成", Modifier.fillMaxWidth().padding(8.dp), textAlign = TextAlign.Center, fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF10B981))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Surface(onClick = { onUpdateStatus(batch.id, BatchStatus.Finished) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(32.dp), color = Color.White, border = BorderStroke(2.dp, Sage800)) {
                        Text("一键归档所有 (Finish All)", Modifier.fillMaxWidth().padding(24.dp), textAlign = TextAlign.Center, fontSize = 16.sp, fontWeight = FontWeight.Black, color = Sage800)
                    }
                }

                BatchStatus.Finished -> { /* empty */ }
            }
        }
    }
}
