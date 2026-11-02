import { apiClient } from './client';
import type {
  GoalUpsertRequest,
  PageResponse,
  ProductivityGoal,
  SessionHistoryItem,
  StatisticsSummary,
} from '../types/api';

export const statsApi = {
  async getSummary(): Promise<StatisticsSummary> {
    const response = await apiClient.get<StatisticsSummary>('/statistics/summary');
    return response.data;
  },

  async getSessionHistory(page = 0, size = 20): Promise<PageResponse<SessionHistoryItem>> {
    const response = await apiClient.get<PageResponse<SessionHistoryItem>>('/pomodoro/sessions', {
      params: { page, size },
    });
    return response.data;
  },

  async upsertGoal(data: GoalUpsertRequest): Promise<ProductivityGoal> {
    const response = await apiClient.post<ProductivityGoal>('/goals', data);
    return response.data;
  },
};
