package com.example.solochef.ui.screens.ingredients

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.solochef.model.IngredientCategory
import com.example.solochef.model.IngredientItem
import com.example.solochef.model.IngredientLibrary
import com.example.solochef.storage.LocalFileManager
import com.example.solochef.ui.theme.*
import kotlinx.coroutines.launch
import java.util.UUID

private val selectedGreen = Color(0xFF2D4A3A)
private val selectedBg = Color(0xFFE8F5E9)

@Composable
fun IngredientSelectorSheet(
    onDismiss: () -> Unit,
    onConfirm: (List<IngredientItem>) -> Unit
) {
    val context = LocalContext.current
    val storage = remember { LocalFileManager(context) }
    val scope = rememberCoroutineScope()

    var library by remember { mutableStateOf<IngredientLibrary?>(null) }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var newIngredientName by remember { mutableStateOf("") }
    var showFavoritePicker by remember { mutableStateOf(false) }

    val FAVORITES_ID = "favorites"

    LaunchedEffect(Unit) {
        var lib = storage.getIngredientLibrary()
        if (lib.categories.none { it.id == FAVORITES_ID }) {
            val favCategory = IngredientCategory(
                id = FAVORITES_ID, name = "常用食材",
                targetCategory = "favorite", emoji = "⭐",
                ingredients = emptyList(), isCustom = true
            )
            lib = lib.copy(categories = listOf(favCategory) + lib.categories)
            storage.saveIngredientLibrary(lib)
        }
        library = lib
    }

    LaunchedEffect(library) {
        val cats = library?.categories
        if (cats != null && selectedCategoryId == null && cats.isNotEmpty()) {
            selectedCategoryId = cats.first().id
        }
    }

    fun saveAndRefresh(updated: IngredientLibrary) {
        scope.launch {
            storage.saveIngredientLibrary(updated)
            library = updated
        }
    }

    val categories = library?.categories ?: emptyList()
    val selectedCategory = categories.find { it.id == selectedCategoryId }

    val lib = library

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            Modifier.fillMaxWidth().fillMaxHeight(0.95f),
            color = Sage100,
            shape = RoundedCornerShape(28.dp)
        ) {
            if (lib == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Sage400, strokeWidth = 2.dp)
                }
                return@Surface
            }

            Column(Modifier.fillMaxSize()) {
                // ─── Top Bar ───
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Close, null, tint = Sage900, modifier = Modifier.size(24.dp))
                    }
                    Text(
                        "选择食材", Modifier.weight(1f).padding(start = 4.dp),
                        fontSize = 20.sp, fontWeight = FontWeight.Black, color = Sage900
                    )
                    TextButton(
                        onClick = {
                            val selectedItems = categories.flatMap { it.ingredients }
                                .filter { it.id in selectedIds }
                            onConfirm(selectedItems)
                        },
                        enabled = selectedIds.isNotEmpty()
                    ) {
                        Text(
                            "确认(${selectedIds.size})",
                            fontSize = 13.sp, fontWeight = FontWeight.Black,
                            color = if (selectedIds.isNotEmpty()) selectedGreen else Sage300
                        )
                    }
                }

                // ─── Search Box ───
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).height(48.dp),
                    placeholder = {
                        Text("搜索食材...",
                            style = TextStyle(fontSize = 11.sp, lineHeight = 11.sp,
                                platformStyle = PlatformTextStyle(includeFontPadding = false)),
                            color = Sage300, fontWeight = FontWeight.Bold
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, null, tint = Sage500, modifier = Modifier.size(18.dp))
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Sage400,
                        unfocusedBorderColor = Sage200,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    textStyle = TextStyle(fontSize = 11.sp, lineHeight = 11.sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false))
                )

                // ─── Content: Left Sidebar + Right Grid ───
                Row(Modifier.fillMaxSize()) {
                    // ─── Left Sidebar ───
                    Column(
                        Modifier.width(100.dp).fillMaxHeight().background(Sage50)
                            .verticalScroll(rememberScrollState()).padding(vertical = 4.dp)
                    ) {
                        categories.forEach { cat ->
                            val selected = cat.id == selectedCategoryId
                            Box(
                                Modifier.fillMaxWidth()
                                    .clickable { selectedCategoryId = cat.id; searchQuery = "" }
                                    .then(
                                        if (selected) Modifier.background(Color.White, RoundedCornerShape(12.dp))
                                        else Modifier
                                    )
                                    .padding(horizontal = 6.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(cat.emoji, fontSize = 18.sp)
                                    Spacer(Modifier.height(2.dp))
                                    Text(cat.name, fontSize = 8.sp, color = Color.Black,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }

                    // ─── Right Content ───
                    val isSearching = searchQuery.isNotBlank()

                    if (isSearching) {
                        // Global search results across all categories
                        val globalResults = categories.flatMap { cat ->
                            cat.ingredients
                                .filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
                                .map { it to cat }
                        }.distinctBy { (ing, _) -> ing.id }
                        Column(Modifier.fillMaxSize().padding(12.dp)) {
                            Text(
                                "搜索 \"${searchQuery.trim()}\" (${globalResults.size})",
                                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Sage900
                            )
                            Spacer(Modifier.height(10.dp))
                            if (globalResults.isEmpty()) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("未找到食材", color = Sage400, fontSize = 14.sp)
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(globalResults, key = { (ing, _) -> ing.id }) { (ingredient, cat) ->
                                        val isSelected = ingredient.id in selectedIds
                                        Surface(
                                            modifier = Modifier.fillMaxWidth().clickable {
                                                selectedIds = if (isSelected)
                                                    selectedIds - ingredient.id
                                                else
                                                    selectedIds + ingredient.id
                                            },
                                            shape = RoundedCornerShape(16.dp),
                                            color = if (isSelected) selectedBg else Color.White,
                                            border = BorderStroke(
                                                if (isSelected) 2.dp else 1.dp,
                                                if (isSelected) selectedGreen else Sage100
                                            )
                                        ) {
                                            Column(
                                                Modifier.fillMaxWidth().padding(10.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(cat.emoji, fontSize = 14.sp)
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    ingredient.name, fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold, color = Sage900,
                                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                                )
                                                Text(cat.name, fontSize = 7.sp, color = Sage400)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (selectedCategory != null) {
                        Column(Modifier.fillMaxSize().padding(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${selectedCategory.emoji} ${IngredientCategory.targetLabel(selectedCategory.targetCategory)}",
                                    fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Sage900
                                )
                                Text(
                                    "${selectedCategory.ingredients.size}",
                                    fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Sage400
                                )
                            }
                            Spacer(Modifier.height(10.dp))

                            val displayIngredients = selectedCategory.ingredients

                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(displayIngredients, key = { it.id }) { ingredient ->
                                    val isSelected = ingredient.id in selectedIds
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            selectedIds = if (isSelected)
                                                selectedIds - ingredient.id
                                            else
                                                selectedIds + ingredient.id
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (isSelected) selectedBg else Color.White,
                                        border = BorderStroke(
                                            if (isSelected) 2.dp else 1.dp,
                                            if (isSelected) selectedGreen else Sage100
                                        )
                                    ) {
                                        Box(
                                            Modifier.fillMaxWidth().padding(12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                ingredient.name, fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold, color = Sage900,
                                                maxLines = 1, overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                            if (selectedCategory.id == FAVORITES_ID) {
                                Button(
                                    onClick = { showFavoritePicker = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                                ) {
                                    Icon(Icons.Default.Star, null, tint = Sage900, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("从分类添加食材", color = Sage900, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        newIngredientName = ""
                                        showAddDialog = true
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                                ) {
                                    Icon(Icons.Default.Star, null, tint = Sage900, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("添加食材", color = Sage900, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("选择一个分类", color = Sage400, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }

    // ─── Add Ingredient Dialog ───
    if (showAddDialog && library != null && selectedCategoryId != null) {
        val catId = selectedCategoryId!!
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(28.dp),
            title = { Text("添加食材", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Sage900) },
            text = {
                OutlinedTextField(
                    value = newIngredientName,
                    onValueChange = { newIngredientName = it },
                    placeholder = { Text("食材名称", color = Sage300, fontSize = 13.sp) },
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
                        val name = newIngredientName.trim()
                        if (name.isNotEmpty()) {
                            val newItem = IngredientItem(
                                id = UUID.randomUUID().toString(),
                                name = name,
                                categoryId = catId,
                                isStandard = false
                            )
                            val updated = lib!!.copy(categories = categories.map { cat ->
                                if (cat.id == catId) cat.copy(ingredients = cat.ingredients + newItem) else cat
                            })
                            saveAndRefresh(updated)
                        }
                        showAddDialog = false
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Sage800)
                ) { Text("添加", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("取消", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Sage500) }
            }
        )
    }

    // ─── Favorite Picker Dialog ───
    if (showFavoritePicker) {
        var pickedIds by remember { mutableStateOf(setOf<String>()) }
        var favSearchQuery by remember { mutableStateOf("") }
        val baseIngredients = remember(categories) {
            categories.filter { it.id != FAVORITES_ID }
                .flatMap { cat -> cat.ingredients.map { it to cat } }
        }
        val allIngredients = if (favSearchQuery.isBlank()) baseIngredients
            else baseIngredients.filter { (ing, _) -> ing.name.contains(favSearchQuery.trim(), ignoreCase = true) }
        val existingFavIds = remember(categories) {
            categories.find { it.id == FAVORITES_ID }?.ingredients?.map { it.id }?.toSet() ?: emptySet()
        }

        AlertDialog(
            onDismissRequest = { showFavoritePicker = false },
            title = { Text("选择常用食材", fontWeight = FontWeight.Bold, color = Sage900) },
            text = {
                Column {
                    OutlinedTextField(
                        value = favSearchQuery,
                        onValueChange = { favSearchQuery = it },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        placeholder = {
                            Text("搜索食材...",
                                style = TextStyle(fontSize = 10.sp, lineHeight = 10.sp,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)),
                                color = Sage300, fontWeight = FontWeight.Bold
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, null, tint = Sage500, modifier = Modifier.size(16.dp))
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Sage400,
                            unfocusedBorderColor = Sage200,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        textStyle = TextStyle(fontSize = 10.sp, lineHeight = 10.sp,
                            platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )
                    Spacer(Modifier.height(8.dp))
                    Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                        allIngredients.forEach { (ingredient, cat) ->
                            val isPicked = ingredient.id in pickedIds
                            val isAlreadyFav = ingredient.id in existingFavIds
                            Surface(
                                onClick = {
                                    if (!isAlreadyFav) {
                                        pickedIds = if (isPicked) pickedIds - ingredient.id else pickedIds + ingredient.id
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isPicked) selectedBg else if (isAlreadyFav) Sage100 else Color.White,
                                border = BorderStroke(1.dp, if (isPicked) selectedGreen else Sage100)
                            ) {
                                Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(cat.emoji, fontSize = 14.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Text(ingredient.name, Modifier.weight(1f), fontSize = 13.sp, color = if (isAlreadyFav) Sage400 else Sage900)
                                    Text(cat.name, fontSize = 10.sp, color = Sage400)
                                    if (isPicked) {
                                        Spacer(Modifier.width(8.dp))
                                        Icon(Icons.Default.CheckCircle, null, tint = selectedGreen, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (pickedIds.isNotEmpty()) {
                        val favCategory = categories.find { it.id == FAVORITES_ID } ?: return@TextButton
                        val pickedItems = allIngredients
                            .filter { (ing, _) -> ing.id in pickedIds }
                            .map { (ing, _) -> IngredientItem(id = ing.id, name = ing.name, categoryId = FAVORITES_ID, isStandard = true) }
                        val updated = lib!!.copy(categories = categories.map { cat ->
                            if (cat.id == FAVORITES_ID) cat.copy(ingredients = favCategory.ingredients + pickedItems) else cat
                        })
                        saveAndRefresh(updated)
                    }
                    showFavoritePicker = false
                }) { Text("添加选中的食材", color = Sage900) }
            },
            dismissButton = {
                TextButton(onClick = { showFavoritePicker = false }) { Text("取消", color = Sage400) }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }
}
