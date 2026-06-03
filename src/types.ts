export type EnergyLevel = 'High' | 'Mid' | 'Low';

export interface SubTask {
  content: string;
  duration: number; // seconds
}

export interface TimelineStep {
  step_id: number;
  type: 'active' | 'waiting';
  content: string;
  duration: number; // seconds
  is_parallel: boolean;
  sub_tasks?: SubTask[];
  images?: string[];
}

export type MaterialCategory = 'meat' | 'vegetable' | 'seasoning';

export interface Material {
  item: string;
  amount: string;
  unit: string;
  is_essential: boolean;
  image?: string;
}

export interface UserStats {
  streak: number;
  last_ignition: string; // ISO String
}

export type FireState = 'extinguished' | 'embers' | 'blazing';

export type FireColor = 'cyan' | 'gold' | 'purple';

export interface Recipe {
  id: string;
  name: string;
  cover_image: string;
  bom_snapshot?: string;
  energy_level: EnergyLevel;
  is_featured: boolean;
  last_cooked_at?: string;
  materials: Record<MaterialCategory, Material[]>;
  timeline: TimelineStep[];
  tags?: string[];
  updated_at?: string;
}

export type BatchStatus = 'picking' | 'processing' | 'finished';

export interface CookingHistoryEntry {
  id: string;
  recipeId: string;
  recipeName: string;
  recipeImage: string;
  orderId: string;
  finishedAt: string;
  durationMins: number;
  batchInfo: string;
  tag?: string;
  materials?: Record<MaterialCategory, Material[]>;
}

export interface OrderBatch {
  id: string;
  status: BatchStatus;
  recipeIds: string[];
  completedRecipeIds: string[]; // Track which recipes are cooked
  created_at: string;
  picked_at?: string;
  finished_at?: string;
  batch_notes?: string;
}
