package com.example.solochef.ui.screens.library

import com.example.solochef.model.EnergyLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LibraryViewModel : androidx.lifecycle.ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    private val _selectedTags = MutableStateFlow<Set<String>>(emptySet())
    val selectedTags: StateFlow<Set<String>> = _selectedTags.asStateFlow()
    private val _selectedEnergyLevels = MutableStateFlow<Set<EnergyLevel>>(emptySet())
    val selectedEnergyLevels: StateFlow<Set<EnergyLevel>> = _selectedEnergyLevels.asStateFlow()
    private val _showFilters = MutableStateFlow(false)
    val showFilters: StateFlow<Boolean> = _showFilters.asStateFlow()

    fun setSearchQuery(q: String) { _searchQuery.value = q }
    fun toggleFilter() { _showFilters.value = !_showFilters.value }
    fun toggleTag(tag: String) { _selectedTags.value = if (tag in _selectedTags.value) _selectedTags.value - tag else _selectedTags.value + tag }
    fun toggleEnergyLevel(l: EnergyLevel) { _selectedEnergyLevels.value = if (l in _selectedEnergyLevels.value) _selectedEnergyLevels.value - l else _selectedEnergyLevels.value + l }
    fun clearAllFilters() { _selectedTags.value = emptySet(); _selectedEnergyLevels.value = emptySet() }
    fun clearProcessTags(tags: List<String>) { _selectedTags.value = _selectedTags.value - tags.toSet() }
    fun clearCuisineTags(tags: List<String>) { _selectedTags.value = _selectedTags.value - tags.toSet() }
}

// Tag constants
val COOKING_PROCESS_TAGS = listOf("清蒸", "爆炒", "慢炖", "油炸", "煎烤", "凉拌", "红烧", "白灼")
val CUISINE_TAGS = listOf("川菜", "粤菜", "湘菜", "鲁菜", "闽菜", "苏菜", "浙菜", "徽菜", "本帮菜", "东北菜", "西北菜", "云贵菜", "客家菜", "西餐", "日料", "韩料", "东南亚菜", "融合创新", "街头小吃", "深夜食堂", "减脂轻食")
val ENERGY_LEVEL_OPTIONS = listOf(EnergyLevel.Low to "极简", EnergyLevel.Mid to "正常", EnergyLevel.High to "满满")
