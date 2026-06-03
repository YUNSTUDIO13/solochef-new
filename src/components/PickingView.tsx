import React, { useState } from 'react';
import { Recipe, MaterialCategory } from '../types';
import { Check, Package, ChevronRight, AlertCircle, Beef, Leaf, Droplets, Camera } from 'lucide-react';
import { motion } from 'motion/react';

interface PickingViewProps {
  recipe: Recipe;
  onConfirm: () => void;
  onCancel: () => void;
}

export default function PickingView({ recipe, onConfirm, onCancel }: PickingViewProps) {
  const [confirmedItems, setConfirmedItems] = useState<Set<string>>(new Set());

  const toggleItem = (category: string, idx: number) => {
    const key = `${category}-${idx}`;
    const newSet = new Set(confirmedItems);
    if (newSet.has(key)) {
      newSet.delete(key);
    } else {
      newSet.add(key);
    }
    setConfirmedItems(newSet);
  };

  const allMaterials = Object.values(recipe.materials).flat();
  const allConfirmed = confirmedItems.size === allMaterials.length;
  const totalCount = allMaterials.length;

  const getAllKeys = () => {
    const keys: string[] = [];
    (['meat', 'vegetable', 'seasoning'] as MaterialCategory[]).forEach(cat => {
      const items = recipe.materials[cat];
      if (items) {
        items.forEach((_, idx) => {
          keys.push(`${cat}-${idx}`);
        });
      }
    });
    return keys;
  };

  const keys = getAllKeys();
  const allChecked = keys.length > 0 && keys.every(k => confirmedItems.has(k));

  const toggleSelectAll = () => {
    if (allChecked) {
      setConfirmedItems(new Set());
    } else {
      setConfirmedItems(new Set(keys));
    }
  };

  return (
    <div className="absolute inset-0 bg-sage-100 text-sage-900 p-6 flex flex-col font-sans overflow-y-auto no-scrollbar">
      <header className="mb-8">
        <div className="flex justify-between items-start">
          <div>
            <h1 className="text-3xl font-black tracking-tighter text-sage-900 uppercase leading-none">拣货确认</h1>
            <p className="text-sage-500 font-mono text-[10px] mt-2 uppercase tracking-widest leading-none">Material Picking / BOM Check</p>
          </div>
          <button onClick={onCancel} className="text-sage-500 hover:text-sage-900 text-xs font-bold border-b border-sage-200 pb-1 transition-colors">取消</button>
        </div>
        
        <div className="mt-6 bg-white border border-sage-200 rounded-[32px] p-5 flex items-center gap-4 shadow-sm">
          <div className="w-14 h-14 bg-sage-100 rounded-2xl flex items-center justify-center text-sage-900">
            <Package size={28} />
          </div>
          <div>
            <h2 className="text-xl font-black text-sage-900 tracking-tight">{recipe.name}</h2>
            <p className="text-xs font-bold text-sage-500 uppercase tracking-widest mt-0.5">共需 {totalCount} 项物料</p>
          </div>
        </div>

        {recipe.bom_snapshot && (
          <div className="mt-6">
            <div className="bg-white border border-sage-200 rounded-[32px] overflow-hidden shadow-sm relative group">
              <div className="absolute top-4 left-4 z-10">
                <span className="px-3 py-1 bg-sage-900/80 backdrop-blur-md rounded-full text-[10px] font-black text-white uppercase tracking-widest flex items-center gap-2">
                  <Camera size={12} /> 食材全家福
                </span>
              </div>
              <img src={recipe.bom_snapshot} className="w-full h-56 object-cover hover:scale-105 transition-transform duration-500 cursor-zoom-in" alt="BOM Snapshot" />
            </div>
          </div>
        )}
      </header>

      <main className="flex-1 space-y-8 overflow-y-auto no-scrollbar pb-32">
        {(['meat', 'vegetable', 'seasoning'] as MaterialCategory[]).map(cat => {
          const catMaterials = recipe.materials[cat];
          if (catMaterials.length === 0) return null;

          return (
            <div key={cat} className="space-y-4">
              <div className="flex items-center gap-2 px-1 text-sage-500">
                {cat === 'meat' && <Beef size={14} />}
                {cat === 'vegetable' && <Leaf size={14} />}
                {cat === 'seasoning' && <Droplets size={14} />}
                <span className="text-[10px] font-black uppercase tracking-widest">
                  {cat === 'meat' ? '肉禽 / 水产' : cat === 'vegetable' ? '蔬菜 / 水果' : '调料 / 干货'}
                </span>
              </div>
              
              <div className={cat === 'seasoning' ? "grid grid-cols-2 gap-3" : "space-y-3"}>
                {catMaterials.map((m, idx) => {
                  const key = `${cat}-${idx}`;
                  const isConfirmed = confirmedItems.has(key);
                  return (
                    <motion.button
                      key={key}
                      whileTap={{ scale: 0.98 }}
                      onClick={() => toggleItem(cat, idx)}
                      className={`w-full ${cat === 'seasoning' ? 'p-4 rounded-3xl' : 'p-5 rounded-[28px]'} border transition-all flex items-center justify-between ${
                        isConfirmed 
                          ? 'bg-sage-800 border-sage-800 text-white shadow-lg shadow-sage-900/10' 
                          : 'bg-white border-sage-200 text-sage-900 shadow-sm'
                      }`}
                    >
                      <div className="flex items-center gap-4 min-w-0 flex-1">
                        <div className={`w-6 h-6 rounded-lg border-2 flex items-center justify-center shrink-0 transition-colors ${
                          isConfirmed ? 'bg-white border-white text-sage-800' : 'border-sage-200'
                        }`}>
                          {isConfirmed && <Check size={14} strokeWidth={4} />}
                        </div>
                        <div className="text-left flex items-center gap-3 min-w-0 flex-1">
                          {m.image && (
                            <div className="w-12 h-12 rounded-xl overflow-hidden border border-sage-100 flex-shrink-0">
                               <img src={m.image} className="w-full h-full object-cover" alt={m.item} />
                            </div>
                          )}
                          <span className={`text-base font-black tracking-tight truncate ${isConfirmed ? 'text-white' : 'text-sage-900'}`}>
                            {m.item}
                          </span>
                        </div>
                      </div>
                      <div className="flex items-center gap-0.5 shrink-0 ml-2">
                        <span className={`text-sm font-black ${isConfirmed ? 'text-white' : 'text-sage-900'}`}>{m.amount}</span>
                        <span className={`text-[10px] font-bold ${isConfirmed ? 'text-white/60' : 'text-sage-300'}`}>{m.unit}</span>
                      </div>
                    </motion.button>
                  );
                })}
              </div>
            </div>
          );
        })}

        {totalCount === 0 && (
          <div className="py-20 text-center border-2 border-dashed border-sage-200 rounded-[32px]">
            <AlertCircle className="mx-auto text-sage-300 mb-2" size={32} />
            <p className="text-sage-500 font-black uppercase tracking-widest text-xs">此菜谱无物料清单</p>
          </div>
        )}
      </main>

      <footer className="absolute bottom-0 left-0 right-0 p-8 bg-gradient-to-t from-sage-100 via-sage-100 to-transparent z-10 flex justify-center">
        <div className="w-full max-w-md flex gap-3">
          {totalCount > 0 && (
            <motion.button
              whileTap={{ scale: 0.95 }}
              onClick={toggleSelectAll}
              title={allChecked ? "取消全选" : "一键全选"}
              className={`h-20 w-20 rounded-[32px] flex items-center justify-center transition-all shadow-xl border shrink-0 ${
                allChecked
                  ? 'bg-sage-800 border-sage-800 text-white shadow-sage-900/15'
                  : 'bg-white border-sage-200 text-sage-500 shadow-sage-900/5'
              }`}
            >
              <div className={`w-6 h-6 rounded-lg border-2 flex items-center justify-center transition-colors ${
                allChecked ? 'bg-white border-white text-sage-800' : 'border-sage-300'
              }`}>
                {allChecked && <Check size={14} strokeWidth={4} />}
              </div>
            </motion.button>
          )}
          <button 
            onClick={onConfirm}
            disabled={!allConfirmed && totalCount > 0}
            className={`flex-1 h-20 rounded-[32px] flex items-center justify-center gap-3 font-black text-xl transition-all shadow-xl ${
              allConfirmed || totalCount === 0
                ? 'bg-sage-800 text-white shadow-sage-900/20 active:scale-95' 
                : 'bg-white text-sage-400 border border-sage-200 opacity-50 cursor-not-allowed'
            }`}
          >
            <span>确认物料并开火</span>
            <ChevronRight size={24} strokeWidth={3} />
          </button>
        </div>
      </footer>
    </div>
  );
}
