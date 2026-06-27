package com.example.solochef.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * 食材库分类 — 对应 MD 文件中的"商品大类"
 * 如: 猪肉、牛肉、叶菜类、调味品 等
 */
@Immutable
@Serializable
data class IngredientCategory(
    val id: String,
    val name: String,          // 商品大类名称
    val targetCategory: String, // 目标分类: meat/vegetable/seasoning
    val emoji: String = DEFAULT_EMOJI,
    val ingredients: List<IngredientItem> = emptyList(),
    val isCustom: Boolean = false  // 用户自定义分类
) {
    companion object {
        const val DEFAULT_EMOJI = "📦"
        fun targetLabel(cat: String) = when (cat) {
            "meat" -> "肉禽 / 水产"
            "vegetable" -> "蔬菜 / 蔬果"
            "seasoning" -> "调味 / 其他"
            "favorite" -> "常用食材"
            else -> cat
        }
    }
}

/**
 * 食材项 — 归属于某个商品大类
 */
@Immutable
@Serializable
data class IngredientItem(
    val id: String,
    val name: String,
    val categoryId: String,     // 所属商品大类 ID
    val isStandard: Boolean = true // true=标准库内置, false=用户自定义
)

/** 食材库完整数据 */
@Immutable
@Serializable
data class IngredientLibrary(
    val categories: List<IngredientCategory> = emptyList(),
    val version: Int = 1
) {
    /** 根据食材名称查找所属商品大类的 emoji */
    fun emojiFor(itemName: String): String {
        for (cat in categories) {
            for (ing in cat.ingredients) {
                if (ing.name == itemName) return cat.emoji
            }
        }
        // fallback: try matching first 2 chars
        val short = itemName.take(2)
        for (cat in categories) {
            for (ing in cat.ingredients) {
                if (ing.name.take(2) == short) return cat.emoji
            }
        }
        return "🥬" // default vegetable
    }
}
