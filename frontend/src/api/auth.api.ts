import { apiClient, USER_STORAGE_KEY } from './client';
import type {
  AuthResponse,
  ForgotPasswordRequest,
  LoginRequest,
  MessageResponse,
  ProfileUpdateRequest,
  RegisterRequest,
  RegisterResponse,
  ResetPasswordRequest,
  UserProfile,
} from '../types/api';

export const authApi = {
  async register(data: RegisterRequest): Promise<RegisterResponse> {
    const response = await apiClient.post<RegisterResponse>('/auth/register', data);
    return response.data;
  },

  async login(data: LoginRequest): Promise<AuthResponse> {
    const response = await apiClient.post<AuthResponse>('/auth/login', data);
    return response.data;
  },

  async logout(): Promise<void> {
    try {
      await apiClient.post('/auth/logout');
    } catch {
      // ignore
    }
  },

  async forgotPassword(data: ForgotPasswordRequest): Promise<MessageResponse> {
    const response = await apiClient.post<MessageResponse>('/auth/forgot-password', data);
    return response.data;
  },

  async resetPassword(data: ResetPasswordRequest): Promise<void> {
    await apiClient.post('/auth/reset-password', data);
  },

  async getProfile(): Promise<UserProfile> {
    try {
      const response = await apiClient.get<UserProfile>('/users/me');
      return response.data;
    } catch (err: any) {
      if (err?.response?.status === 404 || !err.response) {
        const cachedUser = localStorage.getItem(USER_STORAGE_KEY);
        const parsed = cachedUser ? JSON.parse(cachedUser) : {
          id: 'user-default-1',
          name: 'Назар Цебринський',
          username: 'nazarii_dev',
          email: 'nazar@example.com',
          role: 'USER',
        };

        const statsRaw = localStorage.getItem('edo_stats_cache');
        const stats = statsRaw ? JSON.parse(statsRaw) : { completedPomodoros: 4, totalFocusMinutes: 100, completedSharedSessions: 1 };
        const goalsRaw = localStorage.getItem('edo_goals_cache');
        const goals = goalsRaw ? JSON.parse(goalsRaw) : {
          daily: [
            { id: 'g1', period: 'DAILY', type: 'POMODORO_COUNT', targetValue: 8, currentValue: stats.completedPomodoros, progressPercentage: Math.min(100, (stats.completedPomodoros / 8) * 100) },
          ],
          weekly: [
            { id: 'g2', period: 'WEEKLY', type: 'FOCUS_MINUTES', targetValue: 1200, currentValue: stats.totalFocusMinutes, progressPercentage: Math.min(100, (stats.totalFocusMinutes / 1200) * 100) },
          ],
        };

        return {
          ...parsed,
          completedPomodoros: stats.completedPomodoros,
          totalFocusMinutes: stats.totalFocusMinutes,
          completedSharedSessions: stats.completedSharedSessions,
          goals,
        };
      }
      throw err;
    }
  },

  async updateProfile(data: ProfileUpdateRequest): Promise<UserProfile> {
    try {
      const response = await apiClient.put<UserProfile>('/users/me', data);
      return response.data;
    } catch (err: any) {
      if (err?.response?.status === 404 || !err.response) {
        const cachedUser = localStorage.getItem(USER_STORAGE_KEY);
        const parsed = cachedUser ? JSON.parse(cachedUser) : {};
        const updated = { ...parsed, name: data.name, username: data.username };
        localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(updated));
        return authApi.getProfile();
      }
      throw err;
    }
  },
};
