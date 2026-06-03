package com.example.solochef.ui.screens.analytics

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.solochef.model.Recipe
import com.example.solochef.storage.LocalFileManager
import com.example.solochef.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// Heatmap color levels per spec
private val HeatL1 = Color(0xFFE8F5E9)  // 0-25% light green
private val HeatL2 = Color(0xFFC8E6C9)  // 25-50%
private val HeatL3 = Color(0xFF81C784)  // 50-75%
private val HeatL4 = Color(0xFF4CAF50)  // 75-100% deep green
private val HeatGrey = Color(0xFFE5E7EB) // future days

private data class DayHeat(val label: String, val count: Int, val pct: Float, val heat: Color, val isFuture: Boolean)

@Composable
fun AnalyticsScreen(
    onNavigateToLibrary: () -> Unit
) {
    val context = LocalContext.current
    val storage = remember { LocalFileManager(context) }

    // Force reload every time screen composition runs
    var recipes by remember { mutableStateOf<List<Recipe>>(emptyList()) }
    LaunchedEffect(Unit) {
        recipes = withContext(Dispatchers.IO) { storage.getAllRecipes() }
    }

    val oneWeekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
    val lapsedCount = recipes.count { (it.last_cooked_at?.toLongOrNull() ?: 0) < oneWeekAgo }

    // Ingredient ranking: exclude seasoning/other category materials
    data class IngredientCount(val name: String, val count: Int)
    val map = mutableMapOf<String, Int>()
    recipes.forEach { r ->
        r.materials.forEach { (cat, items) ->
            if (cat == "seasoning") return@forEach
            items.forEach { m ->
                map[m.item] = (map[m.item] ?: 0) + 1
            }
        }
    }
    val topIngredients = map.entries
        .sortedByDescending { it.value }
        .take(5)
        .map { IngredientCount(it.key, it.value) }

    val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val now = Calendar.getInstance()
    val todayIdx = ((now.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY) + 7) % 7
    val weeklyData = dayLabels.mapIndexed { i, label ->
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -(6 - i))
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val dayStart = cal.timeInMillis
        val dayEnd = dayStart + TimeUnit.DAYS.toMillis(1)
        val isFuture = i > todayIdx
        val count = if (isFuture) 0 else recipes.count { r ->
            (r.last_cooked_at?.toLongOrNull() ?: 0L) in dayStart..<dayEnd
        }
        val pct = (count.toFloat() / 21f * 100f).coerceAtMost(100f)
        val heat = when {
            isFuture -> HeatGrey
            pct < 25f -> HeatL1
            pct < 50f -> HeatL2
            pct < 75f -> HeatL3
            else -> HeatL4
        }
        DayHeat(label, count, pct, heat, isFuture)
    }
    val totalHomeMeals = weeklyData.sumOf { if (!it.isFuture) it.count else 0 }
    val homeRate = (totalHomeMeals.toFloat() / 21f * 100f).coerceAtMost(100f)

    Column(Modifier.fillMaxSize().background(Sage100).verticalScroll(rememberScrollState()).padding(24.dp)) {
        Text("数据大盘", fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.05).sp, color = Sage900)
        Text("Kitchen BI & Fulfillment Insights", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = Sage500, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(24.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(modifier = Modifier.weight(1f).aspectRatio(1f), shape = RoundedCornerShape(32.dp), color = Color.White, border = BorderStroke(1.dp, Sage200)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Icon(Icons.Default.Tag, contentDescription = null, tint = Sage400, modifier = Modifier.size(14.dp))
                    Column {
                        Text("${recipes.size}", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Sage900)
                        Text("道菜", fontSize = 10.sp, color = Sage400)
                    }
                }
            }
            Surface(modifier = Modifier.weight(1f).aspectRatio(1f).clickable { onNavigateToLibrary() }, shape = RoundedCornerShape(32.dp), color = Sage900) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(14.dp))
                    Column {
                        Text("$lapsedCount", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Text("菜失宠", fontSize = 10.sp, color = Color.White.copy(0.6f))
                        Text("一周未烹饪", fontSize = 8.sp, color = Color.White.copy(0.4f))
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))

        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(32.dp), color = Color.White, border = BorderStroke(1.dp, Sage200)) {
            Column(Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Sage900, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("本周热力图", fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage900)
                }
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth()) {
                    weeklyData.forEach { day ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(36.dp).background(day.heat, RoundedCornerShape(10.dp)))
                            Spacer(Modifier.height(6.dp))
                            Text(day.label, fontSize = 9.sp, color = Sage400, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(2.dp))
                            Text("${day.count}", fontSize = 11.sp, fontWeight = FontWeight.Black, color = if (day.isFuture) Sage300 else Sage900)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Sage100)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("在家吃饭率: ${"%.0f".format(homeRate)}%", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage500)
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Less", fontSize = 9.sp, color = Sage400)
                        Box(Modifier.size(10.dp).background(HeatL1, RoundedCornerShape(2.dp)))
                        Box(Modifier.size(10.dp).background(HeatL2, RoundedCornerShape(2.dp)))
                        Box(Modifier.size(10.dp).background(HeatL3, RoundedCornerShape(2.dp)))
                        Box(Modifier.size(10.dp).background(HeatL4, RoundedCornerShape(2.dp)))
                        Text("More", fontSize = 9.sp, color = Sage400)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))

        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(32.dp), color = Color.White, border = BorderStroke(1.dp, Sage200)) {
            Column(Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Sage900, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("食材消耗排行榜", fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage900)
                }
                Spacer(Modifier.height(20.dp))
                if (topIngredients.isEmpty()) {
                    Text("暂无数据，完成烹饪后自动统计", fontSize = 12.sp, color = Sage300, modifier = Modifier.padding(vertical = 16.dp))
                } else {
                    topIngredients.forEachIndexed { i, ing ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${i + 1}.", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Sage400, modifier = Modifier.width(24.dp))
                                Text(ing.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Sage900)
                            }
                            Text("${ing.count} 次", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Sage500)
                        }
                        if (i < topIngredients.size - 1) HorizontalDivider(color = Sage50)
                    }
                }
            }
        }
    }
}
