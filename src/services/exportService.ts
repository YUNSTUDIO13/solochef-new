import { Recipe, CookingHistoryEntry, UserStats } from '../types';

export const ExportService = {
  formatToMarkdown(recipes: Recipe[], history: CookingHistoryEntry[], stats: UserStats): string {
    let md = `# SoloChef Data Backup\n`;
    md += `Generated at: ${new Date().toLocaleString()}\n\n`;

    md += `## USER STATS\n`;
    md += `- Streak: ${stats.streak} days\n`;
    md += `- Last Ignition: ${stats.last_ignition}\n\n`;

    md += `## RECIPES (${recipes.length})\n\n`;
    recipes.forEach(r => {
      md += `### ${r.name}\n`;
      md += `- ID: ${r.id}\n`;
      md += `- Energy Level: ${r.energy_level}\n`;
      md += `- Tags: ${r.tags?.join(', ') || 'N/A'}\n`;
      md += `- Last Cooked: ${r.last_cooked_at || 'Never'}\n\n`;
      
      md += `#### Materials\n`;
      Object.entries(r.materials).forEach(([cat, list]) => {
        if (list.length > 0) {
          md += `**${cat.toUpperCase()}**\n`;
          list.forEach(m => md += `- [ ] ${m.item} (${m.amount}${m.unit})${m.is_essential ? ' *' : ''}\n`);
        }
      });
      md += `\n`;

      md += `#### Timeline\n`;
      r.timeline.forEach((step, i) => {
        md += `${i + 1}. **${step.type.toUpperCase()}**: ${step.content} (${step.duration}s)\n`;
      });
      md += `\n---\n\n`;
    });

    md += `## COOKING HISTORY (${history.length})\n\n`;
    history.forEach(h => {
      md += `- **${h.recipeName}** on ${new Date(h.finishedAt).toLocaleString()}\n`;
      md += `  Duration: ${h.durationMins} mins | Batch: ${h.batchInfo}\n`;
    });

    const dataPayload = {
      recipes,
      history,
      stats,
      version: '1.0',
      exportedAt: new Date().toISOString()
    };

    md += `\n\n<!-- SOLOCHEF_DATA_START\n${JSON.stringify(dataPayload)}\nSOLOCHEF_DATA_END -->\n`;

    return md;
  },

  async importFromFile(): Promise<{ recipes: Recipe[], history: CookingHistoryEntry[], stats: UserStats } | null> {
    return new Promise((resolve, reject) => {
      const input = document.createElement('input');
      input.type = 'file';
      input.accept = '.md';
      
      input.onchange = async (e: any) => {
        const file = e.target.files[0];
        if (!file) {
          resolve(null);
          return;
        }

        const reader = new FileReader();
        reader.onload = (event: any) => {
          const content = event.target.result;
          const match = content.match(/<!-- SOLOCHEF_DATA_START\n([\s\S]*?)\nSOLOCHEF_DATA_END -->/);
          
          if (!match) {
            alert('无效的备份文件：未找到数据载荷');
            resolve(null);
            return;
          }

          try {
            const data = JSON.parse(match[1]);
            resolve(data);
          } catch (err) {
            console.error('Failed to parse JSON from MD', err);
            alert('解析备份数据失败');
            resolve(null);
          }
        };
        reader.onerror = (err) => {
          console.error('File reading failed', err);
          reject(err);
        };
        reader.readAsText(file);
      };
      
      input.click();
    });
  },

  async syncToLocalFile(content: string) {
    const filename = `SoloChef_Backup_${new Date().toISOString().split('T')[0]}.md`;
    
    // 1. Try File System Access API (Desktop chrome/edge/safari)
    if ('showSaveFilePicker' in window) {
      try {
        const handle = await (window as any).showSaveFilePicker({
          suggestedName: filename,
          types: [{
            description: 'Markdown File',
            accept: { 'text/markdown': ['.md'] },
          }],
        });
        const writable = await handle.createWritable();
        await writable.write(content);
        await writable.close();
        return true;
      } catch (e) {
        console.error('File Picker failed or was cancelled', e);
        // Fallback to auto-download if user cancelled picker or it failed
      }
    }

    // 2. Fallback: Standard Web Download
    // Note: On mobile "App" wrappers like Capacitor, they often intercept this 
    // or you'd use a specific plugin. For pure Web, this triggers a save/download.
    const blob = new Blob([content], { type: 'text/markdown' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    return true;
  }
};
