package com.example.solochef.ui.screens.createrecipe

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.solochef.ImageUtils
import com.example.solochef.model.*
import com.example.solochef.ui.screens.library.*
import com.example.solochef.ui.screens.ingredients.IngredientSelectorSheet
import com.example.solochef.model.IngredientItem
import com.example.solochef.ui.theme.*
import com.example.solochef.storage.LocalFileManager
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.text.input.KeyboardType

private val MATERIAL_UNITS = listOf("g", "kg", "个", "只", "把", "勺", "包", "盒", "份", "片", "块", "根", "条", "升", "毫升", "杯", "盘", "粒", "适量", "少许")

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
    val customTags = remember { mutableStateOf(CustomRecipeTags()) }
    val allRecipes = remember { mutableStateOf<List<Recipe>>(emptyList()) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showDeleteTagDialog by remember { mutableStateOf(false) }
    var tagToDelete by remember { mutableStateOf<CustomTag?>(null) }
    var deleteTagIsProcess by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var addingToProcess by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val cost = remember { mutableStateOf(existingRecipe?.cost?.toString() ?: "") }
    val price = remember { mutableStateOf(existingRecipe?.price?.toString() ?: "") }
    val description = remember { mutableStateOf(existingRecipe?.description ?: "") }

    // ── Validation helper ──
    fun validate(): String? {
        if (name.value.isBlank()) return "菜谱名称不能为空"
        val c = cost.value.toDoubleOrNull()
        if (c == null || c <= 0) return "请输入成本"
        val p = price.value.toDoubleOrNull()
        if (p == null || p <= 0) return "请输入售价"
        val processSelected = tags.value.any { it in COOKING_PROCESS_TAGS || it in customTags.value.cookingProcessTags.map { ct -> ct.name } }
        if (!processSelected) return "请选择烹饪工艺"
        val cuisineSelected = tags.value.any { it in CUISINE_TAGS || it in customTags.value.cuisineTags.map { ct -> ct.name } }
        if (!cuisineSelected) return "请选择菜系维度"
        val hasMaterials = materials.value.values.any { list -> list.any { it.item.isNotBlank() } }
        if (!hasMaterials) return "请至少添加一个物料"
        return null
    }

    val isFormValid by remember(name.value, cost.value, price.value, tags.value, materials.value) {
        derivedStateOf { validate() == null }
    }

    // ── Image targets ──
    var imageTarget by remember { mutableStateOf("") } // "cover" | "bom" | "step"
    var stepImageId by remember { mutableStateOf(0L) }
    var coverLoading by remember { mutableStateOf(false) }
    var bomLoading by remember { mutableStateOf(false) }
    var showIngredientSelector by remember { mutableStateOf(false) }
    var ingredientLibrary by remember { mutableStateOf<IngredientLibrary?>(null) }

    // Pre-load ingredient library for category lookup
    val localStg = remember { LocalFileManager(context) }
    LaunchedEffect(Unit) { ingredientLibrary = localStg.getIngredientLibrary() }
    LaunchedEffect(Unit) {
        val tags = localStg.getCustomRecipeTags()
        customTags.value = tags
        allRecipes.value = localStg.getAllRecipes()
    }

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
    Column(Modifier.fillMaxSize().background(Color.Transparent).imePadding()) {
        // Header
        Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(if (existingRecipe != null) "编辑菜谱" else "新建菜谱", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Sage900)
            IconButton(onClick = onCancel) { Icon(Icons.Default.Close, contentDescription = null, tint = Sage500, modifier = Modifier.size(24.dp)) }
        }

        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 24.dp, end = 24.dp, top = 24.dp)) {
            // ── Cover Image (Web: section > label > div.relative) ──
            Text("成品封面图 (Required)", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage500)
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(40.dp)).frostedGlassBackground().border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(40.dp)).clickable { imageTarget = "cover"; coverLoading = true; pickLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) }, contentAlignment = Alignment.Center) {
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
            Spacer(Modifier.height(20.dp))

            // ── Basic Info (Web: section.space-y-4) ──
            Text("菜谱名称", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage500)
            OutlinedTextField(
                value = name.value,
                onValueChange = { name.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .frostedGlassBackground(),
                placeholder = { Text("例如：红烧肉", color = Sage300) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White.copy(alpha = 0.4f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Black, color = Sage900)
            )
            Spacer(Modifier.height(12.dp))

            // Energy + Featured (Web: grid grid-cols-2)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("精力等级", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage500)
                    var eExpanded by remember { mutableStateOf(false) }
                    var eWidth by remember { mutableStateOf(0.dp) }
                    val density = LocalDensity.current
                    Box(Modifier.onSizeChanged { eWidth = with(density) { it.width.toDp() } }) {
                        Surface(onClick = { eExpanded = true }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp).clip(RoundedCornerShape(16.dp)).frostedGlassBackground().border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(16.dp)), shape = RoundedCornerShape(16.dp), color = Color.Transparent) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Text(ENERGY_LEVEL_OPTIONS.find { it.first == energy.value }?.second ?: "正常", Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Sage900); Icon(Icons.Default.ArrowDropDown, null, tint = Sage400, modifier = Modifier.size(20.dp)) } }
                        DropdownMenu(
                            expanded = eExpanded,
                            onDismissRequest = { eExpanded = false },
                            modifier = Modifier
                                .width(eWidth)
                                .clip(RoundedCornerShape(16.dp))
                                .frostedGlassBackground()
                                .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            containerColor = Color.Transparent,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                        ) {
                            ENERGY_LEVEL_OPTIONS.forEach { (l, lbl) ->
                                DropdownMenuItem(
                                    text = { Text(lbl, fontWeight = FontWeight.Bold, color = Sage900) },
                                    onClick = { energy.value = l; eExpanded = false },
                                    colors = MenuDefaults.itemColors(textColor = Sage900)
                                )
                            }
                        }
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text("主厨力荐", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage500)
                    Surface(modifier = Modifier.fillMaxWidth().padding(top = 4.dp).clip(RoundedCornerShape(16.dp)).frostedGlassBackground().border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(16.dp)), shape = RoundedCornerShape(16.dp), color = Color.Transparent) {
                        Row(Modifier.padding(4.dp)) {
                            Surface(onClick = { featured.value = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), color = if (featured.value) Sage800 else Color.Transparent) { Text("是", modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (featured.value) Color.White else Sage400) }
                            Surface(onClick = { featured.value = false }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), color = if (!featured.value) Sage800 else Color.Transparent) { Text("否", modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (!featured.value) Color.White else Sage400) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            // ── Cost + Price (Web: between basic-info and tags) ──
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("成本（元）", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage500)
                    OutlinedTextField(
                        value = cost.value,
                        onValueChange = { cost.value = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .frostedGlassBackground(),
                        placeholder = { Text("例：5", color = Sage300) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White.copy(alpha = 0.4f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Sage900)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text("售价（元）", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage500)
                    OutlinedTextField(
                        value = price.value,
                        onValueChange = { price.value = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .frostedGlassBackground(),
                        placeholder = { Text("例：10", color = Sage300) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White.copy(alpha = 0.4f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Sage900)
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            // ── Tags (Web: white card inside basic-info section) ──
            Text("分类标签", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage500)
            Spacer(Modifier.height(8.dp))
            Surface(Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).frostedGlassBackground().border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(32.dp)), RoundedCornerShape(32.dp), Color.Transparent) {
                Column(Modifier.padding(16.dp)) {
                    Text("烹饪工艺", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage400)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        COOKING_PROCESS_TAGS.forEach { tag ->
                            val sel = tag in tags.value
                            Surface(
                                onClick = { tags.value = (tags.value.filter { it !in COOKING_PROCESS_TAGS && it !in customTags.value.cookingProcessTags.map { ct -> ct.name } } + tag).toMutableList() },
                                shape = RoundedCornerShape(50),
                                color = if (sel) Sage800 else Color.Transparent,
                                modifier = if (sel) Modifier else Modifier.clip(RoundedCornerShape(50)).frostedGlassBackground().border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(50))
                            ) {
                                Text(tag, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (sel) Color.White else Sage500)
                            }
                        }
                        customTags.value.cookingProcessTags.forEach { ct ->
                            val sel = ct.name in tags.value
                            val isAssociated = allRecipes.value.any { it.tags.contains(ct.name) }
                            Surface(
                                onClick = { tags.value = (tags.value.filter { it !in COOKING_PROCESS_TAGS && it !in customTags.value.cookingProcessTags.map { c -> c.name } } + ct.name).toMutableList() },
                                shape = RoundedCornerShape(50),
                                color = if (sel) Sage800 else Color.Transparent,
                                modifier = if (sel) Modifier else Modifier.clip(RoundedCornerShape(50)).frostedGlassBackground().border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(50))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(ct.name, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (sel) Color.White else Sage500)
                                    if (!isAssociated) {
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "删除",
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) {
                                                    tagToDelete = ct
                                                    deleteTagIsProcess = true
                                                    showDeleteTagDialog = true
                                                },
                                            tint = if (sel) Color.White.copy(0.8f) else Sage500
                                        )
                                    }
                                }
                            }
                        }
                        Surface(
                            onClick = { addingToProcess = true; newCategoryName = ""; showAddCategoryDialog = true },
                            shape = RoundedCornerShape(50),
                            color = Color.Transparent,
                            modifier = Modifier.clip(RoundedCornerShape(50)).frostedGlassBackground().border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(50))
                        ) {
                            Text("＋ 添加分类", Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Sage400)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("菜系维度", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage400)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        CUISINE_TAGS.forEach { tag ->
                            val sel = tag in tags.value
                            Surface(
                                onClick = { tags.value = (tags.value.filter { it !in CUISINE_TAGS && it !in customTags.value.cuisineTags.map { ct -> ct.name } } + tag).toMutableList() },
                                shape = RoundedCornerShape(50),
                                color = if (sel) Sage800 else Color.Transparent,
                                modifier = if (sel) Modifier else Modifier.clip(RoundedCornerShape(50)).frostedGlassBackground().border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(50))
                            ) {
                                Text(tag, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (sel) Color.White else Sage500)
                            }
                        }
                        customTags.value.cuisineTags.forEach { ct ->
                            val sel = ct.name in tags.value
                            val isAssociated = allRecipes.value.any { it.tags.contains(ct.name) }
                            Surface(
                                onClick = { tags.value = (tags.value.filter { it !in CUISINE_TAGS && it !in customTags.value.cuisineTags.map { c -> c.name } } + ct.name).toMutableList() },
                                shape = RoundedCornerShape(50),
                                color = if (sel) Sage800 else Color.Transparent,
                                modifier = if (sel) Modifier else Modifier.clip(RoundedCornerShape(50)).frostedGlassBackground().border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(50))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(ct.name, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (sel) Color.White else Sage500)
                                    if (!isAssociated) {
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "删除",
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) {
                                                    tagToDelete = ct
                                                    deleteTagIsProcess = false
                                                    showDeleteTagDialog = true
                                                },
                                            tint = if (sel) Color.White.copy(0.8f) else Sage500
                                        )
                                    }
                                }
                            }
                        }
                        Surface(
                            onClick = { addingToProcess = false; newCategoryName = ""; showAddCategoryDialog = true },
                            shape = RoundedCornerShape(50),
                            color = Color.Transparent,
                            modifier = Modifier.clip(RoundedCornerShape(50)).frostedGlassBackground().border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(50))
                        ) {
                            Text("＋ 添加分类", Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Sage400)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))

            // ── BOM Section ──
            Text("物料清单", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Sage900)
            Spacer(Modifier.height(12.dp))

            // Unified "从食材库选择" button
            Surface(
                onClick = { showIngredientSelector = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .frostedGlassBackground()
                    .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                color = Color.Transparent
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, null, tint = Sage900, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("从食材库选择", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage900)
                }
            }

            // Unified material list (if any)
            val allMaterials = remember(materials.value) {
                materials.value.entries.flatMap { (cat, items) -> items.map { cat to it } }
            }
            if (allMaterials.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                allMaterials.forEachIndexed { _, pair ->
                    val (cat, mat) = pair
                    val catLabel = when (cat) {
                        "meat" -> "肉禽 / 水产"
                        "vegetable" -> "蔬菜 / 蔬果"
                        "seasoning" -> "调味 / 其它"
                        else -> cat
                    }
                    val catIdx = materials.value[cat]?.indexOf(mat) ?: 0
                    Surface(Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(RoundedCornerShape(28.dp)).frostedGlassBackground().border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(28.dp)), RoundedCornerShape(28.dp), Color.Transparent) {
                        Column(Modifier.padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)) {
                            Text(catLabel, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, color = Sage400)
                            Spacer(Modifier.height(2.dp))
                            OutlinedTextField(mat.item, { updateMaterial(cat, catIdx, mat.copy(item = it)) }, Modifier.fillMaxWidth(), placeholder = { Text("食材名称", color = Sage300) }, singleLine = true, shape = RoundedCornerShape(0.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Sage900))
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("数量", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Sage500)
                                Spacer(Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .width(64.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .frostedGlassBackground()
                                        .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    BasicTextField(
                                        value = mat.amount,
                                        onValueChange = { updateMaterial(cat, catIdx, mat.copy(amount = it)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Sage900, textAlign = TextAlign.Center),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        decorationBox = { innerTextField ->
                                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { innerTextField() }
                                        }
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Text("单位", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Sage500)
                                Spacer(Modifier.width(6.dp))
                                val curUnit = mat.unit.ifEmpty { "g" }
                                var showUnitDialog by remember { mutableStateOf(false) }
                                Surface(
                                    onClick = { showUnitDialog = true },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.Transparent,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .frostedGlassBackground()
                                        .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                ) {
                                    Text(curUnit, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Sage900)
                                }
                                if (showUnitDialog) {
                                    AlertDialog(
                                        onDismissRequest = { showUnitDialog = false },
                                        title = { Text("选择单位", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Sage900) },
                                        text = {
                                            val rows = MATERIAL_UNITS.chunked(6)
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                rows.forEach { rowUnits ->
                                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        rowUnits.forEach { unit ->
                                                            val isSelected = curUnit == unit
                                                            Surface(
                                                                onClick = { updateMaterial(cat, catIdx, mat.copy(unit = unit)); showUnitDialog = false },
                                                                modifier = Modifier.weight(1f),
                                                                shape = RoundedCornerShape(12.dp),
                                                                color = if (isSelected) Sage800 else Sage50,
                                                                border = BorderStroke(1.dp, if (isSelected) Sage800 else Sage100)
                                                            ) {
                                                                Text(unit, Modifier.padding(vertical = 10.dp).fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else Sage500)
                                                            }
                                                        }
                                                        repeat(6 - rowUnits.size) { Spacer(Modifier.weight(1f)) }
                                                    }
                                                }
                                            }
                                        },
                                        confirmButton = {},
                                        dismissButton = { TextButton(onClick = { showUnitDialog = false }) { Text("取消", color = Sage500) } },
                                        containerColor = Color.White,
                                        shape = RoundedCornerShape(24.dp)
                                    )
                                }
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = { removeMaterial(cat, catIdx) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, null, tint = Color(0xFFF87171), modifier = Modifier.size(16.dp)) }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                // "从食材库选择" button to add more
                Surface(
                    onClick = { showIngredientSelector = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .frostedGlassBackground()
                        .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Transparent
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, null, tint = Sage900, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("继续添加食材", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage900)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Timeline Section (Web) ──
            Text("烹饪步骤", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Sage900)
            Spacer(Modifier.height(8.dp))

            timeline.value.forEachIndexed { _, step ->
                Surface(Modifier.fillMaxWidth().padding(bottom = 16.dp).clip(RoundedCornerShape(40.dp)).frostedGlassBackground().border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(40.dp)), RoundedCornerShape(40.dp), Color.Transparent) {
                    Column(Modifier.padding(20.dp)) {
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
                        Spacer(Modifier.height(16.dp))
                        Box(Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = step.content,
                                onValueChange = { updateStep(step.step_id, step.copy(content = it)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .frostedGlassBackground(),
                                placeholder = { Text("步骤描述...", color = Sage300, fontSize = 14.sp) },
                                singleLine = false,
                                maxLines = 4,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.White.copy(alpha = 0.4f),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Sage900)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = if (step.duration > 0) "${step.duration / 60}" else "",
                            onValueChange = {
                                val v = it.toIntOrNull() ?: 0
                                updateStep(step.step_id, step.copy(duration = v * 60))
                            },
                            modifier = Modifier
                                .width(96.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .frostedGlassBackground(),
                            placeholder = { Text("Min", color = Sage400) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.White.copy(alpha = 0.4f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Black, color = Sage900, textAlign = TextAlign.Center)
                        )

                        // Step images (Web: 步骤图解)
                        Spacer(Modifier.height(10.dp))
                        Text("步骤图解", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage400)
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Upload placeholder (always last)
                            item {
                                Box(Modifier.width(120.dp).height(96.dp).clip(RoundedCornerShape(16.dp)).border(2.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(16.dp)).frostedGlassBackground().clickable { imageTarget = "step"; stepImageId = step.step_id; pickLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) }, contentAlignment = Alignment.Center) {
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
                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider(color = Sage50)
                            Spacer(Modifier.height(16.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.ListAlt, null, tint = Sage500, modifier = Modifier.size(12.dp)); Spacer(Modifier.width(8.dp)); Text("并行子任务", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage500) }
                                Text("+ 添入", Modifier.clickable { addSubTask(step.step_id) }.padding(horizontal = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Black, color = Sage800)
                            }
                            Spacer(Modifier.height(8.dp))
                            (step.sub_tasks ?: emptyList()).forEachIndexed { subIdx, sub ->
                                Column(Modifier.fillMaxWidth().padding(bottom = 6.dp).clip(RoundedCornerShape(16.dp)).frostedGlassBackground().border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(16.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                    OutlinedTextField(sub.content, { updateSubTask(step.step_id, subIdx, sub.copy(content = it)) }, Modifier.fillMaxWidth(), placeholder = { Text("子任务...", color = Sage300) }, singleLine = false, shape = RoundedCornerShape(0.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Sage900))
                                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedTextField(
                                            value = if (sub.duration > 0) "${sub.duration / 60}" else "",
                                            onValueChange = { updateSubTask(step.step_id, subIdx, sub.copy(duration = (it.toIntOrNull() ?: 0) * 60)) },
                                            modifier = Modifier
                                                .width(72.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .frostedGlassBackground(),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color.White.copy(alpha = 0.4f),
                                                unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent
                                            ),
                                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Black, color = Sage900, textAlign = TextAlign.Center)
                                        )
                                        IconButton(onClick = { removeSubTask(step.step_id, subIdx) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Delete, null, tint = Sage300, modifier = Modifier.size(14.dp)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Surface(
                onClick = { addStep() },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .frostedGlassBackground()
                    .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                color = Color.Transparent
            ) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, null, tint = Sage900, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("添加步骤", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage900)
                }
            }
            Spacer(Modifier.height(24.dp))

            // ── 菜谱介绍 (Description) ──
            Text("菜谱介绍", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Sage900)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = description.value,
                onValueChange = { description.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .frostedGlassBackground(),
                placeholder = { Text("介绍一下这道菜的故事、口感或烹饪心得...", color = Sage300, fontSize = 13.sp) },
                singleLine = false,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White.copy(alpha = 0.4f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Sage900, lineHeight = 18.sp)
            )
        }

        // ── Save Button (Web: footer absolute bottom-0) ──
        Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp), color = Color.Transparent) {
            Surface(
                onClick = {
                    val error = validate()
                    if (error != null) {
                        android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_SHORT).show()
                        return@Surface
                    }
                    val c = cost.value.toDoubleOrNull() ?: 0.0
                    val p = price.value.toDoubleOrNull() ?: 0.0
                    onSave(Recipe(
                        id = existingRecipe?.id ?: System.currentTimeMillis().toString(),
                        name = name.value, cover_image = cover,
                        bom_snapshot = bomSnapshot,
                        energy_level = energy.value, is_featured = featured.value,
                        cost = c, price = p,
                        last_cooked_at = existingRecipe?.last_cooked_at,
                        cooked_count = existingRecipe?.cooked_count ?: 0,
                        materials = materials.value.mapValues { (_, mats) -> mats.map { if (it.unit.isEmpty()) it.copy(unit = "g") else it } },
                        timeline = timeline.value,
                        tags = tags.value,
                        description = description.value,
                        updated_at = System.currentTimeMillis().toString()
                    ))
                },
                enabled = isFormValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .frostedGlassBackground()
                    .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(32.dp)),
                shape = RoundedCornerShape(32.dp),
                color = Color.Transparent
            ) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Save, null, tint = Sage900, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("保存发布菜谱", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Sage900)
                }
            }
        }

    // ── 食材选择器 overlay ──
    if (showIngredientSelector && ingredientLibrary != null) {
        val lib = ingredientLibrary!!
        IngredientSelectorSheet(
            onDismiss = { showIngredientSelector = false },
            onConfirm = { selected ->
                showIngredientSelector = false
                selected.forEach { ing ->
                    val sourceCat = lib.categories.find { cat -> cat.ingredients.any { it.id == ing.id } }
                    val targetCat = if (sourceCat?.targetCategory == "favorite") {
                        val byCategoryId = lib.categories.find { it.id == ing.categoryId && it.targetCategory != "favorite" }
                        byCategoryId?.targetCategory
                            ?: lib.categories.find { cat ->
                                cat.targetCategory != "favorite" && cat.ingredients.any { it.id == ing.id }
                            }?.targetCategory
                            ?: "seasoning"
                    } else {
                        sourceCat?.targetCategory ?: "seasoning"
                    }
                    val m = Material(item = ing.name, amount = "", unit = "g", is_essential = true)
                    val existing = materials.value[targetCat]?.toMutableList() ?: mutableListOf()
                    if (m.item !in existing.map { it.item }) {
                        existing.add(m)
                        materials.value = materials.value.toMutableMap().also { it[targetCat] = existing }
                    }
                }
            }
        )
    }

    // ── 添加分类 Dialog ──
    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(28.dp),
            title = { Text("添加分类", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Sage900) },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    placeholder = { Text("分类名称", color = Sage300, fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = Sage900),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Sage400,
                        unfocusedBorderColor = Sage200,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newCategoryName.trim()
                        if (name.isNotEmpty()) {
                            val newTag = CustomTag(id = UUID.randomUUID().toString(), name = name)
                            val updated = if (addingToProcess) {
                                customTags.value.copy(cookingProcessTags = customTags.value.cookingProcessTags + newTag)
                            } else {
                                customTags.value.copy(cuisineTags = customTags.value.cuisineTags + newTag)
                            }
                            scope.launch { localStg.saveCustomRecipeTags(updated) }
                            customTags.value = updated
                            // Also add the new tag to the recipe's tags
                            tags.value = tags.value.toMutableList().apply { add(name) }
                        }
                        showAddCategoryDialog = false
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .frostedGlassBackground()
                        .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(50))
                ) { Text("添加", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Sage900) }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("取消", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Sage500)
                }
            }
        )
    }
    // ── 删除分类 Dialog ──
    if (showDeleteTagDialog && tagToDelete != null) {
        val ct = tagToDelete!!
        AlertDialog(
            onDismissRequest = { showDeleteTagDialog = false; tagToDelete = null },
            containerColor = Color.White,
            shape = RoundedCornerShape(28.dp),
            title = { Text("请确认是否删除？", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Sage900) },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = if (deleteTagIsProcess) {
                            customTags.value.copy(cookingProcessTags = customTags.value.cookingProcessTags.filter { it.id != ct.id })
                        } else {
                            customTags.value.copy(cuisineTags = customTags.value.cuisineTags.filter { it.id != ct.id })
                        }
                        scope.launch { localStg.saveCustomRecipeTags(updated) }
                        customTags.value = updated
                        tags.value = tags.value.toMutableList().apply { remove(ct.name) }
                        showDeleteTagDialog = false
                        tagToDelete = null
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .frostedGlassBackground()
                        .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(50))
                ) { Text("确认", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Sage900) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteTagDialog = false; tagToDelete = null }) {
                    Text("取消", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Sage500)
                }
            }
        )
    }
    }
}
