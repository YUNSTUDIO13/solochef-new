import React from 'react';
import { Recipe, OrderBatch } from '../types';
import { LayoutGrid, AlertCircle, ShoppingBag, Zap, Sparkles, ChevronRight, Play } from 'lucide-react';
import { motion } from 'motion/react';

interface DashboardViewProps {
  recipes: Recipe[];
  activeBatch: OrderBatch | null;
  onPlaceOrder: () => void;
  onOpenBatch: (batch: OrderBatch) => void;
  onSelectRecipe: (recipe: Recipe) => void;
  onRandomOrder: (recipe: Recipe) => void;
}

const COOKING_PROCESS_TAGS = ['清蒸', '爆炒', '慢炖', '油炸', '煎烤', '凉拌', '红烧', '白灼'];

export default function DashboardView({ recipes, activeBatch, onPlaceOrder, onOpenBatch, onSelectRecipe, onRandomOrder }: DashboardViewProps) {
  const [selectedProcess, setSelectedProcess] = React.useState('全部');
  const [randomResult, setRandomResult] = React.useState<Recipe | null>(null);
  
  // Random Selection Logic
  const handleRandom = (e: React.MouseEvent) => {
    e.stopPropagation();
    // Logic: Featured or haven't cooked for > 1 week
    const oneWeekAgo = new Date();
    oneWeekAgo.setDate(oneWeekAgo.getDate() - 7);
    
    const candidates = recipes.filter(r => {
      const isLapsed = !r.last_cooked_at || new Date(r.last_cooked_at) < oneWeekAgo;
      return r.is_featured || isLapsed;
    });

    if (candidates.length > 0) {
      const pick = candidates[Math.floor(Math.random() * candidates.length)];
      setRandomResult(pick);
    }
  };
  
  // Dynamic Process Tags: Select distinct cooking processes from recipes that are currently in the database
  const dynamicProcesses = React.useMemo(() => {
    const processes = new Set<string>();
    recipes.forEach(r => {
      if (r.tags) {
        r.tags.forEach(tag => {
          if (COOKING_PROCESS_TAGS.includes(tag)) {
            processes.add(tag);
          }
        });
      }
    });
    return ['全部', ...Array.from(processes).sort((a, b) => COOKING_PROCESS_TAGS.indexOf(a) - COOKING_PROCESS_TAGS.indexOf(b))];
  }, [recipes]);
  
  const featuredRecipes = recipes.filter(r => {
    if (selectedProcess === '全部') {
      return r.tags?.some(tag => COOKING_PROCESS_TAGS.includes(tag));
    }
    return r.tags?.includes(selectedProcess);
  }).sort((a, b) => {
    if (a.is_featured && !b.is_featured) return -1;
    if (!a.is_featured && b.is_featured) return 1;
    return a.name.localeCompare(b.name, 'zh-CN');
  });
  
  // Calculate Lapsed (Activity)
  const oneWeekAgo = new Date();
  oneWeekAgo.setDate(oneWeekAgo.getDate() - 7);
  const lapsedRecipes = recipes.filter(r => {
    if (!r.last_cooked_at) return true;
    return new Date(r.last_cooked_at) < oneWeekAgo;
  });
  return (
    <div className="min-h-screen bg-sage-100 p-6 pb-48 space-y-6">
      <header>
        <h1 className="text-3xl font-black tracking-tighter text-sage-900">独厨SoloChef</h1>
        <p className="text-sage-500 font-mono text-[10px] uppercase tracking-widest mt-1">你的精品线上厨房</p>
      </header>
 
      {/* SoloChef Task Module - Compact Horizontal Centerpiece */}
      <section className="animate-in fade-in slide-in-from-bottom-4 duration-700 -mt-2">
        {!activeBatch ? (
          /* State A: Idle - Unified Height h-40 */
          <div className="flex gap-3 h-40">
            <div 
              onClick={onPlaceOrder}
              className="flex-[2] bg-white border-2 border-sage-800 rounded-[32px] p-5 flex items-center group cursor-pointer active:scale-[0.98] transition-all shadow-[0_15px_40px_rgba(0,0,0,0.04)] border-dashed overflow-hidden"
            >
              <div className="shrink-0">
                <div className="w-12 h-12 bg-sage-50 rounded-2xl flex items-center justify-center text-sage-300 group-hover:scale-110 group-hover:bg-sage-100 transition-all">
                  <ShoppingBag size={24} />
                </div>
              </div>
              <div className="flex-1 text-center px-2">
                <h2 className="text-base font-black text-sage-900 tracking-tight leading-tight">精品厨房空闲中</h2>
                <p className="text-[9px] text-sage-400 font-medium">开启今日美味</p>
                <div className="mt-3 bg-sage-900 text-white w-fit mx-auto px-4 py-2 rounded-full text-[8px] font-black uppercase tracking-widest shadow-lg flex items-center gap-1 group-hover:bg-sage-700 transition-colors">
                  去点单 <ChevronRight size={10} />
                </div>
              </div>
            </div>

            {/* NEW Random Trigger Component */}
            <div 
              onClick={handleRandom}
              className={`flex-1 rounded-[32px] p-4 flex flex-col items-center justify-center relative overflow-hidden cursor-pointer group active:scale-[0.98] transition-all shadow-sm ${
                randomResult ? 'bg-white border-2 border-indigo-400' : 'bg-gradient-to-br from-indigo-500 to-sage-900'
              }`}
            >
              {randomResult ? (
                <motion.div 
                  initial={{ opacity: 0, scale: 0.9 }}
                  animate={{ opacity: 1, scale: 1 }}
                  className="w-full h-full flex flex-col items-center justify-between py-1"
                >
                  <div className="w-12 h-12 rounded-xl overflow-hidden shadow-md">
                    <img src={randomResult.cover_image} className="w-full h-full object-cover" alt="result" />
                  </div>
                  <div className="text-center px-1">
                    <p className="text-[9px] font-black text-sage-900 truncate w-24">{randomResult.name}</p>
                    <button 
                      onClick={(e) => { e.stopPropagation(); onRandomOrder(randomResult); }}
                      className="mt-1.5 bg-sage-900 text-white px-3 py-1 rounded-full text-[8px] font-black shadow-lg"
                    >
                      下单
                    </button>
                  </div>
                </motion.div>
              ) : (
                <>
                  <div className="relative z-10 flex flex-col items-center text-white text-center">
                    <Sparkles size={20} className="mb-2 group-hover:rotate-12 transition-transform text-white/80" />
                    <span className="text-[10px] font-black tracking-tight leading-tight mb-1">今天吃啥？</span>
                    <span className="text-[7px] font-bold opacity-40 uppercase tracking-tighter">随机匹配</span>
                  </div>
                  <Zap className="absolute -right-4 -bottom-4 text-white/5 w-16 h-16 -rotate-12 pointer-events-none" />
                </>
              )}
            </div>
          </div>
        ) : (
          /* State B: In Progress - Unified Compact Height */
          <div 
            className="bg-sage-900 rounded-[32px] p-5 shadow-[0_15px_40px_rgba(20,25,20,0.15)] overflow-hidden relative h-40 flex flex-col justify-between"
          >
            <div className="flex items-center justify-between relative z-10">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 bg-white/10 rounded-lg flex items-center justify-center text-white">
                  <ShoppingBag size={16} />
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <p className="text-sm font-black text-white tracking-tight uppercase">
                      {activeBatch.status === 'picking' ? "物料筹备中" : "烹饪履约中"}
                    </p>
                    <span className="px-1.5 py-0.5 bg-amber-400 text-sage-900 text-[8px] font-black rounded-full">
                      {activeBatch.recipeIds.length}
                    </span>
                  </div>
                </div>
              </div>
              <button 
                onClick={() => onOpenBatch(activeBatch)}
                className="bg-white/10 hover:bg-white text-white hover:text-sage-900 p-2 rounded-xl transition-all"
              >
                <ChevronRight size={16} />
              </button>
            </div>
 
            {/* Content Area: Horizontal Scroll for Thumbnails */}
            <div className="flex-1 flex items-center mt-3 overflow-x-auto no-scrollbar relative z-10 gap-3">
              {activeBatch.recipeIds
                .filter(rid => !activeBatch.completedRecipeIds?.includes(rid))
                .map(rid => {
                const recipe = recipes.find(r => r.id === rid);
                if (!recipe) return null;
                return (
                  <motion.div 
                    key={rid}
                    whileTap={{ scale: 0.95 }}
                    onClick={() => onSelectRecipe(recipe)}
                    className="h-full aspect-square bg-white/5 border border-white/10 rounded-2xl overflow-hidden relative group cursor-pointer flex-shrink-0"
                  >
                    <img src={recipe.cover_image} className="w-full h-full object-cover" alt={recipe.name} />
                    <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-transparent to-transparent" />
                    <div className="absolute inset-x-2 bottom-1.5">
                      <h3 className="text-[8px] font-black text-white leading-tight truncate">{recipe.name}</h3>
                    </div>
                  </motion.div>
                );
              })}
              {/* Hint at more if scrolling possible */}
              <div className="w-4 shrink-0" />
            </div>
            
            <Zap className="absolute -right-20 -bottom-20 text-white/5 w-48 h-48 -rotate-12 pointer-events-none" />
          </div>
        )}
      </section>
 
      {/* B. Discovery & Decision - Tighter Spacing */}
      <section className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-[10px] font-black text-sage-500 uppercase tracking-widest">推荐菜 (Featured)</h2>
          <span className="text-sage-400"><Sparkles size={14} /></span>
        </div>

        {/* Process Filter */}
        <div className="flex gap-2 overflow-x-auto pb-2 no-scrollbar">
          {dynamicProcesses.map(tag => (
            <button
              key={tag}
              onClick={() => setSelectedProcess(tag)}
              className={`px-4 py-2 rounded-full text-[10px] font-black whitespace-nowrap transition-all border ${
                selectedProcess === tag 
                ? 'bg-sage-900 text-white border-sage-900' 
                : 'bg-white text-sage-400 border-sage-200'
              }`}
            >
              {tag}
            </button>
          ))}
        </div>

        {/* 2x2 Grid Layout */}
        <div className="grid grid-cols-2 gap-4">
          {featuredRecipes.slice(0, 4).map(recipe => (
            <motion.div 
              key={recipe.id}
              whileTap={{ scale: 0.95 }}
              onClick={() => onSelectRecipe(recipe)}
              className="aspect-square bg-white border border-sage-200 rounded-[32px] overflow-hidden shadow-sm relative group cursor-pointer"
            >
              <img src={recipe.cover_image} className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-700" alt={recipe.name} />
              <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent" />
              <div className="absolute bottom-3 left-3 right-3">
                <h3 className="text-xs font-black text-white leading-tight break-words">{recipe.name}</h3>
              </div>
            </motion.div>
          ))}
        </div>
        
        {featuredRecipes.length === 0 && (
          <div className="w-full py-12 text-center bg-white border border-sage-100 rounded-[32px] text-sage-300 text-[10px] font-black uppercase tracking-widest">
            暂无推荐菜谱
          </div>
        )}
      </section>
    </div>
  );
}
