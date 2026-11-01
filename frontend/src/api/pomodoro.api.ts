import { apiClient } from './client';
import type {
  PomodoroEndResponse,
  PomodoroStartRequest,
  PomodoroStartResponse,
} from '../types/api';

export const pomodoroApi = {
  async start(data: PomodoroStartRequest): Promise<PomodoroStartResponse> {
    const response = await apiClient.post<PomodoroStartResponse>('/pomodoro/start', data);
    return response.data;
  },

  async complete(sessionId: string): Promise<PomodoroEndResponse> {
    const response = await apiClient.post<PomodoroEndResponse>(`/pomodoro/${sessionId}/complete`);
    return response.data;
  },

  async cancel(sessionId: string): Promise<PomodoroEndResponse> {
    const response = await apiClient.post<PomodoroEndResponse>(`/pomodoro/${sessionId}/cancel`);
    return response.data;
  },
};
