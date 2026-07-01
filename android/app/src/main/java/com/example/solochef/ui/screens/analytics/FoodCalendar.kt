package com.example.solochef.ui.screens.analytics

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import kotlinx.coroutines.launch
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
    onSelectRecipe: (Recipe) -> Unit,
    onShareReceipt: ((List<Recipe>) -> Unit)? = null,
    onDeleteRecord: ((String) -> Unit)? = null,
    onCreateRecord: ((Long) -> Unit)? = null
) {
    val cal = remember { Calendar.getInstance() }
    var currentMonth by remember { mutableIntStateOf(cal.get(Calendar.MONTH)) }
    var currentYear by remember { mutableIntStateOf(cal.get(Calendar.YEAR)) }
    var selectedDay by remember { mutableStateOf<CalendarDay?>(null) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var pickerYear by remember { mutableIntStateOf(cal.get(Calendar.YEAR)) }

    val today = Calendar.getInstance()
    val todayYear = today.get(Calendar.YEAR)
    val todayMonth = today.get(Calendar.MONTH)
    val todayDay = today.get(Calendar.DAY_OF_MONTH)

    // Year range: 2024 to current year + 1
    val yearRange = (2024..todayYear + 1).toList()
    val months = listOf("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月")

    // Build day grid
    val days = remember(currentMonth, currentYear, records) {
        buildDayGrid(currentYear, currentMonth, records, todayYear, todayMonth, todayDay)
    }

    // Weekday headers
    val weekHeaders = listOf("日", "一", "二", "三", "四", "五", "六")

    // Month label
    val monthLabel = "${currentYear}年${currentMonth + 1}月"

    Column(modifier = Modifier.fillMaxWidth()) {
        // Month navigation with clickable picker
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
            Surface(
                onClick = { pickerYear = currentYear; showMonthPicker = true },
                shape = RoundedCornerShape(12.dp),
                color = Sage100
            ) {
                Text(
                    monthLabel, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage900
                )
            }
            Row {
                // Jump to today
                val isCurrentView = currentYear == todayYear && currentMonth == todayMonth
                if (!isCurrentView) {
                    TextButton(
                        onClick = { currentYear = todayYear; currentMonth = todayMonth }
                    ) {
                        Text("今天", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Sage400)
                    }
                }
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
        }

        // Month picker dialog
        if (showMonthPicker) {
            AlertDialog(
                onDismissRequest = { showMonthPicker = false },
                title = { Text("选择月份", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Sage900) },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        // Year selector
                        Text("年份", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage400)
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(yearRange) { yr ->
                                val sel = yr == pickerYear
                                Surface(
                                    onClick = { pickerYear = yr },
                                    shape = RoundedCornerShape(20),
                                    color = if (sel) Sage800 else Color.Transparent
                                ) {
                                    Text(
                                        "${yr}", modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                        fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                        color = if (sel) Color.White else Sage500
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        // Month grid
                        Text("月份", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage400)
                        Spacer(Modifier.height(8.dp))
                        months.chunked(4).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEachIndexed { _, m ->
                                    val mIdx = months.indexOf(m)
                                    val sel = mIdx == currentMonth && pickerYear == currentYear
                                    Surface(
                                        onClick = { currentMonth = mIdx; currentYear = pickerYear; showMonthPicker = false },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(20),
                                        color = if (sel) Sage800 else Color.Transparent
                                    ) {
                                        Text(
                                            m, modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth(),
                                            textAlign = TextAlign.Center,
                                            fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                            color = if (sel) Color.White else Sage500
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        currentYear = pickerYear
                        showMonthPicker = false
                    }) { Text("确定", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Sage800) }
                },
                dismissButton = {
                    TextButton(onClick = { showMonthPicker = false }) { Text("取消", fontSize = 12.sp, color = Sage400) }
                }
            )
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
                                            selectedDay = day
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
            onSelectRecipe = onSelectRecipe,
            onShareReceipt = onShareReceipt,
            onDeleteRecord = onDeleteRecord,
            onCreateRecord = onCreateRecord
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
    onSelectRecipe: (Recipe) -> Unit,
    onShareReceipt: ((List<Recipe>) -> Unit)? = null,
    onDeleteRecord: ((String) -> Unit)? = null,
    onCreateRecord: ((Long) -> Unit)? = null
) {
    val sdf = SimpleDateFormat("M月d日 · EEEE", Locale.CHINESE)
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, day.year)
        set(Calendar.MONTH, day.month)
        set(Calendar.DAY_OF_MONTH, day.date)
    }
    val dateLabel = sdf.format(cal.time)
    val dayTimestamp = cal.timeInMillis

    // Group records by recipeId with counts
    data class DayRecipeEntry(val recipe: Recipe, val count: Int, val recordIds: List<String>)
    val dayEntries: List<DayRecipeEntry> = day.records
        .groupBy { it.recipeId }
        .mapNotNull { (rid, recs) ->
            recipes.find { it.id == rid }?.let { DayRecipeEntry(it, recs.size, recs.map { r -> r.id }) }
        }

    val receiptRecipes: List<Recipe> = day.records.mapNotNull { r -> recipes.find { it.id == r.recipeId } }

    // Delete confirmation dialog
    var deleteRecordId by remember { mutableStateOf<String?>(null) }
    if (deleteRecordId != null) {
        AlertDialog(
            onDismissRequest = { deleteRecordId = null },
            containerColor = Color.White,
            shape = RoundedCornerShape(28.dp),
            title = { Text("请确认是否删除？", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Sage900) },
            text = { Text("删除后无法恢复", fontSize = 13.sp, color = Sage500) },
            confirmButton = {
                Button(
                    onClick = {
                        val id = deleteRecordId; deleteRecordId = null
                        onDeleteRecord?.invoke(id!!)
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) { Text("确认", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { deleteRecordId = null }) {
                    Text("取消", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Sage500)
                }
            }
        )
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
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
                if (dayEntries.isNotEmpty()) {
                    Text("共${dayEntries.sumOf { it.count }}份·${dayEntries.size}道菜", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Sage400, letterSpacing = 2.sp)
                }
            }

            Spacer(Modifier.height(16.dp))

            if (dayEntries.isEmpty()) {
                // Empty state — "新建食光" button
                Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Restaurant, null, tint = Sage300, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(16.dp))
                        Surface(
                            onClick = { onCreateRecord?.invoke(dayTimestamp) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = Sage800
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("新建食光", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }
                    }
                }
            } else {
                // Records with swipe-to-delete
                dayEntries.forEachIndexed { i, entry ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                deleteRecordId = entry.recordIds.first()
                            }
                            false // Always reset to original position
                        }
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            Box(
                                Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                                    .background(Color.White),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                IconButton(
                                    onClick = { deleteRecordId = entry.recordIds.first() },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete, null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        },
                        enableDismissFromStartToEnd = true,
                        enableDismissFromEndToStart = true
                    ) {
                        Surface(color = Color.White) {
                            DayRecipeRow(
                                recipe = entry.recipe,
                                count = entry.count,
                                onClick = { onSelectRecipe(entry.recipe) }
                            )
                        }
                    }
                    if (i < dayEntries.size - 1) {
                        HorizontalDivider(color = Sage50, modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }

            // Share receipt button (only when has records)
            if (onShareReceipt != null && receiptRecipes.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Surface(
                    onClick = {
                        val recipes = receiptRecipes
                        scope.launch {
                            sheetState.hide()
                            onShareReceipt(recipes)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Sage800
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Share, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("分享小票", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun DayRecipeRow(recipe: Recipe, count: Int = 1, onClick: () -> Unit) {
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

        // Quantity badge
        if (count > 1) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Sage800.copy(alpha = 0.1f)
            ) {
                Text(
                    "x$count",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Sage800
                )
            }
            Spacer(Modifier.width(8.dp))
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
