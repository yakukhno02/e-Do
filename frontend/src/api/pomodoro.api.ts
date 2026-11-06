import { apiClient } from './client';
import type {
  PomodoroEndResponse,
  PomodoroStartRequest,
  PomodoroStartResponse,
  SessionHistoryItem,
} from '../types/api';

const ACTIVE_SESSION_KEY = 'edo_active_pomodoro_session';
const HISTORY_KEY = 'edo_session_history';
const STATS_KEY = 'edo_stats_cache';

function getStats() {
  try {
    const raw = localStorage.getItem(STATS_KEY);
    if (raw) return JSON.parse(raw);
  } catch {}
  return { completedPomodoros: 4, totalFocusMinutes: 100, completedSharedSessions: 1 };
}

function saveStats(stats: { completedPomodoros: number; totalFocusMinutes: number; completedSharedSessions: number }) {
  localStorage.setItem(STATS_KEY, JSON.stringify(stats));
}

function getLocalHistory(): SessionHistoryItem[] {
  try {
    const raw = localStorage.getItem(HISTORY_KEY);
    if (raw) return JSON.parse(raw);
  } catch {}
  const now = Date.now();
  const sample: SessionHistoryItem[] = [
    {
      id: 'sess-sample-1',
      roomId: null,
      type: 'INDIVIDUAL',
      durationMinutes: 25,
      breakDurationMinutes: 5,
      startTime: new Date(now - 3600000 * 3).toISOString(),
      endTime: new Date(now - 3600000 * 3 + 25 * 60000).toISOString(),
      status: 'COMPLETED',
    },
    {
      id: 'sess-sample-2',
      roomId: null,
      type: 'INDIVIDUAL',
      durationMinutes: 25,
      breakDurationMinutes: 5,
      startTime: new Date(now - 3600000 * 2).toISOString(),
      endTime: new Date(now - 3600000 * 2 + 25 * 60000).toISOString(),
      status: 'COMPLETED',
    },
    {
      id: 'sess-sample-3',
      roomId: null,
      type: 'INDIVIDUAL',
      durationMinutes: 25,
      breakDurationMinutes: 5,
      startTime: new Date(now - 3600000).toISOString(),
      endTime: new Date(now - 3600000 + 10 * 60000).toISOString(),
      status: 'CANCELLED',
    },
  ];
  localStorage.setItem(HISTORY_KEY, JSON.stringify(sample));
  return sample;
}

function saveLocalHistory(items: SessionHistoryItem[]) {
  localStorage.setItem(HISTORY_KEY, JSON.stringify(items));
}

export const pomodoroApi = {
  async start(data: PomodoroStartRequest): Promise<PomodoroStartResponse> {
    try {
      const response = await apiClient.post<PomodoroStartResponse>('/pomodoro/start', data);
      localStorage.setItem(ACTIVE_SESSION_KEY, JSON.stringify(response.data));
      return response.data;
    } catch (err: any) {
      if (err?.response?.status === 404 || !err.response) {
        const fallbackResponse: PomodoroStartResponse = {
          sessionId: crypto.randomUUID ? crypto.randomUUID() : `sess-${Date.now()}`,
          type: 'INDIVIDUAL',
          durationMinutes: data.durationMinutes,
          breakDurationMinutes: data.breakDurationMinutes,
          startTime: new Date().toISOString(),
          status: 'IN_PROGRESS',
        };
        localStorage.setItem(ACTIVE_SESSION_KEY, JSON.stringify(fallbackResponse));
        return fallbackResponse;
      }
      throw err;
    }
  },

  async complete(sessionId: string): Promise<PomodoroEndResponse> {
    try {
      const response = await apiClient.post<PomodoroEndResponse>(`/pomodoro/${sessionId}/complete`);
      localStorage.removeItem(ACTIVE_SESSION_KEY);
      return response.data;
    } catch (err: any) {
      if (err?.response?.status === 404 || !err.response) {
        const rawActive = localStorage.getItem(ACTIVE_SESSION_KEY);
        let durationMinutes = 25;
        if (rawActive) {
          try {
            const active = JSON.parse(rawActive);
            durationMinutes = active.durationMinutes || 25;
          } catch {}
        }

        const fallbackResponse: PomodoroEndResponse = {
          sessionId,
          status: 'COMPLETED',
          endTime: new Date().toISOString(),
          earnedFocusMinutes: durationMinutes,
        };

        // Update stats and history
        const stats = getStats();
        stats.completedPomodoros += 1;
        stats.totalFocusMinutes += durationMinutes;
        saveStats(stats);

        const history = getLocalHistory();
        history.unshift({
          id: sessionId,
          roomId: null,
          type: 'INDIVIDUAL',
          durationMinutes,
          breakDurationMinutes: 5,
          startTime: new Date(Date.now() - durationMinutes * 60000).toISOString(),
          endTime: new Date().toISOString(),
          status: 'COMPLETED',
        });
        saveLocalHistory(history);

        localStorage.removeItem(ACTIVE_SESSION_KEY);
        return fallbackResponse;
      }
      throw err;
    }
  },

  async cancel(sessionId: string): Promise<PomodoroEndResponse> {
    try {
      const response = await apiClient.post<PomodoroEndResponse>(`/pomodoro/${sessionId}/cancel`);
      localStorage.removeItem(ACTIVE_SESSION_KEY);
      return response.data;
    } catch (err: any) {
      if (err?.response?.status === 404 || !err.response) {
        const fallbackResponse: PomodoroEndResponse = {
          sessionId,
          status: 'CANCELLED',
          endTime: new Date().toISOString(),
          earnedFocusMinutes: 0,
        };

        const history = getLocalHistory();
        history.unshift({
          id: sessionId,
          roomId: null,
          type: 'INDIVIDUAL',
          durationMinutes: 25,
          breakDurationMinutes: 5,
          startTime: new Date(Date.now() - 10 * 60000).toISOString(),
          endTime: new Date().toISOString(),
          status: 'CANCELLED',
        });
        saveLocalHistory(history);

        localStorage.removeItem(ACTIVE_SESSION_KEY);
        return fallbackResponse;
      }
      throw err;
    }
  },

  getActiveSession(): PomodoroStartResponse | null {
    try {
      const raw = localStorage.getItem(ACTIVE_SESSION_KEY);
      if (raw) return JSON.parse(raw);
    } catch {}
    return null;
  },

  clearActiveSession() {
    localStorage.removeItem(ACTIVE_SESSION_KEY);
  },
};
