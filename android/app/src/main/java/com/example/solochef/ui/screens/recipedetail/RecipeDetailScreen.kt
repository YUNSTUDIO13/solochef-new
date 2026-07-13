package com.example.solochef.ui.screens.recipedetail

import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.solochef.model.*
import com.example.solochef.storage.LocalFileManager
import com.example.solochef.ui.theme.*
import androidx.compose.ui.platform.LocalContext

// 1:1 复刻用色（来自 Figma 参考）
private val Copper = Color(0xFFB8622A)
private val WarmCream = Color(0xFFF6F2EC)
private val NearBlack = Color(0xFF1C1916)
private val TextSecondary = Color(0xFF8A8274)
private val TextBody = Color(0xFF4A4540)
private val SageGreen = Color(0xFF4A7C59)
private val SageGreenLight = Color(0xFFEDF5EE)
private val TipBg = Color(0xFFFFF6EF)
private val RedText = Color(0xFFC0392B)
private val DividerAlpha = Color(0xFF1C1916).copy(0.07f)

@Composable
fun RecipeDetailScreen(
    initialRecipe: Recipe,
    onBack: () -> Unit,
    onGoCook: () -> Unit,
    onDelete: (String) -> Unit,
    onEdit: (Recipe) -> Unit
) {
    val context = LocalContext.current
    var recipe by remember { mutableStateOf(initialRecipe) }
    LaunchedEffect(initialRecipe.id) {
        LocalFileManager(context).getRecipe(initialRecipe.id)?.let { recipe = it }
    }
    var ingLib by remember { mutableStateOf<IngredientLibrary?>(null) }
    var customRecipeTags by remember { mutableStateOf<CustomRecipeTags?>(null) }
    LaunchedEffect(Unit) {
        ingLib = LocalFileManager(context).getIngredientLibrary()
        customRecipeTags = LocalFileManager(context).getCustomRecipeTags()
    }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val totalDuration = remember(recipe) { recipe.timeline.sumOf { it.duration } / 60 }

    Box(Modifier.fillMaxSize().background(WarmCream)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp)
        ) {
            // ─── Hero ───
            Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
                AsyncImage(
                    recipe.cover_image, contentDescription = null,
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                )
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(Color.Black.copy(0.28f), Color.Transparent, Color.Black.copy(0.6f)))
                    )
                )
                Box(Modifier.fillMaxSize().statusBarsPadding().padding(start = 20.dp, top = 8.dp), contentAlignment = Alignment.TopStart) {
                    Surface(onClick = onBack, modifier = Modifier.size(36.dp), shape = CircleShape, color = Color.White.copy(0.18f)) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                val allProcessTags = COOKING_PROCESS_TAGS + (customRecipeTags?.cookingProcessTags?.map { it.name } ?: emptyList())
                val allCuisineTags = CUISINE_TAGS + (customRecipeTags?.cuisineTags?.map { it.name } ?: emptyList())
                val processTag = recipe.tags.firstOrNull { it in allProcessTags }
                val cuisineTag = recipe.tags.firstOrNull { it in allCuisineTags }
                Column(Modifier.align(Alignment.BottomStart).padding(horizontal = 22.dp, vertical = 22.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            recipe.name,
                            Modifier.weight(1f),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 34.sp
                        )
                        Row(
                            Modifier.padding(start = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            processTag?.let { TagChip(it) }
                            cuisineTag?.let { TagChip(it) }
                        }
                    }
                }
            }

            // ─── Stats bar ───
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 24.dp, bottom = 20.dp)
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val stats = listOf(
                    "精力等级" to energyLabel(recipe.energy_level),
                    "烹饪时长" to "${totalDuration}min",
                    "成本" to "¥%.0f".format(recipe.cost),
                    "售价" to "¥%.0f".format(recipe.price)
                )
                stats.forEachIndexed { i, (label, value) ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(label, fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Normal)
                        Spacer(Modifier.height(4.dp))
                        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NearBlack)
                    }
                    if (i < stats.size - 1) {
                        VerticalDivider(color = DividerAlpha, modifier = Modifier.fillMaxHeight())
                    }
                }
            }
            Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(0.5.dp).background(DividerAlpha))

            // ─── Description ───
            if (recipe.description.isNotBlank()) {
                Text(
                    recipe.description,
                    Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    fontSize = 12.sp, color = TextBody, lineHeight = 18.sp, fontWeight = FontWeight.Normal
                )
                Spacer(Modifier.height(8.dp))
            }

            // ─── Ingredients ───
            Column(Modifier.padding(horizontal = 24.dp)) {
                Spacer(Modifier.height(12.dp))
                Text("物料清单", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = NearBlack)
                Spacer(Modifier.height(14.dp))
                val allIngredients = remember(recipe) {
                    buildList {
                        recipe.materials["meat"]?.let { addAll(it.map { "meat" to it }) }
                        recipe.materials["vegetable"]?.let { addAll(it.map { "vegetable" to it }) }
                        recipe.materials["seasoning"]?.let { addAll(it.map { "seasoning" to it }) }
                    }
                }
                allIngredients.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { (cat, mat) ->
                            IngredientCard(
                                item = mat,
                                emoji = ingLib?.emojiFor(mat.item) ?: "🥬",
                                modifier = Modifier.weight(1f),
                                isMeat = cat == "meat"
                            )
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            // ─── Cooking Steps ───
            Column(Modifier.padding(horizontal = 24.dp)) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("烹饪步骤", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = NearBlack)
                    Spacer(Modifier.width(8.dp))
                    Text("共 ${recipe.timeline.size} 步", fontSize = 11.sp, color = TextSecondary)
                }
                Spacer(Modifier.height(20.dp))
                recipe.timeline.forEachIndexed { index, step ->
                    StepCard(step = step, index = index, isLast = index == recipe.timeline.size - 1)
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ─── Bottom CTA ───
        Surface(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), color = Color.Transparent) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(onClick = { onEdit(recipe) }, modifier = Modifier.size(46.dp).clip(RoundedCornerShape(16.dp)).frostedGlassBackground().border(1.dp, Color.White.copy(0.4f), RoundedCornerShape(16.dp)), shape = RoundedCornerShape(16.dp), color = Color.Transparent) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Edit, null, tint = Sage400, modifier = Modifier.size(18.dp)) }
                }
                Surface(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(46.dp).clip(RoundedCornerShape(16.dp)).frostedGlassBackground().border(1.dp, Color.White.copy(0.4f), RoundedCornerShape(16.dp)), shape = RoundedCornerShape(16.dp), color = Color.Transparent) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Delete, null, tint = RedText, modifier = Modifier.size(18.dp)) }
                }
                Surface(onClick = onGoCook, modifier = Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(16.dp)), shape = RoundedCornerShape(16.dp), color = NearBlack) {
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Text("开始烹饪", fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp, color = WarmCream)
                    }
                }
            }
        }

        // Delete dialog
        AnimatedVisibility(showDeleteConfirm, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize().background(Black60).clickable { showDeleteConfirm = false }) {
                Surface(modifier = Modifier.align(Alignment.Center).padding(24.dp), shape = RoundedCornerShape(40.dp), color = Color.White, border = BorderStroke(1.dp, Sage200)) {
                    Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(modifier = Modifier.size(64.dp), shape = RoundedCornerShape(16.dp), color = Red50) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Delete, null, tint = Red500, modifier = Modifier.size(32.dp)) } }
                        Spacer(Modifier.height(24.dp))
                        Text("确定要删除该菜谱吗？", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Sage900)
                        Text("删除后不可找回。", fontSize = 12.sp, color = Sage500, modifier = Modifier.padding(top = 6.dp))
                        Spacer(Modifier.height(24.dp))
                        Surface(onClick = { onDelete(recipe.id); showDeleteConfirm = false; onBack() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Red500) {
                            Text("确认删除", Modifier.fillMaxWidth().padding(14.dp), textAlign = TextAlign.Center, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp, color = Color.White)
                        }
                        Spacer(Modifier.height(10.dp))
                        Surface(onClick = { showDeleteConfirm = false }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Sage50) {
                            Text("取消", Modifier.fillMaxWidth().padding(14.dp), textAlign = TextAlign.Center, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp, color = Sage400)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TagChip(value: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(0.18f))
            .border(1.dp, Color.White.copy(0.28f), RoundedCornerShape(50))
            .padding(horizontal = 2.dp, vertical = 1.dp)
    ) {
        Text("#$value", fontSize = 7.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(0.92f))
    }
}

@Composable
private fun IngredientCard(item: Material, emoji: String, modifier: Modifier = Modifier, isMeat: Boolean = false) {
    Column(
        modifier
            .aspectRatio(1f / 1.15f)
            .clip(RoundedCornerShape(16.dp))
            .frostedGlassBackground()
            .border(1.dp, Color.White.copy(0.4f), RoundedCornerShape(16.dp))
    ) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(emoji, fontSize = 28.sp, modifier = Modifier.padding(4.dp))
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(item.item, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = NearBlack, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            Text("${item.amount}${item.unit}", fontSize = 9.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun StepCard(step: TimelineStep, index: Int, isLast: Boolean) {
    val hasSubTasks = !step.sub_tasks.isNullOrEmpty()
    var expanded by remember { mutableStateOf(true) }
    val stepDuration = step.duration / 60

    Row(Modifier.fillMaxWidth().padding(bottom = if (isLast) 4.dp else 0.dp)) {
        // Timeline spine
        Column(Modifier.width(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(modifier = Modifier.size(32.dp), shape = CircleShape, color = Copper) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(String.format("%02d", index + 1), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            if (!isLast) {
                Box(Modifier.width(2.dp).weight(1f).padding(top = 4.dp).background(Copper.copy(0.2f)))
            }
        }
        Spacer(Modifier.width(12.dp))

        // Content
        Column(Modifier.weight(1f).padding(bottom = 16.dp)) {
            // Step content + duration (right-aligned)
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    step.content,
                    fontSize = 13.sp, color = TextBody, lineHeight = 20.sp, fontWeight = FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                if (stepDuration > 0) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${stepDuration} min",
                        fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary
                    )
                }
            }

            // Step images
            (step.images?.firstOrNull())?.let { img ->
                Spacer(Modifier.height(8.dp))
                AsyncImage(img, null, Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, Sage100, RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
            }

            // Sub-tasks (replaces tip area) — only shows expand/collapse when sub-tasks exist
            if (hasSubTasks) {
                Spacer(Modifier.height(8.dp))
                // Collapse toggle button
                Row(
                    Modifier.fillMaxWidth().clickable { expanded = !expanded },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ListAlt, null, tint = Copper, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("并行子任务 (${step.sub_tasks!!.size})", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Copper)
                    }
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        null, tint = TextSecondary, modifier = Modifier.size(16.dp)
                    )
                }

                AnimatedVisibility(expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    Surface(
                        Modifier.fillMaxWidth().padding(top = 6.dp),
                        shape = RoundedCornerShape(14.dp), color = TipBg,
                        border = BorderStroke(1.dp, Copper.copy(0.2f))
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            step.sub_tasks!!.forEach { sub ->
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                                    Box(Modifier.padding(top = 4.dp, end = 6.dp).size(4.dp).clip(CircleShape).background(Copper))
                                    Text(sub.content, Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, lineHeight = 16.sp, color = Sage800)
                                    Text("${sub.duration / 60} min", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Copper, modifier = Modifier.padding(start = 6.dp, top = 1.dp))
                                }
                                if (sub != step.sub_tasks!!.last()) Spacer(Modifier.height(3.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun energyLabel(level: EnergyLevel): String = when (level) {
    EnergyLevel.High -> "满满"
    EnergyLevel.Mid -> "正常"
    EnergyLevel.Low -> "极简"
}

private val COOKING_PROCESS_TAGS = listOf("清蒸", "爆炒", "慢炖", "油炸", "煎烤", "凉拌", "红烧", "白灼")
private val CUISINE_TAGS = listOf("川菜", "粤菜", "湘菜", "鲁菜", "闽菜", "苏菜", "浙菜", "徽菜", "本帮菜", "东北菜", "西北菜", "云贵菜", "客家菜", "西餐", "日料", "韩料", "东南亚菜", "融合创新", "街头小吃", "深夜食堂", "减脂轻食")
