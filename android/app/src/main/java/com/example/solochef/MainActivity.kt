package com.example.solochef

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.solochef.model.*
import com.example.solochef.storage.LocalFileManager
import com.example.solochef.ui.screens.analytics.AnalyticsScreen
import com.example.solochef.ui.screens.analytics.WokHeatRankingScreen
import com.example.solochef.ui.screens.batchdetail.BatchDetailScreen
import com.example.solochef.ui.screens.createrecipe.CreateRecipeScreen
import com.example.solochef.ui.screens.dashboard.DashboardScreen
import com.example.solochef.ui.screens.execution.ExecutionScreen
import com.example.solochef.ui.screens.feedback.FeedbackScreen
import com.example.solochef.ui.screens.library.LibraryScreen
import com.example.solochef.ui.screens.orderengine.OrderEngineScreen
import com.example.solochef.ui.screens.picking.PickingScreen
import com.example.solochef.ui.screens.recipedetail.RecipeDetailScreen
import com.example.solochef.ui.screens.settings.SettingsScreen
import com.example.solochef.ui.theme.*
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Dashboard : Screen("dashboard", "首页", Icons.Default.Dashboard)
    data object Library : Screen("library", "菜谱", Icons.Default.Search)
    data object Analytics : Screen("analytics", "数据", Icons.Default.BarChart)
    data object Settings : Screen("settings", "设置", Icons.Default.Settings)
    data object RecipeDetail : Screen("recipe_detail/{recipeId}", "详情", Icons.Default.Info)
    data object CreateRecipe : Screen("create_recipe?editId={editId}", "新建", Icons.Default.Add)
    data object OrderEngine : Screen("order_engine", "点单", Icons.Default.ShoppingCart)
    data object Picking : Screen("picking/{recipeId}/{isLazy}", "拣货", Icons.Default.Inventory)
    data object Execution : Screen("execution/{recipeId}/{isLazy}", "烹饪", Icons.Default.Timer)
    data object Feedback : Screen("feedback/{recipeId}", "完成", Icons.Default.CheckCircle)
    data object BatchDetail : Screen("batch_detail", "批次", Icons.Default.ListAlt)
    data object WokHeatRanking : Screen("wok_heat_ranking", "锅气榜", Icons.Default.Whatshot)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SoloChefTheme { SoloChefApp() } }
    }
}

@Composable
fun SoloChefApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val storage = remember { LocalFileManager(context) }

    // Shared app state — loaded once, refreshed on save/delete
    var recipes by remember { mutableStateOf<List<Recipe>>(emptyList()) }
    var cookingRecords by remember { mutableStateOf<List<CookingRecord>>(emptyList()) }
    var stats by remember { mutableStateOf(UserStats()) }
    var activeBatch by remember { mutableStateOf<OrderBatch?>(null) }
    var selectedRecipe by remember { mutableStateOf<Recipe?>(null) }
    var feedbackBatchRecipes by remember { mutableStateOf<List<Recipe>>(emptyList()) }
    var analyticsKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        recipes = storage.getAllRecipes()
        cookingRecords = storage.getCookingRecords()
        stats = storage.getStats()
        activeBatch = storage.getActiveBatch()
    }

    val mainScreens = listOf(Screen.Dashboard, Screen.Library, Screen.Analytics, Screen.Settings)
    val showBottomNav = currentRoute in mainScreens.map { it.route }
    val showFAB = currentRoute == Screen.Dashboard.route || currentRoute == Screen.Library.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Sage100,
        // ─── FAB: floating above bottom nav (centered + thin) ───
        floatingActionButton = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                AnimatedVisibility(
                    visible = showFAB,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(0.9f).padding(bottom = 2.dp),
                        shape = RoundedCornerShape(50),
                        color = Sage900.copy(alpha = 0.95f),
                        shadowElevation = 12.dp
                    ) {
                        Row(
                            Modifier.fillMaxWidth().border(1.dp, White20, RoundedCornerShape(50)).padding(horizontal = 4.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                Modifier.weight(1f).clickable { navController.navigate(Screen.OrderEngine.route) }.padding(horizontal = 12.dp, vertical = 0.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(modifier = Modifier.size(28.dp), shape = CircleShape, color = White20) {
                                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp)) }
                                }
                                Column(Modifier.padding(start = 8.dp)) {
                                    Text("Kitchen OMS", fontSize = 7.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = White40, lineHeight = 9.sp)
                                    Text("独厨点单", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White, lineHeight = 14.sp)
                                }
                            }
                            Box(Modifier.width(1.dp).height(24.dp).background(White10))
                            Row(
                                Modifier.clickable { navController.navigate(Screen.CreateRecipe.route) }.padding(horizontal = 16.dp, vertical = 0.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                Text("新建菜谱", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        },
        // ─── Bottom Navigation (centered + thin + uniform margins) ──
        bottomBar = {
            Box(Modifier.fillMaxWidth().padding(bottom = 20.dp), contentAlignment = Alignment.Center) {
                AnimatedVisibility(
                    visible = showBottomNav,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        shape = RoundedCornerShape(50),
                        color = White90,
                        shadowElevation = 8.dp,
                        border = BorderStroke(1.dp, White20)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            mainScreens.forEach { s ->
                                val sel = currentRoute == s.route
                                Surface(
                                    onClick = {
                                        if (sel) return@Surface
                                        navController.navigate(s.route) {
                                            popUpTo(Screen.Dashboard.route)
                                            launchSingleTop = true
                                        }
                                        if (s == Screen.Analytics) analyticsKey++
                                    },
                                    modifier = Modifier.clip(RoundedCornerShape(50)),
                                    shape = RoundedCornerShape(50),
                                    color = if (sel) Sage800 else Color.Transparent
                                ) {
                                    Row(Modifier.padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(s.icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (sel) Color.White else Sage800)
                                        if (sel) { Spacer(Modifier.width(6.dp)); Text(s.label, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Color.White) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        // ─── Position FAB above bottom bar ──────────────
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.fillMaxSize().then(
                if (showBottomNav) Modifier.padding(paddingValues)
                else Modifier.padding(top = paddingValues.calculateTopPadding())
            )
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    activeBatchOverride = activeBatch,
                    onPlaceOrder = { navController.navigate(Screen.OrderEngine.route) },
                    onOpenBatch = { navController.navigate(Screen.BatchDetail.route) },
                    onSelectRecipe = { r -> selectedRecipe = r; navController.navigate("recipe_detail/${r.id}") },
                    onRandomOrder = { id ->
                        scope.launch {
                            val b = OrderBatch(id = System.currentTimeMillis().toString(), status = BatchStatus.Picking, recipeIds = listOf(id), completedRecipeIds = emptyList(), created_at = System.currentTimeMillis().toString())
                            storage.saveActiveBatch(b); activeBatch = b
                            navController.navigate(Screen.BatchDetail.route)
                        }
                    }
                )
            }

            composable(Screen.Library.route) {
                LibraryScreen(
                    onSelectRecipe = { r -> selectedRecipe = r; navController.navigate("recipe_detail/${r.id}") },
                    onCreateClick = { navController.navigate(Screen.CreateRecipe.route) }
                )
            }

            composable(Screen.RecipeDetail.route, arguments = listOf(navArgument("recipeId") { type = NavType.StringType })) { entry ->
                val id = entry.arguments?.getString("recipeId") ?: ""
                recipes.find { it.id == id }?.let { recipe ->
                    RecipeDetailScreen(
                        recipe = recipe,
                        onBack = { navController.popBackStack() },
                        onGoCook = { selectedRecipe = recipe; navController.navigate("picking/${recipe.id}/false") },
                        onDelete = { rid -> scope.launch { storage.deleteRecipe(rid); recipes = storage.getAllRecipes() }; navController.popBackStack() },
                        onEdit = { r -> selectedRecipe = r; navController.navigate("create_recipe?editId=${r.id}") }
                    )
                }
            }

            composable(Screen.CreateRecipe.route, arguments = listOf(navArgument("editId") { type = NavType.StringType; defaultValue = "" })) { entry ->
                val editId = entry.arguments?.getString("editId") ?: ""
                val existing = if (editId.isNotBlank()) recipes.find { it.id == editId } else null
                CreateRecipeScreen(
                    existingRecipe = existing,
                    onSave = { recipe ->
                        scope.launch { storage.saveRecipe(recipe); recipes = storage.getAllRecipes(); stats = stats.ignite().also { storage.saveStats(it) } }
                        navController.navigate(Screen.Library.route) { popUpTo(Screen.Dashboard.route) }
                    },
                    onCancel = { navController.popBackStack() }
                )
            }

            composable(Screen.OrderEngine.route) {
                OrderEngineScreen(
                    activeBatch = activeBatch,
                    onConfirm = { ids ->
                        scope.launch {
                            val existing = activeBatch
                            if (existing != null) {
                                if (existing.status == BatchStatus.Picking) {
                                    // Merge — keep duplicates for quantity tracking
                                    val merged = existing.copy(recipeIds = existing.recipeIds + ids)
                                    storage.saveActiveBatch(merged); activeBatch = merged
                                } else {
                                    // Conflict → overwrite with new batch
                                    val b = OrderBatch(id = System.currentTimeMillis().toString(), status = BatchStatus.Picking, recipeIds = ids, completedRecipeIds = emptyList(), created_at = System.currentTimeMillis().toString())
                                    storage.saveActiveBatch(b); activeBatch = b
                                }
                            } else {
                                val b = OrderBatch(id = System.currentTimeMillis().toString(), status = BatchStatus.Picking, recipeIds = ids, completedRecipeIds = emptyList(), created_at = System.currentTimeMillis().toString())
                                storage.saveActiveBatch(b); activeBatch = b
                            }
                            navController.navigate(Screen.BatchDetail.route) { popUpTo(Screen.OrderEngine.route) { inclusive = true } }
                        }
                    },
                    onCancel = { navController.popBackStack() }
                )
            }

            composable(Screen.BatchDetail.route) {
                activeBatch?.let { batch ->
                    BatchDetailScreen(
                        batch = batch, recipes = recipes,
                        onUpdateStatus = { id, status ->
                            scope.launch {
                                if (status == BatchStatus.Finished) {
                                    // Save last_cooked_at + cooked_count for all recipes (distinct — ×3 qty still counts as 1)
                                    batch.recipeIds.distinct().forEach { rid ->
                                        recipes.find { it.id == rid }?.let { r ->
                                            val u = r.copy(last_cooked_at = System.currentTimeMillis().toString(), cooked_count = r.cooked_count + 1)
                                            storage.saveRecipe(u)
                                            recipes = recipes.map { if (it.id == u.id) u else it }
                                        }
                                    }
                                    // Save cooking records (no distinct — ×2 qty records twice for calendar)
                                    val now = System.currentTimeMillis()
                                    batch.recipeIds.forEach { rid ->
                                        recipes.find { it.id == rid }?.let { r ->
                                            storage.saveCookingRecord(CookingRecord(
                                                recipeId = r.id,
                                                recipeName = r.name,
                                                coverImage = r.cover_image,
                                                cookedAt = now,
                                                tags = r.tags,
                                                durationMins = r.timeline.sumOf { it.duration } / 60
                                            ))
                                        }
                                    }
                                    cookingRecords = storage.getCookingRecords()
                                    stats = stats.ignite().also { storage.saveStats(it) }
                                    // Capture batch recipe objects for thermal receipt
                                    feedbackBatchRecipes = batch.recipeIds.mapNotNull { rid -> recipes.find { it.id == rid } }
                                    // Navigate FIRST, then clear batch
                                    val firstRid = batch.recipeIds.firstOrNull()
                                    if (firstRid != null) {
                                        recipes.find { it.id == firstRid }?.let { selectedRecipe = it }
                                        navController.navigate("feedback/$firstRid") { popUpTo(Screen.BatchDetail.route) { inclusive = true } }
                                    } else {
                                        navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Dashboard.route) { inclusive = true } }
                                    }
                                    storage.saveActiveBatch(null); activeBatch = null
                                }
                                else { val u = batch.copy(status = status); storage.saveActiveBatch(u); activeBatch = u }
                            }
                        },
                        onCompleteRecipe = { rid ->
                            scope.launch {
                                val b = activeBatch ?: return@launch
                                val idx = b.recipeIds.indexOf(rid)
                                if (idx < 0) return@launch
                                val newIds = b.recipeIds.toMutableList().also { it.removeAt(idx) }
                                if (newIds.isEmpty()) {
                                    // All done → save last_cooked_at + cooked_count, then thermal receipt
                                    recipes.find { it.id == rid }?.let { r ->
                                        val u = r.copy(last_cooked_at = System.currentTimeMillis().toString(), cooked_count = r.cooked_count + 1)
                                        storage.saveRecipe(u)
                                        recipes = recipes.map { if (it.id == u.id) u else it }
                                    }
                                    // Save cooking record
                                    recipes.find { it.id == rid }?.let { r ->
                                        storage.saveCookingRecord(CookingRecord(
                                            recipeId = r.id, recipeName = r.name, coverImage = r.cover_image,
                                            cookedAt = System.currentTimeMillis(), tags = r.tags,
                                            durationMins = r.timeline.sumOf { it.duration } / 60
                                        ))
                                    }
                                    cookingRecords = storage.getCookingRecords()
                                    stats = stats.ignite().also { storage.saveStats(it) }
                                    // Capture ALL batch recipe objects for receipt (including the one just completed)
                                    feedbackBatchRecipes = b.recipeIds.mapNotNull { rid2 -> recipes.find { it.id == rid2 } }
                                    selectedRecipe = recipes.find { it.id == rid }
                                    storage.saveActiveBatch(null); activeBatch = null
                                    navController.navigate("feedback/$rid") { popUpTo(Screen.BatchDetail.route) { inclusive = true } }
                                } else {
                                    // Partial complete → stay on current page, update last_cooked_at + cooked_count
                                    recipes.find { it.id == rid }?.let { r ->
                                        val u = r.copy(last_cooked_at = System.currentTimeMillis().toString(), cooked_count = r.cooked_count + 1)
                                        storage.saveRecipe(u)
                                        recipes = recipes.map { if (it.id == u.id) u else it }
                                    }
                                    // Save cooking record
                                    recipes.find { it.id == rid }?.let { r ->
                                        storage.saveCookingRecord(CookingRecord(
                                            recipeId = r.id, recipeName = r.name, coverImage = r.cover_image,
                                            cookedAt = System.currentTimeMillis(), tags = r.tags,
                                            durationMins = r.timeline.sumOf { it.duration } / 60
                                        ))
                                    }
                                    cookingRecords = storage.getCookingRecords()
                                    val u = b.copy(recipeIds = newIds, completedRecipeIds = b.completedRecipeIds + rid)
                                    storage.saveActiveBatch(u); activeBatch = u
                                    stats = stats.ignite().also { storage.saveStats(it) }
                                }
                            }
                        },
                        onClose = { navController.popBackStack() },
                        onOpenSOP = { r -> selectedRecipe = r; navController.navigate("execution/${r.id}/false") }
                    )
                } ?: run { Box(Modifier.fillMaxSize().background(Sage100)) }
            }

            composable(Screen.Picking.route, arguments = listOf(navArgument("recipeId") { type = NavType.StringType }, navArgument("isLazy") { type = NavType.BoolType })) { entry ->
                val rid = entry.arguments?.getString("recipeId") ?: ""
                val lazy = entry.arguments?.getBoolean("isLazy") ?: false
                recipes.find { it.id == rid }?.let { recipe ->
                    PickingScreen(recipe = recipe, onConfirm = { navController.navigate("execution/${recipe.id}/$lazy") { popUpTo(Screen.Picking.route) { inclusive = true } } }, onCancel = { navController.popBackStack() })
                }
            }

            composable(Screen.Execution.route, arguments = listOf(navArgument("recipeId") { type = NavType.StringType }, navArgument("isLazy") { type = NavType.BoolType })) { entry ->
                val rid = entry.arguments?.getString("recipeId") ?: ""
                val lazy = entry.arguments?.getBoolean("isLazy") ?: false
                recipes.find { it.id == rid }?.let { recipe ->
                    ExecutionScreen(recipe = recipe, isLazy = lazy, onComplete = { r ->
                        scope.launch {
                            val u = r.copy(last_cooked_at = System.currentTimeMillis().toString(), cooked_count = r.cooked_count + 1)
                            storage.saveRecipe(u); recipes = recipes.map { if (it.id == u.id) u else it }
                            // Save cooking record for calendar
                            storage.saveCookingRecord(CookingRecord(
                                recipeId = r.id,
                                recipeName = r.name,
                                coverImage = r.cover_image,
                                cookedAt = System.currentTimeMillis(),
                                tags = r.tags,
                                durationMins = r.timeline.sumOf { it.duration } / 60
                            ))
                            cookingRecords = storage.getCookingRecords()
                        }
                        feedbackBatchRecipes = emptyList()
                        navController.navigate("feedback/${recipe.id}") { popUpTo(Screen.Execution.route) { inclusive = true } }
                    })
                }
            }

            composable(Screen.Feedback.route, arguments = listOf(navArgument("recipeId") { type = NavType.StringType })) { entry ->
                val rid = entry.arguments?.getString("recipeId") ?: ""
                recipes.find { it.id == rid }?.let { recipe ->
                    FeedbackScreen(recipe = recipe, batchRecipes = feedbackBatchRecipes, onDone = {
                        scope.launch { stats = stats.ignite().also { storage.saveStats(it) } }
                        navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Dashboard.route) { inclusive = true } }
                    })
                }
            }

            composable(Screen.Analytics.route) { key(analyticsKey) {
                AnalyticsScreen(
                    recipes = recipes,
                    cookingRecords = cookingRecords,
                    onNavigateToLibrary = { navController.navigate(Screen.Library.route) },
                    onViewAllRanking = { navController.navigate(Screen.WokHeatRanking.route) },
                    onSelectRecipe = { r -> selectedRecipe = r; navController.navigate("recipe_detail/${r.id}") }
                )
            } }

            composable(Screen.WokHeatRanking.route) {
                WokHeatRankingScreen(recipes = recipes, onBack = { navController.popBackStack() })
            }

            composable(Screen.Settings.route) {
                SettingsScreen(onNavigateToLibrary = { navController.navigate(Screen.Library.route) }, onNavigateToAnalytics = { analyticsKey++; navController.navigate(Screen.Analytics.route) })
            }
        }
    }
}
