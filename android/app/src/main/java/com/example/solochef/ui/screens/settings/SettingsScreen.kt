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
        Text("SoloChef Operational Center // v1.6", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = Sage500, modifier = Modifier.padding(top = 8.dp))

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

        // ─── Bulk Export All Recipes Button ───────────
        Surface(
            onClick = {
                scope.launch { viewModel.bulkExport(context) }
            },
            enabled = !isExporting,
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Sage200),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Save, contentDescription = null, tint = Sage400, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text("批量导出所有菜谱 (Markdown)", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Sage900)
            }
        }
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
                // Export first recipe via new zip pipeline (single recipe export)
                val recipe = recipes.first()
                val out = context.contentResolver.openOutputStream(uri)
                if (out == null) { _importResult.value = "无法写入目标位置"; return@launch }
                val ok = ImageUtils.exportRecipeToZip(context, recipe, out)
                out.close()
                if (ok) _importResult.value = "导出成功：${recipe.name}"
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
                val imported = ImageUtils.importRecipeFromZip(context, `in`)
                `in`.close()
                if (imported == null) { _importResult.value = "导入失败：无效的 Zip 文件"; return@launch }
                storage.saveRecipe(imported)
                _importResult.value = "导入成功：${imported.name}"
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
