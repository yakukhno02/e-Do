import { apiClient, USER_STORAGE_KEY } from './client';
import type {
  RoomCreateRequest,
  RoomDetails,
  RoomJoinRequest,
} from '../types/api';

const LOCAL_ROOMS_KEY = 'edo_local_rooms_cache';

// Helper to generate a 6-character room code (without confusing chars 0, O, 1, I)
function generateRoomCode(): string {
  const chars = '23456789ABCDEFGHJKLMNPQRSTUVWXYZ';
  let result = '';
  for (let i = 0; i < 6; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return result;
}

function getCurrentUser(): { id: string; name: string; username: string } {
  try {
    const raw = localStorage.getItem(USER_STORAGE_KEY);
    if (raw) return JSON.parse(raw);
  } catch {}
  return { id: 'user-default-1', name: 'Назар Цебринський', username: 'nazarii_dev' };
}

function getLocalRooms(): RoomDetails[] {
  try {
    const raw = localStorage.getItem(LOCAL_ROOMS_KEY);
    if (raw) return JSON.parse(raw);
  } catch {}
  const sampleRooms: RoomDetails[] = [
    {
      id: 'room-demo-1',
      code: 'DEV24K',
      name: 'Розробка e-Do Sprint 2',
      creatorId: 'user-default-1',
      status: 'WAITING',
      pomodoroDuration: 25,
      breakDuration: 5,
      currentSessionStart: null,
      serverTime: new Date().toISOString(),
      version: 1,
      participants: [
        {
          userId: 'user-default-1',
          name: 'Назар Цебринський',
          username: 'nazarii_dev',
          isCreator: true,
        },
        {
          userId: 'user-maria-2',
          name: 'Марія Якухно',
          username: 'ykmshhh',
          isCreator: false,
        },
      ],
    },
  ];
  localStorage.setItem(LOCAL_ROOMS_KEY, JSON.stringify(sampleRooms));
  return sampleRooms;
}

function saveLocalRooms(rooms: RoomDetails[]) {
  localStorage.setItem(LOCAL_ROOMS_KEY, JSON.stringify(rooms));
}

export const roomsApi = {
  async getRoomsList(): Promise<RoomDetails[]> {
    try {
      const response = await apiClient.get<RoomDetails[]>('/rooms');
      return response.data;
    } catch (err: any) {
      if (err?.response?.status === 404 || !err.response) {
        return getLocalRooms();
      }
      throw err;
    }
  },

  async createRoom(data: RoomCreateRequest): Promise<RoomDetails> {
    try {
      const response = await apiClient.post<RoomDetails>('/rooms', data);
      return response.data;
    } catch (err: any) {
      if (err?.response?.status === 404 || !err.response) {
        const user = getCurrentUser();
        const newRoom: RoomDetails = {
          id: crypto.randomUUID ? crypto.randomUUID() : `room-${Date.now()}`,
          code: generateRoomCode(),
          name: data.name,
          creatorId: user.id,
          status: 'WAITING',
          pomodoroDuration: data.pomodoroDuration || 25,
          breakDuration: data.breakDuration || 5,
          currentSessionStart: null,
          serverTime: new Date().toISOString(),
          version: 1,
          participants: [
            {
              userId: user.id,
              name: user.name,
              username: user.username,
              isCreator: true,
            },
          ],
        };
        const all = getLocalRooms();
        all.unshift(newRoom);
        saveLocalRooms(all);
        return newRoom;
      }
      throw err;
    }
  },

  async joinRoom(data: RoomJoinRequest): Promise<RoomDetails> {
    try {
      const response = await apiClient.post<RoomDetails>('/rooms/join', data);
      return response.data;
    } catch (err: any) {
      if (err?.response?.status === 404 || !err.response) {
        const normalized = data.code.trim().toUpperCase();
        const all = getLocalRooms();
        const found = all.find((r) => r.code.toUpperCase() === normalized);
        if (!found) {
          const notFoundErr: any = new Error('Кімнату з таким кодом не знайдено');
          notFoundErr.response = { status: 404, data: { message: 'Кімнату з таким кодом не знайдено' } };
          throw notFoundErr;
        }

        const user = getCurrentUser();
        const alreadyParticipant = found.participants.some((p) => p.userId === user.id);
        if (!alreadyParticipant) {
          found.participants.push({
            userId: user.id,
            name: user.name,
            username: user.username,
            isCreator: false,
          });
          found.version += 1;
          saveLocalRooms(all);
        }
        return { ...found, serverTime: new Date().toISOString() };
      }
      throw err;
    }
  },

  async getRoom(id: string): Promise<RoomDetails> {
    try {
      const response = await apiClient.get<RoomDetails>(`/rooms/${id}`);
      return response.data;
    } catch (err: any) {
      if (err?.response?.status === 404 || !err.response) {
        const all = getLocalRooms();
        const found = all.find((r) => r.id === id);
        if (!found) {
          const notFoundErr: any = new Error('Кімната не знайдена');
          notFoundErr.response = { status: 404, data: { message: 'Кімната не знайдена' } };
          throw notFoundErr;
        }

        // Check if active focus session or break completed
        const now = Date.now();
        if (found.status === 'ACTIVE' && found.currentSessionStart) {
          const start = Date.parse(found.currentSessionStart);
          const elapsed = (now - start) / 1000;
          if (elapsed >= found.pomodoroDuration * 60) {
            found.status = 'BREAK';
            found.currentSessionStart = new Date().toISOString();
            found.version += 1;
            saveLocalRooms(all);
          }
        } else if (found.status === 'BREAK' && found.currentSessionStart) {
          const start = Date.parse(found.currentSessionStart);
          const elapsed = (now - start) / 1000;
          if (elapsed >= found.breakDuration * 60) {
            found.status = 'WAITING';
            found.currentSessionStart = null;
            found.version += 1;
            saveLocalRooms(all);
          }
        }

        return { ...found, serverTime: new Date().toISOString() };
      }
      throw err;
    }
  },

  async startRoomSession(id: string): Promise<RoomDetails> {
    try {
      const response = await apiClient.post<RoomDetails>(`/rooms/${id}/start`);
      return response.data;
    } catch (err: any) {
      if (err?.response?.status === 404 || !err.response) {
        const all = getLocalRooms();
        const found = all.find((r) => r.id === id);
        if (found) {
          found.status = 'ACTIVE';
          found.currentSessionStart = new Date().toISOString();
          found.version += 1;
          saveLocalRooms(all);
          return { ...found, serverTime: new Date().toISOString() };
        }
      }
      throw err;
    }
  },

  async stopRoomSession(id: string): Promise<RoomDetails> {
    try {
      const response = await apiClient.post<RoomDetails>(`/rooms/${id}/stop`);
      return response.data;
    } catch (err: any) {
      if (err?.response?.status === 404 || !err.response) {
        const all = getLocalRooms();
        const found = all.find((r) => r.id === id);
        if (found) {
          found.status = 'FINISHED';
          found.currentSessionStart = null;
          found.version += 1;
          saveLocalRooms(all);
          return { ...found, serverTime: new Date().toISOString() };
        }
      }
      throw err;
    }
  },

  async leaveRoom(id: string): Promise<void> {
    try {
      await apiClient.post(`/rooms/${id}/leave`);
    } catch (err: any) {
      if (err?.response?.status === 404 || !err.response) {
        const user = getCurrentUser();
        const all = getLocalRooms();
        const found = all.find((r) => r.id === id);
        if (found) {
          found.participants = found.participants.filter((p) => p.userId !== user.id);
          if (found.participants.length === 0) {
            // Delete empty room as per Section 11.2 U-07
            const filtered = all.filter((r) => r.id !== id);
            saveLocalRooms(filtered);
          } else {
            // Transfer creator if the leaving user was creator
            if (found.creatorId === user.id) {
              found.creatorId = found.participants[0].userId;
              found.participants[0].isCreator = true;
            }
            found.version += 1;
            saveLocalRooms(all);
          }
        }
        return;
      }
      throw err;
    }
  },
};
