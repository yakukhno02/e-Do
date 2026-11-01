import { apiClient } from './client';
import type {
  AdminRoomItem,
  AdminUserItem,
  PageResponse,
} from '../types/api';

export const adminApi = {
  async getUsers(page = 0, size = 20): Promise<PageResponse<AdminUserItem>> {
    const response = await apiClient.get<PageResponse<AdminUserItem>>('/admin/users', {
      params: { page, size },
    });
    return response.data;
  },

  async deleteUser(userId: string): Promise<void> {
    await apiClient.delete(`/admin/users/${userId}`);
  },

  async getRooms(page = 0, size = 20): Promise<PageResponse<AdminRoomItem>> {
    const response = await apiClient.get<PageResponse<AdminRoomItem>>('/admin/rooms', {
      params: { page, size },
    });
    return response.data;
  },

  async deleteRoom(roomId: string): Promise<void> {
    await apiClient.delete(`/admin/rooms/${roomId}`);
  },
};
