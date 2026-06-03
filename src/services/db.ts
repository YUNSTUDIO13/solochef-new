import { openDB, IDBPDatabase } from 'idb';
import { Recipe, CookingHistoryEntry, OrderBatch, UserStats } from '../types';

const DB_NAME = 'SoloChefDB';
const DB_VERSION = 1;

export interface SoloChefDBSchema {
  recipes: Recipe;
  history: CookingHistoryEntry;
  activeBatch: OrderBatch;
  stats: UserStats;
}

let dbPromise: Promise<IDBPDatabase<any>> | null = null;

export const getDB = () => {
  if (!dbPromise) {
    dbPromise = openDB(DB_NAME, DB_VERSION, {
      upgrade(db) {
        if (!db.objectStoreNames.contains('recipes')) {
          db.createObjectStore('recipes', { keyPath: 'id' });
        }
        if (!db.objectStoreNames.contains('history')) {
          db.createObjectStore('history', { keyPath: 'id' });
        }
        if (!db.objectStoreNames.contains('activeBatch')) {
          db.createObjectStore('activeBatch', { keyPath: 'id' });
        }
        if (!db.objectStoreNames.contains('stats')) {
          db.createObjectStore('stats', { keyPath: 'type' });
        }
      },
    });
  }
  return dbPromise;
};

export const DBService = {
  // Recipes
  async getAllRecipes(): Promise<Recipe[]> {
    const db = await getDB();
    return db.getAll('recipes');
  },
  async saveRecipe(recipe: Recipe) {
    const db = await getDB();
    await db.put('recipes', recipe);
  },
  async deleteRecipe(id: string) {
    const db = await getDB();
    await db.delete('recipes', id);
  },

  // History
  async getAllHistory(): Promise<CookingHistoryEntry[]> {
    const db = await getDB();
    return db.getAll('history');
  },
  async addHistoryEntry(entry: CookingHistoryEntry) {
    const db = await getDB();
    await db.put('history', entry);
  },

  // Active Batch
  async getActiveBatch(): Promise<OrderBatch | null> {
    const db = await getDB();
    const batches = await db.getAll('activeBatch');
    return batches.length > 0 ? batches[0] : null;
  },
  async saveActiveBatch(batch: OrderBatch) {
    const db = await getDB();
    // Clear old ones first to ensure only one active batch
    const tx = db.transaction('activeBatch', 'readwrite');
    await tx.store.clear();
    await tx.store.put(batch);
    await tx.done;
  },
  async clearActiveBatch() {
    const db = await getDB();
    const tx = db.transaction('activeBatch', 'readwrite');
    await tx.store.clear();
    await tx.done;
  },

  // Stats
  async getStats(): Promise<UserStats | null> {
    const db = await getDB();
    const stats = await db.get('stats', 'user');
    return stats || null;
  },
  async saveStats(stats: UserStats) {
    const db = await getDB();
    await db.put('stats', { ...stats, type: 'user' });
  }
};
