import React, { useState } from 'react';
import { EnergyLevel, Recipe, UserStats } from '../types';
import { Zap, Coffee, Moon, Dice5, ChevronRight, Clock, Plus, BarChart3, Star, ChefHat } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import FireStatusHeader from './FireStatusHeader';

interface SelectionViewProps {
  onSelect: (recipe: Recipe, isLazy: boolean) => void;
  recipes: Recipe[];
  stats: UserStats;
  onCreateClick: () => void;
}

export default function SelectionView({ onSelect, recipes, stats, onCreateClick }: SelectionViewProps) {
  const [energy, setEnergy] = useState<EnergyLevel>('Mid');

  const filteredRecipes = recipes.filter(r => r.energy_level === energy);

  const getRandomRecipe = () => {
    return filteredRecipes[Math.floor(Math.random() * filteredRecipes.length)];
  };

  const handleRandom = () => {
    const random = getRandomRecipe();
    if (random) onSelect(random, energy === 'Low');
  };

  return (
    <div className="min-h-screen bg-sage-100 text-sage-800 p-4 flex flex-col pb-40">
      <FireStatusHeader 
        stats={stats} 
      />

      {/* Controls Row: Energy Selector + Quick Dice */}
      <section className="mb-6 flex gap-3">
        <div className="flex-1 flex bg-white p-1 rounded-2xl border border-sage-200 shadow-sm">
          {(['Low', 'Mid', 'High'] as EnergyLevel[]).map((level) => (
            <button
              key={level}
              onClick={() => setEnergy(level)}
              className={`flex-1 flex flex-col items-center gap-1 py-2 rounded-xl transition-all ${
                energy === level ? 'bg-sage-200 text-sage-900 font-bold' : 'text-sage-500 hover:text-sage-700'
              }`}
            >
              {level === 'Low' && <Moon size={14} />}
              {level === 'Mid' && <Coffee size={14} />}
              {level === 'High' && <Zap size={14} />}
              <span className="text-[10px] font-black uppercase tracking-widest leading-none mt-1">
                {level === 'Low' ? '极简' : level === 'Mid' ? '正常' : '满满'}
              </span>
            </button>
          ))}
        </div>

        <motion.button
          whileTap={{ scale: 0.95 }}
          onClick={handleRandom}
          className="w-20 bg-sage-800 rounded-2xl flex flex-col items-center justify-center gap-1 shadow-lg shadow-sage-900/10 text-white"
        >
          <Dice5 size={18} strokeWidth={3} />
          <span className="text-[10px] font-black uppercase tracking-widest">吃啥</span>
        </motion.button>
      </section>

      {/* Recipe Asset List - Waterfall layout */}
      <section className="space-y-5">
        <div className="flex items-center justify-between px-1">
          <h2 className="text-xl font-black tracking-tighter text-sage-900 uppercase">
            我的菜谱
          </h2>
          <span className="text-[10px] font-black uppercase tracking-widest text-sage-500">查看全部</span>
        </div>
        
        <div className="grid grid-cols-2 gap-4">
          <AnimatePresence mode="popLayout">
            {filteredRecipes.map((recipe) => (
              <motion.button
                layout
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, scale: 0.9 }}
                key={recipe.id}
                onClick={() => onSelect(recipe, energy === 'Low')}
                className="group overflow-hidden rounded-[28px] bg-white flex flex-col transition-all active:scale-[0.98] shadow-sm border border-sage-200"
              >
                {/* Image Section */}
                <div className="relative overflow-hidden w-full aspect-square p-2">
                  <div className="w-full h-full rounded-[20px] overflow-hidden bg-sage-50">
                    <img 
                      src={recipe.cover_image} 
                      alt={recipe.name}
                      className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-700" 
                    />
                  </div>
                  {/* Floating Time Tag */}
                  <div className="absolute bottom-4 left-4 bg-white/95 backdrop-blur-md px-2 py-1 rounded-lg flex items-center gap-1 shadow-sm border border-sage-100">
                     <Clock size={10} className="text-sage-900" />
                     <span className="text-[10px] font-bold text-sage-900">{Math.floor(recipe.timeline.reduce((acc, s) => acc + s.duration, 0) / 60)}m</span>
                  </div>
                </div>
                
                {/* Content Section */}
                <div className="px-4 pb-4 bg-white">
                  <h3 className="font-black tracking-tight text-sage-900 leading-tight line-clamp-1 uppercase text-sm">
                    {recipe.name}
                  </h3>
                  <p className="text-[10px] font-bold text-sage-500 uppercase mt-1 tracking-wider">今日推荐</p>
                </div>
              </motion.button>
            ))}
          </AnimatePresence>
        </div>
      </section>
    </div>
  );
}
