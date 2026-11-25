import { apiClient } from './client';
import type {
  AdminRoomItem,
  AdminUserItem,
  PageResponse,
} from '../types/api';

const ADMIN_USERS_KEY = 'edo_admin_users_cache';
const ADMIN_ROOMS_KEY = 'edo_admin_rooms_cache';

function getSampleUsers(): AdminUserItem[] {
  try {
    const raw = localStorage.getItem(ADMIN_USERS_KEY);
    if (raw) return JSON.parse(raw);
  } catch {}
  const sample: AdminUserItem[] = [
    {
      id: 'u-1',
      name: 'Назар Цебринський',
      username: 'nazarii_dev',
      email: 'nazar@example.com',
      role: 'ADMIN',
      createdAt: new Date(Date.now() - 86400000 * 10).toISOString(),
      completedPomodoros: 28,
      totalFocusMinutes: 700,
      completedSharedSessions: 12,
    },
    {
      id: 'u-2',
      name: 'Марія Якухно',
      username: 'ykmshhh',
      email: 'yakukhno.maria@lll.kpi.ua',
      role: 'USER',
      createdAt: new Date(Date.now() - 86400000 * 8).toISOString(),
      completedPomodoros: 34,
      totalFocusMinutes: 850,
      completedSharedSessions: 15,
    },
    {
      id: 'u-3',
      name: 'Олексій Студент',
      username: 'alex_kpi',
      email: 'alex@example.com',
      role: 'USER',
      createdAt: new Date(Date.now() - 86400000 * 2).toISOString(),
      completedPomodoros: 5,
      totalFocusMinutes: 125,
      completedSharedSessions: 2,
    },
  ];
  localStorage.setItem(ADMIN_USERS_KEY, JSON.stringify(sample));
  return sample;
}

function getSampleRooms(): AdminRoomItem[] {
  try {
    const raw = localStorage.getItem(ADMIN_ROOMS_KEY);
    if (raw) return JSON.parse(raw);
  } catch {}
  const sample: AdminRoomItem[] = [
    {
      id: 'r-1',
      code: 'A7X9K2',
      name: 'Командна сесія проєкту e-Do',
      creatorId: 'u-1',
      status: 'ACTIVE',
      pomodoroDuration: 25,
      breakDuration: 5,
      createdAt: new Date(Date.now() - 3600000 * 2).toISOString(),
      participantsCount: 3,
    },
    {
      id: 'r-2',
      code: 'DEV24K',
      name: 'Підготовка до лабораторної',
      creatorId: 'u-2',
      status: 'WAITING',
      pomodoroDuration: 45,
      breakDuration: 10,
      createdAt: new Date(Date.now() - 3600000 * 5).toISOString(),
      participantsCount: 2,
    },
  ];
  localStorage.setItem(ADMIN_ROOMS_KEY, JSON.stringify(sample));
  return sample;
}

export const adminApi = {
  async getUsers(page = 0, size = 20): Promise<PageResponse<AdminUserItem>> {
    try {
      const response = await apiClient.get<PageResponse<AdminUserItem>>('/admin/users', {
        params: { page, size },
      });
      return response.data;
    } catch (err: any) {
      if (err?.response?.status === 404 || !err.response) {
        const users = getSampleUsers();
        const start = page * size;
        return {
          content: users.slice(start, start + size),
          page,
          size,
          totalElements: users.length,
          totalPages: Math.max(1, Math.ceil(users.length / size)),
        };
      }
      throw err;
    }
  },

  async deleteUser(userId: string): Promise<void> {
    try {
      await apiClient.delete(`/admin/users/${userId}`);
    } catch (err: any) {
      if (err?.response?.status === 404 || !err.response) {
        const users = getSampleUsers().filter((u) => u.id !== userId);
        localStorage.setItem(ADMIN_USERS_KEY, JSON.stringify(users));
        return;
      }
      throw err;
    }
  },

  async getRooms(page = 0, size = 20): Promise<PageResponse<AdminRoomItem>> {
    try {
      const response = await apiClient.get<PageResponse<AdminRoomItem>>('/admin/rooms', {
        params: { page, size },
      });
      return response.data;
    } catch (err: any) {
      if (err?.response?.status === 404 || !err.response) {
        const rooms = getSampleRooms();
        const start = page * size;
        return {
          content: rooms.slice(start, start + size),
          page,
          size,
          totalElements: rooms.length,
          totalPages: Math.max(1, Math.ceil(rooms.length / size)),
        };
      }
      throw err;
    }
  },

  async deleteRoom(roomId: string): Promise<void> {
    try {
      await apiClient.delete(`/admin/rooms/${roomId}`);
    } catch (err: any) {
      if (err?.response?.status === 404 || !err.response) {
        const rooms = getSampleRooms().filter((r) => r.id !== roomId);
        localStorage.setItem(ADMIN_ROOMS_KEY, JSON.stringify(rooms));
        return;
      }
      throw err;
    }
  },
};
