package com.example.solochef.ui.screens.library

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.solochef.model.EnergyLevel
import com.example.solochef.model.Recipe
import com.example.solochef.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onSelectRecipe: (Recipe) -> Unit,
    onCreateClick: () -> Unit,
    viewModel: LibraryViewModel = viewModel()
) {
    val recipes by viewModel.recipes.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedTags by viewModel.selectedTags.collectAsStateWithLifecycle()
    val selectedEnergy by viewModel.selectedEnergyLevels.collectAsStateWithLifecycle()
    val showFilters by viewModel.showFilters.collectAsStateWithLifecycle()

    val filtered = remember(recipes, searchQuery, selectedTags, selectedEnergy) {
        recipes.filter { recipe ->
            val mSearch = searchQuery.isBlank() || recipe.name.contains(searchQuery, ignoreCase = true)
            val mTags = selectedTags.isEmpty() || selectedTags.all { recipe.tags.contains(it) }
            val mEnergy = selectedEnergy.isEmpty() || selectedEnergy.contains(recipe.energy_level)
            mSearch && mTags && mEnergy
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Sage100)
            .padding(horizontal = 24.dp)
            
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "菜谱库",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.05).sp,
                    color = Sage900
                )
                Text(
                    "(${recipes.size})",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Sage400,
                    modifier = Modifier.padding(start = 6.dp, bottom = 4.dp)
                )
            }
            Text(
                "好好吃饭，就是修行",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = Sage500,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Search & Filter
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = {
                    Text("搜索菜谱...", color = Sage300, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, null, tint = Sage500, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (showFilters) Sage800 else Color.Transparent)
                            .clickable { viewModel.toggleFilter() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.FilterList, null,
                            tint = if (showFilters) Color.White else Sage500,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Sage400,
                    unfocusedBorderColor = Sage200,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Filter Panel
        AnimatedVisibility(
            visible = showFilters,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .heightIn(max = 400.dp)
                    .background(Color.White, RoundedCornerShape(32.dp))
                    .border(1.dp, Sage200, RoundedCornerShape(32.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Tag counts for dynamic filter display
                val tagCounts = remember(recipes) {
                    val map = mutableMapOf<String, Int>()
                    recipes.forEach { r -> r.tags.forEach { tag -> map[tag] = (map[tag] ?: 0) + 1 } }
                    map
                }

                // Process Tags
                FilterSection(
                    title = "烹饪工艺",
                    tags = COOKING_PROCESS_TAGS,
                    tagCounts = tagCounts,
                    selected = selectedTags,
                    onToggle = { viewModel.toggleTag(it) },
                    onClearAll = { viewModel.clearProcessTags(COOKING_PROCESS_TAGS) }
                )
                Spacer(Modifier.height(20.dp))

                // Cuisine Tags
                FilterSection(
                    title = "菜系维度",
                    tags = CUISINE_TAGS,
                    tagCounts = tagCounts,
                    selected = selectedTags,
                    onToggle = { viewModel.toggleTag(it) },
                    onClearAll = { viewModel.clearCuisineTags(CUISINE_TAGS) }
                )
                Spacer(Modifier.height(20.dp))

                // Energy Level
                Text(
                    "精力等级",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = Sage400,
                    modifier = Modifier
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

                // Clear all
                if (selectedTags.isNotEmpty() || selectedEnergy.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
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

        Spacer(Modifier.height(24.dp))

        // Content
        if (filtered.isEmpty()) {
            EmptyState(onCreateClick)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.id }) { recipe ->
                    RecipeCard(recipe = recipe, onClick = { onSelectRecipe(recipe) })
                }
            }
        }
    }
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
        color = if (selected) selectedColor else Sage50,
        contentColor = if (selected) Color.White else unselectedColor
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
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
    ) {
        // Image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
        ) {
            AsyncImage(
                model = recipe.cover_image,
                contentDescription = recipe.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Cooking time: sum of ALL step durations
            val cookTime = recipe.timeline.sumOf { it.duration } / 60
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .size(44.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.22f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${cookTime}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        lineHeight = 15.sp
                    )
                    Text(
                        "min",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.75f),
                        lineHeight = 9.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Title & Featured badge
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                recipe.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = Sage900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (recipe.is_featured) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Amber400),
                    contentAlignment = Alignment.Center
                ) {
                    Text("荐", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Sage900, textAlign = TextAlign.Center)
                }
            }
        }

        Spacer(Modifier.height(1.dp))

        // Tags
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            recipe.tags.take(2).forEach { tag ->
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Gray100
                ) {
                    Text(
                        tag,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = Gray500
                    )
                }
            }
        }

        Spacer(Modifier.height(1.dp))

        // Steps & Difficulty
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "${recipe.timeline.size} STEPS",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = Sage400
            )
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(Sage200, CircleShape)
            )
            Text(
                when (recipe.energy_level) {
                    EnergyLevel.High -> "困难"
                    EnergyLevel.Mid -> "一般"
                    EnergyLevel.Low -> "容易"
                },
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = Sage400
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
            .border(2.dp, Sage800, RoundedCornerShape(32.dp))
            .background(Color.White)
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
        Button(
            onClick = onCreateClick,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = Sage900)
        ) {
            Text("新建", fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
        }
    }
}
