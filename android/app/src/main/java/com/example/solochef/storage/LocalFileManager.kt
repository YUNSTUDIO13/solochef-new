package com.example.solochef.storage

import android.content.Context
import com.example.solochef.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * LocalFileManager — Markdown-based recipe storage.
 *
 * Architecture:
 * - Each Recipe stored as {id}.md in filesDir/recipes/
 * - Metadata index stored as _index.json for fast loading
 * - Backup export writes a single SoloChef_Backup_{date}.md with embedded JSON payload
 * - Import parses any .md with SOLOCHEF_DATA_START/END markers
 */
class LocalFileManager(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        isLenient = true
        encodeDefaults = false
    }

    private val recipesDir: File
        get() = File(context.filesDir, "recipes").also { it.mkdirs() }

    private val imagesDir: File
        get() = File(context.filesDir, "images").also { it.mkdirs() }

    private val dataDir: File
        get() = context.filesDir

    companion object {
        const val PAYLOAD_START = "<!-- SOLOCHEF_DATA_START"
        const val PAYLOAD_END = "SOLOCHEF_DATA_END -->"
    }

    // ═══════════════════════════════════════════════════
    //  Recipe CRUD
    // ═══════════════════════════════════════════════════

    suspend fun getAllRecipes(): List<Recipe> = withContext(Dispatchers.IO) {
        recipesDir.listFiles()
            ?.filter { it.extension == "md" && !it.name.startsWith("_") }
            ?.mapNotNull { parseRecipeFile(it) }
            ?.sortedByDescending { it.updated_at ?: "0" }
            ?: emptyList()
    }

    suspend fun getRecipe(id: String): Recipe? = withContext(Dispatchers.IO) {
        val file = File(recipesDir, "$id.md")
        if (file.exists()) parseRecipeFile(file) else null
    }

    suspend fun saveRecipe(recipe: Recipe): Unit = withContext(Dispatchers.IO) {
        val updated = recipe.copy(updated_at = System.currentTimeMillis().toString())
        val md = recipeToMarkdown(updated)
        File(recipesDir, "${updated.id}.md").writeText(md)
    }

    suspend fun deleteRecipe(id: String): Unit = withContext(Dispatchers.IO) {
        File(recipesDir, "$id.md").delete()
    }

    // ═══════════════════════════════════════════════════
    //  Stats & History (stored as JSON in filesDir)
    // ═══════════════════════════════════════════════════

    suspend fun getStats(): UserStats = withContext(Dispatchers.IO) {
        val file = File(dataDir, "_stats.json")
        if (file.exists()) {
            try { json.decodeFromString<UserStats>(file.readText()) }
            catch (_: Exception) { UserStats() }
        } else UserStats()
    }

    suspend fun saveStats(stats: UserStats) = withContext(Dispatchers.IO) {
        File(dataDir, "_stats.json").writeText(json.encodeToString(stats))
    }

    suspend fun getHistory(): List<CookingHistoryEntry> = withContext(Dispatchers.IO) {
        val file = File(dataDir, "_history.json")
        if (file.exists()) {
            try { json.decodeFromString<List<CookingHistoryEntry>>(file.readText()) }
            catch (_: Exception) { emptyList() }
        } else emptyList()
    }

    suspend fun saveHistory(history: List<CookingHistoryEntry>) = withContext(Dispatchers.IO) {
        File(dataDir, "_history.json").writeText(json.encodeToString(history))
    }

    suspend fun getActiveBatch(): OrderBatch? = withContext(Dispatchers.IO) {
        val file = File(dataDir, "_batch.json")
        if (file.exists()) {
            try { json.decodeFromString<OrderBatch>(file.readText()) }
            catch (_: Exception) { null }
        } else null
    }

    suspend fun saveActiveBatch(batch: OrderBatch?) = withContext(Dispatchers.IO) {
        if (batch != null) {
            File(dataDir, "_batch.json").writeText(json.encodeToString(batch))
        } else {
            File(dataDir, "_batch.json").delete()
        }
    }

    // ═══════════════════════════════════════════════════
    //  Cooking Records (for calendar / 食光日历)
    // ═══════════════════════════════════════════════════

    suspend fun saveCookingRecord(record: CookingRecord) = withContext(Dispatchers.IO) {
        val records = getCookingRecordsInternal().toMutableList()
        records.add(record)
        File(dataDir, "_cooking_records.json").writeText(json.encodeToString(records))
    }

    suspend fun saveCookingRecords(records: List<CookingRecord>) = withContext(Dispatchers.IO) {
        File(dataDir, "_cooking_records.json").writeText(json.encodeToString(records))
    }

    suspend fun deleteCookingRecord(id: String) = withContext(Dispatchers.IO) {
        val records = getCookingRecordsInternal().toMutableList()
        records.removeAll { it.id == id }
        File(dataDir, "_cooking_records.json").writeText(json.encodeToString(records))
    }

    suspend fun getCookingRecords(): List<CookingRecord> = withContext(Dispatchers.IO) {
        getCookingRecordsInternal()
    }

    /**
     * Get cooking records within a date range (inclusive start, exclusive end).
     * startMs and endMs are epoch millis.
     */
    suspend fun getCookingRecordsInRange(startMs: Long, endMs: Long): List<CookingRecord> = withContext(Dispatchers.IO) {
        getCookingRecordsInternal().filter { it.cookedAt in startMs..<endMs }
    }

    private fun getCookingRecordsInternal(): List<CookingRecord> {
        val file = File(dataDir, "_cooking_records.json")
        return if (file.exists()) {
            try { json.decodeFromString<List<CookingRecord>>(file.readText()) }
            catch (_: Exception) { emptyList() }
        } else emptyList()
    }

    // ═══════════════════════════════════════════════════
    //  Tasting Notes (拾味手记)
    // ═══════════════════════════════════════════════════

    suspend fun getTastingNotes(): List<TastingNote> = withContext(Dispatchers.IO) {
        val file = File(dataDir, "_tasting_notes.json")
        if (file.exists()) {
            try { json.decodeFromString<List<TastingNote>>(file.readText()) }
            catch (_: Exception) { emptyList() }
        } else emptyList()
    }

    suspend fun saveTastingNote(note: TastingNote) = withContext(Dispatchers.IO) {
        val notes = getTastingNotesInternal().toMutableList()
        notes.add(note)
        File(dataDir, "_tasting_notes.json").writeText(json.encodeToString(notes))
    }

    suspend fun saveTastingNotes(notes: List<TastingNote>) = withContext(Dispatchers.IO) {
        File(dataDir, "_tasting_notes.json").writeText(json.encodeToString(notes))
    }

    suspend fun deleteTastingNote(id: String) = withContext(Dispatchers.IO) {
        val notes = getTastingNotesInternal().toMutableList()
        notes.removeAll { it.id == id }
        File(dataDir, "_tasting_notes.json").writeText(json.encodeToString(notes))
    }

    private fun getTastingNotesInternal(): List<TastingNote> {
        val file = File(dataDir, "_tasting_notes.json")
        return if (file.exists()) {
            try { json.decodeFromString<List<TastingNote>>(file.readText()) }
            catch (_: Exception) { emptyList() }
        } else emptyList()
    }

    // ═══════════════════════════════════════════════════
    //  Markdown Serialization
    // ═══════════════════════════════════════════════════

    private fun recipeToMarkdown(recipe: Recipe): String {
        val sb = StringBuilder()
        sb.appendLine("# ${recipe.name}")
        sb.appendLine()
        sb.appendLine("| 属性 | 值 |")
        sb.appendLine("|------|-----|")
        sb.appendLine("| ID | ${recipe.id} |")
        sb.appendLine("| 封面 | ${recipe.cover_image} |")
        if (recipe.bom_snapshot != null) sb.appendLine("| 食材全拼 | ${recipe.bom_snapshot} |")
        sb.appendLine("| 精力等级 | ${recipe.energy_level.name} |")
        sb.appendLine("| 主推菜 | ${if (recipe.is_featured) "是" else "否"} |")
        sb.appendLine("| 成本 | ¥%.2f |".format(recipe.cost))
        sb.appendLine("| 售价 | ¥%.2f |".format(recipe.price))
        sb.appendLine("| 最后烹饪 | ${recipe.last_cooked_at ?: "从未"} |")
        sb.appendLine("| 标签 | ${recipe.tags.joinToString(", ")} |")
        sb.appendLine()

        // Materials
        sb.appendLine("## 物料清单")
        recipe.materials.forEach { (cat, items) ->
            if (items.isNotEmpty()) {
                sb.appendLine("### ${catLabel(cat)}")
                items.forEach { m ->
                    val star = if (m.is_essential) " *" else ""
                    sb.appendLine("- [ ] ${m.item} (${m.amount}${m.unit})$star")
                }
                sb.appendLine()
            }
        }

        // Timeline
        sb.appendLine("## 烹饪步骤")
        recipe.timeline.forEachIndexed { i, step ->
            sb.appendLine("${i + 1}. **${step.type.uppercase()}**: ${step.content} (${step.duration}s)")
            step.sub_tasks?.forEach { sub ->
                sb.appendLine("   - ${sub.content} (${sub.duration}s)")
            }
        }

        // Embedded JSON payload for import
        val wrapper = RecipeWrapper(recipe)
        sb.appendLine()
        sb.appendLine("$PAYLOAD_START")
        sb.appendLine(json.encodeToString(wrapper))
        sb.appendLine(PAYLOAD_END)

        return sb.toString()
    }

    private fun parseRecipeFile(file: File): Recipe? {
        return try {
            val content = file.readText()
            val startIdx = content.indexOf(PAYLOAD_START)
            val endIdx = content.indexOf(PAYLOAD_END, startIdx)

            if (startIdx >= 0 && endIdx > startIdx) {
                val jsonStr = content.substring(
                    content.indexOf('\n', startIdx) + 1,
                    content.lastIndexOf('\n', endIdx)
                ).trim()
                val wrapper = json.decodeFromString<RecipeWrapper>(jsonStr)
                wrapper.recipe
            } else {
                // Legacy: try full JSON
                json.decodeFromString<Recipe>(content)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ═══════════════════════════════════════════════════
    //  Export / Import (full backup)
    // ═══════════════════════════════════════════════════

    suspend fun exportToMarkdown(): String = withContext(Dispatchers.IO) {
        val recipes = getAllRecipes()
        val history = getHistory()
        val stats = getStats()

        val sb = StringBuilder()
        sb.appendLine("# SoloChef Data Backup")
        sb.appendLine("Generated at: ${System.currentTimeMillis()}")
        sb.appendLine()

        sb.appendLine("## USER STATS")
        sb.appendLine("- Streak: ${stats.streak} days")
        sb.appendLine("- Last Ignition: ${stats.last_ignition}")
        sb.appendLine()

        sb.appendLine("## RECIPES (${recipes.size})")
        sb.appendLine()
        recipes.forEach { r ->
            sb.appendLine("### ${r.name}")
            sb.appendLine("- ID: ${r.id}")
            sb.appendLine("- Energy Level: ${r.energy_level}")
            sb.appendLine("- Tags: ${r.tags.joinToString(", ")}")
            sb.appendLine("- Last Cooked: ${r.last_cooked_at ?: "Never"}")
            sb.appendLine()
        }

        sb.appendLine("## COOKING HISTORY (${history.size})")
        sb.appendLine()
        history.forEach { h ->
            sb.appendLine("- **${h.recipeName}** | Duration: ${h.durationMins}min | Batch: ${h.batchInfo}")
        }

        // Embed full data
        val payload = BackupPayload(
            recipes = recipes,
            history = history,
            stats = stats,
            exportedAt = System.currentTimeMillis().toString(),
            ingredient_library = getIngredientLibrary()
        )
        sb.appendLine()
        sb.appendLine("$PAYLOAD_START")
        sb.appendLine(json.encodeToString(payload))
        sb.appendLine(PAYLOAD_END)

        sb.toString()
    }

    suspend fun importFromMarkdown(content: String): BackupPayload? = withContext(Dispatchers.IO) {
        val startIdx = content.indexOf(PAYLOAD_START)
        val endIdx = content.indexOf(PAYLOAD_END, startIdx)
        if (startIdx < 0 || endIdx <= startIdx) return@withContext null

        val jsonStr = content.substring(
            content.indexOf('\n', startIdx) + 1,
            content.lastIndexOf('\n', endIdx)
        ).trim()

        try { json.decodeFromString<BackupPayload>(jsonStr) }
        catch (_: Exception) { null }
    }

    suspend fun mergeImport(payload: BackupPayload): Int = withContext(Dispatchers.IO) {
        var count = 0
        // Merge recipes
        val existing = getAllRecipes().associateBy { it.id }
        payload.recipes.forEach { ir ->
            val ex = existing[ir.id]
            val shouldSave = when {
                ex == null -> true
                ir.updated_at != null && ex.updated_at == null -> true
                ir.updated_at != null && ex.updated_at != null ->
                    ir.updated_at.toLongOrNull() ?: 0 > (ex.updated_at?.toLongOrNull() ?: 0)
                else -> false
            }
            if (shouldSave) {
                saveRecipe(ir)
                count++
            }
        }
        // Merge history
        val hist = getHistory().toMutableList()
        val histIds = hist.map { it.id }.toSet()
        payload.history.forEach { h ->
            if (h.id !in histIds) {
                hist.add(h)
            }
        }
        saveHistory(hist)
        // Stats (take best streak)
        val currentStats = getStats()
        if (payload.stats.streak >= currentStats.streak) {
            saveStats(payload.stats)
        }
        // Merge ingredient library
        payload.ingredient_library?.let { imported ->
            val current = getIngredientLibrary()
            val existingNamesById = current.categories.associate { cat ->
                cat.id to cat.ingredients.map { it.name }.toSet()
            }
            var merged = current
            imported.categories.forEach { impCat ->
                val target = merged.categories.find { it.id == impCat.id }
                if (target != null) {
                    val existingNames = existingNamesById[impCat.id] ?: emptySet()
                    // 常用食材: merge all items; other categories: only merge non-standard (custom) items
                    val newItems = if (impCat.id == "favorites") {
                        impCat.ingredients.filter { it.name !in existingNames }
                    } else {
                        impCat.ingredients.filter { it.name !in existingNames && !it.isStandard }
                    }
                    if (newItems.isNotEmpty()) {
                        val idx = merged.categories.indexOf(target)
                        val updated = target.copy(ingredients = target.ingredients + newItems)
                        merged = merged.copy(categories = merged.categories.toMutableList().also { it[idx] = updated })
                    }
                } else {
                    merged = merged.copy(categories = merged.categories + impCat)
                }
            }
            // Ensure favorites always at top
            val favIdx = merged.categories.indexOfFirst { it.id == "favorites" }
            if (favIdx > 0) {
                merged = merged.copy(categories = listOf(merged.categories[favIdx]) + merged.categories.filterIndexed { i, _ -> i != favIdx })
            }
            if (merged != current) saveIngredientLibrary(merged)
        }
        count
    }

    // ─── Preferences ──────────────────────────────────

    suspend fun getLibraryName(): String = withContext(Dispatchers.IO) {
        val f = File(dataDir, "_library_name.json")
        if (f.exists()) {
            try {
                json.decodeFromString<String>(f.readText())
            } catch (_: Exception) { "菜谱库" }
        } else "菜谱库"
    }

    suspend fun saveLibraryName(name: String) = withContext(Dispatchers.IO) {
        File(dataDir, "_library_name.json").writeText(json.encodeToString(name))
    }

    // ─── Ingredient Library ────────────────────────────

    suspend fun getIngredientLibrary(): IngredientLibrary = withContext(Dispatchers.IO) {
        val f = File(dataDir, "_ingredient_library.json")
        if (f.exists()) {
            try { json.decodeFromString<IngredientLibrary>(f.readText()) }
            catch (_: Exception) { DefaultIngredients.load() }
        } else {
            val lib = DefaultIngredients.load()
            f.writeText(json.encodeToString(lib))
            lib
        }
    }

    suspend fun saveIngredientLibrary(lib: IngredientLibrary) = withContext(Dispatchers.IO) {
        File(dataDir, "_ingredient_library.json").writeText(json.encodeToString(lib))
    }

    // ─── Helpers ───────────────────────────────────────

    private fun catLabel(cat: String) = when (cat) {
        "meat" -> "肉禽 / 水产"
        "vegetable" -> "蔬菜 / 蔬果"
        "seasoning" -> "调味 / 其他"
        else -> cat
    }
}

@kotlinx.serialization.Serializable
private data class RecipeWrapper(val recipe: Recipe)
