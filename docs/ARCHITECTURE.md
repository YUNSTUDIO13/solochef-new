# SoloChef（独厨）架构文档

> 版本：v1.6 | 更新：2026-06-03

---

## 一、项目定位

**SoloChef（独厨）** 是一个私有、移动优先的个人烹饪履约系统。核心理念是将"做饭"这件事拆解为可管理、可执行、可复盘的工作流——从买菜清单、烹饪计时到完成反馈，覆盖一次完整烹饪的全生命周期。

核心设计语言：**厨房 OMS（Order Management System）** ——用点餐系统的思维做家庭厨房。

---

## 二、技术栈

### 2.1 Web 端

| 层面 | 技术选型 |
|------|----------|
| 框架 | React 19 + TypeScript 5.8 |
| 构建 | Vite 6 |
| 样式 | Tailwind CSS 4（Sage 色板） |
| 动画 | motion (Framer Motion) |
| 图表 | Recharts 3.8 |
| 图标 | Lucide React |
| 存储 | IndexedDB（idb 8） |
| AI | @google/genai (Gemini API) |
| 服务端 | Express（可选，用于 Cloud Run 部署） |

### 2.2 Android 原生端

| 层面 | 技术选型 |
|------|----------|
| 语言 | Kotlin 2.1.0 |
| UI | Jetpack Compose + Material3 |
| 导航 | Navigation Compose 2.8.5 |
| 序列化 | kotlinx.serialization 1.7.3 |
| 图片 | Coil 2.7.0 |
| 存储 | Markdown 文件（filesDir） |
| 构建 | AGP 8.9.2 + Gradle 9.4.1 |

### 2.3 双端同步策略

| 能力 | Web（IndexedDB） | Android（Markdown 文件） |
|------|-------------------|---------------------------|
| 菜谱 CRUD | idb `recipes` store | `filesDir/recipes/{id}.md` |
| 用户统计 | idb `stats` store | `filesDir/_stats.json` |
| 活跃批次 | idb `activeBatch` store | `filesDir/_batch.json` |
| 历史记录 | idb `history` store | `filesDir/_history.json` |
| 导出 | Markdown + File System Access API | Markdown + SAF |
| 导入 | FileReader + JSON 解析 | 同 Web 协议，AutoMerge |

---

## 三、架构总览

```
┌─────────────────────────────────────────────┐
│                  App.tsx                     │
│         (Central State Controller)           │
│                                              │
│  ┌────────┐ ┌──────────┐ ┌───────────────┐  │
│  │ recipes│ │  stats   │ │  activeBatch   │  │
│  │  []    │ │  streak  │ │  OrderBatch?   │  │
│  └────────┘ └──────────┘ └───────────────┘  │
│                                              │
│  ┌──────────────┐ ┌─────────────────────┐   │
│  │ cookingHistory│ │ selectedRecipe      │   │
│  │     []        │ │ Recipe | null       │   │
│  └──────────────┘ └─────────────────────┘   │
│                                              │
│          view: ViewState                     │
│     ('dashboard' | 'library' | ... )        │
└──────────┬──────────────────────────────────┘
           │ props ↓           ↑ callbacks
    ┌──────┴──────────────────────────────┐
    │         View Components (11)         │
    │                                      │
    │  Dashboard  Library  Analytics       │
    │  RecipeDetail  CreateRecipe          │
    │  OrderEngine  BatchDetail            │
    │  Picking  Execution  Feedback        │
    │  SelectionView (legacy)              │
    └──────┬──────────────────────────────┘
           │
    ┌──────┴──────────────┐
    │    Services Layer    │
    │                      │
    │  DBService           │
    │  (IndexedDB CRUD)    │
    │                      │
    │  ExportService       │
    │  (Markdown ↔ JSON)   │
    └─────────────────────┘
```

### 3.1 核心设计决策

1. **单一状态源**：`App.tsx` 持有全部共享状态，各 View 组件通过 props 接收数据、通过回调触发变更
2. **视图驱动路由**：不使用 React Router，用一个 `view: ViewState` 字符串 + `AnimatePresence mode="wait"` 实现页面切换动画
3. **双端一致性**：Web 端和 Android 端使用完全相同的数据模型（以 TypeScript `types.ts` 为准，Android 端 1:1 映射 `kotlinx.serialization`）
4. **离线优先**：所有数据本地存储，导入/导出通过 Markdown 协议完成跨设备同步

---

## 四、数据模型

### 4.1 核心类型定义

```
Recipe ────────────── 菜谱
├── id: string
├── name: string
├── cover_image: string
├── bom_snapshot?: string          // 食材全家福照片
├── energy_level: 'High'|'Mid'|'Low'
├── is_featured: boolean
├── last_cooked_at?: string
├── materials: Record<MaterialCategory, Material[]>
│   ├── meat:     Material[]
│   ├── vegetable: Material[]
│   └── seasoning: Material[]
├── timeline: TimelineStep[]
├── tags?: string[]
└── updated_at?: string

Material ──────────── 食材
├── item: string
├── amount: string
├── unit: string
├── is_essential: boolean
└── image?: string

TimelineStep ──────── 烹饪步骤
├── step_id: number
├── type: 'active' | 'waiting'    // 手动执行 | 后台等待
├── content: string
├── duration: number               // 秒
├── is_parallel: boolean
├── sub_tasks?: SubTask[]          // 等待步骤的并行子任务
└── images?: string[]              // 步骤图解

OrderBatch ────────── 订单批次
├── id: string
├── status: 'picking' | 'processing' | 'finished'
├── recipeIds: string[]
├── completedRecipeIds: string[]
├── created_at: string
├── picked_at?: string
├── finished_at?: string
└── batch_notes?: string

UserStats ─────────── 用户统计
├── streak: number                 // 连续点火天数
└── last_ignition: string          // ISO 时间戳

CookingHistoryEntry ─ 烹饪历史
├── id: string
├── recipeId / recipeName / recipeImage
├── orderId / finishedAt / durationMins
└── batchInfo / tag / materials?
```

### 4.2 Fire 状态机

```
         diffHours >= 48h → streak = 1
        ┌──────────────────────────┐
        │                          │
   [extinguished] ──────────→ [blazing]
        │                          │
        │  16h <= diff < 48h       │
        │  streak += 1             │
        │                          │
        └────── diff < 16h ────────┘
                streak 不变
```

Title 演变：厨房新贵 → 烟火气守护者(7d) → 外卖行业死对头(30d) → 生活掌控大师(100d)

---

## 五、视图与路由映射

| Route | ViewState | 组件 | 描述 |
|-------|-----------|------|------|
| `/` | `dashboard` | DashboardView | 首页：状态卡片 + 推荐网格 + 随机选菜 |
| `/library` | `library` | LibraryView | 菜谱库：搜索 + 多级过滤 + 网格浏览 |
| `/analytics` | `analytics` | AnalyticsView | 数据大盘：指标卡 + 热力图 + 消耗榜 |
| `/settings` | `settings` | (内联) | 设置中心：同步/恢复/库存管理 |
| `/recipe/:id` | `recipe-detail` | RecipeDetailView | 菜谱详情：封面 + BOM + 步骤时间线 |
| `/recipe/new` | `create-recipe` | CreateRecipeView | 新建/编辑菜谱：全功能表单 |
| `/order` | `order-engine` | OrderEngineView | 点单引擎：分类侧栏 + 购物车 |
| `/batch` | `batch-detail` | BatchDetailView | 批次详情：双模式（拣货/加工） |
| `/picking` | `picking` | PickingView | 物料确认：逐项勾选 |
| `/execution` | `execution` | ExecutionView | 烹饪执行：步骤计时器 + 播放控制 |
| `/feedback` | `feedback` | FeedbackView | 完成反馈：热敏收据纸 |

### 5.1 核心用户流

```
Dashboard → OrderEngine → BatchDetail(Picking) → BatchDetail(Processing)
                                                         │
                                              ┌──────────┼──────────┐
                                              ▼          ▼          ▼
                                          Execution  Execution  Execution
                                              │          │          │
                                              ▼          ▼          ▼
                                          Feedback   Feedback   Feedback
                                              │          │          │
                                              └──────────┴──────────┘
                                                         │
                                                         ▼
                                                     Dashboard
```

备用流（单菜谱快速模式）：
```
Dashboard → 随机选菜 → Picking → Execution → Feedback → Dashboard
```

---

## 六、服务层

### 6.1 DBService (Web)

基于 IndexedDB 的持久化服务：

```typescript
DBService.getAllRecipes():  Promise<Recipe[]>
DBService.saveRecipe(r):    Promise<void>
DBService.deleteRecipe(id): Promise<void>
DBService.getAllHistory():  Promise<CookingHistoryEntry[]>
DBService.addHistoryEntry(): Promise<void>
DBService.getActiveBatch(): Promise<OrderBatch | null>
DBService.saveActiveBatch(): Promise<void>
DBService.clearActiveBatch(): Promise<void>
DBService.getStats():       Promise<UserStats | null>
DBService.saveStats():      Promise<void>
```

数据库：`SoloChefDB` v1，4 个 ObjectStore：`recipes` / `history` / `activeBatch` / `stats`

### 6.2 ExportService

Markdown 数据交换协议：

```typescript
formatToMarkdown(recipes, history, stats): string
  → 可读 Markdown + 嵌入 JSON payload
  → payload 位于 <!-- SOLOCHEF_DATA_START ... SOLOCHEF_DATA_END --> 之间

importFromFile(): Promise<BackupPayload>
  → 通过 FileReader 解析 Markdown 中的 JSON 载荷

syncToLocalFile(content): Promise<boolean>
  → Web: File System Access API (showSaveFilePicker)
  → 回退: 标准 download blob
```

### 6.3 LocalFileManager (Android)

Android 端对应的持久化服务，使用 `filesDir` + Markdown 文件：

```kotlin
getAllRecipes(): List<Recipe>     // 读 filesDir/recipes/*.md
saveRecipe(r)                     // 写 {id}.md，内嵌 JSON wrapper
deleteRecipe(id)
getStats(): UserStats             // filesDir/_stats.json
saveStats(s)
getActiveBatch(): OrderBatch?     // filesDir/_batch.json
exportToMarkdown(): String        // 全量备份
importFromMarkdown(content): BackupPayload?
mergeImport(payload): Int         // 智能合并
```

---

## 七、关键功能模块深度解析

### 7.1 订单引擎 (OrderEngine)

- 左侧 80px 分类侧栏（工艺 + 菜系 + 主推菜，共 30+ 维度）
- 右侧菜谱列表（卡面 + 加减控制器）
- 底部购物车栏（份数统计 + 生成计划按钮）
- 与现有 Batch 合并逻辑：picking 态合并，processing 态覆盖

### 7.2 批次履约 (BatchDetail)

双模式设计：

**Picking 模式**（物料筹备）：
- 多菜谱 BOM 自动聚合去重（按 `item + unit` 合并同食材用量）
- 三大分类展示（肉禽/蔬菜/调料）
- 逐项勾选 + 一键确认

**Processing 模式**（烹饪加工）：
- 各菜谱独立卡片
- 逐项完成（✓ 按钮 → Feedback）
- 一键归档所有（→ Feedback 热敏纸）

### 7.3 烹饪执行 (ExecutionView)

- 步骤级时间线渲染（仅显当前+未来步骤）
- `active` 步骤全尺寸 + 倒计时进度条
- `waiting` 步骤半透明（scale: 0.95, opacity: 0.4）
- 物理按键映射：VOL+ 暂停/播放，VOL- 跳过步骤
- WakeLock 防止屏幕休眠
- 倒计时 ≤ 3s 时全屏脉冲警告
- 全屏图片查看（zoom-in/zoom-out）

### 7.4 热敏收据 (FeedbackView)

游戏化反馈层：
- 热敏纸票据模拟（锯齿边、斑驳纹理）
- 递增取票号（持久化 counter）
- 动态价格小票（单菜模式 / 批量模式）
- 随机温暖寄语（6 句备选）
- Android 端支持保存小票为 PNG 到相册

### 7.5 菜谱编辑器 (CreateRecipeView)

全功能表单：
- 封面图上传（FileReader → base64）
- 基本信息（名称、精力等级、主推菜标记）
- 分类标签（工艺 8 条 + 菜系 21 条单选 + 自定义多选）
- BOM 快照（食材全家福照片）
- 物料清单（肉/菜/调三类，增删改，"一键调味"）
- 时间线构建器（步骤增删、类型切换、时长、图片、子任务）
- 编辑模式复用（`recipe` prop 存在时预填）

### 7.6 数据大盘 (AnalyticsView)

- 双指标卡：菜谱总量 + 一周未烹饪数
- 周热力图（7 天 × 烹饪密度色阶）
- 食材消耗排行榜（肉 / 菜 / 调 TOP3，仅统计已烹饪菜谱）

---

## 八、目录结构

```
solochef-new/
├── index.html                    # Web 入口
├── package.json                  # 依赖 & 脚本
├── vite.config.ts                # Vite + Tailwind + React + 路径别名
├── tsconfig.json                 # TypeScript 配置
├── .env.example                  # GEMINI_API_KEY + APP_URL
│
├── src/
│   ├── main.tsx                  # React 挂载点
│   ├── App.tsx                   # 中央控制器 (ViewState + 状态 + 回调)
│   ├── index.css                 # Tailwind + Sage 色板 + 手写字体
│   ├── types.ts                  # 全部类型定义
│   ├── mockData.ts               # 初始 Mock 数据（当前为空数组）
│   │
│   ├── components/
│   │   ├── DashboardView.tsx     # 首页
│   │   ├── LibraryView.tsx       # 菜谱库
│   │   ├── AnalyticsView.tsx     # 数据大盘
│   │   ├── SelectionView.tsx     # 选菜页（Legacy）
│   │   ├── OrderEngineView.tsx   # 点单引擎
│   │   ├── BatchDetailView.tsx   # 批次履约
│   │   ├── RecipeDetailView.tsx  # 菜谱详情
│   │   ├── CreateRecipeView.tsx  # 新建/编辑菜谱
│   │   ├── PickingView.tsx       # 物料确认
│   │   ├── ExecutionView.tsx     # 烹饪执行
│   │   ├── FeedbackView.tsx      # 热敏收据
│   │   └── FireStatusHeader.tsx  # 点火状态（Legacy）
│   │
│   └── services/
│       ├── db.ts                 # IndexedDB 封装
│       └── exportService.ts      # 导入/导出协议
│
├── android/                      # Android 原生端
│   ├── app/
│   │   ├── build.gradle.kts      # Compose + Coil + Navigation + Serialization
│   │   └── src/main/
│   │       ├── AndroidManifest.xml
│   │       └── java/com/example/solochef/
│   │           ├── MainActivity.kt          # 导航 + Scaffold + 共享状态
│   │           ├── ImageUtils.kt             # 图片压缩 + ZIP 导出
│   │           ├── model/Models.kt           # Kotlin 数据类
│   │           ├── storage/LocalFileManager.kt # Markdown 存储
│   │           ├── util/ImageCompressor.kt   # 图片压缩工具
│   │           └── ui/
│   │               ├── theme/{Color,Theme,Type}.kt
│   │               └── screens/
│   │                   ├── dashboard/
│   │                   ├── library/
│   │                   ├── analytics/
│   │                   ├── batchdetail/
│   │                   ├── createrecipe/
│   │                   ├── execution/
│   │                   ├── feedback/
│   │                   ├── orderengine/
│   │                   ├── picking/
│   │                   ├── recipedetail/
│   │                   └── settings/
│   │
│   ├── build.gradle.kts
│   └── settings.gradle.kts
│
└── docs/                         # 本文档目录
    ├── ARCHITECTURE.md
    ├── USER_JOURNEY_MAP.md
    └── TEST_CASES.md
```

---

## 九、设计系统

### 9.1 色彩 — Sage 色板

```
sage-50:  #f7f9f6    最浅底色
sage-100: #f2f6f0    主背景
sage-200: #e5ede2    卡片边框
sage-300: #8ba18b    次要文字
sage-400: #6a7f6a
sage-500: #4a5c4a    正文
sage-800: #2d3a2d    主按钮
sage-900: #1a241a    标题/强调
```

### 9.2 字体

- 主字体：Inter（sans-serif）
- 手写字：Long Cang / Ma Shan Zheng / Zhi Mang Xing（热敏纸寄语）

### 9.3 圆角体系

- 全圆角设计语言
- 卡片：`rounded-[28px]` / `rounded-[32px]`
- 按钮：`rounded-2xl` / `rounded-full`
- 导航：pill（`rounded-full`）

### 9.4 交互模式

- `active:scale-95` / `active:scale-[0.98]` 点击反馈
- `motion` 进出场：`opacity + y` / `scale` 动画
- `AnimatePresence mode="wait"` 页面切换
- `whileTap` / `whileHover` 微交互

---

## 十、已知技术债务 & 迭代方向

| 优先级 | 条目 | 说明 |
|--------|------|------|
| P0 | 烹饪历史记录缺失 | `recordToHistory` 为空函数体，`cookingHistory` 状态无数据源 |
| P0 | Android 音量键执行映射 | 需 Activity 级 `dispatchKeyEvent` 拦截，当前仅 Web 端 `ArrowUp/Down` 可用 |
| P1 | Mock 数据为空 | `MOCK_RECIPES = []`，首次加载无体验内容 |
| P1 | 数据分析依赖有限 | 热力图数据为硬编码 WEEKLY_DATA，非动态计算 |
| P1 | 双端数据同步 | 无实时同步机制，依赖手动导入/导出 |
| P2 | SelectionView（Legacy） | 未被导航引用，可清理 |
| P2 | Login 流程移除 | `isLoggedIn` 恒 true，ViewState 含 `login` 但不可达 |
| P2 | 设置页"物料库存"按钮 | 无功能实现（占位） |
| P2 | Gemini API 集成 | 依赖已安装 (`@google/genai`) 但未发现调用点 |
| P3 | Android 端主题硬编码 | Sage 色板通过常量定义，非动态主题系统 |
