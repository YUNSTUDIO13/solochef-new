package com.example.solochef.ui.screens.feedback

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.solochef.model.Recipe
import com.example.solochef.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val sentences = listOf(
    "关火，出锅。趁热尝第一口，是对主厨最好的犒劳。好好享受这一餐吧！",
    "香气已经满屋啦！挑一个你最喜欢的盘子盛出来，拍照记录，然后——开动吧。",
    "辛苦啦！今天也把生活照顾得很好。接下来的时间，只属于你和美味。",
    "看，你又解锁了一道新美味。把期待装盘，对自己说一声：开饭啦！",
    "把锅碗瓢盆先留给解冻的时间吧。现在的任务只有一个：趁热，开动！",
    "厨房的烟火气慢慢散了，餐桌上的美味正当热。洗手，坐下，世界先等等，吃饭第一。"
)

/**
 * LineItem for the receipt: grouped by recipe in batch mode.
 */
private data class ReceiptLine(
    val recipe: Recipe,
    val qty: Int,
    val lineTotal: Double  // qty * recipe.price
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FeedbackScreen(
    recipe: Recipe,
    batchRecipes: List<Recipe> = emptyList(),
    onDone: () -> Unit
) {
    val sentence = remember { sentences.random() }

    // Build receipt lines: single recipe or batch grouped
    val lines: List<ReceiptLine> = remember(recipe, batchRecipes) {
        if (batchRecipes.isEmpty()) {
            listOf(ReceiptLine(recipe, 1, recipe.price))
        } else {
            batchRecipes.groupBy { it.id }.map { (_, group) ->
                val r = group.first()
                ReceiptLine(r, group.size, group.size * r.price)
            }.sortedByDescending { it.qty }
        }
    }
    val grandTotal = lines.sumOf { it.lineTotal }

    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val receiptGraphicsLayer = rememberGraphicsLayer()
    val ticketNo = remember {
        val prefs = context.getSharedPreferences("solochef_feedback", android.content.Context.MODE_PRIVATE)
        val next = prefs.getInt("ticket_counter", 0) + 1
        prefs.edit().putInt("ticket_counter", next).apply()
        "%04d".format(next)
    }
    val dateTime = remember {
        val now = java.util.Date(System.currentTimeMillis())
        java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.getDefault()).format(now)
    }

    val infiniteTransition = rememberInfiniteTransition()
    val yAnim by infiniteTransition.animateFloat(0f, -5f, infiniteRepeatable(tween(600), RepeatMode.Reverse))

    Box(Modifier.fillMaxSize().background(Sage100)) {
        Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {

            // Tape decoration
            Box(Modifier.width(112.dp).height(24.dp).background(Color.White.copy(0.4f), RoundedCornerShape(2.dp)).border(1.dp, Color(0xFFE7E5E4).copy(0.2f), RoundedCornerShape(2.dp)))

            // Receipt paper
            Surface(
                Modifier.fillMaxWidth().offset(y = yAnim.dp).drawWithContent {
                    receiptGraphicsLayer.record { this@drawWithContent.drawContent() }
                    drawContent()
                },
                RoundedCornerShape(32.dp),
                color = Color(0xFFFAF9F4),
                border = BorderStroke(1.dp, Color(0xFFD6D3D1).copy(0.4f)),
                shadowElevation = 4.dp
            ) {
                Column(Modifier.padding(24.dp).padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    // Header
                    Text("solochef", fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Color(0xFF1C1917))
                    Text("独厨（蓝星旗舰店）", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF78716C))
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFFD6D3D1))
                    Spacer(Modifier.height(20.dp))

                    // Dine-in badge
                    Surface(Modifier, RoundedCornerShape(2.dp), Color.Transparent, border = BorderStroke(1.dp, Color(0xFF1C1917))) {
                        Text("堂食", Modifier.padding(horizontal = 28.dp, vertical = 4.dp), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp, color = Color(0xFF1C1917))
                    }
                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider(color = Color(0xFFD6D3D1))
                    Spacer(Modifier.height(20.dp))

                    // Ticket
                    Text("#$ticketNo", fontSize = 40.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.05).sp, color = Color(0xFF0C0A09))
                    Text("取票号", fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Color(0xFFA8A29E))
                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider(color = Color(0xFFD6D3D1))
                    Spacer(Modifier.height(16.dp))

                    // Delivery note
                    Text("Delivery note:", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Color(0xFFA8A29E), modifier = Modifier.fillMaxWidth())
                    Text("DELIVERY $dateTime", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF292524))
                    Text("OMS 3423423023230", fontSize = 12.sp, color = Color(0xFF78716C))
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFFD6D3D1))
                    Spacer(Modifier.height(16.dp))

                    // Order table header
                    Row(Modifier.fillMaxWidth()) {
                        Text("数量", Modifier.width(48.dp), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Color(0xFFA8A29E))
                        Text("商品名称", Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Color(0xFFA8A29E))
                        Text("金额 (¥)", Modifier.width(80.dp), textAlign = TextAlign.End, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Color(0xFFA8A29E))
                    }
                    Spacer(Modifier.height(8.dp))

                    // Order lines — each recipe from batch or single
                    lines.forEach { line ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text("${line.qty}x", Modifier.width(48.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C1917))
                            Text(line.recipe.name, Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF292524), maxLines = 1)
                            Text("%.2f".format(line.lineTotal), Modifier.width(80.dp), textAlign = TextAlign.End, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C1917))
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider(color = Color(0xFFD6D3D1).copy(0.8f))
                    Spacer(Modifier.height(12.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("餐点总价", fontSize = 12.sp, color = Color(0xFF78716C)); Text("%.2f".format(grandTotal), fontSize = 12.sp, color = Color(0xFF78716C)) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("配售税 (0.0%)", fontSize = 12.sp, color = Color(0xFF78716C)); Text("0.00", fontSize = 12.sp, color = Color(0xFF78716C)) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("环保打包费", fontSize = 12.sp, color = Color(0xFF78716C)); Text("0.00", fontSize = 12.sp, color = Color(0xFF78716C)) }
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFFD6D3D1))
                    Spacer(Modifier.height(16.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("顾客实付", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C1917))
                        Text("¥%.2f".format(grandTotal), fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF0C0A09))
                    }
                    Spacer(Modifier.height(24.dp))
                    HorizontalDivider(color = Color(0xFFD6D3D1))
                    Spacer(Modifier.height(24.dp))

                    // Chef's memo
                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(10.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("chef's memo / 温馨寄语", fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Color(0xFF57534E), modifier = Modifier.graphicsLayer(alpha = 0.4f))
                    }
                    Text("\"$sentence\"", Modifier.padding(horizontal = 16.dp, vertical = 12.dp), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF292524), textAlign = TextAlign.Center, lineHeight = 24.sp)
                    Text("✦ THANK YOU FOR DINING WITH US ✦", fontSize = 9.sp, letterSpacing = 2.sp, color = Color(0xFFA8A29E))
                    Text("Made in solochef smart kitchen ecosystem", fontSize = 8.sp, letterSpacing = 2.sp, color = Color(0xFFD6D3D1), modifier = Modifier.padding(top = 4.dp))
                }
            }
        }

        // Bottom bar — two buttons floating
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    onClick = onDone,
                    modifier = Modifier.weight(1f).height(64.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Sage800
                ) {
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("完成烹饪", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
                Surface(
                    onClick = {
                        scope.launch {
                            try {
                                val bmp = withContext(Dispatchers.Main) {
                                    receiptGraphicsLayer.toImageBitmap().asAndroidBitmap()
                                }
                                withContext(Dispatchers.IO) {
                                    val values = ContentValues().apply {
                                        put(MediaStore.Images.Media.DISPLAY_NAME, "SoloChef_$ticketNo.png")
                                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SoloChef")
                                    }
                                    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                                    uri?.let { context.contentResolver.openOutputStream(it)?.use { out -> bmp.compress(Bitmap.CompressFormat.PNG, 100, out) } }
                                }
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "小票已保存至 SoloChef 相册", Toast.LENGTH_SHORT).show()
                                }
                            } catch (_: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "保存失败，请检查存储权限", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).height(64.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Sage200)
                ) {
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Sage800, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("保存小票", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Sage800)
                    }
                }
        }
    }
}

