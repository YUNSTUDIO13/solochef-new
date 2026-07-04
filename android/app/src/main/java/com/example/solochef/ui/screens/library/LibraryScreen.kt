package com.example.solochef.ui.screens.library

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.solochef.model.EnergyLevel
import com.example.solochef.model.Recipe
import com.example.solochef.R
import com.example.solochef.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    libraryName: String = "菜谱库",
    onLibraryNameChange: (String) -> Unit = {},
    onSelectRecipe: (Recipe) -> Unit,
    onCreateClick: () -> Unit,
    viewModel: LibraryViewModel = viewModel()
) {
    val recipes by viewModel.recipes.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedTags by viewModel.selectedTags.collectAsStateWithLifecycle()
    val selectedEnergy by viewModel.selectedEnergyLevels.collectAsStateWithLifecycle()
    var showSearchPopup by remember { mutableStateOf(false) }
    var showRenamePopup by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }

    val filtered = remember(recipes, searchQuery, selectedTags, selectedEnergy) {
        recipes.filter { recipe ->
            val mSearch = searchQuery.isBlank() || recipe.name.contains(searchQuery, ignoreCase = true)
            val mTags = selectedTags.isEmpty() || selectedTags.any { recipe.tags.contains(it) }
            val mEnergy = selectedEnergy.isEmpty() || selectedEnergy.contains(recipe.energy_level)
            mSearch && mTags && mEnergy
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 6.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    libraryName,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.05).sp,
                    color = Color(0xFF2D4A3A),
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        renameText = libraryName
                        showRenamePopup = true
                    }
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "好好吃饭，就是修行",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = Sage500
                    )
                }
            }
            // Search button
            Box {
            Surface(
                onClick = { showSearchPopup = true },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .frostedGlassBackground()
                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                shape = CircleShape,
                color = Color.Transparent
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(painter = painterResource(id = R.drawable.ic_union), null, tint = Color(0xFF2D4A3A), modifier = Modifier.size(20.dp))
                }
            }

            }
            Spacer(Modifier.width(12.dp))
            // New recipe button + count
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                onClick = onCreateClick,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .frostedGlassBackground()
                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                shape = CircleShape,
                color = Color.Transparent
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, null, tint = Color(0xFF2D4A3E), modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "${recipes.size}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = Sage400
            )
        }
        } // end Header Row

        // Content with gradient overlay
        Box(Modifier.fillMaxSize()) {
            if (filtered.isEmpty()) {
                EmptyState(onCreateClick)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 0.dp, bottom = 80.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(filtered, key = { it.id }) { recipe ->
                        RecipeCard(recipe = recipe, onClick = { onSelectRecipe(recipe) })
                    }
                }
            }
        }
        } // end Column

        // Search popup overlay — frosted glass, centered at top
        AnimatedVisibility(
            visible = showSearchPopup,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(150))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showSearchPopup = false }
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 80.dp)
                        .align(Alignment.TopCenter),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { /* consume click, prevent dismiss */ }
                    ) {
                        // Search field (compact height)
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = {
                                Text("搜索菜谱...",
                                    style = TextStyle(fontSize = 11.sp, lineHeight = 11.sp, platformStyle = PlatformTextStyle(includeFontPadding = false)),
                                    color = Sage300, fontWeight = FontWeight.Bold)
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Search, null, tint = Sage500, modifier = Modifier.size(18.dp))
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Sage400,
                                unfocusedBorderColor = Sage200,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            textStyle = TextStyle(fontSize = 11.sp, lineHeight = 11.sp, platformStyle = PlatformTextStyle(includeFontPadding = false)),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        
                        val tagCounts = remember(recipes) {
                            val map = mutableMapOf<String, Int>()
                            recipes.forEach { r -> r.tags.forEach { tag -> map[tag] = (map[tag] ?: 0) + 1 } }
                            map
                        }
                        
                        // Auto-expand; scroll when near screen edge
                        Column(
                            modifier = Modifier
                                .heightIn(max = 420.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            FilterSection(
                                title = "烹饪工艺",
                                tags = COOKING_PROCESS_TAGS,
                                tagCounts = tagCounts,
                                selected = selectedTags,
                                onToggle = { viewModel.toggleTag(it) },
                                onClearAll = { viewModel.clearProcessTags(COOKING_PROCESS_TAGS) }
                            )
                            Spacer(Modifier.height(14.dp))
                            FilterSection(
                                title = "菜系维度",
                                tags = CUISINE_TAGS,
                                tagCounts = tagCounts,
                                selected = selectedTags,
                                onToggle = { viewModel.toggleTag(it) },
                                onClearAll = { viewModel.clearCuisineTags(CUISINE_TAGS) }
                            )
                            Spacer(Modifier.height(14.dp))
                            Text(
                                "精力等级",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = Sage400
                            )
                            @OptIn(ExperimentalLayoutApi::class)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    label = "全部",
                                    selected = selectedEnergy.isEmpty(),
                                    onClick = { viewModel.clearAllFilters() },
                                    selectedColor = Sage800,
                                    unselectedColor = Sage500
                                )
                                ENERGY_LEVEL_OPTIONS.forEach { (level, label) ->
                                    FilterChip(
                                        label = label,
                                        selected = level in selectedEnergy,
                                        onClick = { viewModel.toggleEnergyLevel(level) },
                                        selectedColor = Sage800,
                                        unselectedColor = Sage500
                                    )
                                }
                            }
                            if (selectedTags.isNotEmpty() || selectedEnergy.isNotEmpty()) {
                                Spacer(Modifier.height(12.dp))
                                Divider(color = Sage100)
                                TextButton(
                                    onClick = { viewModel.clearAllFilters() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "清除所有过滤",
                                        color = Red500,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 2.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Rename popup overlay
        if (showRenamePopup) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showRenamePopup = false }
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .align(Alignment.Center),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { /* consume click */ }
                    ) {
                        Text(
                            "修改名称",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Sage900
                        )
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = renameText,
                            onValueChange = { if (it.length <= 4) renameText = it },
                            placeholder = {
                                Text("请输入修改后的名称",
                                    style = TextStyle(fontSize = 11.sp, lineHeight = 11.sp, platformStyle = PlatformTextStyle(includeFontPadding = false)),
                                    color = Sage300, fontWeight = FontWeight.Bold)
                            },
                            shape = RoundedCornerShape(14.dp),
                            textStyle = TextStyle(fontSize = 13.sp, color = Sage900),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Sage400,
                                unfocusedBorderColor = Sage200,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showRenamePopup = false }) {
                                Text("取消", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Sage500)
                            }
                            Spacer(Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    if (renameText.isNotBlank()) onLibraryNameChange(renameText.trim())
                                    showRenamePopup = false
                                },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(containerColor = Sage800)
                            ) {
                                Text("确认", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    } // end Box
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSection(
    title: String,
    tags: List<String>,
    tagCounts: Map<String, Int>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onClearAll: () -> Unit
) {
    Text(
        title,
        fontSize = 10.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 2.sp,
        color = Sage400,
        modifier = Modifier
    )
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val hasSelection = tags.any { it in selected }
        FilterChip(
            label = "全部",
            selected = !hasSelection,
            onClick = onClearAll,
            selectedColor = Sage800,
            unselectedColor = Sage500
        )
        tags.forEach { tag ->
            val cnt = tagCounts[tag] ?: 0
            if (cnt > 0) {
                FilterChip(
                    label = "$tag ($cnt)",
                    selected = tag in selected,
                    onClick = { onToggle(tag) },
                    selectedColor = Sage800,
                    unselectedColor = Sage500
                )
            }
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    selectedColor: Color,
    unselectedColor: Color
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) selectedColor else Color.Transparent,
        border = if (selected) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
        contentColor = if (selected) Color.White else unselectedColor,
        modifier = if (selected) Modifier else Modifier
            .clip(RoundedCornerShape(50))
            .frostedGlassBackground()
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RecipeCard(recipe: Recipe, onClick: () -> Unit) {
    val processTag = recipe.tags.firstOrNull { it in COOKING_PROCESS_TAGS }
    val cuisineTag = recipe.tags.firstOrNull { it in CUISINE_TAGS }
    val cookTime = recipe.timeline.sumOf { it.duration } / 60

    // 构建三字段文本行（无背景，纯白色文字）
    val metaLine = listOfNotNull(
        "${cookTime}min",
        processTag,
        cuisineTag
    ).joinToString("  ·  ")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = recipe.cover_image,
            contentDescription = recipe.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 渐变蒙层 (与首页推荐菜一致: transparent → Black60)
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Black60),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // 推荐标签 — 右上角
        if (recipe.is_featured) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Amber400),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "荐",
                    style = TextStyle(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 9.sp,
                        textAlign = TextAlign.Center,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    ),
                    color = Sage900
                )
            }
        }

        // 底部：三字段（上） + 菜谱名称（下）
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        ) {
            // 烹饪时长 · 烹饪工艺 · 菜系纬度（7sp，无背景纯文字，单行省略）
            Text(
                metaLine,
                style = TextStyle(fontSize = 7.sp, fontWeight = FontWeight.Bold, lineHeight = 7.sp, platformStyle = PlatformTextStyle(includeFontPadding = false)),
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // 菜谱名称（最下方）
            Text(
                recipe.name,
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Black, lineHeight = 13.sp, platformStyle = PlatformTextStyle(includeFontPadding = false)),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmptyState(onCreateClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .frostedGlassBackground()
            .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(32.dp))
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Sage50,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Search, null,
                    tint = Sage300,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("暂无相关菜谱", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Sage900)
            Text("去新建一个展示你的厨艺吧", fontSize = 10.sp, color = Sage400, modifier = Modifier.padding(top = 2.dp))
        }
        Surface(
            onClick = onCreateClick,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .frostedGlassBackground()
                .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(50)),
            shape = RoundedCornerShape(50),
            color = Color.Transparent
        ) {
            Text("新建", Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage900)
        }
    }
}
