import React from 'react';
import { Beef, Leaf, Droplets, TrendingUp, Calendar, Hash, Zap, ChevronRight } from 'lucide-react';
import { motion } from 'motion/react';
import { Recipe } from '../types';

interface AnalyticsViewProps {
  recipes: Recipe[];
  onNavigateToLibrary: () => void;
}

const WEEKLY_DATA = [
  { day: 'Mon', count: 0 },
  { day: 'Tue', count: 0 },
  { day: 'Wed', count: 0 },
  { day: 'Thu', count: 0 },
  { day: 'Fri', count: 0 },
  { day: 'Sat', count: 0 },
  { day: 'Sun', count: 0 },
];

const TOP_INGREDIENTS = {
  meat: [],
  vegetable: [],
  seasoning: [],
};

export default function AnalyticsView({ recipes, onNavigateToLibrary }: AnalyticsViewProps) {
  const oneWeekAgo = new Date();
  oneWeekAgo.setDate(oneWeekAgo.getDate() - 7);

  const lapsedRecipesCount = recipes.filter(r => {
    if (!r.last_cooked_at) return true;
    return new Date(r.last_cooked_at) < oneWeekAgo;
  }).length;

  return (
    <div className="min-h-screen bg-sage-100 text-sage-900 p-6 flex flex-col pb-44">
      <header className="mb-6">
        <h1 className="text-6xl font-black tracking-tighter text-sage-900 uppercase">数据大盘</h1>
        <p className="text-sage-500 font-mono text-[10px] mt-2 uppercase tracking-widest leading-none">Kitchen BI & Fulfillment Insights</p>
      </header>

      <div className="space-y-6">
        {/* New Top Metrics Section */}
        <section className="grid grid-cols-2 gap-4">
          <div className="bg-white border border-sage-200 rounded-[32px] p-5 shadow-sm">
            <div className="flex items-center gap-2 mb-2 text-sage-400">
               <Hash size={14} />
               <span className="text-[10px] font-black uppercase tracking-widest">菜谱总量</span>
            </div>
            <div className="flex items-baseline gap-1">
              <span className="text-3xl font-black text-sage-900">{recipes.length}</span>
              <span className="text-[10px] font-bold text-sage-400">道菜</span>
            </div>
          </div>
          
          <button 
            onClick={onNavigateToLibrary}
            className="text-left bg-sage-900 rounded-[32px] p-5 shadow-lg shadow-sage-900/10 flex flex-col justify-between group active:scale-95 transition-all"
          >
            <div>
              <div className="flex items-center gap-2 mb-2 text-white/40">
                <Zap size={14} className="group-hover:text-amber-400 transition-colors" />
                <span className="text-[10px] font-black uppercase tracking-widest">活跃度</span>
              </div>
              <div className="flex items-baseline gap-1">
                <span className="text-3xl font-black text-white">{lapsedRecipesCount}</span>
                <span className="text-[10px] font-bold text-white/60">菜失宠</span>
              </div>
            </div>
            <div className="flex items-center justify-between mt-2 pt-2 border-t border-white/10">
              <span className="text-[8px] font-bold text-white/40 uppercase tracking-tight">一周未烹饪</span>
              <ChevronRight size={12} className="text-white/40 group-hover:text-white transition-colors" />
            </div>
          </button>
        </section>

        {/* Heatmap Section */}
        <section className="bg-white border border-sage-200 rounded-[32px] p-6 shadow-sm relative overflow-hidden">
          <div className="flex items-center gap-2 mb-6 text-sage-900">
            <Calendar size={18} />
            <h2 className="text-sm font-black uppercase tracking-widest">本周热力图</h2>
          </div>
          
          <div className="grid grid-cols-7 gap-2 mb-4 relative z-10">
            {WEEKLY_DATA.map((day, i) => (
              <div key={i} className="flex flex-col items-center gap-2">
                <div 
                   className={`w-full aspect-square rounded-xl transition-all ${
                    day.count === 0 ? 'bg-sage-100' :
                    day.count === 1 ? 'bg-sage-200' :
                    day.count === 2 ? 'bg-sage-300' :
                    day.count === 3 ? 'bg-sage-400' :
                    'bg-sage-800 shadow-lg shadow-sage-900/10'
                  }`}
                />
                <span className="text-[10px] font-mono text-sage-500 font-bold uppercase">{day.day}</span>
              </div>
            ))}
          </div>
          <div className="flex justify-between items-center text-[10px] font-black text-sage-500 uppercase tracking-widest border-t border-sage-100 pt-4 relative z-10">
            <span>在家吃饭率: 0%</span>
            <div className="flex items-center gap-1">
              <span>Less</span>
              <div className="flex gap-1">
                <div className="w-2 h-2 bg-sage-100 rounded-sm" />
                <div className="w-2 h-2 bg-sage-200 rounded-sm" />
                <div className="w-2 h-2 bg-sage-300 rounded-sm" />
                <div className="w-2 h-2 bg-sage-800 rounded-sm" />
              </div>
              <span>More</span>
            </div>
          </div>
        </section>

        {/* Consumption Leaderboard */}
        <section className="bg-white border border-sage-200 rounded-[32px] p-6 shadow-sm">
          <div className="flex items-center gap-2 mb-8 text-sage-900">
            <TrendingUp size={18} />
            <h2 className="text-sm font-black uppercase tracking-widest">食材消耗排行榜</h2>
          </div>

          <div className="space-y-8">
            {Object.entries(TOP_INGREDIENTS).map(([cat, items]) => (
              <div key={cat} className="space-y-4">
                <div className="flex items-center gap-2 px-1 text-sage-500">
                  {cat === 'meat' && <Beef size={16} />}
                  {cat === 'vegetable' && <Leaf size={16} />}
                  {cat === 'seasoning' && <Droplets size={16} />}
                  <span className="text-[10px] font-black uppercase tracking-widest text-sage-500">
                    {cat === 'meat' ? '肉禽 TOP 3' : cat === 'vegetable' ? '蔬菜 TOP 3' : '调味 TOP 3'}
                  </span>
                </div>
                
                <div className="space-y-2">
                  {items.map((item, i) => (
                    <div key={i} className="relative h-12 w-full bg-sage-50 rounded-[18px] overflow-hidden border border-sage-100">
                      <motion.div 
                        initial={{ width: 0 }}
                        animate={{ width: `${(item.count / 20) * 100}%` }}
                        transition={{ duration: 1, ease: [0.16, 1, 0.3, 1], delay: i * 0.1 }}
                        className="absolute inset-y-0 left-0 bg-sage-800 opacity-[0.08]"
                      />
                      <div className="absolute inset-0 px-5 flex items-center justify-between">
                        <span className="text-sm font-bold text-sage-900">{item.name}</span>
                        <div className="flex items-center gap-2">
                          <span className="text-xs font-black text-sage-400">{item.count}</span>
                          <span className="text-[10px] font-black text-sage-500 uppercase">次</span>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </section>
      </div>
    </div>
  );
}
