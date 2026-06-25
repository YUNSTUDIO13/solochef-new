package com.example.solochef.ui.screens.tasting

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.solochef.model.TastingNote
import com.example.solochef.storage.LocalFileManager
import com.example.solochef.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTastingScreen(
    existingNote: TastingNote? = null,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val storage = remember { LocalFileManager(context.applicationContext as android.app.Application) }
    val scope = rememberCoroutineScope()

    var coverImage by remember { mutableStateOf(existingNote?.coverImage ?: "") }
    var url by remember { mutableStateOf(existingNote?.url ?: "") }
    var rating by remember { mutableFloatStateOf(existingNote?.rating ?: 0f) }
    var note by remember { mutableStateOf(existingNote?.note ?: "") }
    var coverLoading by remember { mutableStateOf(false) }

    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri == null) { coverLoading = false; return@rememberLauncherForActivityResult }
        scope.launch {
            // Compress image same as CreateRecipe
            val outputFile = java.io.File(context.filesDir, "tasting_${System.currentTimeMillis()}.jpg")
            val ok = com.example.solochef.ImageUtils.compressAndSaveImage(context, uri, outputFile)
            if (ok) coverImage = outputFile.absolutePath else coverImage = uri.toString()
            coverLoading = false
        }
    }

    Column(Modifier.fillMaxSize().background(Sage100)) {
        // Header
        Surface(Modifier.fillMaxWidth(), color = Sage100.copy(alpha = 0.95f)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.ArrowBack, null, tint = Sage900, modifier = Modifier.size(24.dp)) }
                Text("新建拾味手记", Modifier.weight(1f).padding(start = 4.dp), fontSize = 20.sp, fontWeight = FontWeight.Black, color = Sage900)
            }
        }

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(16.dp))

            // Cover image
            Text("封面图", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage500)
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(24.dp)).background(Sage50).clickable { coverLoading = true; pickLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, contentAlignment = Alignment.Center) {
                if (coverLoading) CircularProgressIndicator(Modifier.size(32.dp), strokeWidth = 2.dp, color = Sage800)
                else if (coverImage.isNotEmpty()) AsyncImage(coverImage, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CameraAlt, null, tint = Sage300, modifier = Modifier.size(32.dp))
                    Text("点击上传封面", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Sage400, modifier = Modifier.padding(top = 8.dp))
                }
            }

            Spacer(Modifier.height(24.dp))

            // URL
            Text("菜谱地址", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage500)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(url, { url = it }, Modifier.fillMaxWidth(), placeholder = { Text("输入链接地址", color = Sage300) }, singleLine = true, shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Sage400, unfocusedBorderColor = Sage200, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White))

            Spacer(Modifier.height(24.dp))

            // Rating
            Text("拾味等级", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage500)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).border(1.dp, Sage200, RoundedCornerShape(16.dp)).padding(16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                val starColor = Color(0xFFFF9800)
                for (i in 1..5) {
                    IconButton(onClick = { rating = i.toFloat() }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            if (i <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                            null, tint = starColor, modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Text(String.format(" %.1f", rating), fontSize = 16.sp, fontWeight = FontWeight.Black, color = starColor, modifier = Modifier.width(48.dp))
            }

            Spacer(Modifier.height(24.dp))

            // Note
            Text("备注", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage500)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth().height(120.dp), placeholder = { Text("写下你的品尝感受…", color = Sage300) }, shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Sage400, unfocusedBorderColor = Sage200, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White))

            Spacer(Modifier.height(32.dp))

            // Save
            Surface(onClick = {
                scope.launch {
                    val tastingNote = TastingNote(
                        id = existingNote?.id ?: System.currentTimeMillis().toString(),
                        coverImage = coverImage,
                        url = url,
                        rating = rating,
                        note = note,
                        createdAt = existingNote?.createdAt ?: System.currentTimeMillis()
                    )
                    storage.saveTastingNote(tastingNote)
                    onSaved()
                }
            }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(32.dp), color = Sage900) {
                Text("保存", Modifier.fillMaxWidth().padding(20.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
