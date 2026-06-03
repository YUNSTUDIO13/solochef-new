import React, { useState, useEffect, useRef } from 'react';
import { Recipe, TimelineStep } from '../types';
import { Timer, Zap, Play, Pause, ChevronRight, Volume2, SkipForward, X } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';

interface ExecutionViewProps {
  recipe: Recipe;
  isLazy: boolean;
  onComplete: (recipe: Recipe) => void;
}

export default function ExecutionView({ recipe, isLazy, onComplete }: ExecutionViewProps) {
  const [activeStepIndex, setActiveStepIndex] = useState(0);
  const [timeLeft, setTimeLeft] = useState(0);
  const [isPaused, setIsPaused] = useState(false);
  const [fullScreenImage, setFullScreenImage] = useState<string | null>(null);
  const timerRef = useRef<NodeJS.Timeout | null>(null);
  const wakeLockRef = useRef<any>(null);

  const currentStep = recipe.timeline[activeStepIndex];

  // Initialize step
  useEffect(() => {
    if (currentStep) {
      setTimeLeft(currentStep.duration);
    } else {
      onComplete(recipe);
    }
  }, [activeStepIndex, recipe.timeline.length, onComplete]); // Fixed dependency array

  // Timer logic
  useEffect(() => {
    if (!isPaused && timeLeft > 0) {
      timerRef.current = setInterval(() => {
        setTimeLeft(prev => {
          if (prev <= 1) {
            handleNextStep();
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
    } else {
      if (timerRef.current) clearInterval(timerRef.current);
    }
    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [isPaused, timeLeft]);

  // Physical button mapping (Volume keys)
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      // Note: In browser, volume keys might not be catchable easily without specific permissions or in some environments.
      // We map ArrowUp/Down as fallback for testing.
      if (e.key === 'ArrowUp' || e.key === 'AudioVolumeUp') {
        e.preventDefault();
        setIsPaused(prev => !prev);
      }
      if (e.key === 'ArrowDown' || e.key === 'AudioVolumeDown') {
        e.preventDefault();
        handleNextStep();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [activeStepIndex]);

  // Wake Lock
  useEffect(() => {
    const requestWakeLock = async () => {
      try {
        if ('wakeLock' in navigator) {
          wakeLockRef.current = await (navigator as any).wakeLock.request('screen');
        }
      } catch (err) {
        console.warn('WakeLock is disallowed or unsupported in this context:', err);
      }
    };
    requestWakeLock();
    return () => {
      if (wakeLockRef.current) wakeLockRef.current.release();
    };
  }, []);

  const handleNextStep = () => {
    // Play "ding" sound (simulated)
    const audio = new Audio('https://assets.mixkit.co/active_storage/sfx/2869/2869-preview.mp3');
    audio.play().catch(() => {}); // Ignore if blocked by browser

    if (activeStepIndex < recipe.timeline.length - 1) {
      setActiveStepIndex(prev => prev + 1);
    } else {
      onComplete(recipe);
    }
  };

  const formatTime = (seconds: number) => {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  };

  const totalRemaining = recipe.timeline
    .slice(activeStepIndex)
    .reduce((acc, step, i) => acc + (i === 0 ? timeLeft : step.duration), 0);

  return (
    <div className="absolute inset-0 bg-sage-100 text-sage-900 p-6 flex flex-col font-sans select-none overflow-y-auto no-scrollbar">
      {/* Header */}
      <header className="flex justify-between items-start mb-10 border-b border-sage-200 pb-6">
        <div>
          <h1 className="text-3xl font-black tracking-tighter text-sage-900 uppercase leading-none">{recipe.name}</h1>
          <div className="flex items-center gap-2 mt-2">
            <span className="px-2 py-0.5 bg-sage-900 rounded text-[10px] font-bold text-white tracking-widest uppercase">
              {isLazy ? '懒人模式' : '标准模式'}
            </span>
            <span className="text-[10px] font-black text-sage-500 tracking-widest uppercase">
              {recipe.energy_level === 'Low' ? '极简' : recipe.energy_level === 'Mid' ? '正常' : '满满'} 精力
            </span>
          </div>
        </div>
        <div className="text-right">
          <div className="text-3xl font-mono font-black text-sage-900 leading-tight tracking-tighter">{formatTime(totalRemaining)}</div>
          <div className="text-[9px] text-sage-500 font-bold uppercase tracking-widest mt-1">剩余总时长</div>
        </div>
      </header>

      {/* Execution Area */}
      <main className="flex-1 space-y-8 overflow-y-auto pb-32 no-scrollbar">
        {recipe.timeline.map((step, index) => {
          const isActive = index === activeStepIndex;
          const isPast = index < activeStepIndex;

          if (isPast) return null;

          return (
            <motion.div
              key={step.step_id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ 
                opacity: isActive ? 1 : 0.4, 
                scale: isActive ? 1 : 0.95,
                y: 0 
              }}
              transition={{ duration: 0.5 }}
              className={`relative rounded-[40px] overflow-hidden border-l-8 shadow-sm transition-shadow ${
                step.type === 'waiting' ? 'border-sage-400 bg-white' : 'border-sage-900 bg-white'
              } ${isActive ? 'shadow-2xl shadow-sage-900/10 scale-100' : 'scale-95'}`}
            >
              {step.images && step.images.length > 0 && (
                <div className="w-full flex gap-1 overflow-x-auto no-scrollbar bg-sage-50 border-b border-sage-100">
                  {step.images.map((img, imgIdx) => (
                    <motion.div 
                      key={imgIdx}
                      whileHover={{ scale: 1.02 }}
                      className="min-w-[80%] aspect-video relative cursor-zoom-in"
                      onClick={() => setFullScreenImage(img)}
                    >
                      <img src={img} className="w-full h-full object-cover" alt={`Step ${index + 1} img ${imgIdx + 1}`} />
                      <div className="absolute inset-0 bg-black/5 opacity-0 hover:opacity-100 transition-opacity" />
                    </motion.div>
                  ))}
                </div>
              )}
              
              <div className="p-6 relative z-10">
                <div className="space-y-4 mb-6">
                  {/* Top Meta Row: Step Info + Timer */}
                  <div className="flex justify-between items-center gap-4">
                    <div className="flex items-center gap-3">
                      <div className={`w-7 h-7 rounded-full flex items-center justify-center shrink-0 ${step.type === 'waiting' ? 'bg-sage-100 text-sage-500' : 'bg-sage-900 text-white'}`}>
                        {step.type === 'waiting' ? <Timer size={14} /> : <Zap size={14} />}
                      </div>
                      <span className="text-[9px] font-black uppercase tracking-widest text-sage-500 truncate">
                        STEP {index + 1} • {step.type === 'waiting' ? '后台等待' : '手动执行'}
                      </span>
                    </div>
                    
                    {isActive && step.duration > 0 && (
                      <div className="text-3xl font-mono font-black text-sage-900 tabular-nums tracking-tighter shrink-0">
                        {formatTime(timeLeft)}
                      </div>
                    )}
                  </div>

                  {/* Main Instruction Text: Now has full width */}
                  <h2 className={`font-black tracking-tight leading-tight text-sage-900 ${isActive ? 'text-3xl' : 'text-xl'}`}>
                    {isLazy && isActive ? "建议点外卖或备餐" : step.content}
                  </h2>
                </div>

                {/* Sub-tasks */}
                {isActive && step.sub_tasks && step.sub_tasks.length > 0 && (
                  <div className="mt-10 pt-10 border-t border-sage-50 space-y-4">
                    <div className="flex items-center gap-2 text-[10px] font-black text-sage-500 uppercase tracking-widest">
                      <ChevronRight size={14} /> 后续并行任务
                    </div>
                    <div className="grid gap-3">
                      {step.sub_tasks.map((sub, i) => (
                        <div key={i} className="flex items-center justify-between bg-sage-50 p-5 rounded-[24px]">
                          <div className="flex items-center gap-4">
                            <div className="w-1.5 h-1.5 rounded-full bg-sage-900" />
                            <span className="text-xl font-bold text-sage-900">{sub.content}</span>
                          </div>
                          <span className="font-mono text-xl text-sage-500 font-black">{formatTime(sub.duration)}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Progress Bar */}
                {isActive && step.duration > 0 && (
                  <div className="absolute bottom-0 left-0 h-2 bg-sage-50 w-full">
                    <motion.div 
                      initial={{ width: '100%' }}
                      animate={{ width: `${(timeLeft / step.duration) * 100}%` }}
                      transition={{ duration: 1, ease: 'linear' }}
                      className="h-full bg-sage-900"
                    />
                  </div>
                )}
              </div>
            </motion.div>
          );
        })}
      </main>

      {/* Bottom Controls */}
      <footer className="absolute bottom-0 left-0 right-0 p-8 bg-gradient-to-t from-sage-100 via-sage-100/90 to-transparent z-20">
        <div className="max-w-md mx-auto flex flex-col items-center gap-8">
          <div className="flex items-center gap-4 w-full">
            <button 
              onClick={() => setIsPaused(!isPaused)}
              className="flex-1 bg-white border border-sage-200 h-24 rounded-[32px] flex items-center justify-center gap-4 active:scale-95 transition-all text-sage-900 shadow-sm"
            >
              {isPaused ? <Play size={36} fill="currentColor" /> : <Pause size={36} fill="currentColor" />}
              <span className="text-2xl font-black uppercase tracking-tighter">{isPaused ? '继续' : '暂停'}</span>
            </button>
            <button 
              onClick={handleNextStep}
              className="w-28 bg-sage-900 text-white h-24 rounded-[32px] flex items-center justify-center active:scale-95 transition-all shadow-xl shadow-sage-900/20"
            >
              <SkipForward size={36} strokeWidth={3} />
            </button>
          </div>

          <div className="flex gap-8 text-[10px] font-black text-sage-500 tracking-widest uppercase">
            <div className="flex items-center gap-2">
              <span className="bg-white border border-sage-200 px-2 py-1 rounded text-sage-900 shadow-sm">VOL +</span>
              <span>PAUSE / PLAY</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="bg-white border border-sage-200 px-2 py-1 rounded text-sage-900 shadow-sm">VOL -</span>
              <span>NEXT STEP</span>
            </div>
          </div>
        </div>
      </footer>

      {/* Full Screen Image Modal */}
      <AnimatePresence>
        {fullScreenImage && (
          <motion.div 
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-[100] bg-black/95 flex flex-col p-6 items-center justify-center cursor-zoom-out"
            onClick={() => setFullScreenImage(null)}
          >
            <motion.div
              layoutId="fullscreen-img"
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              className="w-full max-w-4xl rounded-2xl overflow-hidden shadow-2xl relative"
            >
               <img src={fullScreenImage} className="w-full h-auto max-h-[80vh] object-contain" alt="Fullscreen preview" />
            </motion.div>
            <div className="mt-8 flex flex-col items-center gap-2">
              <h3 className="text-white text-xl font-black uppercase tracking-tighter">引导视察模式</h3>
              <p className="text-white/40 text-[10px] font-black uppercase tracking-[0.2em]">{recipe.name} • {currentStep?.content.slice(0, 20)}...</p>
            </div>
            <button className="absolute top-8 right-8 text-white/50 hover:text-white transition-colors">
              <X size={32} />
            </button>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Audio Feedback Visualizer (Optional) */}
      <AnimatePresence>
        {timeLeft <= 3 && timeLeft > 0 && !isPaused && (
          <motion.div 
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 pointer-events-none border-[20px] border-yellow-500/20 z-50 animate-pulse"
          />
        )}
      </AnimatePresence>
    </div>
  );
}
