import { apiClient } from './client';
import type {
  GoalUpsertRequest,
  PageResponse,
  ProductivityGoal,
  SessionHistoryItem,
  StatisticsSummary,
} from '../types/api';

const STATS_KEY = 'edo_stats_cache';
const HISTORY_KEY = 'edo_session_history';
const GOALS_KEY = 'edo_goals_cache';

export const statsApi = {
  async getSummary(): Promise<StatisticsSummary> {
    try {
      const response = await apiClient.get<StatisticsSummary>('/statistics/summary');
      return response.data;
    } catch (err: any) {
      if (err?.response?.status === 404 || !err.response) {
        let stats = { completedPomodoros: 4, totalFocusMinutes: 100, completedSharedSessions: 1 };
        try {
          const rawStats = localStorage.getItem(STATS_KEY);
          if (rawStats) stats = JSON.parse(rawStats);
        } catch {}

        let history: SessionHistoryItem[] = [];
        try {
          const rawHist = localStorage.getItem(HISTORY_KEY);
          if (rawHist) history = JSON.parse(rawHist);
        } catch {}

        return {
          totalCompletedPomodoros: stats.completedPomodoros,
          totalFocusMinutes: stats.totalFocusMinutes,
          totalSharedSessions: stats.completedSharedSessions,
          recentSessions: history.slice(0, 5),
        };
      }
      throw err;
    }
  },

  async getSessionHistory(page = 0, size = 20): Promise<PageResponse<SessionHistoryItem>> {
    try {
      const response = await apiClient.get<PageResponse<SessionHistoryItem>>('/pomodoro/sessions', {
        params: { page, size },
      });
      return response.data;
    } catch (err: any) {
      if (err?.response?.status === 404 || !err.response) {
        let history: SessionHistoryItem[] = [];
        try {
          const rawHist = localStorage.getItem(HISTORY_KEY);
          if (rawHist) history = JSON.parse(rawHist);
        } catch {}

        const start = page * size;
        const paged = history.slice(start, start + size);
        return {
          content: paged,
          page,
          size,
          totalElements: history.length,
          totalPages: Math.max(1, Math.ceil(history.length / size)),
        };
      }
      throw err;
    }
  },

  async upsertGoal(data: GoalUpsertRequest): Promise<ProductivityGoal> {
    try {
      const response = await apiClient.post<ProductivityGoal>('/goals', data);
      return response.data;
    } catch (err: any) {
      if (err?.response?.status === 404 || !err.response) {
        let goals = { daily: [], weekly: [] };
        try {
          const raw = localStorage.getItem(GOALS_KEY);
          if (raw) goals = JSON.parse(raw);
        } catch {}

        const listKey = data.period === 'DAILY' ? 'daily' : 'weekly';
        const existingIdx = (goals as any)[listKey].findIndex((g: ProductivityGoal) => g.type === data.type);

        const newGoal: ProductivityGoal = {
          id: crypto.randomUUID ? crypto.randomUUID() : `goal-${Date.now()}`,
          period: data.period,
          type: data.type,
          targetValue: data.targetValue,
          currentValue: 0,
          progressPercentage: 0,
        };

        if (existingIdx !== -1) {
          (goals as any)[listKey][existingIdx] = {
            ...(goals as any)[listKey][existingIdx],
            targetValue: data.targetValue,
            progressPercentage: Math.min(100, Math.round(((goals as any)[listKey][existingIdx].currentValue / data.targetValue) * 100)),
          };
        } else {
          (goals as any)[listKey].push(newGoal);
        }

        localStorage.setItem(GOALS_KEY, JSON.stringify(goals));
        return existingIdx !== -1 ? (goals as any)[listKey][existingIdx] : newGoal;
      }
      throw err;
    }
  },
};
