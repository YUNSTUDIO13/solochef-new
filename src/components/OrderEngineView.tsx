import React, { useState } from 'react';
import { Recipe } from '../types';
import { X, ShoppingCart, Plus, Minus, ChevronRight, CheckCircle2 } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';

interface OrderEngineViewProps {
  recipes: Recipe[];
  onConfirm: (selectedRecipeIds: string[]) => void;
  onCancel: () => void;
}

const COOKING_PROCESS_TAGS = ['清蒸', '爆炒', '慢炖', '油炸', '煎烤', '凉拌', '红烧', '白灼'];
const CUISINE_TAGS = ['川菜', '粤菜', '湘菜', '鲁菜', '闽菜', '苏菜', '浙菜', '徽菜', '本帮菜', '东北菜', '西北菜', '云贵菜', '客家菜', '西餐', '日料', '韩料', '东南亚菜', '融合创新', '街头小吃', '深夜食堂', '减脂轻食'];
const ALL_ORDER = [...COOKING_PROCESS_TAGS, ...CUISINE_TAGS];

export default function OrderEngineView({ recipes, onConfirm, onCancel }: OrderEngineViewProps) {
  const [cart, setCart] = useState<string[]>([]);
  const [activeCategory, setActiveCategory] = useState<string>('all');

  const categories = ['all', '主推菜', ...COOKING_PROCESS_TAGS, ...CUISINE_TAGS];

  const getSortIndex = (recipe: Recipe) => {
    const matchingIndices = (recipe.tags || [])
      .map(t => ALL_ORDER.indexOf(t))
      .filter(idx => idx !== -1);
    return matchingIndices.length > 0 ? Math.min(...matchingIndices) : Infinity;
  };

  const filteredRecipes = recipes
    .filter(r => {
      if (activeCategory === 'all') return true;
      if (activeCategory === '主推菜') return r.is_featured;
      return r.tags?.includes(activeCategory);
    })
    .sort((a, b) => {
      if (activeCategory === '主推菜') {
        return a.name.localeCompare(b.name, 'zh-CN');
      }
      if (activeCategory === 'all') {
        const idxA = getSortIndex(a);
        const idxB = getSortIndex(b);
        if (idxA !== idxB) return idxA - idxB;
        return a.name.localeCompare(b.name, 'zh-CN');
      }
      return 0; // Default order for specific tag categories
    });

  const addToCart = (id: string) => {
    setCart([...cart, id]);
  };

  const removeFromCart = (id: string) => {
    const idx = cart.indexOf(id);
    if (idx > -1) {
      const newCart = [...cart];
      newCart.splice(idx, 1);
      setCart(newCart);
    }
  };

  const getCount = (id: string) => cart.filter(cid => cid === id).length;

  return (
    <div className="absolute inset-0 bg-white z-[80] flex flex-col no-scrollbar">
      <header className="absolute top-0 inset-x-0 bg-white/80 backdrop-blur-md h-[80px] border-b border-sage-100 flex items-center justify-between px-6 z-10">
        <div className="flex items-center gap-3">
          <button onClick={onCancel} className="p-2 -ml-2 text-sage-400 hover:text-sage-900 transition-colors">
            <X size={24} />
          </button>
          <h1 className="text-xl font-black tracking-tighter text-sage-900 uppercase">独厨点单</h1>
        </div>
        <div className="flex items-center gap-4">
           {cart.length > 0 && (
             <div className="bg-sage-900 text-white rounded-2xl px-4 py-2 flex items-center gap-2 shadow-lg">
                <ShoppingCart size={16} />
                <span className="text-xs font-black">{cart.length}</span>
             </div>
           )}
        </div>
      </header>

      {/* Main Content Area */}
      <div className="flex-1 flex overflow-hidden pt-[80px]">
        {/* Sidebar Categories - Precise 80px width as per PRD */}
        <div className="w-20 bg-sage-50 overflow-y-auto border-r border-sage-100 no-scrollbar shrink-0">
          {categories.map(cat => (
            <button
              key={cat}
              onClick={() => setActiveCategory(cat)}
              className={`w-full py-5 px-1 text-center transition-all ${
                activeCategory === cat ? 'bg-white border-l-4 border-sage-900' : 'text-sage-400'
              }`}
            >
              <span className={`text-[9px] font-black uppercase tracking-tighter break-words block ${
                activeCategory === cat ? 'text-sage-900' : ''
              }`}>{cat === 'all' ? "全部" : cat}</span>
            </button>
          ))}
        </div>

        {/* Recipe Cards List - Auto adaptive width */}
        <div className="flex-1 overflow-y-auto p-3 space-y-3 no-scrollbar pb-32">
          {filteredRecipes.map(recipe => (
            <div 
              key={recipe.id}
              className="bg-white border border-sage-200 rounded-[28px] p-2 pr-3 flex gap-3 shadow-sm active:scale-[0.98] transition-transform"
            >
              <div className="w-20 h-20 rounded-xl overflow-hidden shrink-0">
                <img src={recipe.cover_image} className="w-full h-full object-cover" alt={recipe.name} />
              </div>
              <div className="flex-1 flex flex-col justify-between py-0.5 min-w-0">
                <div>
                  <h3 className="text-sm font-black text-sage-900 leading-tight truncate">{recipe.name}</h3>
                  <div className="flex items-center gap-1.5 mt-0.5">
                    <span className={`w-1.5 h-1.5 rounded-full ${
                      recipe.energy_level === 'High' ? 'bg-red-400' : recipe.energy_level === 'Mid' ? 'bg-amber-400' : 'bg-emerald-400'
                    }`} />
                    <span className="text-[8px] font-bold text-sage-400 uppercase tracking-tighter">{recipe.energy_level}</span>
                  </div>
                </div>
                <div className="flex items-center justify-between mt-1">
                  <span className="text-[8px] font-black text-sage-400 uppercase tracking-widest shrink-0">~25 min</span>
                  <div className="flex items-center gap-2">
                    {getCount(recipe.id) > 0 ? (
                      <div className="flex items-center gap-2 bg-sage-50 rounded-full border border-sage-100 px-1 py-0.5">
                        <button 
                          onClick={() => removeFromCart(recipe.id)}
                          className="w-7 h-7 rounded-full bg-white border border-sage-200 flex items-center justify-center text-sage-800 shadow-sm active:scale-90"
                        >
                          <Minus size={14} />
                        </button>
                        <span className="text-xs font-black text-sage-900 w-4 text-center">{getCount(recipe.id)}</span>
                        <button 
                          onClick={() => addToCart(recipe.id)}
                          className="w-7 h-7 rounded-full bg-sage-900 text-white flex items-center justify-center shadow-sm active:scale-90"
                        >
                          <Plus size={14} />
                        </button>
                      </div>
                    ) : (
                      <button 
                        onClick={() => addToCart(recipe.id)}
                        className="h-8 px-3 rounded-xl bg-sage-900 text-white flex items-center gap-1 active:scale-95 shadow-md shadow-sage-900/10"
                      >
                        <Plus size={14} />
                        <span className="text-[10px] font-black uppercase">选购</span>
                      </button>
                    )}
                  </div>
                </div>
              </div>
            </div>
          ))}
          {filteredRecipes.length === 0 && (
            <div className="py-20 text-center text-sage-300 text-[10px] font-black uppercase tracking-widest">
              该分类下暂无菜谱
            </div>
          )}
        </div>
      </div>

      {/* Footer / Cart Summary */}
      <AnimatePresence>
        {cart.length > 0 && (
          <motion.div 
            initial={{ y: 100 }}
            animate={{ y: 0 }}
            exit={{ y: 100 }}
            className="absolute bottom-0 inset-x-0 p-4 bg-gradient-to-t from-white via-white to-transparent z-20"
          >
            <motion.div 
              key={cart.length}
              initial={{ scale: 1 }}
              animate={{ scale: [1, 1.05, 1] }}
              transition={{ duration: 0.2 }}
              className="bg-sage-900 text-white rounded-[32px] p-5 shadow-2xl flex items-center justify-between"
            >
              <div>
                <span className="text-[10px] font-black text-white/40 uppercase tracking-widest block">待采订单 (Selected)</span>
                <p className="text-lg font-black">{cart.length} 份菜品 </p>
              </div>
              <button 
                onClick={() => onConfirm(cart)}
                className="bg-white text-sage-900 px-6 py-3.5 rounded-2xl font-black text-xs uppercase tracking-widest flex items-center gap-2 active:scale-95 transition-transform"
              >
                生成今日计划 <ChevronRight size={16} />
              </button>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
