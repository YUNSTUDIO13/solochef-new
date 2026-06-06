package com.example.solochef.ui.screens.analytics

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.solochef.model.CookingRecord
import com.example.solochef.model.Recipe
import com.example.solochef.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// ─── Data class for calendar day ──────────────────────

data class CalendarDay(
    val date: Int,          // day of month (1-31), 0 = padding
    val month: Int,
    val year: Int,
    val records: List<CookingRecord>,
    val isToday: Boolean,
    val isCurrentMonth: Boolean
)

// ─── FoodCalendar ─────────────────────────────────────

@Composable
fun FoodCalendar(
    records: List<CookingRecord>,
    recipes: List<Recipe>,
    onSelectRecipe: (Recipe) -> Unit
) {
    val cal = remember { Calendar.getInstance() }
    var currentMonth by remember { mutableIntStateOf(cal.get(Calendar.MONTH)) }
    var currentYear by remember { mutableIntStateOf(cal.get(Calendar.YEAR)) }
    var selectedDay by remember { mutableStateOf<CalendarDay?>(null) }

    val today = Calendar.getInstance()
    val todayYear = today.get(Calendar.YEAR)
    val todayMonth = today.get(Calendar.MONTH)
    val todayDay = today.get(Calendar.DAY_OF_MONTH)

    // Build day grid
    val days = remember(currentMonth, currentYear, records) {
        buildDayGrid(currentYear, currentMonth, records, todayYear, todayMonth, todayDay)
    }

    // Weekday headers
    val weekHeaders = listOf("日", "一", "二", "三", "四", "五", "六")

    // Month label
    val monthLabel = "${currentYear}年${currentMonth + 1}月"

    Column(modifier = Modifier.fillMaxWidth()) {
        // Month navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (currentMonth == 0) { currentMonth = 11; currentYear-- }
                    else currentMonth--
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.KeyboardArrowLeft, null, tint = Sage500, modifier = Modifier.size(20.dp))
            }
            Text(monthLabel, fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage900)
            IconButton(
                onClick = {
                    if (currentMonth == 11) { currentMonth = 0; currentYear++ }
                    else currentMonth++
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.KeyboardArrowRight, null, tint = Sage500, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(Modifier.height(8.dp))

        // Weekday headers
        Row(Modifier.fillMaxWidth()) {
            weekHeaders.forEach { day ->
                Text(
                    day, modifier = Modifier.weight(1f),
                    fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    color = Sage400, textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Day grid (chunk into weeks)
        Box {
            Column {
                days.chunked(7).forEachIndexed { _, weekDays ->
                    Row(Modifier.fillMaxWidth()) {
                        weekDays.forEach { day ->
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp)) {
                                if (day.date > 0 && day.isCurrentMonth) {
                                    CalendarDayCell(
                                        day = day,
                                        isSelected = selectedDay?.let { it.date == day.date && it.month == day.month && it.year == day.year } == true,
                                        onClick = {
                                            selectedDay = if (day.records.isNotEmpty()) day else null
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Legend row — hidden per user request
    }

    // ── Day detail bottom sheet ──
    if (selectedDay != null) {
        DayDetailSheet(
            day = selectedDay!!,
            recipes = recipes,
            onDismiss = { selectedDay = null },
            onSelectRecipe = onSelectRecipe
        )
    }
}

// ─── CalendarDayCell ──────────────────────────────────

@Composable
private fun CalendarDayCell(
    day: CalendarDay,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val hasRecords = day.records.isNotEmpty()
    val coverImage = if (hasRecords) {
        // Pick the latest recipe's cover image
        day.records.maxByOrNull { it.cookedAt }?.coverImage ?: ""
    } else ""

    // Breathing animation ring
    val infiniteTransition = rememberInfiniteTransition(label = "breath")
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringScale"
    )
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) Sage100 else Color.White)
            .then(
                if (hasRecords) Modifier.border(
                    1.5.dp,
                    (if (day.isToday) Sage800 else Sage400).copy(alpha = ringAlpha),
                    RoundedCornerShape(10.dp)
                ) else Modifier.border(1.5.dp, Sage100, RoundedCornerShape(10.dp))
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (hasRecords && coverImage.isNotBlank()) {
            AsyncImage(
                model = coverImage,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
            // Subtle overlay
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
            )
        }

        // Breathing ring overlay on top
        if (hasRecords) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(scaleX = ringScale, scaleY = ringScale),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.88f)
                        .border(1.dp, Sage400.copy(alpha = ringAlpha), RoundedCornerShape(8.dp))
                )
            }
        }

        // Date number — only show if no records (image covers it)
        if (!hasRecords) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${day.date}",
                    fontSize = 12.sp,
                    fontWeight = if (day.isToday) FontWeight.Black else FontWeight.Bold,
                    color = if (day.isToday) Sage900 else Sage500,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Today indicator dot
        if (day.isToday && !hasRecords) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(Sage800)
            )
        }
    }
}

// ─── DayDetailSheet ───────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayDetailSheet(
    day: CalendarDay,
    recipes: List<Recipe>,
    onDismiss: () -> Unit,
    onSelectRecipe: (Recipe) -> Unit
) {
    val sdf = SimpleDateFormat("M月d日 · EEEE", Locale.CHINESE)
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, day.year)
        set(Calendar.MONTH, day.month)
        set(Calendar.DAY_OF_MONTH, day.date)
    }
    val dateLabel = sdf.format(cal.time)

    // Map record recipeIds to actual Recipe objects
    val dayRecipes = day.records.mapNotNull { record ->
        recipes.find { it.id == record.recipeId }
    }.distinctBy { it.id }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Sage200))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(dateLabel, fontSize = 16.sp, fontWeight = FontWeight.Black, color = Sage900)
                Text("共${dayRecipes.size}道菜", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Sage400, letterSpacing = 2.sp)
            }

            Spacer(Modifier.height(16.dp))

            if (dayRecipes.isEmpty()) {
                Text("暂无菜谱数据", fontSize = 12.sp, color = Sage300, modifier = Modifier.padding(vertical = 16.dp))
            } else {
                dayRecipes.forEachIndexed { i, recipe ->
                    DayRecipeRow(recipe = recipe, onClick = { onSelectRecipe(recipe) })
                    if (i < dayRecipes.size - 1) {
                        HorizontalDivider(color = Sage50, modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DayRecipeRow(recipe: Recipe, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
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

        Column(modifier = Modifier.weight(1f)) {
            Text(recipe.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Sage900, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                recipe.tags.firstOrNull()?.let { tag ->
                    Surface(shape = RoundedCornerShape(4.dp), color = Amber50) {
                        Text(tag, modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Amber400)
                    }
                }
                Text(
                    "~${recipe.timeline.sumOf { it.duration } / 60}分钟",
                    fontSize = 9.sp,
                    color = Sage400,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Icon(Icons.Default.KeyboardArrowRight, null, tint = Sage300, modifier = Modifier.size(18.dp))
    }
}

// ─── Day grid builder ─────────────────────────────────

private fun buildDayGrid(
    year: Int,
    month: Int,
    records: List<CookingRecord>,
    todayYear: Int,
    todayMonth: Int,
    todayDay: Int
): List<CalendarDay> {
    val cal = Calendar.getInstance()
    cal.set(year, month, 1)
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun ... 7=Sat
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val result = mutableListOf<CalendarDay>()

    // Padding days from previous month
    val paddingBefore = firstDayOfWeek - 1
    for (i in 0 until paddingBefore) {
        result.add(CalendarDay(0, month, year, emptyList(), false, false))
    }

    // Actual days
    for (day in 1..daysInMonth) {
        cal.set(year, month, day)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startMs = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        val endMs = cal.timeInMillis

        val dayRecords = records.filter { it.cookedAt in startMs..<endMs }
        val isToday = year == todayYear && month == todayMonth && day == todayDay

        result.add(CalendarDay(day, month, year, dayRecords, isToday, true))
    }

    // Padding to fill last row
    val remaining = (7 - result.size % 7) % 7
    for (i in 0 until remaining) {
        result.add(CalendarDay(0, month, year, emptyList(), false, false))
    }

    return result
}
