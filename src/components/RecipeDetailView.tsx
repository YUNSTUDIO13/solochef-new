import React, { useState } from 'react';
import { Recipe, MaterialCategory } from '../types';
import { ArrowLeft, Trash2, ChevronRight, Play, Edit3, Plus, Beef, Leaf, Droplets } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';

interface RecipeDetailViewProps {
  recipe: Recipe;
  onBack: () => void;
  onGoCook: () => void;
  onDelete: (id: string) => void;
  onEdit: (recipe: Recipe) => void;
}

export default function RecipeDetailView({ recipe, onBack, onGoCook, onDelete, onEdit }: RecipeDetailViewProps) {
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  return (
    <div className="absolute inset-0 bg-sage-50 z-[120] flex flex-col no-scrollbar overflow-y-auto pb-32">
      {/* Header Image Area */}
      <div className="relative h-[45vh] shrink-0 overflow-hidden">
        <motion.img 
          initial={{ scale: 1.1 }}
          animate={{ scale: 1 }}
          transition={{ duration: 0.8, ease: "easeOut" }}
          src={recipe.cover_image} 
          className="w-full h-full object-cover" 
          alt={recipe.name} 
        />
        <div className="absolute inset-0 bg-gradient-to-t from-sage-50 via-transparent to-black/20" />
        
        {/* Back Button */}
        <button 
          onClick={onBack}
          className="absolute top-6 left-6 w-10 h-10 bg-white/20 backdrop-blur-xl rounded-full flex items-center justify-center text-white active:scale-90 transition-transform z-10 border border-white/10"
        >
          <ArrowLeft size={20} />
        </button>
      </div>

      {/* Content Area */}
      <div className="px-6 -mt-10 relative z-10 space-y-10 pb-10">
        {/* Title & Tags */}
        <div className="space-y-4">
          <motion.h1 
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            className="text-3xl font-black text-sage-900 tracking-tighter"
          >
            {recipe.name}
          </motion.h1>
          <div className="flex flex-wrap gap-2">
            {recipe.tags?.map(tag => (
              <span key={tag} className="px-3 py-1 bg-white border border-sage-200 text-sage-400 text-[10px] font-black rounded-full uppercase tracking-widest">
                {tag}
              </span>
            ))}
          </div>
        </div>

        {/* Ingredients Section (Static List) */}
        <section className="space-y-4">
          <div className="flex items-center gap-3">
            <h2 className="text-[10px] font-black text-sage-900 uppercase tracking-[0.2em]">物料清单 / INGREDIENTS</h2>
            <div className="h-px flex-1 bg-sage-200/50" />
          </div>

          {/* BOM Snapshot (Ingredients Group Photo) */}
          {recipe.bom_snapshot && (
            <motion.div 
              initial={{ opacity: 0, scale: 0.95 }}
              whileInView={{ opacity: 1, scale: 1 }}
              viewport={{ once: true }}
              className="rounded-[32px] overflow-hidden border border-sage-200 shadow-sm bg-white p-2"
            >
              <img 
                src={recipe.bom_snapshot} 
                className="w-full h-48 object-cover rounded-[24px]" 
                alt="Ingredients Snapshot" 
              />
            </motion.div>
          )}

          {/* Grouped materials list with 3 categories side-by-side in a 2-column grid layout */}
          <div className="space-y-6">
            {(['meat', 'vegetable', 'seasoning'] as MaterialCategory[]).map(cat => {
              const items = recipe.materials[cat] || [];
              if (items.length === 0) return null;

              const label = cat === 'meat' ? '肉禽 / 水产' : cat === 'vegetable' ? '蔬菜 / 蔬果' : '调味 / 其他';
              const Icon = cat === 'meat' ? Beef : cat === 'vegetable' ? Leaf : Droplets;

              return (
                <div key={cat} className="space-y-3">
                  <div className="flex items-center gap-2 px-1 text-sage-500">
                    <Icon size={14} className="text-sage-400" />
                    <span className="text-[10px] font-black uppercase tracking-widest leading-none">{label}</span>
                  </div>
                  
                  <div className="grid grid-cols-2 gap-3">
                    {items.map((item, idx) => (
                      <div 
                        key={`${item.item}-${idx}`}
                        className="bg-white border border-sage-200 rounded-[20px] p-4 flex items-center justify-between group shadow-sm transition-all hover:border-sage-300"
                      >
                        <div className="flex items-center gap-3 min-w-0">
                          <div className="w-8 h-8 bg-sage-50 rounded-xl flex items-center justify-center shrink-0">
                            <span className="text-base text-sage-600">🥣</span>
                          </div>
                          <span className="text-sm font-bold text-sage-900 truncate leading-none">{item.item}</span>
                        </div>
                        <div className="flex items-center gap-0.5 shrink-0 ml-1">
                          <span className="text-xs font-black text-sage-900">{item.amount}</span>
                          <span className="text-[10px] font-bold text-sage-300">{item.unit}</span>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              );
            })}
          </div>
        </section>

        {/* Cooking Steps Section */}
        <section className="space-y-6">
          <div className="flex items-center gap-3">
            <h2 className="text-[10px] font-black text-sage-900 uppercase tracking-[0.2em]">烹饪步骤 / PROCESS</h2>
            <div className="h-px flex-1 bg-sage-200/50" />
          </div>
          <div className="space-y-8 relative">
            {/* Timeline Line */}
            <div className="absolute left-4 top-4 bottom-4 w-0.5 bg-sage-100" />
            
            {recipe.timeline.map((step, idx) => (
              <div key={idx} className="relative flex gap-5 items-start">
                {/* Step Circle */}
                <div className="w-8 h-8 shrink-0 bg-white border-2 border-sage-100 rounded-full flex items-center justify-center z-10">
                  <span className="text-[10px] font-black text-sage-900">{idx + 1}</span>
                </div>
                <div className="flex-1 pt-1.5">
                  <h3 className="text-sm font-black text-sage-900 mb-1 leading-tight">{step.content}</h3>
                  <div className="flex items-center gap-1 text-sage-300">
                    <span className="text-[9px] font-bold uppercase tracking-tight">{(step.duration / 60).toFixed(0)} MINS</span>
                  </div>

                  {/* Sub-Tasks Display */}
                  {step.type === 'waiting' && step.sub_tasks && step.sub_tasks.length > 0 && (
                    <div className="mt-3 space-y-2">
                       <p className="text-[10px] font-black text-sage-400 uppercase tracking-widest flex items-center gap-1.5">
                         <span className="w-1 h-1 rounded-full bg-sage-400" /> 并行子任务 / Parallel Tasks
                       </p>
                       <div className="bg-sage-100/50 rounded-2xl p-3 space-y-2 border border-sage-200/30">
                         {step.sub_tasks.map((sub, sIdx) => (
                           <div key={sIdx} className="flex items-start justify-between gap-3">
                             <div className="flex items-start gap-2">
                               <div className="w-4 h-4 rounded bg-sage-900 flex items-center justify-center shrink-0 mt-0.5">
                                 <Plus size={10} className="text-white" />
                               </div>
                               <span className="text-[11px] font-bold text-sage-800 leading-tight">{sub.content}</span>
                             </div>
                             <span className="text-[9px] font-black text-sage-400 shrink-0 font-mono">{(sub.duration / 60).toFixed(0)} MIN</span>
                           </div>
                         ))}
                       </div>
                    </div>
                  )}
                  
                  {/* Step Illustration */}
                  {step.images && step.images.length > 0 && (
                    <motion.div 
                      initial={{ opacity: 0, y: 10 }}
                      whileInView={{ opacity: 1, y: 0 }}
                      viewport={{ once: true }}
                      className="mt-3 rounded-2xl overflow-hidden border border-sage-100 shadow-sm"
                    >
                      <img 
                        src={step.images[0]} 
                        className="w-full h-auto object-cover max-h-48" 
                        alt={`Step ${idx + 1} illustration`} 
                      />
                    </motion.div>
                  )}
                </div>
              </div>
            ))}
          </div>
        </section>
      </div>

      {/* Floating Action Bar (Bottom Fixed) */}
      <div className="fixed bottom-8 left-1/2 -translate-x-1/2 w-[92%] max-w-sm z-[130]">
        <div className="flex gap-3">
          {/* Delete Button */}
          <button 
            onClick={() => setShowDeleteConfirm(true)}
            className="w-14 h-14 bg-white border border-sage-200 rounded-[24px] flex items-center justify-center text-sage-400 active:scale-95 transition-all shadow-lg shrink-0"
          >
            <Trash2 size={24} />
          </button>

          {/* Edit Button */}
          <button 
            onClick={() => onEdit(recipe)}
            className="w-14 h-14 bg-white border border-sage-200 rounded-[24px] flex items-center justify-center text-sage-400 active:scale-95 transition-all shadow-lg shrink-0"
          >
            <Edit3 size={24} />
          </button>

          {/* Go Cook Button */}
          <button 
            onClick={onGoCook}
            className="flex-1 bg-[#282C27] text-white rounded-[24px] flex items-center justify-center gap-3 active:scale-[0.98] transition-all shadow-xl hover:shadow-2xl"
          >
            <Play size={20} fill="currentColor" className="text-[#07C160]" />
            <span className="text-sm font-black tracking-widest uppercase">去做饭</span>
          </button>
        </div>
      </div>

      {/* Delete Confirmation Modal */}
      <AnimatePresence>
        {showDeleteConfirm && (
          <div className="fixed inset-0 z-[150] flex items-center justify-center p-6">
            <motion.div 
              initial={{ opacity: 0 }} 
              animate={{ opacity: 1 }} 
              exit={{ opacity: 0 }}
              onClick={() => setShowDeleteConfirm(false)}
              className="absolute inset-0 bg-sage-900/60 backdrop-blur-sm"
            />
            <motion.div 
              initial={{ scale: 0.9, opacity: 0, y: 20 }}
              animate={{ scale: 1, opacity: 1, y: 0 }}
              exit={{ scale: 0.9, opacity: 0, y: 20 }}
              className="bg-white rounded-[40px] p-8 w-full max-w-xs relative z-10 shadow-2xl border border-sage-200"
            >
              <div className="w-16 h-16 bg-red-50 rounded-2xl flex items-center justify-center text-red-500 mb-6">
                <Trash2 size={32} />
              </div>
              <h2 className="text-xl font-black text-sage-900 tracking-tight leading-tight">确定要删除该菜谱吗？</h2>
              <p className="mt-2 text-sm text-sage-500 font-medium leading-relaxed">删除后不可找回。</p>
              
              <div className="mt-8 space-y-3">
                <button 
                  onClick={() => {
                    onDelete(recipe.id);
                    setShowDeleteConfirm(false);
                    onBack();
                  }}
                  className="w-full bg-red-500 text-white py-4 rounded-2xl text-[11px] font-black uppercase tracking-widest active:scale-95 transition-all shadow-lg"
                >
                  确认删除
                </button>
                <button 
                  onClick={() => setShowDeleteConfirm(false)}
                  className="w-full bg-sage-50 text-sage-400 py-4 rounded-2xl text-[11px] font-black uppercase tracking-widest active:scale-95 transition-all"
                >
                  取消
                </button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
}
