package com.example.solochef.ui.screens.tasting

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.solochef.model.TastingNote
import com.example.solochef.ui.theme.*

@Composable
fun TastingNotesListScreen(
    tastingNotes: List<TastingNote>,
    onBack: () -> Unit,
    onSelectTasting: (String) -> Unit
) {
    val sorted = remember(tastingNotes) { tastingNotes.sortedByDescending { it.rating } }

    Column(Modifier.fillMaxSize().background(Sage100)) {
        Surface(Modifier.fillMaxWidth(), color = Sage100.copy(alpha = 0.95f)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.ArrowBack, null, tint = Sage900, modifier = Modifier.size(24.dp)) }
                Text("拾味手记", Modifier.weight(1f).padding(start = 4.dp), fontSize = 20.sp, fontWeight = FontWeight.Black, color = Sage900)
            }
        }

        if (sorted.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无拾味手记", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Sage300)
            }
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalItemSpacing = 12.dp,
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 100.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(sorted) { note ->
                    Box(
                        modifier = Modifier.fillMaxWidth().aspectRatio(0.75f).clip(RoundedCornerShape(24.dp))
                            .background(Sage100).clickable { onSelectTasting(note.id) }
                    ) {
                        if (note.coverImage.isNotEmpty()) {
                            AsyncImage(note.coverImage, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Box(Modifier.fillMaxSize().background(Sage200), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Image, null, tint = Sage300, modifier = Modifier.size(48.dp))
                            }
                        }
                        Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color(0x66000000)))))
                        Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val fullStars = note.rating.toInt()
                                val hasHalf = note.rating - fullStars >= 0.5f
                                repeat(fullStars) { Icon(Icons.Default.Star, null, tint = Color(0xFFFF9800), modifier = Modifier.size(12.dp)) }
                                if (hasHalf) Icon(Icons.Default.Star, null, tint = Color(0xFFFF9800).copy(alpha = 0.5f), modifier = Modifier.size(12.dp))
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(if (note.note.length > 10) note.note.take(10) + "…" else note.note.ifEmpty { "未命名" }, fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}
