package com.example.solochef.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Whatshot
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
import com.example.solochef.model.CookingRecord
import com.example.solochef.model.Recipe
import com.example.solochef.ui.theme.*
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WokHeatRankingScreen(
    recipes: List<Recipe>,
    cookingRecords: List<CookingRecord>,
    selectedYear: Int,
    selectedMonth: Int,
    onBack: () -> Unit
) {
    val monthLabel = "${selectedYear}年${selectedMonth + 1}月"

    val ranked = remember(cookingRecords, selectedYear, selectedMonth, recipes) {
        val cal = Calendar.getInstance().apply {
            set(selectedYear, selectedMonth, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val monthStart = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val monthEnd = cal.timeInMillis

        cookingRecords
            .filter { it.cookedAt >= monthStart && it.cookedAt < monthEnd }
            .groupBy { it.recipeId }
            .mapNotNull { (recipeId, recs) ->
                recipes.find { it.id == recipeId }?.let { it to recs.size }
            }
            .sortedByDescending { it.second }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        // Top bar
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Whatshot, null, tint = Amber400, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("锅气榜 · $monthLabel", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Sage900)
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, null, tint = Sage900)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        if (ranked.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("本月暂无数据，完成烹饪后上榜", fontSize = 12.sp, color = Sage300, fontWeight = FontWeight.Bold)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(ranked) { index, (recipe, count) ->
                    RankingRow(rank = index + 1, recipe = recipe, count = count)
                }
                // bottom spacing
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun RankingRow(rank: Int, recipe: Recipe, count: Int) {
    val accentColor = when (rank) {
        1 -> Color(0xFFFFD700)  // gold
        2 -> Color(0xFFC0C0C0)  // silver
        3 -> Color(0xFFCD7F32)  // bronze
        else -> Sage800
    }

    // Top3 keep red, others follow rank color
    val countColor = if (rank <= 3) Color(0xFFE53935) else accentColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cover image
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Sage100)
        ) {
            AsyncImage(
                model = recipe.cover_image,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(Modifier.width(12.dp))

        // Name
        Text(
            recipe.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Sage900,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // Cooked count + "次"
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${count}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = countColor
            )
            Text(
                " 次",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = countColor.copy(alpha = 0.8f)
            )
        }
    }
}
