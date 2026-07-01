package com.example.solochef.ui.screens.recipedetail

import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.shadow
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
import com.example.solochef.model.IngredientLibrary
import com.example.solochef.storage.LocalFileManager
import com.example.solochef.ui.theme.*
import androidx.compose.ui.platform.LocalContext

@Composable
fun RecipeDetailScreen(
    recipe: Recipe,
    onBack: () -> Unit,
    onGoCook: () -> Unit,
    onDelete: (String) -> Unit,
    onEdit: (Recipe) -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var ingLib by remember { mutableStateOf<IngredientLibrary?>(null) }
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    LaunchedEffect(Unit) {
        ingLib = LocalFileManager(context).getIngredientLibrary()
    }

    Box(Modifier.fillMaxSize().background(Sage50)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 80.dp)
        ) {
            // Cover image — naturally starts at screen top, extends into status bar
            Box(Modifier.fillMaxWidth().height(280.dp + statusBarHeight)) {
                AsyncImage(recipe.cover_image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(0.15f), Color.Transparent, Color.Transparent, Sage50.copy(0.92f)))))
                // Back button — sits below status bar with systemBarsPadding
                Box(Modifier.fillMaxSize().statusBarsPadding(), contentAlignment = Alignment.TopStart) {
                    Surface(
                        onClick = onBack,
                        modifier = Modifier.padding(start = 20.dp, top = 8.dp).size(36.dp),
                        shape = CircleShape,
                        color = Color.Black.copy(0.3f),
                        shadowElevation = 0.dp
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Column(Modifier.offset(y = (-32).dp).padding(horizontal = 20.dp)) {
                // Recipe name
                Text(recipe.name, fontSize = 26.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp, color = Sage900)
                Spacer(Modifier.height(10.dp))

                // Tags
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    recipe.tags.forEach { tag ->
                        Surface(shape = RoundedCornerShape(50), color = Color.White, border = BorderStroke(1.dp, Sage200)) {
                            Text(tag, Modifier.padding(horizontal = 10.dp, vertical = 3.dp), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp, color = Sage400)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))

                // Cost + Price card
                Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), Color.White, border = BorderStroke(1.dp, Sage200), shadowElevation = 0.dp) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("成本", fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage400)
                            Spacer(Modifier.height(2.dp))
                            Text("¥%.2f".format(recipe.cost), fontSize = 16.sp, fontWeight = FontWeight.Black, color = Sage900)
                        }
                        Box(Modifier.width(1.dp).height(28.dp).background(Sage100))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("售价", fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage400)
                            Spacer(Modifier.height(2.dp))
                            Text("¥%.2f".format(recipe.price), fontSize = 16.sp, fontWeight = FontWeight.Black, color = Sage800)
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))

                // ─── 物料清单 ───
                Text("物料清单 / INGREDIENTS", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage900)
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = Sage200.copy(0.4f), thickness = 0.5.dp)
                Spacer(Modifier.height(12.dp))

                recipe.bom_snapshot?.let {
                    Box(Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(24.dp)).background(Color.White).border(1.dp, Sage200, RoundedCornerShape(24.dp)).padding(6.dp)) {
                        AsyncImage(it, null, Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)), contentScale = ContentScale.Crop)
                    }
                    Spacer(Modifier.height(16.dp))
                }

                listOf(MaterialCategory.Meat, MaterialCategory.Vegetable, MaterialCategory.Seasoning).forEach { cat ->
                    val items = recipe.materials[cat.name.lowercase()] ?: emptyList()
                    if (items.isEmpty()) return@forEach
                    val label = cat.label
                    val icon = when (cat) { MaterialCategory.Meat -> "🍖"; MaterialCategory.Vegetable -> "🥬"; MaterialCategory.Seasoning -> "🧂" }
                    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(icon, fontSize = 13.sp)
                        Spacer(Modifier.width(4.dp))
                        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage500)
                    }
                    items.chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { m ->
                                val matEmoji = ingLib?.emojiFor(m.item) ?: "🥬"
                                Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, Sage200)) {
                                    Row(Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(matEmoji, fontSize = 15.sp)
                                        Spacer(Modifier.width(6.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                m.item,
                                                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Black, lineHeight = 12.sp),
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
                        Spacer(Modifier.height(8.dp))
                    }
                    Spacer(Modifier.height(14.dp))
                }

                // ─── 烹饪步骤 ───
                Text("烹饪步骤 / PROCESS", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage900)
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = Sage200.copy(0.4f), thickness = 0.5.dp)
                Spacer(Modifier.height(16.dp))

                recipe.timeline.forEachIndexed { idx, step ->
                    Column(Modifier.padding(bottom = 22.dp)) {
                        // Title Row: step number (center-aligned with first line) + title + right-aligned duration
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(modifier = Modifier.size(30.dp), shape = CircleShape, color = Color.White, border = BorderStroke(1.5.dp, Sage200)) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("${idx + 1}", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Sage900) }
                            }
                            Spacer(Modifier.width(14.dp))
                            Text(
                                step.content,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                lineHeight = 16.sp,
                                color = Sage900,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("${step.duration / 60} MINS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Sage300)
                        }
                        // Sub-tasks — indented to align with title text
                        val indent = 44.dp
                        if (step.sub_tasks.isNotEmpty()) {
                            Surface(modifier = Modifier.fillMaxWidth().padding(start = indent, top = 6.dp), shape = RoundedCornerShape(12.dp), color = Sage50, border = BorderStroke(0.5.dp, Sage200)) {
                                Column(Modifier.padding(8.dp)) {
                                    Text("并行子任务 / Parallel Tasks", fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp, color = Sage400)
                                    Spacer(Modifier.height(4.dp))
                                    step.sub_tasks.forEach { sub ->
                                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                                            Box(
                                                Modifier
                                                    .padding(top = 5.dp, end = 6.dp)
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(Sage900)
                                            )
                                            Text(sub.content, fontSize = 11.sp, fontWeight = FontWeight.Bold, lineHeight = 16.sp, color = Sage800, modifier = Modifier.weight(1f))
                                            Text("${sub.duration / 60} MIN", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Sage400, modifier = Modifier.padding(start = 8.dp, top = 3.dp))
                                        }
                                    }
                                }
                            }
                        }
                        step.images?.firstOrNull()?.let { img ->
                            AsyncImage(img, null, Modifier.padding(start = indent, top = 8.dp).fillMaxWidth().height(160.dp).clip(RoundedCornerShape(14.dp)).border(1.dp, Sage100, RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
                        }
                    }
                }
            }
        }

        // Action bar — sticky at bottom
        Surface(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), color = Color.Transparent) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(52.dp), shape = RoundedCornerShape(20.dp), color = Color.White, border = BorderStroke(1.dp, Sage200)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Delete, contentDescription = null, tint = Sage400, modifier = Modifier.size(22.dp)) }
                }
                Surface(onClick = { onEdit(recipe) }, modifier = Modifier.size(52.dp), shape = RoundedCornerShape(20.dp), color = Color.White, border = BorderStroke(1.dp, Sage200)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Edit, contentDescription = null, tint = Sage400, modifier = Modifier.size(22.dp)) }
                }
                Surface(onClick = onGoCook, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(20.dp), color = DarkButton, shadowElevation = 4.dp) {
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = GreenPlay, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
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
