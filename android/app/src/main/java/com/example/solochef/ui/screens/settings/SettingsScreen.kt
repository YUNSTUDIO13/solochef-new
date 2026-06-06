package com.example.solochef.ui.screens.settings

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.solochef.ImageUtils
import com.example.solochef.model.Recipe
import com.example.solochef.storage.LocalFileManager
import com.example.solochef.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    onNavigateToLibrary: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isExporting: Boolean by viewModel.isExporting.collectAsState()
    val isImporting: Boolean by viewModel.isImporting.collectAsState()
    val importResult: String? by viewModel.importResult.collectAsState()

    // ── Export: SAF CreateDocument for .zip ──
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch { viewModel.exportToUri(context, uri) }
    }

    // ── Import: SAF OpenDocument for .zip ──
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch { viewModel.importFromUri(context, uri) }
    }

    LaunchedEffect(importResult) {
        importResult?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearImportResult()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Sage100)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp)
    ) {
        Spacer(Modifier.height(24.dp))

        // Header
        Text("独厨中心", fontSize = 40.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.05).sp, color = Sage900)
        Text("饿的时候打开，会有好事发生", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = Sage500, modifier = Modifier.padding(top = 8.dp))

        Spacer(Modifier.height(24.dp))

        // ─── Export Button ────────────────────────────
        Surface(
            onClick = {
                exportLauncher.launch("SoloChef_Backup_${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())}.zip")
            },
            enabled = !isExporting,
            shape = RoundedCornerShape(32.dp),
            color = Sage900,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(16.dp), color = White10, modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isExporting) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                        else Icon(Icons.Default.Save, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
                Column(Modifier.padding(start = 16.dp)) {
                    Text("导出菜谱 (Zip)", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Text("压缩图片 + 相对路径打包", fontSize = 10.sp, color = White40, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ─── Import Button ────────────────────────────
        Surface(
            onClick = { importLauncher.launch(arrayOf("application/zip")) },
            enabled = !isImporting,
            shape = RoundedCornerShape(32.dp),
            color = Color.White,
            border = BorderStroke(2.dp, Sage200),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(16.dp), color = Sage50, modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isImporting) CircularProgressIndicator(Modifier.size(22.dp), color = Sage400, strokeWidth = 2.dp)
                        else Icon(Icons.Default.Add, null, tint = Sage400, modifier = Modifier.size(22.dp))
                    }
                }
                Column(Modifier.padding(start = 16.dp)) {
                    Text("导入菜谱 (Zip)", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Sage900)
                    Text("解压图片至本地沙箱", fontSize = 10.sp, color = Sage400, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

class SettingsViewModel(application: Application) : androidx.lifecycle.AndroidViewModel(application) {
    private val storage = LocalFileManager(application)

    private val _isExporting = MutableStateFlow(false)
    val isExporting = _isExporting.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting = _isImporting.asStateFlow()

    private val _importResult = MutableStateFlow<String?>(null)
    val importResult = _importResult.asStateFlow()

    fun clearImportResult() { _importResult.value = null }

    fun exportToUri(context: android.content.Context, uri: Uri) {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val recipes = storage.getAllRecipes()
                if (recipes.isEmpty()) {
                    _importResult.value = "暂无菜谱可导出"
                    return@launch
                }
                val out = context.contentResolver.openOutputStream(uri)
                if (out == null) { _importResult.value = "无法写入目标位置"; return@launch }
                // Export ALL recipes into one zip with subfolder per recipe
                var count = 0
                kotlinx.coroutines.withContext(Dispatchers.IO) {
                    val exportJson = kotlinx.serialization.json.Json { prettyPrint = true; encodeDefaults = true }
                    java.util.zip.ZipOutputStream(out).use { zos ->
                        recipes.forEach { recipe ->
                            val safeName = recipe.name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                            val prefix = "recipe_${"%02d".format(count + 1)}_$safeName/"
                            // Default: keep original paths; rewrite only for files actually included
                            var coverRel = recipe.cover_image
                            var bomRel = recipe.bom_snapshot
                            val timelineRel = recipe.timeline.toMutableList()
                            // Cover image
                            val coverFile = java.io.File(recipe.cover_image)
                            if (coverFile.exists()) {
                                zos.putNextEntry(java.util.zip.ZipEntry("${prefix}images/cover.jpg"))
                                zos.write(coverFile.readBytes()); zos.closeEntry()
                                coverRel = "./images/cover.jpg"
                            }
                            // BOM snapshot
                            recipe.bom_snapshot?.let { bom ->
                                val bf = java.io.File(bom)
                                if (bf.exists()) {
                                    zos.putNextEntry(java.util.zip.ZipEntry("${prefix}images/bom.jpg"))
                                    zos.write(bf.readBytes()); zos.closeEntry()
                                    bomRel = "./images/bom.jpg"
                                }
                            }
                            // Step images
                            for (si in recipe.timeline.indices) {
                                val step = recipe.timeline[si]
                                val newImgs = step.images?.toMutableList() ?: mutableListOf()
                                var changed = false
                                step.images?.forEachIndexed { ii, imgPath ->
                                    val imgFile = java.io.File(imgPath)
                                    if (imgFile.exists()) {
                                        zos.putNextEntry(java.util.zip.ZipEntry("${prefix}images/step_${si}_${ii}.jpg"))
                                        zos.write(imgFile.readBytes()); zos.closeEntry()
                                        newImgs[ii] = "./images/step_${si}_${ii}.jpg"
                                        changed = true
                                    }
                                }
                                if (changed) timelineRel[si] = step.copy(images = newImgs)
                            }
                            // Recipe JSON with relative paths
                            val exported = recipe.copy(cover_image = coverRel, bom_snapshot = bomRel, timeline = timelineRel)
                            zos.putNextEntry(java.util.zip.ZipEntry("${prefix}recipe.json"))
                            zos.write(exportJson.encodeToString(Recipe.serializer(), exported).toByteArray(Charsets.UTF_8))
                            zos.closeEntry()
                            count++
                        }
                    }
                }
                out.close()
                if (count > 0) _importResult.value = "导出成功：${count} 份菜谱"
                else _importResult.value = "导出失败"
            } catch (e: Exception) {
                _importResult.value = "导出失败：${e.message}"
            } finally { _isExporting.value = false }
        }
    }

    fun importFromUri(context: android.content.Context, uri: Uri) {
        viewModelScope.launch {
            _isImporting.value = true
            try {
                val `in` = context.contentResolver.openInputStream(uri)
                if (`in` == null) { _importResult.value = "无法读取文件"; return@launch }
                // Try single recipe first (legacy format), then multi-recipe (subfolder format)
                var count = 0
                kotlinx.coroutines.withContext(Dispatchers.IO) {
                    val buf = ByteArray(8192)
                    val importDir = java.io.File(context.filesDir, "imported").also { it.mkdirs() }
                    // Collect all recipe.json entries grouped by subfolder prefix
                    val recipeEntries = mutableMapOf<String, String>() // prefix -> json content
                    val imageEntries = mutableMapOf<String, MutableMap<String, String>>() // prefix -> (relPath -> localPath)
                    
                    java.util.zip.ZipInputStream(`in`).use { zis ->
                        var e = zis.nextEntry
                        while (e != null) {
                            val name = e.name
                            if (name == "recipe.json") {
                                recipeEntries[""] = zis.bufferedReader().readText()
                            } else if (name.endsWith("/recipe.json")) {
                                val prefix = name.removeSuffix("recipe.json")
                                recipeEntries[prefix] = zis.bufferedReader().readText()
                            } else if (name.contains("/images/")) {
                                val prefix = name.substringBefore("/images/") + "/"
                                val relPath = "./" + name.substringAfter("$prefix")
                                val t = java.io.File(importDir, "import_${System.currentTimeMillis()}_${name.substringAfterLast("/")}")
                                t.outputStream().use { o -> var l: Int; while (zis.read(buf).also { l = it } > 0) o.write(buf, 0, l) }
                                imageEntries.getOrPut(prefix) { mutableMapOf() }[relPath] = t.absolutePath
                            }
                            zis.closeEntry(); e = zis.nextEntry
                        }
                    }
                    
                    recipeEntries.forEach { (prefix, json) ->
                        val recipe = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true }
                            .decodeFromString<Recipe>(json)
                        val pathMap = imageEntries[prefix] ?: emptyMap()
                        val imported = recipe.copy(
                            id = System.currentTimeMillis().toString() + "_" + count,
                            cover_image = pathMap[recipe.cover_image] ?: recipe.cover_image,
                            bom_snapshot = recipe.bom_snapshot?.let { pathMap[it] ?: it },
                            timeline = recipe.timeline.map { s -> s.copy(images = s.images?.map { pathMap[it] ?: it } ?: s.images) },
                            updated_at = System.currentTimeMillis().toString()
                        )
                        storage.saveRecipe(imported)
                        count++
                    }
                }
                `in`.close()
                if (count > 0) _importResult.value = "导入成功：${count} 份菜谱"
                else _importResult.value = "导入失败：无效的 Zip 文件"
            } catch (e: Exception) {
                _importResult.value = "导入失败：${e.message}"
            } finally { _isImporting.value = false }
        }
    }

    fun bulkExport(context: android.content.Context) {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val content = storage.exportToMarkdown()
                val filename = "SoloChef_Backup_${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())}.md"
                val file = java.io.File(context.filesDir, filename)
                file.writeText(content)
                _importResult.value = "导出成功：${file.absolutePath}"
            } catch (e: Exception) {
                _importResult.value = "导出失败：${e.message}"
            } finally { _isExporting.value = false }
        }
    }
}
