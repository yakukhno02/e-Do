import { apiClient } from './client';
import type {
  TaskCreateRequest,
  TaskFilterParams,
  TaskItem,
  TaskStatus,
  TaskUpdateRequest,
} from '../types/api';

const LOCAL_TASKS_KEY = 'edo_local_tasks_cache';

function getLocalTasks(): TaskItem[] {
  try {
    const raw = localStorage.getItem(LOCAL_TASKS_KEY);
    if (raw) return JSON.parse(raw);
  } catch (e) {
    console.error('Failed to parse local tasks:', e);
  }
  // Default sample tasks if empty
  const initial: TaskItem[] = [
    {
      id: 'task-sample-1',
      title: 'Спроєктувати схему БД для e-Do',
      description: 'Створити DDL таблиць users, tasks, rooms, pomodoro_sessions',
      deadline: new Date(Date.now() + 86400000 * 2).toISOString(),
      status: 'DONE',
      priority: 'HIGH',
      createdAt: new Date(Date.now() - 86400000 * 3).toISOString(),
      updatedAt: new Date(Date.now() - 86400000 * 1).toISOString(),
    },
    {
      id: 'task-sample-2',
      title: 'Налаштувати JWT автентифікацію',
      description: 'Інтегрувати Spring Security з JwtAuthenticationFilter та BCrypt',
      deadline: new Date(Date.now() + 86400000 * 3).toISOString(),
      status: 'IN_PROGRESS',
      priority: 'HIGH',
      createdAt: new Date(Date.now() - 86400000 * 2).toISOString(),
      updatedAt: new Date().toISOString(),
    },
    {
      id: 'task-sample-3',
      title: 'Розробити інтерфейс таск-менеджера',
      description: 'Створити сторінку списку завдань із можливістю швидкої зміни статусу та фільтрації',
      deadline: new Date(Date.now() + 86400000 * 5).toISOString(),
      status: 'TODO',
      priority: 'MEDIUM',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    },
  ];
  localStorage.setItem(LOCAL_TASKS_KEY, JSON.stringify(initial));
  return initial;
}

function saveLocalTasks(tasks: TaskItem[]) {
  localStorage.setItem(LOCAL_TASKS_KEY, JSON.stringify(tasks));
}

export const tasksApi = {
  async getTasks(filters?: TaskFilterParams): Promise<TaskItem[]> {
    try {
      const response = await apiClient.get<TaskItem[]>('/tasks', { params: filters });
      return response.data;
    } catch (err: any) {
      // If endpoint doesn't exist yet on backend (404/500/network), fallback gracefully to local storage
      if (err?.response?.status === 404 || !err.response) {
        let tasks = getLocalTasks();
        if (filters?.status) {
          tasks = tasks.filter((t) => t.status === filters.status);
        }
        if (filters?.priority) {
          tasks = tasks.filter((t) => t.priority === filters.priority);
        }
        return tasks;
      }
      throw err;
    }
  },

  async createTask(data: TaskCreateRequest): Promise<TaskItem> {
    try {
      const response = await apiClient.post<TaskItem>('/tasks', data);
      return response.data;
    } catch (err: any) {
      if (err?.response?.status === 404 || !err.response) {
        const newTask: TaskItem = {
          id: crypto.randomUUID ? crypto.randomUUID() : `task-${Date.now()}`,
          title: data.title,
          description: data.description || null,
          deadline: data.deadline || null,
          status: data.status,
          priority: data.priority,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        };
        const all = getLocalTasks();
        all.unshift(newTask);
        saveLocalTasks(all);
        return newTask;
      }
      throw err;
    }
  },

  async getTask(id: string): Promise<TaskItem> {
    try {
      const response = await apiClient.get<TaskItem>(`/tasks/${id}`);
      return response.data;
    } catch (err: any) {
      if (err?.response?.status === 404 || !err.response) {
        const found = getLocalTasks().find((t) => t.id === id);
        if (found) return found;
      }
      throw err;
    }
  },

  async updateTask(id: string, data: TaskUpdateRequest): Promise<TaskItem> {
    try {
      const response = await apiClient.put<TaskItem>(`/tasks/${id}`, data);
      return response.data;
    } catch (err: any) {
      if (err?.response?.status === 404 || !err.response) {
        const all = getLocalTasks();
        const idx = all.findIndex((t) => t.id === id);
        if (idx !== -1) {
          all[idx] = {
            ...all[idx],
            title: data.title,
            description: data.description || null,
            deadline: data.deadline || null,
            status: data.status,
            priority: data.priority,
            updatedAt: new Date().toISOString(),
          };
          saveLocalTasks(all);
          return all[idx];
        }
      }
      throw err;
    }
  },

  async updateStatus(id: string, status: TaskStatus): Promise<TaskItem> {
    try {
      const response = await apiClient.patch<TaskItem>(`/tasks/${id}/status`, { status });
      return response.data;
    } catch (err: any) {
      if (err?.response?.status === 404 || !err.response) {
        const all = getLocalTasks();
        const idx = all.findIndex((t) => t.id === id);
        if (idx !== -1) {
          all[idx] = {
            ...all[idx],
            status,
            updatedAt: new Date().toISOString(),
          };
          saveLocalTasks(all);
          return all[idx];
        }
      }
      throw err;
    }
  },

  async deleteTask(id: string): Promise<void> {
    try {
      await apiClient.delete(`/tasks/${id}`);
    } catch (err: any) {
      if (err?.response?.status === 404 || !err.response) {
        const all = getLocalTasks().filter((t) => t.id !== id);
        saveLocalTasks(all);
        return;
      }
      throw err;
    }
  },
};
