package com.example.solochef.ui.screens.recipedetail

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.solochef.model.MaterialCategory
import com.example.solochef.model.Recipe
import com.example.solochef.ui.theme.*

@Composable
fun RecipeDetailScreen(
    recipe: Recipe,
    onBack: () -> Unit,
    onGoCook: () -> Unit,
    onDelete: (String) -> Unit,
    onEdit: (Recipe) -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Sage50)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // Cover image — fixed height, not fillMaxHeight(fraction) which breaks in scroll
            Box(Modifier.fillMaxWidth().height(240.dp)) {
                AsyncImage(recipe.cover_image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, Sage50.copy(0.9f)))))
                // Back button — ALL named params to avoid overload confusion
                Surface(
                    onClick = onBack,
                    modifier = Modifier.padding(24.dp).size(40.dp),
                    shape = CircleShape,
                    color = Color.Black.copy(0.35f)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Column(Modifier.offset(y = (-40).dp).padding(horizontal = 24.dp)) {
                Text(recipe.name, fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.05).sp, color = Sage900)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    recipe.tags.forEach { tag ->
                        Surface(modifier = Modifier, shape = RoundedCornerShape(50), color = Color.White, border = BorderStroke(1.dp, Sage200)) {
                            Text(tag, Modifier.padding(horizontal = 12.dp, vertical = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage400)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                // Cost + Price in one row
                Surface(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), Color.White, border = BorderStroke(1.dp, Sage200)) {
                    Row(Modifier.padding(horizontal = 20.dp, vertical = 14.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("成本", fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage400)
                            Text("¥%.2f".format(recipe.cost), fontSize = 16.sp, fontWeight = FontWeight.Black, color = Sage900)
                        }
                        Box(Modifier.width(1.dp).height(32.dp).background(Sage100))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("售价", fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage400)
                            Text("¥%.2f".format(recipe.price), fontSize = 16.sp, fontWeight = FontWeight.Black, color = Sage800)
                        }
                    }
                }
                Spacer(Modifier.height(40.dp))

                Text("物料清单 / INGREDIENTS", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage900)
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = Sage200.copy(0.5f))
                Spacer(Modifier.height(16.dp))

                recipe.bom_snapshot?.let {
                    Box(Modifier.fillMaxWidth().height(192.dp).clip(RoundedCornerShape(32.dp)).background(Color.White).border(1.dp, Sage200, RoundedCornerShape(32.dp)).padding(8.dp)) {
                        AsyncImage(it, null, Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)), contentScale = ContentScale.Crop)
                    }
                    Spacer(Modifier.height(24.dp))
                }

                listOf(MaterialCategory.Meat, MaterialCategory.Vegetable, MaterialCategory.Seasoning).forEach { cat ->
                    val items = recipe.materials[cat.name.lowercase()] ?: emptyList()
                    if (items.isEmpty()) return@forEach
                    val label = cat.label
                    val icon = when (cat) { MaterialCategory.Meat -> Icons.Default.SetMeal; MaterialCategory.Vegetable -> Icons.Default.Eco; MaterialCategory.Seasoning -> Icons.Default.Opacity }
                    Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, contentDescription = null, tint = Sage400, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage500)
                    }
                    items.chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { m ->
                                Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(20.dp), color = Color.White, border = BorderStroke(1.dp, Sage200)) {
                                    Row(Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("🥬", fontSize = 16.sp)
                                        Spacer(Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                m.item,
                                                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Black, lineHeight = 13.sp),
                                                color = Sage900,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                "${m.amount}${m.unit}",
                                                style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, lineHeight = 10.sp),
                                                color = Sage300
                                            )
                                        }
                                    }
                                }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    Spacer(Modifier.height(20.dp))
                }

                Text("烹饪步骤 / PROCESS", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage900)
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = Sage200.copy(0.5f))
                Spacer(Modifier.height(24.dp))

                recipe.timeline.forEachIndexed { idx, step ->
                    Row(Modifier.padding(bottom = 32.dp)) {
                        Surface(modifier = Modifier.size(32.dp).align(Alignment.Top), shape = CircleShape, color = Color.White, border = BorderStroke(2.dp, Sage100)) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("${idx + 1}", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Sage900) }
                        }
                        Spacer(Modifier.width(20.dp))
                        Column {
                            Text(step.content, fontSize = 13.sp, fontWeight = FontWeight.Black, color = Sage900)
                            Text("${step.duration / 60} MINS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Sage300, modifier = Modifier.padding(top = 4.dp))
                            // Sub-tasks
                            if (step.sub_tasks.isNotEmpty()) {
                                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Sage50) {
                                    Column(Modifier.padding(8.dp)) {
                                        Text("并行子任务 / Parallel Tasks", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage400)
                                        step.sub_tasks.forEach { sub ->
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                    Surface(modifier = Modifier.size(16.dp), shape = RoundedCornerShape(4.dp), color = Sage900) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp)) } }
                                                    Spacer(Modifier.width(8.dp))
                                                    Text(sub.content, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Sage800, maxLines = 4, overflow = TextOverflow.Ellipsis)
                                                }
                                                Text("${sub.duration / 60} MIN", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Sage400)
                                            }
                                        }
                                    }
                                }
                            }
                            step.images?.firstOrNull()?.let { img ->
                                AsyncImage(img, null, Modifier.padding(top = 12.dp).fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp)).border(1.dp, Sage100, RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
                            }
                        }
                    }
                }
            }
        }

        // Action bar — sticky at bottom (outside scroll)
        Surface(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp), color = Color.Transparent) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(56.dp), shape = RoundedCornerShape(24.dp), color = Color.White, border = BorderStroke(1.dp, Sage200)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Delete, contentDescription = null, tint = Sage400, modifier = Modifier.size(24.dp)) }
                }
                Surface(onClick = { onEdit(recipe) }, modifier = Modifier.size(56.dp), shape = RoundedCornerShape(24.dp), color = Color.White, border = BorderStroke(1.dp, Sage200)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Edit, contentDescription = null, tint = Sage400, modifier = Modifier.size(24.dp)) }
                }
                Surface(onClick = onGoCook, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(24.dp), color = DarkButton) {
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = GreenPlay, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("去做饭", fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Color.White)
                    }
                }
            }
        }

        // Delete dialog
        AnimatedVisibility(showDeleteConfirm, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize().background(Black60).clickable { showDeleteConfirm = false }) {
                Surface(modifier = Modifier.align(Alignment.Center).padding(24.dp), shape = RoundedCornerShape(40.dp), color = Color.White, border = BorderStroke(1.dp, Sage200)) {
                    Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(modifier = Modifier.size(64.dp), shape = RoundedCornerShape(16.dp), color = Red50) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Delete, contentDescription = null, tint = Red500, modifier = Modifier.size(32.dp)) } }
                        Spacer(Modifier.height(24.dp))
                        Text("确定要删除该菜谱吗？", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Sage900)
                        Text("删除后不可找回。", fontSize = 13.sp, color = Sage500, modifier = Modifier.padding(top = 8.dp))
                        Spacer(Modifier.height(32.dp))
                        Surface(onClick = { onDelete(recipe.id); showDeleteConfirm = false; onBack() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Red500) {
                            Text("确认删除", Modifier.fillMaxWidth().padding(16.dp), textAlign = TextAlign.Center, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Color.White)
                        }
                        Spacer(Modifier.height(12.dp))
                        Surface(onClick = { showDeleteConfirm = false }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Sage50) {
                            Text("取消", Modifier.fillMaxWidth().padding(16.dp), textAlign = TextAlign.Center, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage400)
                        }
                    }
                }
            }
        }
    }
}
