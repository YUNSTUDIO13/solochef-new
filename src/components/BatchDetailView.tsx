import React, { useState } from 'react';
import { Recipe, OrderBatch, MaterialCategory, Material } from '../types';
import { X, CheckCircle2, ChevronRight, ShoppingBasket, ChefHat, Beef, Leaf, Droplets, Camera } from 'lucide-react';
import { motion } from 'motion/react';

interface BatchDetailViewProps {
  batch: OrderBatch;
  recipes: Recipe[];
  onUpdateStatus: (id: string, status: 'picking' | 'processing' | 'finished') => void;
  onCompleteRecipe: (id: string) => void;
  onClose: () => void;
  onOpenSOP: (recipe: Recipe) => void;
}

export default function BatchDetailView({ batch, recipes, onUpdateStatus, onCompleteRecipe, onClose, onOpenSOP }: BatchDetailViewProps) {
  const batchRecipes = recipes.filter(r => batch.recipeIds.includes(r.id));
  const [checkedMaterials, setCheckedMaterials] = useState<string[]>([]);
  
  const toggleMaterial = (item: string) => {
    setCheckedMaterials(prev => 
      prev.includes(item) ? prev.filter(i => i !== item) : [...prev, item]
    );
  };

  
  // Aggregate Materials Logic
  const aggregateMaterials = () => {
    const aggregated: Record<MaterialCategory, Material[]> = {
      meat: [],
      vegetable: [],
      seasoning: []
    };

    batchRecipes.forEach(recipe => {
      Object.entries(recipe.materials).forEach(([category, materials]) => {
        materials.forEach(mat => {
          const cat = category as MaterialCategory;
          const existing = aggregated[cat].find(a => a.item === mat.item && a.unit === mat.unit);
          
          if (existing) {
            // Try to add numeric amounts, otherwise keep as string concat
            const isNumeric = !isNaN(parseFloat(mat.amount)) && !isNaN(parseFloat(existing.amount));
            if (isNumeric) {
              existing.amount = (parseFloat(existing.amount) + parseFloat(mat.amount)).toString();
            } else {
              existing.amount += ` + ${mat.amount}`;
            }
          } else {
            aggregated[cat].push({ ...mat });
          }
        });
      });
    });

    return aggregated;
  };

  const materials = aggregateMaterials();

  return (
    <div className="absolute inset-0 bg-sage-50 z-50 overflow-y-auto no-scrollbar pb-44">
      <header className="sticky top-0 bg-sage-50/80 backdrop-blur-md p-6 z-10 flex items-center justify-between border-b border-sage-200">
        <div className="flex items-center gap-3">
          <button onClick={onClose} className="p-2 text-sage-400 hover:text-sage-900 transition-colors">
            <X size={24} />
          </button>
          <div>
            <h1 className="text-xl font-black tracking-tighter text-sage-900 uppercase">独厨履约 (Kitchen OMS)</h1>
            <span className="text-[10px] font-black text-sage-400 uppercase tracking-widest block">批次编号: {batch.id.slice(-6)}</span>
          </div>
        </div>
        <div className="bg-sage-900 text-white px-3 py-1 rounded-full text-[10px] font-black uppercase tracking-widest">
          {batch.status === 'picking' ? "物料筹备" : "待烹饪"}
        </div>
      </header>

      <div className="p-6 space-y-8">
        {/* Progress Timeline */}
        <div className="flex items-center gap-2 px-2">
          <div className="flex-1 h-2 bg-sage-900 rounded-full" />
          <div className={`flex-1 h-2 rounded-full ${batch.status === 'processing' ? 'bg-sage-900' : 'bg-sage-200'}`} />
          <div className="flex-1 h-2 bg-sage-200 rounded-full" />
        </div>

        {/* Mode A: Batch BOM (Picking) */}
        {batch.status === 'picking' && (
          <div className="space-y-6">
            <div className="flex items-center gap-4 bg-white p-6 rounded-[32px] border border-sage-200 shadow-sm">
               <div className="w-16 h-16 bg-sage-100 rounded-2xl flex items-center justify-center text-sage-900">
                 <ShoppingBasket size={32} />
               </div>
               <div className="flex-1">
                 <h2 className="text-lg font-black text-sage-900 tracking-tight">批量采购清单 (Batch BOM)</h2>
                 <p className="text-xs text-sage-500">系统已按分区自动去重合并食材清单</p>
               </div>
            </div>

            {(['meat', 'vegetable', 'seasoning'] as MaterialCategory[]).map(cat => (
              <div key={cat} className="space-y-3">
                <div className="flex items-center gap-2 px-4">
                  {cat === 'meat' ? <Beef size={14} className="text-rose-400" /> : cat === 'vegetable' ? <Leaf size={14} className="text-emerald-400" /> : <Droplets size={14} className="text-amber-400" />}
                  <h3 className="text-[10px] font-black text-sage-400 uppercase tracking-widest">
                    {cat === 'meat' ? "肉禽 / 蛋 / 水产" : cat === 'vegetable' ? "蔬菜 / 瓜果 / 菌菇" : "调料 / 辅料 / 干货"}
                  </h3>
                </div>
                {materials[cat].map((m, idx) => (
                  <div 
                    key={idx} 
                    onClick={() => toggleMaterial(m.item)}
                    className={`bg-white border border-sage-200 rounded-[28px] p-4 flex items-center justify-between group shadow-sm cursor-pointer transition-all ${checkedMaterials.includes(m.item) ? 'opacity-50 grayscale scale-[0.98]' : ''}`}
                  >
                    <div className="flex items-center gap-4">
                      <div className={`w-10 h-10 rounded-xl overflow-hidden flex items-center justify-center shrink-0 ${checkedMaterials.includes(m.item) ? 'bg-emerald-500 text-white' : ''}`}>
                        {checkedMaterials.includes(m.item) ? <CheckCircle2 size={20} /> : (
                          m.image ? <img src={m.image} className="w-full h-full object-cover" alt={m.item} /> : <Camera size={16} className="text-sage-300" />
                        )}
                      </div>
                      <span className={`text-sm font-black text-sage-900 ${checkedMaterials.includes(m.item) ? 'line-through text-sage-400' : ''}`}>{m.item}</span>
                    </div>
                    <div className="flex items-center gap-3">
                       <span className="text-sm font-black text-sage-900 bg-sage-50 px-3 py-1.5 rounded-xl">{m.amount}</span>
                       <span className="text-[10px] font-black text-sage-400 uppercase">{m.unit}</span>
                    </div>
                  </div>
                ))}
              </div>
            ))}
            
            <button 
              onClick={() => onUpdateStatus(batch.id, 'processing')}
              className="w-full bg-sage-900 text-white rounded-[32px] p-6 text-lg font-black tracking-tight active:scale-95 transition-transform shadow-xl"
            >
              采购完成 (Confirm)
            </button>
          </div>
        )}

        {/* Mode B: Processing (Cook) */}
        {batch.status === 'processing' && (
          <div className="space-y-6">
            <div className="flex items-center gap-4 bg-white p-6 rounded-[32px] border border-sage-200 shadow-sm">
               <div className="w-16 h-16 bg-sage-800 rounded-2xl flex items-center justify-center text-white">
                 <ChefHat size={32} />
               </div>
               <div className="flex-1">
                 <h2 className="text-lg font-black text-sage-900 tracking-tight">今日待做 (Cook)</h2>
                 <p className="text-xs text-sage-500">点击进入标准图解或一键快速完成</p>
               </div>
            </div>

            <div className="grid grid-cols-1 gap-4">
              {batchRecipes.map(recipe => {
                const isDone = batch.completedRecipeIds?.includes(recipe.id);
                return (
                  <div 
                    key={recipe.id}
                    className={`bg-white border border-sage-200 rounded-[32px] overflow-hidden group shadow-sm flex transition-all ${isDone ? 'opacity-50 grayscale' : ''}`}
                  >
                    <div className="w-32 aspect-square shrink-0 relative">
                      <img src={recipe.cover_image} className="w-full h-full object-cover" alt={recipe.name} />
                      {isDone && (
                        <div className="absolute inset-0 bg-emerald-500/20 flex items-center justify-center text-emerald-500">
                          <CheckCircle2 size={32} />
                        </div>
                      )}
                    </div>
                    <div className="p-4 flex flex-col justify-between flex-1">
                      <div>
                        <h3 className="text-lg font-black text-sage-900 tracking-tight">{recipe.name}</h3>
                        <div className="flex gap-1 mt-1">
                          {recipe.tags?.slice(0, 2).map(tag => (
                            <span key={tag} className="text-[8px] font-bold text-sage-400 uppercase">#{tag}</span>
                          ))}
                        </div>
                      </div>
                      <div className="flex gap-2">
                        {!isDone ? (
                          <>
                             <button 
                               onClick={() => onOpenSOP(recipe)}
                               className="flex-1 bg-sage-800 text-white py-2 rounded-xl text-[10px] font-black uppercase tracking-widest flex items-center justify-center gap-1 active:scale-95 transition-transform"
                             >
                               开始加工 <ChevronRight size={12} />
                             </button>
                             <button 
                               onClick={() => onCompleteRecipe(recipe.id)}
                               className="w-12 bg-sage-100 text-sage-500 rounded-xl flex items-center justify-center hover:bg-emerald-50 hover:text-emerald-500 transition-colors"
                             >
                               <CheckCircle2 size={18} />
                             </button>
                          </>
                        ) : (
                          <div className="flex-1 bg-emerald-50 text-emerald-500 py-2 rounded-xl text-[10px] font-black uppercase tracking-widest flex items-center justify-center gap-1">
                            制作完成
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>

            <button 
              onClick={() => onUpdateStatus(batch.id, 'finished')}
              className="w-full border-4 border-dashed border-sage-800 text-sage-800 bg-white rounded-[32px] p-6 text-lg font-black tracking-tight active:scale-95 transition-transform"
            >
              一键归档所有 (Finish All)
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
