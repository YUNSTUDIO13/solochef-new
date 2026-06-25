package com.example.solochef.ui.screens.tasting

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.solochef.model.TastingNote
import com.example.solochef.ui.theme.*

@Composable
fun TastingDetailScreen(
    note: TastingNote,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onTurnToRecipe: (String) -> Unit
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认移除当前拾味？", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Sage900) },
            text = { Text("移除后该菜品记录将永久清除，无法恢复", fontSize = 13.sp, color = Sage500) },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("保留", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Sage800)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text("移除", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFFEF4444))
                }
            }
        )
    }

    Column(Modifier.fillMaxSize().background(Sage100)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.ArrowBack, null, tint = Sage900, modifier = Modifier.size(24.dp)) }
            Text("拾味手记", Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.Black, color = Sage900)
        }

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            // Cover
            if (note.coverImage.isNotEmpty()) {
                Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(24.dp)).background(Sage100)) {
                    AsyncImage(note.coverImage, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
            }

            Spacer(Modifier.height(20.dp))

            // URL
            if (note.url.isNotEmpty()) {
                Surface(Modifier.fillMaxWidth().clickable {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("url", note.url))
                    android.widget.Toast.makeText(context, "链接已复制", android.widget.Toast.LENGTH_SHORT).show()
                }, RoundedCornerShape(16.dp), Color.White, border = BorderStroke(1.dp, Sage200)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Link, null, tint = Sage400, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(note.url, Modifier.weight(1f), fontSize = 12.sp, color = Sage500, maxLines = 2)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.ContentCopy, null, tint = Sage400, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Rating
            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), Color.White, border = BorderStroke(1.dp, Sage200)) {
                Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    val starColor = Color(0xFFFF9800)
                    val fullStars = note.rating.toInt()
                    val hasHalf = note.rating - fullStars >= 0.5f
                    repeat(fullStars) { Icon(Icons.Default.Star, null, tint = starColor, modifier = Modifier.size(28.dp)) }
                    if (hasHalf) Icon(Icons.Default.Star, null, tint = starColor.copy(alpha = 0.5f), modifier = Modifier.size(28.dp))
                    repeat(5 - fullStars - (if (hasHalf) 1 else 0)) { Icon(Icons.Default.StarBorder, null, tint = Sage200, modifier = Modifier.size(28.dp)) }
                    Spacer(Modifier.width(12.dp))
                    Text(String.format("%.1f", note.rating), fontSize = 20.sp, fontWeight = FontWeight.Black, color = starColor)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Note
            if (note.note.isNotEmpty()) {
                Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), Color.White, border = BorderStroke(1.dp, Sage200)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("备注", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage400)
                        Spacer(Modifier.height(8.dp))
                        Text(note.note, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Sage900, lineHeight = 22.sp)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Turn to recipe button
            Surface(onClick = { onTurnToRecipe(note.id) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(32.dp), color = Sage900) {
                Text("搞定，做成菜谱", Modifier.fillMaxWidth().padding(20.dp), textAlign = TextAlign.Center, fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
            }

            Spacer(Modifier.height(16.dp))

            // Delete
            TextButton(onClick = { showDeleteDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Text("删除此拾味手记", color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Black)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
