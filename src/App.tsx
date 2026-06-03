import React, { useState, useEffect } from 'react';
import SelectionView from './components/SelectionView';
import ExecutionView from './components/ExecutionView';
import FeedbackView from './components/FeedbackView';
import LibraryView from './components/LibraryView';
import CreateRecipeView from './components/CreateRecipeView';
import PickingView from './components/PickingView';
import AnalyticsView from './components/AnalyticsView';
import DashboardView from './components/DashboardView';
import OrderEngineView from './components/OrderEngineView';
import BatchDetailView from './components/BatchDetailView';
import RecipeDetailView from './components/RecipeDetailView';
import { Recipe, UserStats, OrderBatch, BatchStatus, CookingHistoryEntry } from './types';
import { MOCK_RECIPES } from './mockData';
import { LayoutGrid, Search, Plus, BarChart3, Settings, ShoppingBag, ChevronRight, ClipboardList, Clock, Save, FileText } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { DBService } from './services/db';
import { ExportService } from './services/exportService';

type ViewState = 'login' | 'dashboard' | 'picking' | 'execution' | 'feedback' | 'library' | 'create-recipe' | 'settings' | 'analytics' | 'order-engine' | 'batch-detail' | 'recipe-detail';

export default function App() {
  const [isLoggedIn, setIsLoggedIn] = useState(true);
  const [view, setView] = useState<ViewState>('dashboard');
  const [recipes, setRecipes] = useState<Recipe[]>([]);
  const [cookingHistory, setCookingHistory] = useState<CookingHistoryEntry[]>([]);
  const [selectedRecipe, setSelectedRecipe] = useState<Recipe | null>(null);
  const [selectedHistoryEntry, setSelectedHistoryEntry] = useState<CookingHistoryEntry | null>(null);
  const [isLazy, setIsLazy] = useState(false);
  const [activeBatch, setActiveBatch] = useState<OrderBatch | null>(null);
  const [userStats, setUserStats] = useState<UserStats>(() => {
    const saved = localStorage.getItem('solochef_stats');
    if (saved) return JSON.parse(saved);
    return { streak: 0, last_ignition: new Date().toISOString() }; 
  });

  const [showConflictDialog, setShowConflictDialog] = useState<{ recipeIds: string[] } | null>(null);
  const [isSyncing, setIsSyncing] = useState(false);
  const [isFinishingAllBatch, setIsFinishingAllBatch] = useState(false);

  // Initial Load from DB
  useEffect(() => {
    const initData = async () => {
      const savedRecipes = await DBService.getAllRecipes();
      const savedHistory = await DBService.getAllHistory();
      const savedBatch = await DBService.getActiveBatch();
      const savedStats = await DBService.getStats();

      if (savedRecipes.length > 0) {
        setRecipes(savedRecipes);
      } else {
        // First time load with mocks
        setRecipes(MOCK_RECIPES);
        MOCK_RECIPES.forEach(r => DBService.saveRecipe(r));
      }

      if (savedHistory) setCookingHistory(savedHistory.sort((a, b) => new Date(b.finishedAt).getTime() - new Date(a.finishedAt).getTime()));
      if (savedBatch) setActiveBatch(savedBatch);
      if (savedStats) setUserStats(savedStats);
    };

    initData();
  }, []);

  const recordToHistory = async (recipe: Recipe, orderId: string) => {
    // SoloChef Order module feature removed. No longer saving or adding history entries.
  };

  // Auth logic removed as requested

  const handleConfirmOrder = async (recipeIds: string[]) => {
    // 1. If no active batch, just create one
    if (!activeBatch) {
      const newBatch: OrderBatch = {
        id: Date.now().toString(),
        status: 'picking',
        recipeIds: recipeIds,
        completedRecipeIds: [], 
        created_at: new Date().toISOString()
      };
      setActiveBatch(newBatch);
      await DBService.saveActiveBatch(newBatch);
      setView('batch-detail');
      return;
    }

    // 2. If status is picking, Merge
    if (activeBatch.status === 'picking') {
      const mergedRecipeIds = Array.from(new Set([...activeBatch.recipeIds, ...recipeIds]));
      const updatedBatch = {
        ...activeBatch,
        recipeIds: mergedRecipeIds
      };
      setActiveBatch(updatedBatch);
      await DBService.saveActiveBatch(updatedBatch);
      setView('batch-detail');
      return;
    }

    // 3. If status is processing or later, Conflict
    if (activeBatch.status === 'processing') {
      setShowConflictDialog({ recipeIds });
    }
  };

  const handleOverrideBatch = async (recipeIds: string[]) => {
    const newBatch: OrderBatch = {
      id: Date.now().toString(),
      status: 'picking',
      recipeIds: recipeIds,
      completedRecipeIds: [], 
      created_at: new Date().toISOString()
    };
    setActiveBatch(newBatch);
    await DBService.saveActiveBatch(newBatch);
    setShowConflictDialog(null);
    setView('batch-detail');
  };

  const updateBatchStatus = async (id: string, status: BatchStatus) => {
    if (activeBatch && activeBatch.id === id) {
      if (status === 'finished') {
        const finishedRecipeIds = activeBatch.recipeIds;
        setRecipes(prev => {
          const updated = prev.map(r => 
            finishedRecipeIds.includes(r.id) 
              ? { ...r, last_cooked_at: r.last_cooked_at || new Date().toISOString() } 
              : r
          );
          // Sync all updated recipes to DB
          updated.forEach(r => {
             if (finishedRecipeIds.includes(r.id)) DBService.saveRecipe(r);
          });
          return updated;
        });

        setActiveBatch(null);
        await DBService.clearActiveBatch();
        setView('dashboard');
        ignite(); // Trigger session ignition on batch completion
      } else {
        const updated = { ...activeBatch, status };
        setActiveBatch(updated);
        await DBService.saveActiveBatch(updated);
      }
    }
  };

  const completeRecipeInBatch = async (rid: string) => {
    if (!activeBatch) return;
    const updatedCompleted = [...activeBatch.completedRecipeIds, rid];

    // Check if all are done
    if (updatedCompleted.length === activeBatch.recipeIds.length) {
      await updateBatchStatus(activeBatch.id, 'finished');
    } else {
      const updated = {
        ...activeBatch,
        completedRecipeIds: updatedCompleted
      };
      setActiveBatch(updated);
      await DBService.saveActiveBatch(updated);
      setView('dashboard');
    }
    ignite();
  };

  const ignite = async () => {
    const last = new Date(userStats.last_ignition).getTime();
    const now = Date.now();
    const diffHours = (now - last) / (1000 * 60 * 60);

    let newStreak = userStats.streak;
    // If it's more than 48 hours, streak is lost, start at 1
    if (diffHours >= 48) {
      newStreak = 1;
    } 
    // If it's been more than 16 hours since last (preventing multi-clicks same day but allowing next day)
    // and less than 48 hours, then increment streak
    else if (diffHours >= 16 && diffHours < 48) {
      newStreak += 1;
    }
    // If less than 16 hours, we just update the time but keep streak

    const updatedStats = {
      streak: newStreak,
      last_ignition: new Date().toISOString()
    };
    setUserStats(updatedStats);
    await DBService.saveStats(updatedStats);
  };

  useEffect(() => {
    localStorage.setItem('solochef_stats', JSON.stringify(userStats));
  }, [userStats]);

  useEffect(() => {
    localStorage.setItem('solochef_history', JSON.stringify(cookingHistory));
  }, [cookingHistory]);

  const handleSelect = (recipe: Recipe, lazy: boolean) => {
    setSelectedRecipe(recipe);
    setIsLazy(lazy);
    setView('picking');
  };

  const handlePickingConfirm = () => {
    setView('execution');
  };

  const handleExecutionComplete = (recipe: Recipe) => {
    // Record completion time immediately for the recipe
    const now = new Date().toISOString();
    setRecipes(prev => prev.map(r => 
      r.id === recipe.id ? { ...r, last_cooked_at: now } : r
    ));

    setView('feedback');
  };

  const handleFeedbackDone = () => {
    if (isFinishingAllBatch && activeBatch) {
      updateBatchStatus(activeBatch.id, 'finished');
      setIsFinishingAllBatch(false);
    } else if (selectedRecipe && activeBatch && activeBatch.recipeIds.includes(selectedRecipe.id)) {
      completeRecipeInBatch(selectedRecipe.id);
    } else {
      setView('dashboard');
    }
    setSelectedRecipe(null);
  };

  const handleSaveRecipe = async (recipe: Recipe) => {
    ignite();
    const recipeWithUpdate = { ...recipe, updated_at: new Date().toISOString() };
    setRecipes(prev => {
      const exists = prev.find(r => r.id === recipe.id);
      if (exists) {
        return prev.map(r => r.id === recipe.id ? recipeWithUpdate : r);
      }
      return [recipeWithUpdate, ...prev];
    });
    await DBService.saveRecipe(recipeWithUpdate);
    setSelectedRecipe(null);
    setView('library');
  };

  const handleEditRecipe = (recipe: Recipe) => {
    setSelectedRecipe(recipe);
    setView('create-recipe');
  };

  const handleDeleteRecipe = async (id: string) => {
    setRecipes(prev => prev.filter(r => r.id !== id));
    await DBService.deleteRecipe(id);
  };

  const handleSyncToLocal = async () => {
    setIsSyncing(true);
    try {
      const content = ExportService.formatToMarkdown(recipes, cookingHistory, userStats);
      await ExportService.syncToLocalFile(content);
    } catch (e) {
      console.error(e);
    } finally {
      setTimeout(() => setIsSyncing(false), 800);
    }
  };

  const handleImportFromLocal = async () => {
    setIsSyncing(true);
    try {
      const data = await ExportService.importFromFile();
      if (!data) return;

      const { recipes: importedRecipes, history: importedHistory, stats: importedStats } = data;

      // 1. Merge Recipes
      setRecipes(prev => {
        const merged = [...prev];
        importedRecipes.forEach(ir => {
          const idx = merged.findIndex(r => r.id === ir.id);
          if (idx > -1) {
            const existing = merged[idx];
            // Only overwrite if imported is newer or existing has no timestamp
            const existingTime = existing.updated_at ? new Date(existing.updated_at).getTime() : 0;
            const importedTime = ir.updated_at ? new Date(ir.updated_at).getTime() : 0;
            
            if (importedTime > existingTime) {
              merged[idx] = ir;
              DBService.saveRecipe(ir);
            }
          } else {
            merged.unshift(ir);
            DBService.saveRecipe(ir);
          }
        });
        return merged;
      });

      // 2. Merge History
      setCookingHistory(prev => {
        const merged = [...prev];
        importedHistory.forEach(ih => {
          const idx = merged.findIndex(h => h.id === ih.id);
          if (idx === -1) {
            merged.push(ih);
            DBService.addHistoryEntry(ih);
          }
        });
        return merged.sort((a, b) => new Date(b.finishedAt).getTime() - new Date(a.finishedAt).getTime());
      });

      // 3. Stats (Take most advanced streak or latest?)
      if (importedStats.streak >= userStats.streak) {
        setUserStats(importedStats);
        DBService.saveStats(importedStats);
      }

      alert(`数据恢复成功：已合并 ${importedRecipes.length} 条菜谱和 ${importedHistory.length} 条历史纪录。`);
    } catch (e) {
      console.error(e);
      alert('导入失败，请检查文件格式');
    } finally {
      setIsSyncing(false);
    }
  };

  const isMainView = ['dashboard', 'library', 'analytics', 'settings'].includes(view);

  return (
    <div className="max-w-md mx-auto min-h-screen bg-sage-100 shadow-2xl overflow-hidden relative flex flex-col font-sans">
      {/* Conflict Dialog */}
      <AnimatePresence>
        {showConflictDialog && (
          <div className="fixed inset-0 z-[200] flex items-center justify-center p-6">
            <motion.div 
              initial={{ opacity: 0 }} 
              animate={{ opacity: 1 }} 
              exit={{ opacity: 0 }}
              onClick={() => setShowConflictDialog(null)}
              className="absolute inset-0 bg-sage-900/60 backdrop-blur-sm"
            />
            <motion.div 
              initial={{ scale: 0.9, opacity: 0, y: 20 }}
              animate={{ scale: 1, opacity: 1, y: 0 }}
              exit={{ scale: 0.9, opacity: 0, y: 20 }}
              className="bg-white rounded-[40px] p-8 w-full max-w-xs relative z-10 shadow-2xl border border-sage-200"
            >
              <div className="w-16 h-16 bg-amber-50 rounded-2xl flex items-center justify-center text-amber-500 mb-6">
                <ShoppingBag size={32} />
              </div>
              <h2 className="text-xl font-black text-sage-900 tracking-tight leading-tight">发现冲突计划</h2>
              <p className="mt-2 text-sm text-sage-500 font-medium leading-relaxed">当前已有正在履约中的独厨订单，请确认是否覆盖现有计划？</p>
              
              <div className="mt-8 space-y-3">
                <button 
                  onClick={() => handleOverrideBatch(showConflictDialog.recipeIds)}
                  className="w-full bg-sage-900 text-white py-4 rounded-2xl text-[11px] font-black uppercase tracking-widest active:scale-95 transition-all shadow-lg"
                >
                  确认覆盖
                </button>
                <button 
                  onClick={() => setShowConflictDialog(null)}
                  className="w-full bg-sage-50 text-sage-400 py-4 rounded-2xl text-[11px] font-black uppercase tracking-widest active:scale-95 transition-all"
                >
                  取消
                </button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      <div className="flex-1 overflow-y-auto no-scrollbar">
        <AnimatePresence mode="wait">
          {/* Login view removed */}

          {view === 'dashboard' && (
            <motion.div key="dashboard" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="h-full">
              <DashboardView 
                recipes={recipes} 
                activeBatch={activeBatch}
                onPlaceOrder={() => setView('order-engine')}
                onOpenBatch={() => setView('batch-detail')}
                onSelectRecipe={(r) => { setSelectedRecipe(r); setView('recipe-detail'); }}
                onRandomOrder={(r) => handleConfirmOrder([r.id])}
              />
            </motion.div>
          )}

          {view === 'recipe-detail' && selectedRecipe && (
            <motion.div key="recipe-detail" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
              <RecipeDetailView 
                recipe={selectedRecipe}
                onBack={() => setView('dashboard')}
                onGoCook={() => setView('picking')}
                onDelete={handleDeleteRecipe}
                onEdit={handleEditRecipe}
              />
            </motion.div>
          )}

          {view === 'order-engine' && (
            <motion.div key="order-engine" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
              <OrderEngineView 
                recipes={recipes} 
                onConfirm={handleConfirmOrder} 
                onCancel={() => setView('dashboard')} 
              />
            </motion.div>
          )}

          {view === 'batch-detail' && activeBatch && (
            <motion.div key="batch-detail" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
              <BatchDetailView 
                batch={activeBatch} 
                recipes={recipes} 
                onUpdateStatus={async (id, status) => {
                  if (status === 'finished') {
                    const firstRecipeId = activeBatch.recipeIds[0];
                    const targetRecipe = recipes.find(r => r.id === firstRecipeId);
                    if (targetRecipe) {
                      setSelectedRecipe(targetRecipe);
                      setIsFinishingAllBatch(true);
                      setView('feedback');
                    } else {
                      updateBatchStatus(id, 'finished');
                    }
                  } else {
                    updateBatchStatus(id, status);
                  }
                }}
                onCompleteRecipe={(rid) => {
                  const targetRecipe = recipes.find(r => r.id === rid);
                  if (targetRecipe) {
                    setSelectedRecipe(targetRecipe);
                    setIsFinishingAllBatch(false);
                    setView('feedback');
                  }
                }}
                onClose={() => setView('dashboard')}
                onOpenSOP={(recipe) => { setSelectedRecipe(recipe); setView('execution'); }}
              />
            </motion.div>
          )}

          {view === 'picking' && selectedRecipe && (
            <motion.div key="picking" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
              <PickingView 
                recipe={selectedRecipe}
                onConfirm={handlePickingConfirm}
                onCancel={() => setView('dashboard')}
              />
            </motion.div>
          )}

          {view === 'execution' && selectedRecipe && (
            <motion.div key="execution" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
              <ExecutionView 
                recipe={selectedRecipe} 
                isLazy={isLazy} 
                onComplete={handleExecutionComplete} 
              />
            </motion.div>
          )}

          {view === 'feedback' && selectedRecipe && (
            <motion.div key="feedback" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
              <FeedbackView 
                recipe={selectedRecipe} 
                activeBatch={activeBatch}
                onDone={handleFeedbackDone} 
              />
            </motion.div>
          )}

          {view === 'library' && (
            <motion.div key="library" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
              <LibraryView 
                recipes={recipes} 
                onSelect={(r) => { setSelectedRecipe(r); setView('recipe-detail'); }} 
                onCreateClick={() => setView('create-recipe')}
              />
            </motion.div>
          )}

          {view === 'create-recipe' && (
            <motion.div key="create-recipe" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
              <CreateRecipeView 
                recipe={selectedRecipe || undefined}
                onSave={handleSaveRecipe} 
                onCancel={() => {
                  setSelectedRecipe(null);
                  setView('dashboard');
                }} 
              />
            </motion.div>
          )}

          {view === 'analytics' && (
            <motion.div key="analytics" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
              <AnalyticsView 
                recipes={recipes} 
                onNavigateToLibrary={() => setView('library')} 
              />
            </motion.div>
          )}

          {view === 'settings' && (
            <motion.div key="settings" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="p-8 pb-32 bg-sage-100 min-h-full">
              <h1 className="text-3xl font-black text-sage-900 uppercase tracking-tighter">独厨中心</h1>
              <p className="mt-4 text-sage-500 font-mono text-[10px] uppercase tracking-widest leading-none">SoloChef Operational Center // v1.6</p>
              
              <div className="mt-12 space-y-4">
                <div className="pt-4 space-y-4">
                   <button 
                    onClick={handleSyncToLocal}
                    disabled={isSyncing}
                    className={`w-full bg-sage-900 text-white p-6 rounded-[32px] flex items-center justify-between group active:scale-[0.98] transition-all shadow-sm ${isSyncing ? 'opacity-70' : ''}`}
                  >
                    <div className="flex items-center gap-4">
                      <div className="w-12 h-12 bg-white/10 rounded-2xl flex items-center justify-center text-white">
                        <Save size={22} className={isSyncing ? 'animate-pulse' : ''} />
                      </div>
                      <div className="text-left">
                        <h2 className="text-sm font-black text-white tracking-tight">同步数据到本地</h2>
                        <p className="text-[10px] text-white/40 font-medium">导出 Markdown 备份文件</p>
                      </div>
                    </div>
                    {isSyncing ? (
                      <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                    ) : (
                      <FileText size={18} className="text-white/20 group-hover:text-white transition-colors" />
                    )}
                  </button>

                  <button 
                    onClick={handleImportFromLocal}
                    disabled={isSyncing}
                    className={`w-full bg-white border-2 border-dashed border-sage-200 text-sage-600 p-6 rounded-[32px] flex items-center justify-between group active:scale-[0.98] transition-all ${isSyncing ? 'opacity-70' : ''}`}
                  >
                    <div className="flex items-center gap-4">
                      <div className="w-12 h-12 bg-sage-50 rounded-2xl flex items-center justify-center text-sage-400">
                        <Plus size={22} className="rotate-45" />
                      </div>
                      <div className="text-left">
                        <h2 className="text-sm font-black text-sage-900 tracking-tight">从本地恢复数据</h2>
                        <p className="text-[10px] text-sage-400 font-medium">选择备份 Markdown 文件</p>
                      </div>
                    </div>
                  </button>

                  <button className="w-full bg-white border border-sage-200 text-sage-900 py-5 rounded-[24px] font-black text-xs uppercase tracking-widest active:scale-95 transition-all">
                    管理物料库存
                  </button>
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      {/* Floating Action Capsule - Integrated "SoloChef" Order + "Create" button */}
      {isMainView && (view === 'dashboard' || view === 'library') && (
        <div className="fixed bottom-24 left-1/2 -translate-x-1/2 w-[95%] max-w-sm z-[100]">
          <div className="bg-sage-900/95 backdrop-blur-xl rounded-full p-1.5 shadow-[0_20px_50px_rgba(0,0,0,0.3)] flex items-center border border-white/20">
             <button 
              onClick={() => setView('order-engine')}
              className="flex-1 flex items-center gap-3 py-1 px-3 text-white group active:scale-[0.98] transition-all"
            >
              <div className="w-10 h-10 bg-white/20 rounded-full flex items-center justify-center shrink-0 shadow-inner">
                <ShoppingBag size={20} />
              </div>
              <div className="text-left overflow-hidden">
                <span className="text-[8px] font-black text-white/40 uppercase tracking-widest block mb-0.5">Kitchen OMS</span>
                <p className="text-sm font-black tracking-tight truncate">独厨点单</p>
              </div>
            </button>
            <div className="w-px h-8 bg-white/10 mx-2" />
            <button 
              onClick={() => setView('create-recipe')}
              className="px-6 py-1.5 flex items-center gap-2 text-white font-black text-[11px] uppercase tracking-wider bg-transparent hover:bg-white/10 rounded-full active:scale-95 transition-all"
            >
              <Plus size={16} strokeWidth={3} />
              <span>新建菜谱</span>
            </button>
          </div>
        </div>
      )}

      {/* Persistent Bottom Pill Navigation */}
      {isMainView && (
        <div className="fixed bottom-6 left-1/2 -translate-x-1/2 w-[90%] max-w-sm z-50">
          <nav className="bg-white/90 backdrop-blur-2xl border border-white/20 px-2 py-2 flex justify-around items-center rounded-full shadow-[0_8px_32px_rgba(0,0,0,0.08)]">
            <button 
              onClick={() => setView('dashboard')}
              className={`flex items-center gap-2 px-4 py-2 rounded-full transition-all ${view === 'dashboard' ? 'bg-sage-800 text-white' : 'text-sage-800'}`}
            >
              <LayoutGrid size={16} />
              {view === 'dashboard' && <span className="text-[10px] font-black tracking-widest">首页</span>}
            </button>
            <button 
              onClick={() => setView('library')}
              className={`flex items-center gap-2 px-4 py-2 rounded-full transition-all ${view === 'library' ? 'bg-sage-800 text-white' : 'text-sage-800'}`}
            >
              <Search size={16} />
              {view === 'library' && <span className="text-[10px] font-black tracking-widest">菜谱</span>}
            </button>
            <button 
              onClick={() => setView('analytics')}
              className={`flex items-center gap-2 px-4 py-2 rounded-full transition-all ${view === 'analytics' ? 'bg-sage-800 text-white' : 'text-sage-800'}`}
            >
              <BarChart3 size={16} />
              {view === 'analytics' && <span className="text-[10px] font-black tracking-widest">数据</span>}
            </button>
            <button 
              onClick={() => setView('settings')}
              className={`flex items-center gap-2 px-4 py-2 rounded-full transition-all ${view === 'settings' ? 'bg-sage-800 text-white' : 'text-sage-800'}`}
            >
              <Settings size={16} />
              {view === 'settings' && <span className="text-[10px] font-black tracking-widest">设置</span>}
            </button>
          </nav>
        </div>
      )}
    </div>
  );
}
