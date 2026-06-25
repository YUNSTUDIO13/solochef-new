package com.example.solochef.ui.screens.picking

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.solochef.model.Recipe
import com.example.solochef.ui.theme.*

@Composable
fun PickingScreen(
    recipe: Recipe,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val allMaterials = recipe.materials.values.flatten()
    var checkedKeys by remember { mutableStateOf(setOf<String>()) }

    val allKeys = remember(recipe) {
        val keys = mutableListOf<String>()
        recipe.materials.forEach { (cat, items) -> items.forEachIndexed { i, _ -> keys.add("$cat-$i") } }
        keys
    }
    val allChecked = allKeys.isNotEmpty() && allKeys.all { it in checkedKeys }
    val totalCount = allMaterials.size

    Box(Modifier.fillMaxSize().background(Sage100)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState()).padding(bottom = 120.dp)) {
            Spacer(Modifier.height(24.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("拣货确认", fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.05).sp, color = Sage900)
                    Text("Material Picking / BOM Check", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = Sage500, modifier = Modifier.padding(top = 8.dp))
                }
                TextButton(onClick = onCancel) { Text("取消", color = Sage500, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(24.dp))

            // Recipe header card
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(32.dp), color = Color.White, border = BorderStroke(1.dp, Sage200)) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(modifier = Modifier.size(56.dp), shape = RoundedCornerShape(16.dp), color = Sage100) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Inventory, contentDescription = null, tint = Sage900, modifier = Modifier.size(28.dp)) } }
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
                Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(32.dp)).background(Color.White).border(1.dp, Sage200, RoundedCornerShape(32.dp))) {
                    AsyncImage(snap, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    Surface(modifier = Modifier.padding(16.dp), shape = RoundedCornerShape(50), color = Sage900.copy(0.8f)) {
                        Text("食材全家福", Modifier.padding(horizontal = 12.dp, vertical = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Material checklist
            recipe.materials.forEach { (cat, items) ->
                if (items.isEmpty()) return@forEach
                val label = when (cat) { "meat" -> "肉禽 / 水产"; "vegetable" -> "蔬菜 / 水果"; else -> "调料 / 干货" }
                val icon = when (cat) { "meat" -> Icons.Default.SetMeal; "vegetable" -> Icons.Default.Eco; else -> Icons.Default.Opacity }

                Column {
                    Row(Modifier.padding(horizontal = 4.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, contentDescription = null, tint = Sage500, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Sage500)
                    }
                    items.withIndex().toList().chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { (idx, m) ->
                                val key = "$cat-$idx"
                                val isChecked = key in checkedKeys
                                Surface(
                                    onClick = { checkedKeys = if (isChecked) checkedKeys - key else checkedKeys + key },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(24.dp),
                                    color = if (isChecked) Sage800 else Color.White,
                                    border = BorderStroke(1.dp, if (isChecked) Sage800 else Sage200)
                                ) {
                                    Row(Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(18.dp).border(2.dp, if (isChecked) Color.White else Sage200, RoundedCornerShape(5.dp)).background(if (isChecked) Color.White else Color.Transparent, RoundedCornerShape(5.dp)), contentAlignment = Alignment.Center) {
                                            if (isChecked) Icon(Icons.Default.Check, contentDescription = null, tint = Sage800, modifier = Modifier.size(11.dp))
                                        }
                                        Spacer(Modifier.width(6.dp))
                                        Text("🥬", fontSize = 14.sp)
                                        Spacer(Modifier.width(6.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(m.item, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Black, lineHeight = 13.sp), color = if (isChecked) Color.White else Sage900, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            Text("${m.amount}${m.unit}", style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, lineHeight = 10.sp), color = if (isChecked) Color.White.copy(0.6f) else Sage300)
                                        }
                                    }
                                }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }

        // Footer — full-width solid background
        Surface(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(), color = Sage100, shadowElevation = 8.dp) {
            Row(Modifier.padding(horizontal = 24.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (totalCount > 0) {
                    Surface(
                        onClick = { checkedKeys = if (allChecked) emptySet() else allKeys.toSet() },
                        modifier = Modifier.size(80.dp),
                        shape = RoundedCornerShape(32.dp),
                        color = if (allChecked) Sage800 else Color.White,
                        border = BorderStroke(1.dp, Sage200)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Box(Modifier.size(24.dp).border(2.dp, if (allChecked) Color.White else Sage300, RoundedCornerShape(6.dp)).background(if (allChecked) Color.White else Color.Transparent, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                                if (allChecked) Icon(Icons.Default.Check, contentDescription = null, tint = Sage800, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
                Surface(
                    onClick = onConfirm,
                    enabled = allChecked || totalCount == 0,
                    modifier = Modifier.weight(1f).height(80.dp),
                    shape = RoundedCornerShape(32.dp),
                    color = if (allChecked || totalCount == 0) Sage800 else Sage200
                ) {
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Text("确认物料并开火", fontSize = 18.sp, fontWeight = FontWeight.Black, color = if (allChecked || totalCount == 0) Color.White else Sage400)
                        Spacer(Modifier.width(12.dp))
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = if (allChecked || totalCount == 0) Color.White else Sage400, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}
