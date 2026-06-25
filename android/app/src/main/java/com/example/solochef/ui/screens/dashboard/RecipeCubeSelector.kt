package com.example.solochef.ui.screens.dashboard

import android.app.Activity
import android.view.Window
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.*
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import com.example.solochef.model.*
import com.example.solochef.ui.theme.*
import kotlinx.coroutines.delay

/** 温暖米色底色 — 与参考图一致 */
val CubeBgCream = Color(0xFFF2EDE8)

/** 卡片圆角 */
private val CardRadius = RoundedCornerShape(20.dp)

@Composable
fun RecipeCubeSelector(
    recipes: List<Recipe>,
    onClose: () -> Unit,
    onViewRecipe: (Recipe) -> Unit,
    onCookRecipe: (Recipe) -> Unit
) {
    val cutoff = System.currentTimeMillis() - 72L * 3600 * 1000
    val pool = remember(recipes) { recipes.filter { (it.last_cooked_at?.toLongOrNull() ?: 0) < cutoff } }

    var shuffleKey by remember { mutableIntStateOf(0) }
    val gridItems = remember(pool, shuffleKey) {
        if (pool.isEmpty()) emptyList<Recipe>()
        else List(9) { pool[it % pool.size] }.shuffled()
    }
    if (gridItems.size < 9) { LaunchedEffect(Unit) { onClose() }; return }

    var isScanning by remember { mutableStateOf(false) }
    var highlightIdx by remember { mutableIntStateOf(-1) }
    var finalIdx by remember { mutableIntStateOf(-1) }
    var showDialog by remember { mutableStateOf(false) }
    var selectedRecipe by remember { mutableStateOf<Recipe?>(null) }

    fun startScan() { if (!isScanning) { isScanning = true; highlightIdx = -1; finalIdx = -1; showDialog = false; selectedRecipe = null } }

    LaunchedEffect(isScanning) {
        if (!isScanning) return@LaunchedEffect
        val target = (0..8).random()
        val trace = intArrayOf(0, 1, 2, 5, 8, 7, 6, 3)
        val steps = 20 + (0..6).random()
        var pathIdx = 0; var speed = 40L
        for (i in 0 until steps) {
            highlightIdx = trace[pathIdx]; pathIdx = (pathIdx + 1) % trace.size
            speed = when { i >= steps - 4 -> 500L; i >= steps - 8 -> 250L; i >= steps - 14 -> 100L; else -> 40L }
            delay(speed)
        }
        finalIdx = target; highlightIdx = target
        delay(200)
        selectedRecipe = gridItems[target]; showDialog = true
        isScanning = false
    }

    var triggerReshuffle by remember { mutableStateOf(false) }
    LaunchedEffect(triggerReshuffle) { if (triggerReshuffle) { delay(350); triggerReshuffle = false; shuffleKey++ } }
    LaunchedEffect(shuffleKey) { if (shuffleKey > 0) { delay(200); startScan() } }

    // ── 修复：系统返回手势 → 关闭此页面 ──
    BackHandler { onClose() }

    // ── 修复：状态栏底色与页面一致 + 深色状态栏图标 ──
    val view = LocalView.current
    val window = remember(view) { (view.context as Activity).window }
    DisposableEffect(Unit) {
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.isAppearanceLightStatusBars = true
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        val origFlags = window.decorView.systemUiVisibility
        onDispose {
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    // ── 自适应卡片尺寸 ──
    val density = LocalDensity.current
    val screenWidthDp = with(density) { LocalConfiguration.current.screenWidthDp.dp }
    val horizontalPadding = 24.dp * 2
    val gapTotal = 10.dp * 2
    val cellSize = (screenWidthDp - horizontalPadding - gapTotal) / 3
    val g = 10.dp

    Box(Modifier.fillMaxSize().background(CubeBgCream)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 40.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── 标题区域：左对齐 ──
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontSize = 30.sp, fontWeight = FontWeight.Black, color = Color(0xFF1C1917))) { append("今天到底\n") }
                        withStyle(SpanStyle(fontSize = 30.sp, fontWeight = FontWeight.Black, color = Color(0xFF1C1917))) { append("吃点啥?") }
                    },
                    lineHeight = 36.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "让食运做出最好的安排",
                    fontSize = 13.sp,
                    color = Color(0xFF9CA3AF),
                    letterSpacing = 1.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── 3×3 菜谱网格 ──
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(g)
                ) {
                    for (row in 0..2) {
                        Row(horizontalArrangement = Arrangement.spacedBy(g)) {
                            for (col in 0..2) {
                                val i = row * 3 + col
                                val r = gridItems[i]
                                val hl = highlightIdx == i
                                val fin = finalIdx == i
                                RecipeCard(cell = cellSize, recipe = r, highlighted = hl, finished = fin)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── 底部按钮区：纵向堆叠 ──
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 食来运转 — 绿色填充圆角胶囊按钮（无图标）
                Surface(
                    onClick = { startScan() },
                    enabled = !isScanning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(50),
                    color = if (isScanning) Color(0xFFBDBDBD) else Color(0xFF4A7C59)
                ) {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (isScanning) "转动中..." else "食来运转",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // 返回首页 — 白底绿色边框圆角胶囊按钮
                Surface(
                    onClick = onClose,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(50),
                    color = Color.White,
                    border = BorderStroke(1.5.dp, Color(0xFF4A7C59))
                ) {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "返回首页",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF4A7C59)
                        )
                    }
                }
            }
        }

        // ── 结果弹窗 ──
        if (showDialog && selectedRecipe != null) {
            val r = selectedRecipe!!
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.padding(32.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    color = Color(0xFF1C1917),
                    shadowElevation = 24.dp
                ) {
                    Column(
                        Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "命运的食光之轮选中了【${r.name}】，今日犒劳就它了！",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFCD34D),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(14.dp))
                        Surface(
                            modifier = Modifier.size(120.dp),
                            shape = RoundedCornerShape(24.dp),
                            shadowElevation = 8.dp
                        ) {
                            AsyncImage(r.cover_image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            r.name,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        if (r.tags.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                r.tags.take(3).forEach { t ->
                                    Surface(shape = RoundedCornerShape(8.dp), color = Color.White.copy(alpha = 0.08f)) {
                                        Text("#$t", Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 9.sp, color = Color.White.copy(alpha = 0.6f))
                                    }
                                }
                            }
                        }
                        if (r.cost > 0) {
                            Spacer(Modifier.height(4.dp))
                            Text("¥${"%.1f".format(r.cost)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA8A29E))
                        }
                        Spacer(Modifier.height(20.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(
                                onClick = { showDialog = false; selectedRecipe = null; triggerReshuffle = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.08f)
                            ) {
                                Box(Modifier.padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                                    Text("再来一次", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.6f))
                                }
                            }
                            Surface(
                                onClick = { onCookRecipe(r) },
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(50),
                                color = GreenPlay
                            ) {
                                Box(Modifier.padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                                    Text("就吃这个", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { showDialog = false }) {
                            Text("关闭", fontSize = 12.sp, color = Color.White.copy(alpha = 0.35f))
                        }
                    }
                }
            }
        }
    }
}

/** 单个菜谱卡片 — 圆角大图 + 底部渐变蒙层 + 居中文案 */
@Composable
private fun RecipeCard(
    cell: Dp,
    recipe: Recipe,
    highlighted: Boolean,
    finished: Boolean
) {
    Box(
        Modifier
            .size(cell)
            .clip(CardRadius)
            .then(
                when {
                    finished -> Modifier.border(2.5.dp, Color(0xFFFF7043), CardRadius)
                    highlighted -> Modifier.border(2.5.dp, GreenPlay, CardRadius)
                    else -> Modifier.border(0.5.dp, Color(0x1A000000), CardRadius)
                }
            )
    ) {
        AsyncImage(
            recipe.cover_image,
            null,
            Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        if (highlighted && !finished) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(GreenPlay.copy(alpha = 0.15f))
            )
        }

        Text(
            recipe.name,
            Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 6.dp, vertical = 8.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
