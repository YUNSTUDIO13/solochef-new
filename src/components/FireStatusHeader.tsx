import React, { useMemo } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { Flame, Sparkles, ChevronRight, Plus } from 'lucide-react';
import { UserStats, FireState, FireColor } from '../types';

interface FireStatusHeaderProps {
  stats: UserStats;
  onClick?: () => void;
}

export default function FireStatusHeader({ stats, onClick }: FireStatusHeaderProps) {
  const { state, color, label } = useMemo(() => {
    const last = new Date(stats.last_ignition).getTime();
    const now = Date.now();
    const diffHours = (now - last) / (1000 * 60 * 60);

    let state: FireState = 'extinguished';
    if (diffHours < 12) state = 'embers';
    if (stats.streak >= 3 && diffHours < 12) state = 'blazing';
    if (diffHours >= 24) state = 'extinguished';

    let color: FireColor = 'gold';
    if (stats.streak <= 3) color = 'cyan';
    else if (stats.streak <= 10) color = 'gold';
    else color = 'purple';

    let label = '厨房新贵';
    if (stats.streak >= 100) label = '生活掌控大师';
    else if (stats.streak >= 30) label = '外卖行业死对头';
    else if (stats.streak >= 7) label = '烟火气守护者';

    return { state, color, label };
  }, [stats]);

  const ringColors = {
    cyan: 'bg-cyan-100 text-cyan-700',
    gold: 'bg-yellow-100 text-yellow-700',
    purple: 'bg-purple-100 text-purple-700',
  };

  const textColors = {
    cyan: 'text-cyan-600',
    gold: 'text-yellow-600',
    purple: 'text-purple-600',
  };

  return (
    <motion.header 
      initial={{ opacity: 0, y: -10 }}
      animate={{ opacity: 1, y: 0 }}
      className="mb-6 bg-sage-200 border border-sage-300 rounded-[32px] p-5 flex items-center gap-5 relative overflow-hidden shadow-sm"
    >
      {/* Flame Icon Container */}
      <div className="relative z-10 shrink-0" onClick={onClick}>
        <div className={`w-16 h-16 rounded-2xl flex items-center justify-center transition-all ${
          state === 'extinguished' ? 'bg-sage-300 text-sage-800 grayscale' : ringColors[color]
        }`}>
          <motion.div
            animate={state !== 'extinguished' ? {
              scale: state === 'blazing' ? [1, 1.1, 1] : [1, 1.05, 1],
            } : {}}
            transition={{ duration: 2, repeat: Infinity }}
          >
            <Flame size={32} strokeWidth={2.5} />
          </motion.div>
        </div>
      </div>

      {/* Info Section */}
      <div className="flex-1 min-w-0" onClick={onClick}>
        <div className="flex flex-col">
          <h1 className="text-xl font-black tracking-tighter text-sage-900 uppercase truncate">
            已连续点火 {stats.streak} 天
          </h1>
          <div className="flex items-center gap-2 mt-1">
            <span className={`text-[10px] font-black px-2 py-0.5 rounded-full border uppercase tracking-wider ${
              state === 'extinguished' ? 'text-sage-900 border-sage-900' : 
              color === 'cyan' ? 'text-cyan-800 bg-cyan-100 border-cyan-200' : 
              color === 'gold' ? 'text-yellow-800 bg-yellow-100 border-yellow-200' : 
              'text-purple-800 bg-purple-100 border-purple-200'
            }`}>
              {label}
            </span>
          </div>
        </div>
        <p className="text-[11px] font-bold text-sage-700 mt-2 uppercase tracking-widest line-clamp-1">
          {state === 'extinguished' ? '重新开启生活掌控权' : '今晚准备做什么？'}
        </p>
      </div>
    </motion.header>
  );
}
