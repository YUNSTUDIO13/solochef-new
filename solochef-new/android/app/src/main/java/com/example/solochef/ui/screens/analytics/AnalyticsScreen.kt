package com.example.solochef.ui.screens.analytics

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.solochef.model.CookingRecord
import com.example.solochef.model.Recipe
import com.example.solochef.ui.theme.*

@Composable
fun AnalyticsScreen(
    recipes: List<Recipe>,
    cookingRecords: List<CookingRecord>,
    onNavigateToLibrary: () -> Unit,
    onViewAllRanking: () -> Unit,
    onSelectRecipe: (Recipe) -> Unit,
    onShareReceipt: ((List<Recipe>) -> Unit)? = null,
    onDeleteRecord: ((String) -> Unit)? = null,
    onCreateRecord: ((Long) -> Unit)? = null
) {
    // 锅气榜：按cooked_count降序，取前10
    val topWokHeats = remember(recipes) {
        recipes.filter { it.cooked_count > 0 }
            .sortedByDescending { it.cooked_count }
            .take(10)
    }

    Column(Modifier.fillMaxSize().background(Color.Transparent).verticalScroll(rememberScrollState()).padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 100.dp)) {
        Text("食光日历", fontSize = 40.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.05).sp, color = Color(0xFF2D4A3A))
        Text("时间长河里的烟火气", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = Sage500, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(8.dp))

        // ─── 食光日历 ───
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .frostedGlassBackground()
                .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(32.dp)),
            shape = RoundedCornerShape(32.dp),
            color = Color.Transparent
        ) {
            Column(Modifier.padding(20.dp)) {
                FoodCalendar(
                    records = cookingRecords,
                    recipes = recipes,
                    onSelectRecipe = onSelectRecipe,
                    onShareReceipt = onShareReceipt,
                    onDeleteRecord = onDeleteRecord,
                    onCreateRecord = onCreateRecord
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ─── 锅气榜 TOP10 ───
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .frostedGlassBackground()
                .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(32.dp)),
            shape = RoundedCornerShape(32.dp),
            color = Color.Transparent
        ) {
            Column(Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Whatshot, contentDescription = null, tint = Amber400, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("锅气榜 TOP10", fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage900)
                    }
                    if (recipes.count { it.cooked_count > 0 } > 10) {
                        TextButton(onClick = onViewAllRanking) {
                            Text("更多", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage500)
                            Icon(Icons.Default.KeyboardArrowRight, null, tint = Sage500, modifier = Modifier.size(14.dp))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                if (topWokHeats.isEmpty()) {
                    Text("暂无数据，完成烹饪后上榜", fontSize = 12.sp, color = Sage300, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 16.dp))
                } else {
                    topWokHeats.forEachIndexed { i, recipe ->
                        WokHeatRow(rank = i + 1, recipe = recipe)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun WokHeatRow(rank: Int, recipe: Recipe) {
    val accentColor = when (rank) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> Sage800
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .frostedGlassBackground()
            .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent
    ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text("${rank}", fontSize = 11.sp, fontWeight = FontWeight.Black, color = accentColor, style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)))
        }

        Spacer(Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Sage100)
        ) {
            AsyncImage(
                model = recipe.cover_image,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(Modifier.width(10.dp))

        Text(
            recipe.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Sage900,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        FlameBadge(count = recipe.cooked_count)
    }
    }
}

@Composable
fun FlameBadge(count: Int, badgeSize: Int = 28) {
    val dpSize = badgeSize.dp
    Box(
        modifier = Modifier.size(dpSize),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "${count}",
            fontSize = (badgeSize * 0.6f).sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFFE53935)
        )
    }
}
