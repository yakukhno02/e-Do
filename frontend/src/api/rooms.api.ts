import { apiClient } from './client';
import type {
  RoomCreateRequest,
  RoomDetails,
  RoomJoinRequest,
} from '../types/api';

export const roomsApi = {
  async createRoom(data: RoomCreateRequest): Promise<RoomDetails> {
    const response = await apiClient.post<RoomDetails>('/rooms', data);
    return response.data;
  },

  async joinRoom(data: RoomJoinRequest): Promise<RoomDetails> {
    const response = await apiClient.post<RoomDetails>('/rooms/join', data);
    return response.data;
  },

  async getRoom(id: string): Promise<RoomDetails> {
    const response = await apiClient.get<RoomDetails>(`/rooms/${id}`);
    return response.data;
  },

  async startRoomSession(id: string): Promise<RoomDetails> {
    const response = await apiClient.post<RoomDetails>(`/rooms/${id}/start`);
    return response.data;
  },

  async stopRoomSession(id: string): Promise<RoomDetails> {
    const response = await apiClient.post<RoomDetails>(`/rooms/${id}/stop`);
    return response.data;
  },

  async leaveRoom(id: string): Promise<void> {
    await apiClient.post(`/rooms/${id}/leave`);
  },
};
