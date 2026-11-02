import { apiClient } from './client';
import type {
  AuthResponse,
  ForgotPasswordRequest,
  LoginRequest,
  MessageResponse,
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
    await apiClient.post('/auth/logout');
  },

  async forgotPassword(data: ForgotPasswordRequest): Promise<MessageResponse> {
    const response = await apiClient.post<MessageResponse>('/auth/forgot-password', data);
    return response.data;
  },

  async resetPassword(data: ResetPasswordRequest): Promise<void> {
    await apiClient.post('/auth/reset-password', data);
  },

  async getProfile(): Promise<UserProfile> {
    const response = await apiClient.get<UserProfile>('/users/me');
    return response.data;
  },
};
