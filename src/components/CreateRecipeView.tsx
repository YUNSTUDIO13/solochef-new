import React, { useState } from 'react';
import { Recipe, TimelineStep, EnergyLevel, SubTask, Material, MaterialCategory } from '../types';
import { Plus, Trash2, Clock, Zap, ChevronDown, ChevronUp, Save, X, ListPlus, Beef, Leaf, Droplets, Sparkles, Image as ImageIcon, Camera } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';

interface CreateRecipeViewProps {
  recipe?: Recipe;
  onSave: (recipe: Recipe) => void;
  onCancel: () => void;
}

export default function CreateRecipeView({ recipe, onSave, onCancel }: CreateRecipeViewProps) {
  const [name, setName] = useState(recipe?.name || '');
  const [energy, setEnergy] = useState<EnergyLevel>(recipe?.energy_level || 'Mid');
  const [isFeatured, setIsFeatured] = useState(recipe?.is_featured || false);
  const [coverImage, setCoverImage] = useState(recipe?.cover_image || '');
  const [bomSnapshot, setBomSnapshot] = useState<string | undefined>(recipe?.bom_snapshot);
  const [materials, setMaterials] = useState<Record<MaterialCategory, Material[]>>(recipe?.materials || {
    meat: [],
    vegetable: [],
    seasoning: []
  });
  const [timeline, setTimeline] = useState<TimelineStep[]>(recipe?.timeline || []);
  const [selectedTags, setSelectedTags] = useState<string[]>(recipe?.tags || []);
  const [customTag, setCustomTag] = useState('');

  const COOKING_PROCESS_TAGS = ['清蒸', '爆炒', '慢炖', '油炸', '煎烤', '凉拌', '红烧', '白灼'];
  const CUISINE_TAGS = ['川菜', '粤菜', '湘菜', '鲁菜', '闽菜', '苏菜', '浙菜', '徽菜', '本帮菜', '东北菜', '西北菜', '云贵菜', '客家菜', '西餐', '日料', '韩料', '东南亚菜', '融合创新', '街头小吃', '深夜食堂', '减脂轻食'];

  const toggleTag = (tag: string) => {
    if (selectedTags.includes(tag)) {
      setSelectedTags(selectedTags.filter(t => t !== tag));
    } else {
      setSelectedTags([...selectedTags, tag]);
    }
  };

  const addCustomTag = () => {
    if (customTag.trim() && !selectedTags.includes(customTag.trim())) {
      setSelectedTags([...selectedTags, customTag.trim()]);
      setCustomTag('');
    }
  };

  const addMaterial = (category: MaterialCategory) => {
    setMaterials({
      ...materials,
      [category]: [...materials[category], { item: '', amount: '', unit: '', is_essential: true }]
    });
  };

  const updateMaterial = (category: MaterialCategory, index: number, updates: Partial<Material>) => {
    const newCategoryItems = [...materials[category]];
    newCategoryItems[index] = { ...newCategoryItems[index], ...updates };
    setMaterials({ ...materials, [category]: newCategoryItems });
  };

  const removeMaterial = (category: MaterialCategory, index: number) => {
    setMaterials({
      ...materials,
      [category]: materials[category].filter((_, i) => i !== index)
    });
  };

  const addCommonSeasonings = () => {
    const common: Material[] = [
      { item: '盐', amount: '适量', unit: '', is_essential: true },
      { item: '油', amount: '适量', unit: '', is_essential: true },
      { item: '生抽', amount: '1', unit: '勺', is_essential: true },
      { item: '糖', amount: '少许', unit: '', is_essential: false }
    ];
    
    // Avoid duplicates
    const existingItems = new Set(materials.seasoning.map(m => m.item));
    const newItems = common.filter(c => !existingItems.has(c.item));
    
    setMaterials({
      ...materials,
      seasoning: [...materials.seasoning, ...newItems]
    });
  };

  const addStep = () => {
    const newStep: TimelineStep = {
      step_id: Date.now(),
      type: 'active',
      content: '',
      duration: 60,
      is_parallel: false,
      sub_tasks: []
    };
    setTimeline([...timeline, newStep]);
  };

  const updateStep = (id: number, updates: Partial<TimelineStep>) => {
    setTimeline(timeline.map(s => s.step_id === id ? { ...s, ...updates } : s));
  };

  const removeStep = (id: number) => {
    setTimeline(timeline.filter(s => s.step_id !== id));
  };

  const addSubTask = (stepId: number) => {
    setTimeline(timeline.map(s => {
      if (s.step_id === stepId) {
        return {
          ...s,
          sub_tasks: [...(s.sub_tasks || []), { content: '', duration: 60 }]
        };
      }
      return s;
    }));
  };

  const updateSubTask = (stepId: number, subIndex: number, updates: Partial<SubTask>) => {
    setTimeline(timeline.map(s => {
      if (s.step_id === stepId && s.sub_tasks) {
        const newSubTasks = [...s.sub_tasks];
        newSubTasks[subIndex] = { ...newSubTasks[subIndex], ...updates };
        return { ...s, sub_tasks: newSubTasks };
      }
      return s;
    }));
  };

  const removeSubTask = (stepId: number, subIndex: number) => {
    setTimeline(timeline.map(s => {
      if (s.step_id === stepId && s.sub_tasks) {
        return {
          ...s,
          sub_tasks: s.sub_tasks.filter((_, i) => i !== subIndex)
        };
      }
      return s;
    }));
  };

  const handleImageUpload = (e: React.ChangeEvent<HTMLInputElement>, callback: (url: string) => void) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onloadend = () => {
        callback(reader.result as string);
      };
      reader.readAsDataURL(file);
    }
  };

  const handleStepImageUpload = (stepId: number, e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onloadend = () => {
        setTimeline(timeline.map(s => {
          if (s.step_id === stepId) {
            return {
              ...s,
              images: [...(s.images || []), reader.result as string]
            };
          }
          return s;
        }));
      };
      reader.readAsDataURL(file);
    }
  };

  const removeStepImage = (stepId: number, imgIndex: number) => {
    setTimeline(timeline.map(s => {
      if (s.step_id === stepId && s.images) {
        return {
          ...s,
          images: s.images.filter((_, i) => i !== imgIndex)
        };
      }
      return s;
    }));
  };

  const handleSave = () => {
    if (!name) return;
    const newRecipe: Recipe = {
      id: recipe?.id || Date.now().toString(),
      name,
      cover_image: coverImage,
      bom_snapshot: bomSnapshot,
      energy_level: energy,
      is_featured: isFeatured,
      materials,
      timeline,
      tags: selectedTags
    };
    onSave(newRecipe);
  };

  return (
    <div className="min-h-screen bg-sage-100 text-sage-800 p-6 flex flex-col pb-32">
      <header className="mb-8 flex justify-between items-center">
        <h1 className="text-2xl font-black tracking-tighter text-sage-900 uppercase">
          {recipe ? '编辑菜谱' : '新建菜谱'}
        </h1>
        <button onClick={onCancel} className="text-sage-500 hover:text-sage-900 transition-colors">
          <X size={24} />
        </button>
      </header>

      <div className="space-y-8 no-scrollbar overflow-y-auto">
        {/* Cover Image Upload */}
        <section>
          <label className="text-[10px] font-black text-sage-500 uppercase tracking-widest mb-4 block">成品封面图 (Required)</label>
          <div className="relative aspect-[16/9] w-full rounded-[40px] border-2 border-dashed border-sage-200 bg-white overflow-hidden flex items-center justify-center group cursor-pointer shadow-sm">
            {coverImage ? (
              <>
                <img src={coverImage} className="absolute inset-0 w-full h-full object-cover opacity-80" alt="Cover" />
                <div className="absolute inset-0 bg-sage-900/20 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
                   <div className="bg-white/90 backdrop-blur-md p-4 rounded-3xl flex items-center gap-2 text-sage-900 font-bold">
                     <Camera size={20} />
                     <span>更换封面</span>
                   </div>
                </div>
              </>
            ) : (
              <div className="flex flex-col items-center gap-2 text-sage-400">
                <ImageIcon size={48} />
                <span className="text-xs font-bold uppercase text-sage-500">点击上传主图</span>
              </div>
            )}
            <input 
              type="file" 
              className="absolute inset-0 opacity-0 cursor-pointer" 
              accept="image/*" 
              onChange={(e) => handleImageUpload(e, (url) => setCoverImage(url))}
            />
          </div>
        </section>

        {/* Basic Info */}
        <section className="space-y-4">
          <div>
            <label className="text-[10px] font-black text-sage-500 uppercase tracking-widest mb-2 block">菜谱名称</label>
            <input 
              type="text" 
              value={name}
              onChange={e => setName(e.target.value)}
              placeholder="例如：红烧肉"
              className="w-full bg-white border border-sage-200 rounded-2xl p-5 text-lg font-black text-sage-900 focus:outline-none focus:border-sage-400 shadow-sm placeholder:text-sage-300"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-[10px] font-black text-sage-500 uppercase tracking-widest mb-2 block">精力等级</label>
              <div className="relative">
                <select 
                  value={energy}
                  onChange={e => setEnergy(e.target.value as EnergyLevel)}
                  className="w-full bg-white border border-sage-200 rounded-2xl p-5 text-sm font-bold text-sage-900 focus:outline-none appearance-none shadow-sm"
                >
                  <option value="Low">极简 (Low)</option>
                  <option value="Mid">正常 (Mid)</option>
                  <option value="High">满满 (High)</option>
                </select>
                <ChevronDown className="absolute right-5 top-1/2 -translate-y-1/2 text-sage-500 pointer-events-none" size={16} />
              </div>
            </div>
            <div>
              <label className="text-[10px] font-black text-sage-500 uppercase tracking-widest mb-2 block">主推菜</label>
              <div className="flex gap-2 bg-white border border-sage-200 rounded-2xl p-2.5 shadow-sm">
                <button
                  onClick={() => setIsFeatured(true)}
                  className={`flex-1 py-2.5 rounded-xl text-xs font-bold transition-all ${isFeatured ? 'bg-sage-800 text-white shadow-md' : 'text-sage-400'}`}
                >
                  是 (Yes)
                </button>
                <button
                  onClick={() => setIsFeatured(false)}
                  className={`flex-1 py-2.5 rounded-xl text-xs font-bold transition-all ${!isFeatured ? 'bg-sage-800 text-white shadow-md' : 'text-sage-400'}`}
                >
                  否 (No)
                </button>
              </div>
            </div>
          </div>

          {/* Classification Tags */}
          <div>
            <label className="text-[10px] font-black text-sage-500 uppercase tracking-widest mb-4 block">分类标签 (Tags)</label>
            <div className="space-y-4 bg-white border border-sage-200 rounded-[32px] p-6 shadow-sm">
              <div className="space-y-4">
                <div>
                  <span className="text-[10px] font-black text-sage-400 uppercase tracking-widest mb-2 block">烹饪工艺</span>
                  <div className="flex flex-wrap gap-2">
                    {COOKING_PROCESS_TAGS.map(tag => (
                      <button
                        key={tag}
                        onClick={() => toggleTag(tag)}
                        className={`px-4 py-2 rounded-full text-xs font-bold transition-all ${
                          selectedTags.includes(tag) 
                            ? 'bg-sage-800 text-white shadow-lg shadow-sage-900/10' 
                            : 'bg-sage-50 text-sage-500 hover:bg-sage-100'
                        }`}
                      >
                        {tag}
                      </button>
                    ))}
                  </div>
                </div>

                <div>
                  <span className="text-[10px] font-black text-sage-400 uppercase tracking-widest mb-2 block">菜系维度</span>
                  <div className="flex flex-wrap gap-2">
                    {CUISINE_TAGS.map(tag => (
                      <button
                        key={tag}
                        onClick={() => toggleTag(tag)}
                        className={`px-4 py-2 rounded-full text-xs font-bold transition-all ${
                          selectedTags.includes(tag) 
                            ? 'bg-sage-800 text-white shadow-lg shadow-sage-900/10' 
                            : 'bg-sage-50 text-sage-500 hover:bg-sage-100'
                        }`}
                      >
                        {tag}
                      </button>
                    ))}
                  </div>
                </div>

                <div className="pt-2">
                  <span className="text-[10px] font-black text-sage-400 uppercase tracking-widest mb-2 block">自选 / 其它</span>
                  <div className="flex gap-2">
                    <input
                      type="text"
                      value={customTag}
                      onChange={e => setCustomTag(e.target.value)}
                      onKeyDown={e => e.key === 'Enter' && addCustomTag()}
                      placeholder="输入自定义标签..."
                      className="flex-1 bg-sage-50 border border-sage-100 rounded-xl px-4 py-2 text-xs font-bold text-sage-900 focus:outline-none"
                    />
                    <button
                      onClick={addCustomTag}
                      className="bg-sage-800 text-white px-4 rounded-xl text-xs font-bold"
                    >
                      添加
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* 物料清单 (BOM) */}
        <section className="mb-10">
          <div className="flex justify-between items-center mb-6">
            <h2 className="text-sage-900 font-black uppercase tracking-tighter text-sm">物料清单 (BOM)</h2>
            <button 
              onClick={addCommonSeasonings}
              className="px-3 py-1.5 bg-sage-800 rounded-xl text-white text-[10px] font-black uppercase tracking-widest active:scale-95 transition-all shadow-md shadow-sage-900/10"
            >
              一键调味
            </button>
          </div>

          <div className="space-y-8">
            <div className="bg-white border border-sage-200 rounded-[40px] p-8 shadow-sm group">
              <div className="flex justify-between items-center mb-6">
                <div>
                  <label className="text-[10px] font-black text-sage-900 uppercase tracking-widest block">食材全家福 (BOM Snapshot)</label>
                  <p className="text-[10px] text-sage-400 mt-1 font-bold">拍摄已准备好的所有食材，方便后续核对</p>
                </div>
                {bomSnapshot && (
                  <button 
                    onClick={() => setBomSnapshot(undefined)}
                    className="text-red-500 hover:bg-red-50 p-2 rounded-xl transition-colors"
                  >
                    <Trash2 size={18} />
                  </button>
                )}
              </div>
              
              <div className="relative aspect-video w-full rounded-3xl border-2 border-dashed border-sage-100 bg-sage-50 overflow-hidden flex items-center justify-center transition-all group-hover:border-sage-300">
                {bomSnapshot ? (
                  <img src={bomSnapshot} className="absolute inset-0 w-full h-full object-cover" alt="BOM Snapshot" />
                ) : (
                  <div className="flex flex-col items-center gap-3 text-sage-300 group-hover:text-sage-500 transition-colors">
                    <div className="w-16 h-16 bg-white rounded-full flex items-center justify-center shadow-sm">
                      <Camera size={32} />
                    </div>
                    <span className="text-[10px] font-black uppercase tracking-widest">点击拍摄食材全拼</span>
                  </div>
                )}
                <input 
                  type="file" 
                  className="absolute inset-0 opacity-0 cursor-pointer" 
                  accept="image/*" 
                  onChange={(e) => handleImageUpload(e, (url) => setBomSnapshot(url))}
                />
              </div>
            </div>

            {(['meat', 'vegetable', 'seasoning'] as MaterialCategory[]).map(cat => (
              <div key={cat} className="space-y-4">
                <div className="flex justify-between items-center px-1">
                  <div className="flex items-center gap-2 text-sage-500">
                    {cat === 'meat' && <Beef size={14} />}
                    {cat === 'vegetable' && <Leaf size={14} />}
                    {cat === 'seasoning' && <Droplets size={14} />}
                    <span className="text-[10px] font-black uppercase tracking-widest text-sage-500">
                      {cat === 'meat' ? '肉禽 / 水产' : cat === 'vegetable' ? '蔬菜 / 蔬果' : '调味 / 其它'}
                    </span>
                  </div>
                  <button 
                    onClick={() => addMaterial(cat)}
                    className="text-sage-800 flex items-center gap-1 text-[10px] font-black uppercase tracking-widest"
                  >
                    <Plus size={12}/> 添加
                  </button>
                </div>

                <div className="space-y-3">
                  {materials[cat].map((m, idx) => (
                    <div key={idx} className="bg-white border border-sage-200 rounded-[28px] p-2 pr-4 shadow-sm group/item">
                      <div className="flex items-center gap-3">
                        <input 
                          type="text" 
                          placeholder="食材" 
                          value={m.item}
                          onChange={e => updateMaterial(cat, idx, { item: e.target.value })}
                          className="bg-transparent flex-1 min-w-0 text-sm font-bold outline-none text-sage-900 pl-4" 
                        />
                        <div className="flex items-center bg-sage-50 p-1.5 rounded-2xl gap-2">
                          <input 
                            type="text" 
                            placeholder="量" 
                            value={m.amount}
                            onChange={e => updateMaterial(cat, idx, { amount: e.target.value })}
                            className="bg-transparent w-12 text-center text-xs font-bold text-sage-900 placeholder:text-sage-300" 
                          />
                          <input 
                            type="text" 
                            placeholder="单" 
                            value={m.unit}
                            onChange={e => updateMaterial(cat, idx, { unit: e.target.value })}
                            className="bg-sage-200 w-10 text-center text-[10px] font-black rounded-lg py-1 text-sage-600 uppercase placeholder:text-sage-400" 
                          />
                        </div>
                        <button 
                          onClick={() => removeMaterial(cat, idx)}
                          className="w-10 h-10 flex-shrink-0 flex items-center justify-center text-red-400 hover:bg-red-50 rounded-2xl transition-all"
                          title="删除物料"
                        >
                          <Trash2 size={18} />
                        </button>
                      </div>
                    </div>
                  ))}
                  {materials[cat].length === 0 && (
                    <div className="py-4 text-center text-sage-400 text-[10px] font-black uppercase tracking-widest border border-dashed border-sage-200 rounded-[28px]">
                      无
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        </section>

        {/* Timeline Builder */}
        <section className="space-y-6">
          <div className="flex justify-between items-center mb-4">
            <h2 className="text-sm font-black text-sage-900 uppercase tracking-tighter">烹饪步骤 (Timeline)</h2>
            <button 
              onClick={addStep}
              className="flex items-center gap-2 text-[10px] font-black text-white uppercase tracking-widest bg-sage-800 px-4 py-2 rounded-2xl shadow-lg shadow-sage-900/10"
            >
              <Plus size={14} /> 添加步骤
            </button>
          </div>

          <div className="space-y-6">
            {timeline.map((step, index) => (
              <div key={step.step_id} className="bg-white border border-sage-200 rounded-[40px] p-8 space-y-6 relative overflow-hidden shadow-sm">
                <div className="flex justify-between items-center">
                  <div className="flex items-center gap-4">
                    <span className="text-xs font-black text-sage-400 uppercase tracking-widest">#{index + 1}</span>
                    <div className="flex bg-sage-50 p-1.5 rounded-2xl">
                      <button 
                        onClick={() => updateStep(step.step_id, { type: 'active' })}
                        className={`px-4 py-2 rounded-xl text-[10px] font-black uppercase tracking-widest transition-all flex items-center gap-2 ${
                          step.type === 'active' ? 'bg-sage-800 text-white' : 'text-sage-500'
                        }`}
                      >
                        <Zap size={14} /> 主动
                      </button>
                      <button 
                        onClick={() => updateStep(step.step_id, { type: 'waiting' })}
                        className={`px-4 py-2 rounded-xl text-[10px] font-black uppercase tracking-widest transition-all flex items-center gap-2 ${
                          step.type === 'waiting' ? 'bg-sage-800 text-white' : 'text-sage-500'
                        }`}
                      >
                        <Clock size={14} /> 等待
                      </button>
                    </div>
                  </div>
                  <button onClick={() => removeStep(step.step_id)} className="text-sage-300 hover:text-red-500 transition-colors">
                    <Trash2 size={20} />
                  </button>
                </div>

                <div className="flex gap-4">
                  <div className="flex-1">
                    <textarea 
                      value={step.content}
                      onChange={e => updateStep(step.step_id, { content: e.target.value })}
                      placeholder="步骤描述..."
                      className="w-full bg-sage-50 border border-sage-100 rounded-2xl p-4 text-sm font-bold text-sage-900 focus:outline-none placeholder:text-sage-300 resize-y min-h-[48px]"
                    />
                  </div>
                  <div className="w-24">
                    <input 
                      type="number" 
                      value={step.duration / 60}
                      onChange={e => updateStep(step.step_id, { duration: parseInt(e.target.value) * 60 })}
                      placeholder="Min"
                      className="w-full bg-sage-50 border border-sage-100 rounded-2xl p-4 text-sm font-black text-center text-sage-900 focus:outline-none placeholder:text-sage-400"
                    />
                  </div>
                </div>

                {/* Step Images Upload */}
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <span className="text-[10px] font-black text-sage-400 uppercase tracking-widest flex items-center gap-2">
                       <ImageIcon size={12} /> 步骤图解 (Instructional Media)
                    </span>
                  </div>
                  
                  <div className="flex gap-4 overflow-x-auto pb-2 no-scrollbar">
                    {/* Persistent Upload Placeholder */}
                    <div className="relative min-w-[120px] h-24 rounded-2xl border-2 border-dashed border-sage-100 bg-sage-50 flex flex-col items-center justify-center gap-1 text-sage-300 hover:text-sage-500 transition-all cursor-pointer">
                      <Camera size={24} />
                      <span className="text-[8px] font-black uppercase tracking-wider">上传图解</span>
                      <input 
                        type="file" 
                        className="absolute inset-0 opacity-0 cursor-pointer" 
                        accept="image/*"
                        onChange={(e) => handleStepImageUpload(step.step_id, e)}
                      />
                    </div>

                    {step.images && step.images.length > 0 && step.images.map((img, imgIdx) => (
                      <div key={imgIdx} className="relative min-w-[120px] h-24 rounded-2xl overflow-hidden border border-sage-100 group shadow-sm flex-shrink-0">
                        <img src={img} className="w-full h-full object-cover" alt={`Step ${index + 1} img ${imgIdx + 1}`} />
                        <button 
                          onClick={() => removeStepImage(step.step_id, imgIdx)}
                          className="absolute top-2 right-2 p-1.5 bg-black/50 text-white rounded-xl active:scale-95 transition-all"
                        >
                          <X size={12} strokeWidth={3} />
                        </button>
                      </div>
                    ))}
                  </div>
                </div>

                {step.type === 'waiting' && (
                  <div className="pt-6 border-t border-sage-50 space-y-4">
                    <div className="flex justify-between items-center">
                      <span className="text-[10px] font-black text-sage-500 uppercase tracking-widest flex items-center gap-2">
                        <ListPlus size={12} /> 并行子任务
                      </span>
                      <button 
                        onClick={() => addSubTask(step.step_id)}
                        className="text-[10px] font-black text-sage-800 uppercase tracking-widest"
                      >
                        + 添入
                      </button>
                    </div>
                    
                    <div className="space-y-2">
                      {step.sub_tasks?.map((sub, subIndex) => (
                        <div key={subIndex} className="flex gap-3 items-start bg-sage-50/50 p-2 rounded-2xl">
                          <textarea 
                            value={sub.content}
                            onChange={e => updateSubTask(step.step_id, subIndex, { content: e.target.value })}
                            placeholder="子任务..."
                            className="flex-1 bg-transparent text-xs font-bold text-sage-900 px-2 outline-none placeholder:text-sage-300 resize-y min-h-[32px] py-1"
                          />
                          <div className="flex items-center gap-2 mt-1">
                            <input 
                              type="number" 
                              value={sub.duration / 60}
                              onChange={e => updateSubTask(step.step_id, subIndex, { duration: parseInt(e.target.value) * 60 })}
                              className="w-12 bg-white border border-sage-100 rounded-xl py-1.5 text-[10px] font-black text-center text-sage-900"
                            />
                            <button onClick={() => removeSubTask(step.step_id, subIndex)} className="text-sage-300 hover:text-red-500 pr-1">
                              <Trash2 size={14} />
                            </button>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            ))}
          </div>
        </section>
      </div>

      {/* Save Button */}
      <footer className="absolute bottom-0 left-0 right-0 p-8 bg-gradient-to-t from-sage-100 via-sage-100 to-transparent z-10">
        <button 
          onClick={handleSave}
          disabled={!name}
          className="w-full max-w-md mx-auto h-20 bg-black text-white rounded-[32px] flex items-center justify-center gap-3 font-black text-xl active:scale-95 transition-all shadow-2xl shadow-black/20 disabled:opacity-20 translate-y-[-10%]"
        >
          <Save size={28} strokeWidth={3} />
          <span>保存发布菜谱</span>
        </button>
      </footer>
    </div>
  );
}
