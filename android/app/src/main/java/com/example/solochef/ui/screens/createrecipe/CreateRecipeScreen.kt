package com.example.solochef.ui.screens.createrecipe

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.solochef.ImageUtils
import com.example.solochef.model.*
import com.example.solochef.ui.screens.library.*
import com.example.solochef.ui.theme.*
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateRecipeScreen(
    existingRecipe: Recipe?,
    onSave: (Recipe) -> Unit,
    onCancel: () -> Unit
) {
    val name = remember { mutableStateOf(existingRecipe?.name ?: "") }
    val energy = remember { mutableStateOf(existingRecipe?.energy_level ?: EnergyLevel.Mid) }
    val featured = remember { mutableStateOf(existingRecipe?.is_featured ?: false) }
    var cover by remember { mutableStateOf(existingRecipe?.cover_image ?: "") }
    var bomSnapshot by remember { mutableStateOf(existingRecipe?.bom_snapshot) }
    val materials = remember {
        mutableStateOf(existingRecipe?.materials?.toMutableMap() ?: mutableMapOf(
            "meat" to mutableListOf<Material>(),
            "vegetable" to mutableListOf<Material>(),
            "seasoning" to mutableListOf<Material>()
        ))
    }
    val timeline = remember { mutableStateOf(existingRecipe?.timeline?.toMutableList() ?: mutableListOf()) }
    val tags = remember { mutableStateOf((existingRecipe?.tags ?: emptyList()).toMutableList()) }
    val customTag = remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val cost = remember { mutableStateOf(existingRecipe?.cost?.toString() ?: "") }
    val price = remember { mutableStateOf(existingRecipe?.price?.toString() ?: "") }

    // ── Image targets ──
    var imageTarget by remember { mutableStateOf("") } // "cover" | "bom" | "step"
    var stepImageId by remember { mutableStateOf(0L) }
    var coverLoading by remember { mutableStateOf(false) }
    var bomLoading by remember { mutableStateOf(false) }

    // ── Material helpers ──
    fun addMaterial(cat: String) {
        val m = materials.value.toMutableMap()
        m[cat] = ((m[cat] ?: mutableListOf()) + Material()).toMutableList()
        materials.value = m
    }
    fun updateMaterial(cat: String, idx: Int, u: Material) {
        val m = materials.value.toMutableMap()
        val list = (m[cat] ?: mutableListOf()).toMutableList()
        list[idx] = u; m[cat] = list; materials.value = m
    }
    fun removeMaterial(cat: String, idx: Int) {
        val m = materials.value.toMutableMap()
        val list = (m[cat] ?: mutableListOf()).toMutableList()
        list.removeAt(idx); m[cat] = list; materials.value = m
    }
    fun addCommonSeasonings() {
        val seasonings = listOf(
            Material(item = "盐", amount = "适量", unit = ""),
            Material(item = "油", amount = "适量", unit = ""),
            Material(item = "生抽", amount = "1", unit = "勺"),
            Material(item = "糖", amount = "少许", unit = "")
        )
        val existing = (materials.value["seasoning"] ?: emptyList()).map { it.item }.toSet()
        val new = seasonings.filter { it.item !in existing }
        if (new.isNotEmpty()) {
            val m = materials.value.toMutableMap()
            m["seasoning"] = ((m["seasoning"] ?: mutableListOf()) + new).toMutableList()
            materials.value = m
        }
    }

    // ── Timeline helpers ──
    fun addStep() {
        val ts = TimelineStep(step_id = System.currentTimeMillis(), type = "action", content = "", duration = 60, sub_tasks = mutableListOf())
        timeline.value = timeline.value.toMutableList().apply { add(ts) }
    }
    fun updateStep(id: Long, u: TimelineStep) {
        timeline.value = timeline.value.map { if (it.step_id == id) u else it }.toMutableList()
    }
    fun removeStep(id: Long) {
        timeline.value = timeline.value.filter { it.step_id != id }.toMutableList()
    }
    fun addSubTask(stepId: Long) {
        timeline.value = timeline.value.map { s ->
            if (s.step_id == stepId) s.copy(sub_tasks = (s.sub_tasks ?: mutableListOf()) + SubTask("", 60))
            else s
        }.toMutableList()
    }
    fun updateSubTask(stepId: Long, subIdx: Int, sub: SubTask) {
        timeline.value = timeline.value.map { s ->
            if (s.step_id == stepId) {
                val subs = (s.sub_tasks ?: mutableListOf()).toMutableList()
                subs[subIdx] = sub; s.copy(sub_tasks = subs)
            } else s
        }.toMutableList()
    }
    fun removeSubTask(stepId: Long, subIdx: Int) {
        timeline.value = timeline.value.map { s ->
            if (s.step_id == stepId) {
                val subs = (s.sub_tasks ?: mutableListOf()).toMutableList()
                subs.removeAt(subIdx); s.copy(sub_tasks = subs)
            } else s
        }.toMutableList()
    }

    // ── Image compression → local file (Dispatchers.IO) ──
    fun dispatchImage(path: String) {
        when (imageTarget) {
            "cover" -> cover = path
            "bom" -> bomSnapshot = path
            "step" -> {
                timeline.value = timeline.value.map { s ->
                    if (s.step_id == stepImageId) s.copy(images = (s.images?.toMutableList() ?: mutableListOf()).apply { add(path) }.toMutableList())
                    else s
                }.toMutableList()
                stepImageId = 0L
            }
        }
        imageTarget = ""
    }

    val pickLauncher = rememberLauncherForActivityResult(PickVisualMedia()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val target = imageTarget
        if (target == "cover") coverLoading = true
        if (target == "bom") bomLoading = true
        scope.launch {
            val targetFile = File(context.filesDir, "img_${target}_${System.currentTimeMillis()}.jpg")
            try {
                val ok = ImageUtils.compressAndSaveImage(context, uri, targetFile)
                if (ok) dispatchImage(targetFile.absolutePath)
            } finally {
                coverLoading = false
                bomLoading = false
            }
        }
    }

    // ── UI matching Web pixel-for-pixel ──
    Column(Modifier.fillMaxSize().background(Sage100)) {
        // Header
        Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(if (existingRecipe != null) "编辑菜谱" else "新建菜谱", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Sage900)
            IconButton(onClick = onCancel) { Icon(Icons.Default.Close, contentDescription = null, tint = Sage500, modifier = Modifier.size(24.dp)) }
        }

        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 24.dp, end = 24.dp, top = 24.dp)) {
            // ── Cover Image (Web: section > label > div.relative) ──
            Text("成品封面图 (Required)", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage500)
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(40.dp)).border(2.dp, Sage200, RoundedCornerShape(40.dp)).background(Color.White).clickable { imageTarget = "cover"; coverLoading = true; pickLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) }, contentAlignment = Alignment.Center) {
                if (coverLoading) {
                    CircularProgressIndicator(Modifier.size(36.dp), strokeWidth = 3.dp, color = Sage800)
                } else if (cover.isNotEmpty()) {
                    AsyncImage(cover, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.2f)), contentAlignment = Alignment.Center) {
                        Surface(Modifier.padding(horizontal = 24.dp, vertical = 16.dp).clip(RoundedCornerShape(24.dp)), color = Color.White.copy(0.9f)) {
                            Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Sage900, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp)); Text("更换封面", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Sage900)
                            }
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = Sage400, modifier = Modifier.size(48.dp))
                        Text("点击上传主图", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Sage500, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
            Spacer(Modifier.height(32.dp))

            // ── Basic Info (Web: section.space-y-4) ──
            Text("菜谱名称", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage500)
            OutlinedTextField(name.value, { name.value = it }, Modifier.fillMaxWidth().padding(top = 4.dp), placeholder = { Text("例如：红烧肉", color = Sage300) }, singleLine = true, shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Sage400, unfocusedBorderColor = Sage200, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Black, color = Sage900))
            Spacer(Modifier.height(16.dp))

            // Energy + Featured (Web: grid grid-cols-2)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("精力等级", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage500)
                    var eExpanded by remember { mutableStateOf(false) }
                    Box { Surface(onClick = { eExpanded = true }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp), shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, Sage200)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Text(ENERGY_LEVEL_OPTIONS.find { it.first == energy.value }?.second ?: "正常", Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Sage900); Icon(Icons.Default.ArrowDropDown, null, tint = Sage400, modifier = Modifier.size(20.dp)) } }
                        DropdownMenu(eExpanded, { eExpanded = false }) { ENERGY_LEVEL_OPTIONS.forEach { (l, lbl) -> DropdownMenuItem(text = { Text(lbl, fontWeight = FontWeight.Bold) }, onClick = { energy.value = l; eExpanded = false }) } }
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text("主推菜", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage500)
                    Surface(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, Sage200)) {
                        Row(Modifier.padding(4.dp)) {
                            Surface(onClick = { featured.value = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), color = if (featured.value) Sage800 else Color.Transparent) { Text("是 (Yes)", modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (featured.value) Color.White else Sage400) }
                            Surface(onClick = { featured.value = false }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), color = if (!featured.value) Sage800 else Color.Transparent) { Text("否 (No)", modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (!featured.value) Color.White else Sage400) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))

            // ── Cost + Price (Web: between basic-info and tags) ──
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("成本（元）", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage500)
                    OutlinedTextField(cost.value, { cost.value = it }, Modifier.fillMaxWidth().padding(top = 4.dp), placeholder = { Text("例：5", color = Sage300) }, singleLine = true, shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Sage400, unfocusedBorderColor = Sage200, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Sage900))
                }
                Column(Modifier.weight(1f)) {
                    Text("售价（元）", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage500)
                    OutlinedTextField(price.value, { price.value = it }, Modifier.fillMaxWidth().padding(top = 4.dp), placeholder = { Text("例：10", color = Sage300) }, singleLine = true, shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Sage400, unfocusedBorderColor = Sage200, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Sage900))
                }
            }
            Spacer(Modifier.height(32.dp))

            // ── Tags (Web: white card inside basic-info section) ──
            Text("分类标签 (Tags)", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage500)
            Spacer(Modifier.height(12.dp))
            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(32.dp), Color.White, border = BorderStroke(1.dp, Sage200)) {
                Column(Modifier.padding(24.dp)) {
                    Text("烹饪工艺", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage400)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        COOKING_PROCESS_TAGS.forEach { tag ->
                            val sel = tag in tags.value
                            Surface(onClick = { tags.value = (tags.value.filter { it !in COOKING_PROCESS_TAGS } + tag).toMutableList() }, shape = RoundedCornerShape(50), color = if (sel) Sage800 else Sage50) {
                                Text(tag, Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (sel) Color.White else Sage500)
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("菜系维度", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage400)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CUISINE_TAGS.forEach { tag ->
                            val sel = tag in tags.value
                            Surface(onClick = { tags.value = (tags.value.filter { it !in CUISINE_TAGS } + tag).toMutableList() }, shape = RoundedCornerShape(50), color = if (sel) Sage800 else Sage50) {
                                Text(tag, Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (sel) Color.White else Sage500)
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("自选 / 其它", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage400)
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(customTag.value, { customTag.value = it }, Modifier.weight(1f), placeholder = { Text("输入自定义标签...", color = Sage300) }, singleLine = true, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Sage400, unfocusedBorderColor = Sage100, focusedContainerColor = Sage50, unfocusedContainerColor = Sage50), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Sage900))
                        Spacer(Modifier.width(8.dp))
                        Surface(onClick = { val t = customTag.value.trim(); if (t.isNotEmpty() && t !in tags.value) { tags.value = tags.value.toMutableList().apply { add(t) }; customTag.value = "" } }, shape = RoundedCornerShape(12.dp), color = Sage800) {
                            Text("添加", Modifier.padding(horizontal = 16.dp, vertical = 10.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
            Spacer(Modifier.height(40.dp))

            // ── BOM Section (Web: section.mb-10 > h2 + 3-categories + snapshot) ──
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("物料清单 (BOM)", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Sage900)
                Surface(onClick = { addCommonSeasonings() }, shape = RoundedCornerShape(12.dp), color = Sage800) {
                    Text("一键调味", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Color.White)
                }
            }
            Spacer(Modifier.height(20.dp))

            // BOM snapshot inside white card (Web)
            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(40.dp), Color.White, border = BorderStroke(1.dp, Sage200)) {
                Column(Modifier.padding(32.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("食材全家福 (BOM Snapshot)", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage900)
                            Text("拍摄已准备好的所有食材，方便后续核对", fontSize = 10.sp, color = Sage400)
                        }
                        if (bomSnapshot != null) IconButton(onClick = { bomSnapshot = null }) { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp)) }
                    }
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(24.dp)).border(2.dp, Sage100, RoundedCornerShape(24.dp)).background(Sage50).clickable { imageTarget = "bom"; bomLoading = true; pickLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) }, contentAlignment = Alignment.Center) {
                        if (bomLoading) CircularProgressIndicator(Modifier.size(32.dp), strokeWidth = 2.dp, color = Sage800)
                        else if (bomSnapshot?.isNotEmpty() == true) AsyncImage(bomSnapshot!!, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(Modifier.size(64.dp), RoundedCornerShape(50), Color.White) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.CameraAlt, null, tint = Sage300, modifier = Modifier.size(32.dp)) } }
                            Text("点击拍摄食材全拼", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage500, modifier = Modifier.padding(top = 12.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))

            // Material categories (Web)
            listOf("meat" to "肉禽 / 水产", "vegetable" to "蔬菜 / 蔬果", "seasoning" to "调味 / 其它").forEach { (cat, label) ->
                val icon = when (cat) { "meat" -> Icons.Default.SetMeal; "vegetable" -> Icons.Default.Eco; else -> Icons.Default.Opacity }
                val items = materials.value[cat] ?: mutableListOf()
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = Sage500, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(8.dp)); Text(label, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage500) }
                    Surface(onClick = { addMaterial(cat) }, shape = RoundedCornerShape(50), color = Color.Transparent) { Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Add, null, tint = Sage800, modifier = Modifier.size(12.dp)); Text("添加", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Sage800) } }
                }
                Spacer(Modifier.height(12.dp))
                if (items.isEmpty()) {
                    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(28.dp), Color.Transparent, border = BorderStroke(1.dp, Sage200)) { Text("无", Modifier.padding(vertical = 16.dp).fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage400) }
                }
                items.forEachIndexed { idx, m ->
                    Surface(Modifier.fillMaxWidth().padding(vertical = 2.dp), RoundedCornerShape(28.dp), Color.White, border = BorderStroke(1.dp, Sage200)) {
                        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(m.item, { updateMaterial(cat, idx, m.copy(item = it)) }, Modifier.weight(1f), placeholder = { Text("食材", color = Sage300) }, singleLine = true, shape = RoundedCornerShape(0.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Sage900))
                            Surface(modifier = Modifier.clip(RoundedCornerShape(16.dp)), color = Color.White, border = BorderStroke(1.dp, Sage100)) {
                                val combinedAmount = if (m.unit.isNotEmpty()) "${m.amount}${m.unit}" else m.amount
                                BasicTextField(combinedAmount, { updateMaterial(cat, idx, m.copy(amount = it, unit = "")) }, Modifier.width(92.dp).padding(vertical = 4.dp), singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Sage900, textAlign = TextAlign.Center), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text), decorationBox = { innerTextField -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { innerTextField() } })
                            }
                            IconButton(onClick = { removeMaterial(cat, idx) }, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.Delete, null, tint = Color(0xFFF87171), modifier = Modifier.size(18.dp)) }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            // ── Timeline Section (Web) ──
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("烹饪步骤 (Timeline)", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Sage900)
                Surface(onClick = { addStep() }, shape = RoundedCornerShape(16.dp), color = Sage800) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(8.dp)); Text("添加步骤", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Color.White)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))

            timeline.value.forEachIndexed { _, step ->
                Surface(Modifier.fillMaxWidth().padding(bottom = 24.dp), RoundedCornerShape(40.dp), Color.White, border = BorderStroke(1.dp, Sage200)) {
                    Column(Modifier.padding(32.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("#${timeline.value.indexOf(step) + 1}", fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage400)
                                Spacer(Modifier.width(16.dp))
                                Surface(modifier = Modifier.clip(RoundedCornerShape(16.dp)), color = Sage50) {
                                    Row(Modifier.padding(6.dp)) {
                                        Surface(onClick = { updateStep(step.step_id, step.copy(type = "action")) }, modifier = Modifier.clip(RoundedCornerShape(12.dp)), color = if (step.type == "action") Sage800 else Sage50) {
                                            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Bolt, null, tint = if (step.type == "action") Color.White else Sage500, modifier = Modifier.size(14.dp)); Text("主动", Modifier.padding(start = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = if (step.type == "action") Color.White else Sage500) }
                                        }
                                        Surface(onClick = { updateStep(step.step_id, step.copy(type = "waiting")) }, modifier = Modifier.clip(RoundedCornerShape(12.dp)), color = if (step.type == "waiting") Sage800 else Sage50) {
                                            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Timer, null, tint = if (step.type == "waiting") Color.White else Sage500, modifier = Modifier.size(14.dp)); Text("等待", Modifier.padding(start = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = if (step.type == "waiting") Color.White else Sage500) }
                                        }
                                    }
                                }
                            }
                            IconButton(onClick = { removeStep(step.step_id) }) { Icon(Icons.Default.Delete, null, tint = Sage300, modifier = Modifier.size(20.dp)) }
                        }
                        Spacer(Modifier.height(24.dp))
                        Box(Modifier.fillMaxWidth()) {
                            OutlinedTextField(step.content, { updateStep(step.step_id, step.copy(content = it)) }, Modifier.fillMaxWidth(), placeholder = { Text("步骤描述...", color = Sage300, fontSize = 14.sp) }, singleLine = false, maxLines = 4, shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Sage100, unfocusedBorderColor = Sage100, focusedContainerColor = Sage50, unfocusedContainerColor = Sage50), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Sage900))
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(if (step.duration > 0) "${step.duration / 60}" else "", {
                            val v = it.toIntOrNull() ?: 0
                            updateStep(step.step_id, step.copy(duration = v * 60))
                        }, Modifier.width(96.dp), placeholder = { Text("Min", color = Sage400) }, singleLine = true, shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Sage100, unfocusedBorderColor = Sage100, focusedContainerColor = Sage50, unfocusedContainerColor = Sage50), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Black, color = Sage900, textAlign = TextAlign.Center))

                        // Step images (Web: 步骤图解)
                        Spacer(Modifier.height(16.dp))
                        Text("步骤图解 (Instructional Media)", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage400)
                        Spacer(Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Upload placeholder (always last)
                            item {
                                Box(Modifier.width(120.dp).height(96.dp).clip(RoundedCornerShape(16.dp)).border(2.dp, Sage100, RoundedCornerShape(16.dp)).background(Sage50).clickable { imageTarget = "step"; stepImageId = step.step_id; pickLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) }, contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.CameraAlt, null, tint = Sage300, modifier = Modifier.size(24.dp))
                                        Text("上传图解", fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, color = if (step.images?.isEmpty() != false) Sage500 else Sage300)
                                    }
                                }
                            }
                            step.images?.let { imgs -> items(imgs.size) { i ->
                                Box(Modifier.width(120.dp).height(96.dp).clip(RoundedCornerShape(16.dp)).border(1.dp, Sage100, RoundedCornerShape(16.dp))) {
                                    AsyncImage(imgs[i], null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    IconButton(onClick = {
                                        timeline.value = timeline.value.map { s ->
                                            if (s.step_id == step.step_id) s.copy(images = (s.images?.toMutableList()?.apply { removeAt(i) } ?: mutableListOf()).toMutableList())
                                            else s
                                        }.toMutableList()
                                    }, modifier = Modifier.align(Alignment.TopEnd).size(28.dp).padding(2.dp)) { Surface(Modifier.size(24.dp), RoundedCornerShape(8.dp), Color.Black.copy(0.5f)) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(12.dp)) } } }
                                }
                            } }
                        }

                        // Sub-tasks (only for waiting type)
                        if (step.type == "waiting") {
                            Spacer(Modifier.height(24.dp))
                            HorizontalDivider(color = Sage50)
                            Spacer(Modifier.height(24.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.ListAlt, null, tint = Sage500, modifier = Modifier.size(12.dp)); Spacer(Modifier.width(8.dp)); Text("并行子任务", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage500) }
                                Text("+ 添入", Modifier.clickable { addSubTask(step.step_id) }.padding(horizontal = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Black, color = Sage800)
                            }
                            Spacer(Modifier.height(8.dp))
                            (step.sub_tasks ?: emptyList()).forEachIndexed { subIdx, sub ->
                                Column(Modifier.fillMaxWidth().padding(bottom = 6.dp).clip(RoundedCornerShape(16.dp)).background(Sage50).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                    OutlinedTextField(sub.content, { updateSubTask(step.step_id, subIdx, sub.copy(content = it)) }, Modifier.fillMaxWidth(), placeholder = { Text("子任务...", color = Sage300) }, singleLine = false, shape = RoundedCornerShape(0.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Sage900))
                                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedTextField(if (sub.duration > 0) "${sub.duration / 60}" else "", { updateSubTask(step.step_id, subIdx, sub.copy(duration = (it.toIntOrNull() ?: 0) * 60)) }, Modifier.width(72.dp), singleLine = true, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Sage100, unfocusedBorderColor = Sage100, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Black, color = Sage900, textAlign = TextAlign.Center))
                                        IconButton(onClick = { removeSubTask(step.step_id, subIdx) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Delete, null, tint = Sage300, modifier = Modifier.size(14.dp)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Surface(onClick = { addStep() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Sage800) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("添加步骤", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Color.White)
                }
            }
        }

        // ── Save Button (Web: footer absolute bottom-0) ──
        Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp), color = Color.Transparent) {
            Surface(
                onClick = {
                    if (name.value.isBlank()) return@Surface
                    val c = cost.value.toDoubleOrNull() ?: 0.0
                    val p = price.value.toDoubleOrNull() ?: 0.0
                    onSave(Recipe(
                        id = existingRecipe?.id ?: System.currentTimeMillis().toString(),
                        name = name.value, cover_image = cover,
                        bom_snapshot = bomSnapshot,
                        energy_level = energy.value, is_featured = featured.value,
                        cost = c, price = p,
                        materials = materials.value.mapValues { it.value.toList() },
                        timeline = timeline.value,
                        tags = tags.value,
                        updated_at = System.currentTimeMillis().toString()
                    ))
                },
                enabled = name.value.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(80.dp),
                shape = RoundedCornerShape(32.dp),
                color = if (name.value.isNotBlank()) Color.Black else Color.Black.copy(0.2f)
            ) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Save, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("保存发布菜谱", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
            }
        }
    }
}
