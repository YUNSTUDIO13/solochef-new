package com.example.solochef.ui.screens.dashboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.solochef.model.Recipe
import com.example.solochef.ui.screens.library.COOKING_PROCESS_TAGS
import com.example.solochef.ui.screens.library.CUISINE_TAGS
import com.example.solochef.ui.theme.*

@Composable
fun FeaturedAllScreen(
    recipes: List<Recipe>,
    onBack: () -> Unit,
    onSelectRecipe: (Recipe) -> Unit
) {
    val allTags = COOKING_PROCESS_TAGS + CUISINE_TAGS
    var selectedCategory by remember { mutableStateOf("全部") }

    val tagCounts = remember(recipes) {
        val map = mutableMapOf<String, Int>()
        recipes.filter { it.is_featured }.forEach { r ->
            r.tags.forEach { tag -> map[tag] = (map[tag] ?: 0) + 1 }
        }
        map
    }

    val activeTags = allTags.filter { (tagCounts[it] ?: 0) > 0 }
    val categories = listOf("全部") + activeTags

    val filtered = remember(recipes, selectedCategory) {
        recipes.filter { r ->
            if (!r.is_featured) return@filter false
            selectedCategory == "全部" || r.tags.contains(selectedCategory)
        }.sortedByDescending { it.cooked_count }
    }

    Column(Modifier.fillMaxSize().background(Sage100)) {
        // Header with back button
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Sage100.copy(alpha = 0.95f)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.ArrowBack, null, tint = Sage900, modifier = Modifier.size(24.dp))
                }
                Column(Modifier.weight(1f).padding(start = 4.dp)) {
                    Text("主厨力荐", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Sage900)
                    Text("精选好菜 · 以厨心，遇好味", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = Sage400)
                }
            }
        }

        // Category chips with counts
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                val sel = cat == selectedCategory
                val cnt = if (cat == "全部") recipes.count { it.is_featured } else (tagCounts[cat] ?: 0)
                Surface(
                    onClick = { selectedCategory = cat },
                    shape = RoundedCornerShape(20),
                    color = if (sel) Sage800 else Color.White,
                    border = BorderStroke(1.dp, if (sel) Sage800 else Sage200)
                ) {
                    Text(
                        text = "$cat ($cnt)",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (sel) Color.White else Sage500
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Content
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无匹配菜谱", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Sage300)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 100.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.id }) { recipe ->
                    Box(
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(32.dp))
                            .background(Color.White).clickable { onSelectRecipe(recipe) }
                    ) {
                        AsyncImage(recipe.cover_image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color(0x66000000)))))
                        Text(recipe.name, Modifier.align(Alignment.BottomStart).padding(12.dp), fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
            }
        }
    }
}
