package com.example.solochef

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
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
import com.example.solochef.ui.screens.dashboard.FeaturedAllScreen
import com.example.solochef.ui.screens.dashboard.DashboardScreen
import com.example.solochef.ui.screens.dashboard.FeaturedAllScreen
import com.example.solochef.ui.screens.tasting.CreateTastingScreen
import com.example.solochef.ui.screens.tasting.TastingNotesListScreen
import com.example.solochef.ui.screens.tasting.TastingDetailScreen
import com.example.solochef.ui.screens.execution.ExecutionScreen
import com.example.solochef.ui.screens.feedback.FeedbackScreen
import com.example.solochef.ui.screens.library.LibraryScreen
import com.example.solochef.ui.screens.orderengine.OrderEngineScreen
import com.example.solochef.ui.screens.picking.PickingScreen
import com.example.solochef.ui.screens.recipedetail.RecipeDetailScreen
import com.example.solochef.ui.screens.settings.SettingsScreen
import com.example.solochef.ui.screens.ingredients.IngredientLibraryScreen
import com.example.solochef.ui.theme.*
import kotlinx.coroutines.launch

sealed class IconSource

data class VectorIcon(val imageVector: ImageVector) : IconSource()
data class DrawableIcon(@DrawableRes val resId: Int) : IconSource()

sealed class Screen(val route: String, val label: String, val icon: IconSource) {
    data object Dashboard : Screen("dashboard", "首页", VectorIcon(Icons.Default.Dashboard))
    data object Library : Screen("library", "菜谱", DrawableIcon(R.drawable.ic_guo))
    data object Analytics : Screen("analytics", "数据", VectorIcon(Icons.Default.BarChart))
    data object Settings : Screen("settings", "我的", VectorIcon(Icons.Default.Settings))
    data object RecipeDetail : Screen("recipe_detail/{recipeId}", "详情", VectorIcon(Icons.Default.Info))
    data object CreateRecipe : Screen("create_recipe?editId={editId}", "新建", VectorIcon(Icons.Default.Add))
    data object OrderEngine : Screen("order_engine", "点单", VectorIcon(Icons.Default.ShoppingCart))
    data object Picking : Screen("picking/{recipeId}/{isLazy}", "拣货", VectorIcon(Icons.Default.Inventory))
    data object Execution : Screen("execution/{recipeId}/{isLazy}", "烹饪", VectorIcon(Icons.Default.Timer))
    data object Feedback : Screen("feedback/{recipeId}", "完成", VectorIcon(Icons.Default.CheckCircle))
    data object BatchDetail : Screen("batch_detail", "批次", VectorIcon(Icons.Default.ListAlt))
    data object WokHeatRanking : Screen("wok_heat_ranking", "锅气榜", VectorIcon(Icons.Default.Whatshot))
    data object FeaturedAll : Screen("featured_all", "主厨力荐", VectorIcon(Icons.Default.AutoAwesome))
    data object CreateTasting : Screen("create_tasting?editId={editId}", "拾味手记", VectorIcon(Icons.Default.Star))
    data object TastingAll : Screen("tasting_all", "拾味手记", VectorIcon(Icons.Default.Star))
    data object TastingDetail : Screen("tasting_detail/{tastingId}", "拾味手记", VectorIcon(Icons.Default.Star))
    data object IngredientLibrary : Screen("ingredient_library", "食材库", VectorIcon(Icons.Default.Whatshot))
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
    var tastingNotes by remember { mutableStateOf<List<TastingNote>>(emptyList()) }
    var stats by remember { mutableStateOf(UserStats()) }
    var activeBatch by remember { mutableStateOf<OrderBatch?>(null) }
    var selectedRecipe by remember { mutableStateOf<Recipe?>(null) }
    var feedbackBatchRecipes by remember { mutableStateOf<List<Recipe>>(emptyList()) }
    var feedbackReceiptDate by remember { mutableStateOf<Long?>(null) }
    var analyticsKey by remember { mutableIntStateOf(0) }
    var analyticsYear by remember { mutableIntStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)) }
    var analyticsMonth by remember { mutableIntStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)) }
    var libraryName by remember { mutableStateOf("菜谱库") }

    LaunchedEffect(Unit) {
        recipes = storage.getAllRecipes()
        cookingRecords = storage.getCookingRecords()
        tastingNotes = storage.getTastingNotes()
        stats = storage.getStats()
        activeBatch = storage.getActiveBatch()
        libraryName = storage.getLibraryName()
    }

    val mainScreens = listOf(Screen.Dashboard, Screen.Library, Screen.Analytics, Screen.Settings)
    var hideBottomForCube by remember { mutableStateOf(false) }
    val showBottomNav = currentRoute in mainScreens.map { it.route } && !hideBottomForCube
    val showFAB = currentRoute == Screen.Dashboard.route || currentRoute == Screen.Library.route

    Box(Modifier.fillMaxSize().warmGradientBackground()) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
        // ─── Floating Bottom Tab Bar ───
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomNav,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // ─── Liquid Glass Pill with smooth sliding indicator ───
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .liquidGlassBackground()
                    ) {
                        val selectedIndex = mainScreens.indexOfFirst { currentRoute == it.route }

                        BoxWithConstraints(
                            Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp)
                        ) {
                            val tabWidth = maxWidth / 4
                            val indicatorWidth = 56.dp
                            val indicatorOffset by animateDpAsState(
                                targetValue = when (selectedIndex) {
                                    0 -> (tabWidth - indicatorWidth) / 2
                                    1 -> tabWidth + (tabWidth - indicatorWidth) / 2
                                    2 -> tabWidth + tabWidth + (tabWidth - indicatorWidth) / 2
                                    else -> tabWidth + tabWidth + tabWidth + (tabWidth - indicatorWidth) / 2
                                },
                                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                            )

                            // Sliding white pill indicator (80% opacity)
                            Box(
                                modifier = Modifier
                                    .offset(x = indicatorOffset)
                                    .width(indicatorWidth)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(percent = 50))
                                    .background(Color.White.copy(alpha = 0.8f))
                                    .align(Alignment.CenterStart)
                            )

                            // Tab icons
                            Row(
                                Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                mainScreens.forEach { s ->
                                    val sel = currentRoute == s.route
                                    val interactionSource = remember { MutableInteractionSource() }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(1f)
                                            .clickable(
                                                interactionSource = interactionSource,
                                                indication = null
                                            ) {
                                                if (sel) return@clickable
                                                navController.navigate(s.route) {
                                                    popUpTo(Screen.Dashboard.route)
                                                    launchSingleTop = true
                                                }
                                                if (s == Screen.Analytics) analyticsKey++
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        when (val icon = s.icon) {
                                            is VectorIcon -> Icon(icon.imageVector, null, Modifier.size(20.dp), tint = if (sel) Sage900 else Sage500)
                                            is DrawableIcon -> Icon(painter = painterResource(icon.resId), contentDescription = null, modifier = Modifier.size(20.dp), tint = if (sel) Sage900 else Sage500)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ─── OrderEngine standalone liquid glass circle ───
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .liquidGlassBackground()
                            .clickable { navController.navigate(Screen.OrderEngine.route) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_eat),
                            contentDescription = null,
                            tint = Sage500,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        },
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier
                .fillMaxSize()
        ) {
            composable(Screen.Dashboard.route) {
                Box(Modifier.statusBarsPadding()) {
                DashboardScreen(
                    activeBatchOverride = activeBatch,
                    tastingNotes = tastingNotes,
                    onPlaceOrder = { navController.navigate(Screen.OrderEngine.route) },
                    onOpenBatch = { navController.navigate(Screen.BatchDetail.route) },
                    onSelectRecipe = { r -> selectedRecipe = r; navController.navigate("recipe_detail/${r.id}") },
                    onViewAllFeatured = { navController.navigate(Screen.FeaturedAll.route) },
                    onViewAllTasting = { navController.navigate(Screen.TastingAll.route) },
                    onSelectTasting = { id -> navController.navigate("tasting_detail/$id") },
                    onCubeSelectorChanged = { hideBottomForCube = it },
                    onRandomOrder = { id ->
                        scope.launch {
                            val b = OrderBatch(id = System.currentTimeMillis().toString(), status = BatchStatus.Picking, recipeIds = listOf(id), completedRecipeIds = emptyList(), created_at = System.currentTimeMillis().toString())
                            storage.saveActiveBatch(b); activeBatch = b
                            navController.navigate(Screen.BatchDetail.route)
                        }
                    }
                )
                }
            }

            composable(Screen.Library.route) {
                Box(Modifier.statusBarsPadding()) {
                LibraryScreen(
                    libraryName = libraryName,
                    onLibraryNameChange = { libraryName = it; scope.launch { storage.saveLibraryName(it) } },
                    onSelectRecipe = { r -> selectedRecipe = r; navController.navigate("recipe_detail/${r.id}") },
                    onCreateClick = { navController.navigate(Screen.CreateRecipe.route) }
                )
                }
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
                Box(Modifier.statusBarsPadding()) {
                CreateRecipeScreen(
                    existingRecipe = existing,
                    onSave = { recipe ->
                        scope.launch { storage.saveRecipe(recipe); recipes = storage.getAllRecipes(); stats = stats.ignite().also { storage.saveStats(it) } }
                        navController.navigate(Screen.Library.route) { popUpTo(Screen.Dashboard.route) }
                    },
                    onCancel = { navController.popBackStack() }
                )
                }
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
                                    // Save cooking records — use back-fill date if set in batch_notes
                                    val now = System.currentTimeMillis()
                                    val cookedAt = batch.batch_notes?.toLongOrNull() ?: now
                                    batch.recipeIds.forEach { rid ->
                                        recipes.find { it.id == rid }?.let { r ->
                                            storage.saveCookingRecord(CookingRecord(
                                                recipeId = r.id,
                                                recipeName = r.name,
                                                coverImage = r.cover_image,
                                                cookedAt = cookedAt,
                                                tags = r.tags,
                                                durationMins = r.timeline.sumOf { it.duration } / 60
                                            ))
                                        }
                                    }
                                    cookingRecords = storage.getCookingRecords()
                                    stats = stats.ignite().also { storage.saveStats(it) }
                                    // Capture batch recipe objects for thermal receipt
                                    feedbackBatchRecipes = batch.recipeIds.mapNotNull { rid -> recipes.find { it.id == rid } }
                                    feedbackReceiptDate = cookedAt
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
                                // Don't remove from recipeIds — just mark completed to grey out
                                val doneIds = b.completedRecipeIds + rid
                                val u = b.copy(completedRecipeIds = doneIds)
                                storage.saveActiveBatch(u); activeBatch = u
                            }
                        },
                        onClose = { navController.popBackStack() },
                        onOpenSOP = { r -> selectedRecipe = r; navController.navigate("execution/${r.id}/false") }
                    )
                } ?: run { Box(Modifier.fillMaxSize().background(Color.Transparent)) }
            }

            composable(Screen.Picking.route, arguments = listOf(navArgument("recipeId") { type = NavType.StringType }, navArgument("isLazy") { type = NavType.BoolType })) { entry ->
                val rid = entry.arguments?.getString("recipeId") ?: ""
                val lazy = entry.arguments?.getBoolean("isLazy") ?: false
                recipes.find { it.id == rid }?.let { recipe ->
                    PickingScreen(recipe = recipe, onConfirm = { navController.navigate("execution/${recipe.id}/$lazy") { popUpTo(Screen.Picking.route) { inclusive = true } } }, onCancel = { navController.popBackStack() })
                }
            }

            composable(Screen.Execution.route, arguments = listOf(navArgument("recipeId") { type = NavType.StringType }, navArgument("isLazy") { type = NavType.BoolType })) { entry ->
                Box(Modifier.statusBarsPadding()) {
                val rid = entry.arguments?.getString("recipeId") ?: ""
                val lazy = entry.arguments?.getBoolean("isLazy") ?: false
                recipes.find { it.id == rid }?.let { recipe ->
                    ExecutionScreen(recipe = recipe, isLazy = lazy, onComplete = { r ->
                        scope.launch {
                            val u = r.copy(last_cooked_at = System.currentTimeMillis().toString(), cooked_count = r.cooked_count + 1)
                            storage.saveRecipe(u); recipes = recipes.map { if (it.id == u.id) u else it }
                            // Use back-fill date from batch_notes if available
                            val cookedAt = activeBatch?.batch_notes?.toLongOrNull() ?: System.currentTimeMillis()
                            storage.saveCookingRecord(CookingRecord(
                                recipeId = r.id,
                                recipeName = r.name,
                                coverImage = r.cover_image,
                                cookedAt = cookedAt,
                                tags = r.tags,
                                durationMins = r.timeline.sumOf { it.duration } / 60
                            ))
                            cookingRecords = storage.getCookingRecords()
                            feedbackBatchRecipes = emptyList()
                            feedbackReceiptDate = cookedAt
                        }
                        navController.navigate("feedback/${recipe.id}") { popUpTo(Screen.Execution.route) { inclusive = true } }
                    })
                }
                }
            }

            composable(Screen.Feedback.route, arguments = listOf(navArgument("recipeId") { type = NavType.StringType })) { entry ->
                Box(Modifier.statusBarsPadding()) {
                val rid = entry.arguments?.getString("recipeId") ?: ""
                recipes.find { it.id == rid }?.let { recipe ->
                    FeedbackScreen(recipe = recipe, batchRecipes = feedbackBatchRecipes, receiptDate = feedbackReceiptDate ?: activeBatch?.batch_notes?.toLongOrNull(), onDone = {
                        scope.launch { stats = stats.ignite().also { storage.saveStats(it) } }
                        navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Dashboard.route) { inclusive = true } }
                    })
                }
                }
            }

            composable(Screen.Analytics.route) { key(analyticsKey) {
                Box(Modifier.statusBarsPadding()) {
                AnalyticsScreen(
                    recipes = recipes,
                    cookingRecords = cookingRecords,
                    onNavigateToLibrary = { navController.navigate(Screen.Library.route) },
                    onViewAllRanking = { navController.navigate(Screen.WokHeatRanking.route) },
                    onSelectRecipe = { r -> selectedRecipe = r; navController.navigate("recipe_detail/${r.id}") },
                    onShareReceipt = { dayRecipes ->
                        feedbackBatchRecipes = dayRecipes
                        feedbackReceiptDate = null
                        if (dayRecipes.isNotEmpty()) {
                            navController.navigate("feedback/${dayRecipes.first().id}")
                        }
                    },
                    onDeleteRecord = { recordId ->
                        scope.launch {
                            val record = cookingRecords.find { it.id == recordId }
                            record?.let { rec ->
                                recipes.find { it.id == rec.recipeId }?.let { recipe ->
                                    val updated = recipe.copy(cooked_count = (recipe.cooked_count - 1).coerceAtLeast(0))
                                    storage.saveRecipe(updated)
                                    recipes = recipes.map { if (it.id == updated.id) updated else it }
                                }
                            }
                            storage.deleteCookingRecord(recordId)
                            cookingRecords = storage.getCookingRecords()
                            analyticsKey++
                        }
                    },
                    onCreateRecord = { targetDateMs ->
                        scope.launch {
                            val batchId = System.currentTimeMillis().toString()
                            val batch = OrderBatch(
                                id = batchId,
                                status = BatchStatus.Picking,
                                recipeIds = emptyList(),
                                completedRecipeIds = emptyList(),
                                created_at = System.currentTimeMillis().toString(),
                                batch_notes = targetDateMs.toString()
                            )
                            storage.saveActiveBatch(batch)
                            activeBatch = batch
                            navController.navigate(Screen.OrderEngine.route)
                        }
                    },
                    onMonthChanged = { year, month ->
                        analyticsYear = year
                        analyticsMonth = month
                    }
                )
                }
            } }

            composable(Screen.WokHeatRanking.route) {
                Box(Modifier.statusBarsPadding()) {
                WokHeatRankingScreen(
                    recipes = recipes,
                    cookingRecords = cookingRecords,
                    selectedYear = analyticsYear,
                    selectedMonth = analyticsMonth,
                    onBack = { navController.popBackStack() }
                )
                }
            }

            composable(Screen.FeaturedAll.route) {
                Box(Modifier.statusBarsPadding()) {
                FeaturedAllScreen(recipes = recipes, onBack = { navController.popBackStack() }, onSelectRecipe = { r -> selectedRecipe = r; navController.navigate("recipe_detail/${r.id}") })
                }
            }

            composable(Screen.TastingAll.route) {
                Box(Modifier.statusBarsPadding()) {
                TastingNotesListScreen(tastingNotes = tastingNotes, onBack = { navController.popBackStack() }, onSelectTasting = { id -> navController.navigate("tasting_detail/$id") })
                }
            }

            composable(Screen.CreateTasting.route) { backStackEntry ->
                Box(Modifier.statusBarsPadding()) {
                val editId = backStackEntry.arguments?.getString("editId")
                CreateTastingScreen(
                    existingNote = tastingNotes.find { it.id == editId },
                    onBack = { navController.popBackStack() },
                    onSaved = {
                        scope.launch { tastingNotes = storage.getTastingNotes() }
                        navController.popBackStack()
                    }
                )
                }
            }

            composable(Screen.TastingDetail.route) { backStackEntry ->
                val tastingId = backStackEntry.arguments?.getString("tastingId") ?: ""
                val note = tastingNotes.find { it.id == tastingId }
                if (note != null) {
                    Box(Modifier.statusBarsPadding()) {
                    TastingDetailScreen(
                        note = note,
                        onBack = { navController.popBackStack() },
                        onDelete = {
                            scope.launch {
                                storage.deleteTastingNote(tastingId)
                                tastingNotes = storage.getTastingNotes()
                            }
                            navController.popBackStack()
                        },
                        onTurnToRecipe = { rid ->
                            scope.launch { tastingNotes = storage.getTastingNotes() }
                            navController.navigate("create_recipe?editId=$rid") { popUpTo(Screen.TastingDetail.route) { inclusive = true } }
                        }
                    )
                }
                }
            }

            composable(Screen.IngredientLibrary.route) {
                IngredientLibraryScreen(onBack = { navController.popBackStack() })
            }

            composable(Screen.Settings.route) {
                Box(Modifier.statusBarsPadding()) {
                SettingsScreen(
                    onNavigateToLibrary = { navController.navigate(Screen.Library.route) },
                    onNavigateToAnalytics = { analyticsKey++; navController.navigate(Screen.Analytics.route) },
                    onDataChanged = {
                        scope.launch {
                            recipes = storage.getAllRecipes()
                            cookingRecords = storage.getCookingRecords()
                            tastingNotes = storage.getTastingNotes()
                        }
                    },
                    onNavigateToTasting = { navController.navigate("create_tasting?editId=${System.currentTimeMillis()}") },
                    onNavigateToIngredients = { navController.navigate(Screen.IngredientLibrary.route) }
                )
                }
            }
        }
    }
}
}
