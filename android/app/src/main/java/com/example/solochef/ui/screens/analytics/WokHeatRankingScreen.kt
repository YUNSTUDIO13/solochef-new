package com.example.solochef.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.solochef.model.Recipe
import com.example.solochef.ui.theme.*
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WokHeatRankingScreen(
    recipes: List<Recipe>,
    onBack: () -> Unit
) {
    val ranked = remember(recipes) {
        recipes.filter { it.cooked_count > 0 }
            .sortedByDescending { it.cooked_count }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Sage100)
    ) {
        // Top bar
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Whatshot, null, tint = Amber400, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("锅气榜", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Sage900)
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, null, tint = Sage900)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Sage100)
        )

        if (ranked.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无数据，完成烹饪后上榜", fontSize = 12.sp, color = Sage300, fontWeight = FontWeight.Bold)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(ranked) { index, recipe ->
                    RankingRow(rank = index + 1, recipe = recipe)
                }
                // bottom spacing
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun RankingRow(rank: Int, recipe: Recipe) {
    val accentColor = when (rank) {
        1 -> Color(0xFFFFD700)  // gold
        2 -> Color(0xFFC0C0C0)  // silver
        3 -> Color(0xFFCD7F32)  // bronze
        else -> Sage200
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = if (rank <= 3) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank number
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${rank}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = accentColor
                )
            }

            Spacer(Modifier.width(12.dp))

            // Cover image
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
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

            // Cooked count
            FlameBadge(count = recipe.cooked_count, badgeSize = 36)
        }
    }
}
