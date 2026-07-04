package com.example.solochef.ui.screens.picking

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.solochef.model.Material
import com.example.solochef.model.Recipe
import com.example.solochef.ui.theme.*

@Composable
fun PickingScreen(
    recipe: Recipe,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val allMaterialsWithKeys = remember(recipe) {
        buildList {
            recipe.materials.forEach { (cat, items) ->
                items.forEachIndexed { idx, m -> add("$cat-$idx" to m) }
            }
        }
    }
    var checkedKeys by remember { mutableStateOf(setOf<String>()) }
    val allChecked = allMaterialsWithKeys.isNotEmpty() && allMaterialsWithKeys.all { it.first in checkedKeys }
    val totalCount = allMaterialsWithKeys.size

    Box(Modifier.fillMaxSize().warmGradientBackground().statusBarsPadding()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState()).padding(bottom = 120.dp)) {
            Spacer(Modifier.height(24.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("拣货确认", fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.05).sp, color = Sage900)
                TextButton(onClick = onCancel) { Text("取消", color = Sage500, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(24.dp))

            // Recipe header card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .frostedGlassBackground()
                    .border(1.dp, Color.White.copy(0.4f), RoundedCornerShape(32.dp))
            ) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(Sage100), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Inventory, contentDescription = null, tint = Sage900, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(recipe.name, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Sage900)
                        Text("共需 $totalCount 项物料", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = Sage500)
                    }
                }
            }

            // BOM snapshot
            recipe.bom_snapshot?.let { snap ->
                Spacer(Modifier.height(24.dp))
                Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(32.dp)).background(Color.White).border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(32.dp))) {
                    AsyncImage(snap, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    Box(Modifier.padding(16.dp).clip(RoundedCornerShape(50)).background(Sage900.copy(0.8f))) {
                        Text("食材全家福", Modifier.padding(horizontal = 12.dp, vertical = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Material grid
            allMaterialsWithKeys.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { (key, m) ->
                        val isSelected = key in checkedKeys
                        PickingCard(
                            item = m,
                            isSelected = isSelected,
                            onClick = { checkedKeys = if (isSelected) checkedKeys - key else checkedKeys + key },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(10.dp))
            }
        }

        // Footer
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (totalCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .frostedGlassBackground()
                            .border(1.dp, Color.White.copy(0.4f), RoundedCornerShape(24.dp))
                            .clickable { checkedKeys = if (allChecked) emptySet() else allMaterialsWithKeys.map { it.first }.toSet() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (allChecked) "取消全选" else "全选", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Sage900, textAlign = TextAlign.Center)
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .then(
                            if (allChecked || totalCount == 0)
                                Modifier.frostedGlassBackground()
                            else
                                Modifier.frostedGlassBackground() // same for both; visual difference comes from text color
                        )
                        .border(1.dp, Color.White.copy(0.4f), RoundedCornerShape(24.dp))
                        .clickable(enabled = allChecked || totalCount == 0) { onConfirm() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("确认物料并开火", fontSize = 16.sp, fontWeight = FontWeight.Black, color = if (allChecked || totalCount == 0) Sage900 else Sage400)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = if (allChecked || totalCount == 0) Sage900 else Sage400, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PickingCard(item: Material, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .aspectRatio(1f / 1.15f)
            .clip(RoundedCornerShape(16.dp))
            .then(if (isSelected) Modifier.background(SelectedGreen) else Modifier.frostedGlassBackground())
            .border(1.dp, Color.White.copy(0.4f), RoundedCornerShape(16.dp))
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
            Text(item.item, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Sage900, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, lineHeight = 16.sp)
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${item.amount}${item.unit}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Sage500, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
    }
}
