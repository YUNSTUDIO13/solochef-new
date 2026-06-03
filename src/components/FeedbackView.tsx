import React, { useState, useEffect } from 'react';
import { Recipe, OrderBatch } from '../types';
import { Printer, Heart, Receipt } from 'lucide-react';
import { motion } from 'motion/react';

interface FeedbackViewProps {
  recipe: Recipe;
  activeBatch?: OrderBatch | null;
  onDone: () => void;
}

const sentences = [
  "关火，出锅。趁热尝第一口，是对主厨最好的犒劳。好好享受这一餐吧！",
  "香气已经满屋啦！挑一个你最喜欢的盘子盛出来，拍照记录，然后——开动吧。",
  "辛苦啦！今天也把生活照顾得很好。接下来的时间，只属于你和美味。",
  "看，你又解锁了一道新美味。把期待装盘，对自己说一声：开饭啦！",
  "把锅碗瓢盆先留给解冻的时间吧。现在的任务只有一个：趁热，开动！",
  "厨房的烟火气慢慢散了，餐桌上的美味正当热。洗手，坐下，世界先等等，吃饭第一。"
];

export default function FeedbackView({ recipe, activeBatch, onDone }: FeedbackViewProps) {
  // Select a random sentence once upon mount
  const [sentence] = useState(() => {
    const randomIndex = Math.floor(Math.random() * sentences.length);
    return sentences[randomIndex];
  });

  // Calculate ticket number sequence (starting at 0001, increments on each done)
  const [ticketNo, setTicketNo] = useState("0001");
  useEffect(() => {
    const stored = localStorage.getItem('solochef_ticket_counter');
    let nextVal = 1;
    if (stored) {
      const parsed = parseInt(stored, 10);
      if (!isNaN(parsed)) {
        nextVal = parsed + 1;
      }
    }
    localStorage.setItem('solochef_ticket_counter', String(nextVal));
    setTicketNo(String(nextVal).padStart(4, '0'));
  }, []);

  // Generate random price state (Integer e.g. 18 to 118) to avoid re-render shifting
  const [randomPrice] = useState(() => {
    const min = 18;
    const max = 118;
    return Math.floor(Math.random() * (max - min + 1)) + min;
  });

  // Calculate dynamic quantity
  // Direct cooking = 1. Active batch order = occurrences in the batch
  const qty = activeBatch && activeBatch.recipeIds 
    ? activeBatch.recipeIds.filter(id => id === recipe.id).length || 1 
    : 1;

  const itemTotal = randomPrice * qty;

  // Format planning time or current time to YYYY/MM/DD HH:mm:ss (precision to seconds)
  const getFormattedDateTime = () => {
    const rawTime = activeBatch?.created_at ? new Date(activeBatch.created_at) : new Date();
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${rawTime.getFullYear()}/${pad(rawTime.getMonth() + 1)}/${pad(rawTime.getDate())} ${pad(rawTime.getHours())}:${pad(rawTime.getMinutes())}:${pad(rawTime.getSeconds())}`;
  };

  return (
    <div className="absolute inset-0 bg-sage-100 text-stone-850 flex flex-col font-sans overflow-hidden">
      
      {/* Scrollable Receipt Area */}
      <div className="flex-1 overflow-y-auto no-scrollbar p-6 pb-28 flex flex-col items-center">
        
        {/* Decorative Tape decoration on top of the receipt sheet */}
        <div className="w-full max-w-sm flex justify-center -mb-2 z-10 opacity-80 pointer-events-none select-none">
          <div className="w-28 h-6 bg-white/40 backdrop-blur-[1px] border border-stone-200/20 shadow-sm" />
        </div>

        {/* The Thermal Receipt Paper Sheet */}
        <motion.div 
          initial={{ opacity: 0, y: 30, scale: 0.98 }}
          animate={{ opacity: 1, y: 0, scale: 1 }}
          transition={{ type: "spring", stiffness: 80, damping: 14 }}
          className="w-full max-w-md bg-[#faf9f4] border border-stone-300/40 shadow-[0_15px_40px_rgba(40,48,40,0.08),_0_5px_15px_rgba(40,48,40,0.03)] px-6 py-8 relative rounded-[32px] overflow-hidden select-none"
        >
          {/* Jagged / Decorative Paper Tearing Pattern simulation at top & bottom margins */}
          <div className="absolute top-0 inset-x-0 h-1 flex justify-between overflow-hidden opacity-10">
            {Array.from({ length: 40 }).map((_, i) => (
              <div key={i} className="w-2 h-2 bg-stone-900 rotate-45 -translate-y-1 transform shrink-0" />
            ))}
          </div>

          <div className="absolute bottom-0 inset-x-0 h-1 flex justify-between overflow-hidden opacity-10">
            {Array.from({ length: 40 }).map((_, i) => (
              <div key={i} className="w-2 h-2 bg-stone-900 rotate-45 translate-y-1 transform shrink-0" />
            ))}
          </div>

          {/* 1. Header Information */}
          <div className="text-center pb-5 border-b border-dashed border-stone-300">
            <h1 className="text-2xl font-black tracking-widest text-stone-900 uppercase">solochef</h1>
            <p className="text-[11px] font-bold text-stone-500 mt-1">独厨（蓝星旗舰店）</p>
          </div>

          {/* 2. Service type block: "堂食" (Dine-in) with elegant spacing */}
          <div className="flex flex-col items-center py-5">
            <div className="border border-stone-900 px-7 py-1 text-sm font-extrabold tracking-widest text-stone-900 uppercase">
              堂食
            </div>
          </div>

          {/* 3. High-visibility Ticket Numbers */}
          <div className="text-center pb-5 border-b border-dashed border-stone-300">
            <h2 className="text-5xl font-mono font-black text-stone-950 tracking-tighter mb-2">
              #{ticketNo}
            </h2>
            <p className="text-[11px] font-mono font-black text-stone-400 uppercase tracking-widest">取票号</p>
          </div>

          {/* 4. Delivery note and timestamps section */}
          <div className="py-5 text-left border-b border-dashed border-stone-300 space-y-2">
            <span className="text-[10px] font-mono font-black text-stone-400 uppercase tracking-widest block">Delivery note:</span>
            <div className="space-y-1 text-xs">
              <p className="font-mono font-bold text-stone-850">
                DELIVERY {getFormattedDateTime()}
              </p>
              <p className="font-mono text-stone-500">
                OMS 3423423023230
              </p>
            </div>
          </div>

          {/* 6. Product table: Qty, Name, price, aggregate totals */}
          <div className="py-5 border-b border-dashed border-stone-300">
            {/* Table headers */}
            <div className="flex justify-between items-center text-[10px] font-mono font-black tracking-widest text-stone-400 pb-2">
              <div className="w-12 text-left">数量</div>
              <div className="flex-1 text-left px-2">商品名称</div>
              <div className="w-24 text-right">金额 ($)</div>
            </div>

            {/* Main Item Row */}
            <div className="flex justify-between items-center text-xs font-mono font-bold py-2.5 text-stone-900">
              <div className="w-12 text-left">{qty}x</div>
              <div className="flex-1 text-left px-2 font-sans font-black text-stone-850 truncate">
                {recipe.name}
              </div>
              <div className="w-24 text-right">{randomPrice.toFixed(2)}</div>
            </div>

            {/* Summary details */}
            <div className="mt-5 pt-4 border-t border-dotted border-stone-300/80 space-y-1.5 text-xs font-mono">
              <div className="flex justify-between items-center text-stone-500">
                <span>餐点总价</span>
                <span>{itemTotal.toFixed(2)}</span>
              </div>
              <div className="flex justify-between items-center text-stone-500">
                <span>配售税 (0.0%)</span>
                <span>0.00</span>
              </div>
              <div className="flex justify-between items-center text-stone-500">
                <span>环保打包费</span>
                <span>0.00</span>
              </div>
            </div>
          </div>

          {/* 7. Grand total pay amount */}
          <div className="py-5 flex justify-between items-center border-b border-dashed border-stone-300">
            <span className="text-sm font-bold text-stone-900">顾客实付</span>
            <span className="text-xl font-mono font-black text-stone-950">${itemTotal.toFixed(2)}</span>
          </div>

          {/* 8. Cozy random warm quote element styled elegantly in handwriting script */}
          <div className="pt-6 pb-2 text-center flex flex-col items-center">
            <div className="inline-flex items-center gap-1.5 opacity-40 mb-3.5">
              <Heart size={10} className="text-red-400 fill-red-400" />
              <span className="text-[9px] font-mono tracking-widest font-black uppercase text-stone-600">chef's memo / 温馨寄语</span>
            </div>
            
            <p className="font-handwriting text-lg md:text-xl text-stone-850 leading-relaxed font-bold tracking-wide max-w-sm italic px-4">
              “{sentence}”
            </p>

            <span className="text-[9px] font-mono text-stone-400 tracking-[0.2em] font-black uppercase mt-6 block">
              ✦ THANK YOU FOR DINING WITH US ✦
            </span>
            <p className="text-[8px] font-mono text-stone-300 uppercase tracking-widest mt-1">Made in solochef smart kitchen ecosystem</p>
          </div>

        </motion.div>
      </div>

      {/* Primary Action Button: "打印小票" */}
      <footer className="absolute bottom-0 left-0 right-0 p-6 bg-gradient-to-t from-sage-100 via-sage-100 to-transparent z-10 flex justify-center">
        <button 
          onClick={onDone}
          className="w-full max-w-md h-18 bg-sage-800 text-white rounded-[28px] flex items-center justify-center gap-3 font-black text-lg hover:bg-sage-900 active:scale-95 transition-all shadow-xl shadow-sage-900/20"
        >
          <Printer size={22} strokeWidth={2.5} />
          <span>打印小票</span>
        </button>
      </footer>

    </div>
  );
}
