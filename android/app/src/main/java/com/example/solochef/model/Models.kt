package com.example.solochef.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

// ─── Enums ────────────────────────────────────────────

enum class EnergyLevel { High, Mid, Low }

enum class MaterialCategory(val label: String) {
    Meat("肉禽 / 水产"),
    Vegetable("蔬菜 / 蔬果"),
    Seasoning("调味 / 其他")
}

enum class BatchStatus { Picking, Processing, Finished }

// ─── Core Data Classes ────────────────────────────────

@Immutable
@Serializable
data class SubTask(
    val content: String = "",
    val duration: Int = 60  // seconds
)

@Immutable
@Serializable
data class TimelineStep(
    val step_id: Long = System.currentTimeMillis(),
    val type: String = "active",  // "active" | "waiting"
    val content: String = "",
    val duration: Int = 60,       // seconds
    val is_parallel: Boolean = false,
    val sub_tasks: List<SubTask> = emptyList(),
    val images: List<String> = emptyList()  // base64 or file paths
)

@Immutable
@Serializable
data class Material(
    val item: String = "",
    val amount: String = "",
    val unit: String = "g",
    val is_essential: Boolean = true,
    val image: String? = null
)

@Immutable
@Serializable
data class Recipe(
    val id: String = System.currentTimeMillis().toString(),
    val name: String = "",
    val cover_image: String = "",       // base64 or file path
    val bom_snapshot: String? = null,   // BOM group photo
    val energy_level: EnergyLevel = EnergyLevel.Mid,
    val is_featured: Boolean = false,
    val cost: Double = 0.0,
    val price: Double = 0.0,
    val last_cooked_at: String? = null,
    val cooked_count: Int = 0,              // 锅气值：累计完成烹饪次数
    val materials: Map<String, List<Material>> = mapOf(
        "meat" to emptyList(),
        "vegetable" to emptyList(),
        "seasoning" to emptyList()
    ),
    val timeline: List<TimelineStep> = emptyList(),
    val tags: List<String> = emptyList(),
    val description: String = "",
    val updated_at: String? = null
)

@Immutable
@Serializable
data class UserStats(
    val streak: Int = 0,
    val last_ignition: String = System.currentTimeMillis().toString()
) {
    // 48h reset, 16h cooldown for increment
    fun ignite(): UserStats {
        val now = System.currentTimeMillis()
        val last = last_ignition.toLongOrNull() ?: 0L
        val diffHours = (now - last) / (1000.0 * 60 * 60)

        val newStreak = when {
            diffHours >= 48 -> 1
            diffHours >= 16 -> streak + 1
            else -> streak
        }
        return copy(streak = newStreak, last_ignition = now.toString())
    }
}

@Immutable
@Serializable
data class CookingRecord(
    val id: String = System.currentTimeMillis().toString(),
    val recipeId: String = "",
    val recipeName: String = "",
    val coverImage: String = "",
    val cookedAt: Long = System.currentTimeMillis(),
    val tags: List<String> = emptyList(),
    val durationMins: Int = 0
)

@Immutable
@Serializable
data class CookingHistoryEntry(
    val id: String = System.currentTimeMillis().toString(),
    val recipeId: String = "",
    val recipeName: String = "",
    val recipeImage: String = "",
    val orderId: String = "",
    val finishedAt: String = "",
    val durationMins: Int = 0,
    val batchInfo: String = "",
    val tag: String? = null,
    val materials: Map<String, List<Material>>? = null
)

@Immutable
@Serializable
data class OrderBatch(
    val id: String = System.currentTimeMillis().toString(),
    val status: BatchStatus = BatchStatus.Picking,
    val recipeIds: List<String> = emptyList(),
    val completedRecipeIds: List<String> = emptyList(),
    val created_at: String = "",
    val picked_at: String? = null,
    val finished_at: String? = null,
    val batch_notes: String? = null
)

// ─── 拾味手记 ──────────────────────────────────────

@Immutable
@Serializable
data class TastingNote(
    val id: String = System.currentTimeMillis().toString(),
    val coverImage: String = "",       // file path or empty
    val url: String = "",              // 菜谱链接地址
    val rating: Float = 0f,            // 拾味等级 0-5, 支持半星
    val note: String = "",             // 备注
    val createdAt: Long = System.currentTimeMillis()
)

// ─── Export/Import Payload ────────────────────────────

@Serializable
data class CustomTag(
    val id: String,
    val name: String
)

@Serializable
data class CustomRecipeTags(
    val cookingProcessTags: List<CustomTag> = emptyList(),
    val cuisineTags: List<CustomTag> = emptyList()
)

@Serializable
data class BackupPayload(
    val recipes: List<Recipe>,
    val history: List<CookingHistoryEntry>,
    val stats: UserStats,
    val version: String = "1.0",
    val exportedAt: String,
    val ingredient_library: IngredientLibrary? = null,
    val custom_recipe_tags: CustomRecipeTags? = null
)
