import React, { useState } from 'react';
import { Recipe } from '../types';
import { ChevronRight, Plus, Search, Filter, Sparkles } from 'lucide-react';
import { motion } from 'motion/react';

interface LibraryViewProps {
  recipes: Recipe[];
  onSelect: (recipe: Recipe) => void;
  onCreateClick: () => void;
}

const COOKING_PROCESS_TAGS = ['清蒸', '爆炒', '慢炖', '油炸', '煎烤', '凉拌', '红烧', '白灼'];
const CUISINE_TAGS = ['川菜', '粤菜', '湘菜', '鲁菜', '闽菜', '苏菜', '浙菜', '徽菜', '本帮菜', '东北菜', '西北菜', '云贵菜', '客家菜', '西餐', '日料', '韩料', '东南亚菜', '融合创新', '街头小吃', '深夜食堂', '减脂轻食'];
const ENERGY_LEVEL_TAGS = [
  { value: 'Low', label: '极简' },
  { value: 'Mid', label: '正常' },
  { value: 'High', label: '满满' }
] as const;

export default function LibraryView({ recipes, onSelect, onCreateClick }: LibraryViewProps) {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedTags, setSelectedTags] = useState<string[]>([]);
  const [selectedEnergyLevels, setSelectedEnergyLevels] = useState<string[]>([]);
  const [showFilters, setShowFilters] = useState(false);

  const toggleTag = (tag: string) => {
    if (selectedTags.includes(tag)) {
      setSelectedTags(selectedTags.filter(t => t !== tag));
    } else {
      setSelectedTags([...selectedTags, tag]);
    }
  };

  const clearProcessTags = () => {
    setSelectedTags(selectedTags.filter(t => !COOKING_PROCESS_TAGS.includes(t)));
  };

  const clearCuisineTags = () => {
    setSelectedTags(selectedTags.filter(t => !CUISINE_TAGS.includes(t)));
  };

  const toggleEnergyLevel = (level: string) => {
    if (selectedEnergyLevels.includes(level)) {
      setSelectedEnergyLevels(selectedEnergyLevels.filter(l => l !== level));
    } else {
      setSelectedEnergyLevels([...selectedEnergyLevels, level]);
    }
  };

  const filteredRecipes = recipes.filter(recipe => {
    const matchesSearch = recipe.name.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesTags = selectedTags.length === 0 || selectedTags.every(tag => recipe.tags?.includes(tag));
    const matchesEnergy = selectedEnergyLevels.length === 0 || selectedEnergyLevels.includes(recipe.energy_level);
    return matchesSearch && matchesTags && matchesEnergy;
  });

  return (
    <div className="min-h-screen bg-sage-100 text-sage-900 p-6 flex flex-col pb-44">
      <header className="mb-8 flex justify-between items-end">
        <div>
          <h1 className="text-3xl font-black tracking-tighter text-sage-900 uppercase">菜谱库</h1>
          <p className="text-sage-500 font-mono text-xs mt-1 uppercase tracking-widest">TOTAL_RECORDS: {filteredRecipes.length}</p>
        </div>
      </header>

      <div className="space-y-4 mb-8">
        <div className="relative">
          <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-sage-500" size={18} />
          <input 
            type="text" 
            placeholder="搜索菜谱..." 
            value={searchQuery}
            onChange={e => setSearchQuery(e.target.value)}
            className="w-full bg-white border border-sage-200 rounded-2xl py-4 pl-12 pr-12 text-sm font-bold text-sage-900 focus:outline-none focus:border-sage-400 transition-colors shadow-sm placeholder:text-sage-300"
          />
          <button 
            onClick={() => setShowFilters(!showFilters)}
            className={`absolute right-4 top-1/2 -translate-y-1/2 p-2 rounded-xl transition-colors ${showFilters ? 'bg-sage-800 text-white' : 'text-sage-500 hover:bg-sage-50'}`}
          >
            <Filter size={18} />
          </button>
        </div>

        {showFilters && (
          <div className="bg-white border border-sage-200 rounded-[32px] p-6 shadow-sm space-y-6 animate-in fade-in slide-in-from-top-4 duration-300">
            <div>
              <span className="text-[10px] font-black text-sage-400 uppercase tracking-widest mb-3 block">烹饪工艺</span>
              <div className="flex flex-wrap gap-2">
                <button
                  onClick={clearProcessTags}
                  className={`px-3 py-1.5 rounded-full text-[10px] font-bold transition-all ${
                    !selectedTags.some(t => COOKING_PROCESS_TAGS.includes(t))
                      ? 'bg-sage-800 text-white' 
                      : 'bg-sage-50 text-sage-500 hover:bg-sage-100'
                  }`}
                >
                  全部
                </button>
                {COOKING_PROCESS_TAGS.map(tag => (
                  <button
                    key={tag}
                    onClick={() => toggleTag(tag)}
                    className={`px-3 py-1.5 rounded-full text-[10px] font-bold transition-all ${
                      selectedTags.includes(tag) 
                        ? 'bg-sage-800 text-white' 
                        : 'bg-sage-50 text-sage-500 hover:bg-sage-100'
                    }`}
                  >
                    {tag}
                  </button>
                ))}
              </div>
            </div>

            <div>
              <span className="text-[10px] font-black text-sage-400 uppercase tracking-widest mb-3 block">菜系维度</span>
              <div className="flex flex-wrap gap-2">
                <button
                  onClick={clearCuisineTags}
                  className={`px-3 py-1.5 rounded-full text-[10px] font-bold transition-all ${
                    !selectedTags.some(t => CUISINE_TAGS.includes(t))
                      ? 'bg-sage-800 text-white' 
                      : 'bg-sage-50 text-sage-500 hover:bg-sage-100'
                  }`}
                >
                  全部
                </button>
                {CUISINE_TAGS.map(tag => (
                  <button
                    key={tag}
                    onClick={() => toggleTag(tag)}
                    className={`px-3 py-1.5 rounded-full text-[10px] font-bold transition-all ${
                      selectedTags.includes(tag) 
                        ? 'bg-sage-800 text-white' 
                        : 'bg-sage-50 text-sage-500 hover:bg-sage-100'
                    }`}
                  >
                    {tag}
                  </button>
                ))}
              </div>
            </div>

            <div>
              <span className="text-[10px] font-black text-sage-400 uppercase tracking-widest mb-3 block">精力等级</span>
              <div className="flex flex-wrap gap-2">
                <button
                  onClick={() => setSelectedEnergyLevels([])}
                  className={`px-3 py-1.5 rounded-full text-[10px] font-bold transition-all ${
                    selectedEnergyLevels.length === 0
                      ? 'bg-sage-800 text-white' 
                      : 'bg-sage-50 text-sage-500 hover:bg-sage-100'
                  }`}
                >
                  全部
                </button>
                {ENERGY_LEVEL_TAGS.map(tag => (
                  <button
                    key={tag.value}
                    onClick={() => toggleEnergyLevel(tag.value)}
                    className={`px-3 py-1.5 rounded-full text-[10px] font-bold transition-all ${
                      selectedEnergyLevels.includes(tag.value) 
                        ? 'bg-sage-800 text-white' 
                        : 'bg-sage-50 text-sage-500 hover:bg-sage-100'
                    }`}
                  >
                    {tag.label}
                  </button>
                ))}
              </div>
            </div>

            {(selectedTags.length > 0 || selectedEnergyLevels.length > 0) && (
              <button 
                onClick={() => { setSelectedTags([]); setSelectedEnergyLevels([]); }}
                className="text-[10px] font-black text-red-500 uppercase tracking-widest pt-2 border-t border-sage-100 w-full text-center"
              >
                清除所有过滤
              </button>
            )}
          </div>
        )}
      </div>

      <div className="flex-1">
        {filteredRecipes.length === 0 ? (
          <div className="bg-white border-2 border-sage-800 rounded-[32px] p-6 flex items-center group shadow-[0_15px_40px_rgba(0,0,0,0.04)] border-dashed min-h-[120px]">
            <div className="shrink-0 flex items-center justify-center p-4">
              <div className="w-12 h-12 bg-sage-50 rounded-2xl flex items-center justify-center text-sage-300">
                <Search size={24} />
              </div>
            </div>
            <div className="flex-1 text-center px-2">
              <h2 className="text-sm font-black text-sage-900 tracking-tight leading-tight">暂无相关菜谱</h2>
              <p className="text-[10px] text-sage-400 mt-1 font-medium">去新建一个展示你的厨艺吧</p>
            </div>
            <button 
              onClick={onCreateClick}
              className="shrink-0 bg-sage-900 text-white px-4 py-2.5 rounded-full text-[9px] font-black uppercase tracking-widest flex items-center gap-1 active:scale-95 transition-all"
            >
              新建 <Plus size={10} />
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-2 gap-4">
            {filteredRecipes.map(recipe => (
              <motion.button
                key={recipe.id}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                onClick={() => onSelect(recipe)}
                className="flex flex-col text-left group active:scale-[0.98] transition-all"
              >
                <div className="aspect-square bg-white border border-sage-200 rounded-[28px] overflow-hidden shadow-sm relative mb-3">
                  <img 
                    src={recipe.cover_image} 
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-700" 
                    alt={recipe.name} 
                  />
                  {/* Time Overlay */}
                  <div className="absolute bottom-2 left-2 bg-black/40 backdrop-blur-md px-2 py-1 rounded-lg">
                    <span className="text-[8px] font-black text-white uppercase">{recipe.energy_level === 'High' ? '15m' : recipe.energy_level === 'Mid' ? '10m' : '5m'}</span>
                  </div>
                </div>
                
                <div className="px-1 space-y-1.5">
                  <div className="flex items-center justify-between gap-1">
                    <h3 className="text-sm font-black text-sage-900 truncate tracking-tight">{recipe.name}</h3>
                    {recipe.is_featured && (
                      <div className="bg-amber-400 w-5 h-5 rounded-full flex items-center justify-center shrink-0">
                        <span className="text-[8px] font-black text-sage-900 italic">荐</span>
                      </div>
                    )}
                  </div>
                  
                  <div className="flex flex-wrap gap-1">
                    {recipe.tags?.slice(0, 2).map(tag => (
                      <span key={tag} className="bg-gray-100 text-[8px] font-black text-gray-500 px-1.5 py-0.5 rounded-full uppercase tracking-tighter">
                        {tag}
                      </span>
                    ))}
                  </div>

                  <div className="flex items-center gap-2">
                    <span className="text-[9px] font-bold text-sage-400 uppercase tracking-widest">{recipe.timeline.length} STEPS</span>
                    <div className="w-1 h-1 bg-sage-200 rounded-full" />
                    <span className="text-[9px] font-bold text-sage-400 uppercase tracking-widest">
                      {recipe.energy_level === 'High' ? '困难' : recipe.energy_level === 'Mid' ? '一般' : '容易'}
                    </span>
                  </div>
                </div>
              </motion.button>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
