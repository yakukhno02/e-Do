import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import type { ApiErrorResponse } from '../types/api';

export const TOKEN_STORAGE_KEY = 'accessToken';
export const USER_STORAGE_KEY = 'currentUser';

export const apiClient = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  },
  timeout: 10000,
});

// Request Interceptor: Attach Bearer Token if available
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem(TOKEN_STORAGE_KEY);
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response Interceptor: Handle 401 Unauthorized
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiErrorResponse>) => {
    if (error.response?.status === 401) {
      // Don't auto-redirect if we are already on auth pages (login/register)
      const currentPath = window.location.pathname;
      const isAuthPage = currentPath === '/login' || currentPath === '/register' || currentPath === '/forgot-password' || currentPath === '/reset-password';
      
      localStorage.removeItem(TOKEN_STORAGE_KEY);
      localStorage.removeItem(USER_STORAGE_KEY);
      window.dispatchEvent(new Event('auth:unauthorized'));

      if (!isAuthPage && !currentPath.startsWith('/login')) {
        window.location.href = `/login?redirect=${encodeURIComponent(currentPath)}`;
      }
    }
    return Promise.reject(error);
  }
);

/**
 * Extracts a friendly error message from AxiosError or returns fallback
 */
export function extractErrorMessage(error: unknown, fallbackMessage = 'Сталася непередбачена помилка'): string {
  if (axios.isAxiosError(error)) {
    const apiError = error.response?.data as ApiErrorResponse | undefined;
    if (apiError?.message) {
      return apiError.message;
    }
    if (apiError?.error) {
      return apiError.error;
    }
    if (error.message) {
      return error.message;
    }
  }
  if (error instanceof Error) {
    return error.message;
  }
  return fallbackMessage;
}

/**
 * Extracts field-specific validation errors from AxiosError
 */
export function extractValidationErrors(error: unknown): Record<string, string> {
  const result: Record<string, string> = {};
  if (axios.isAxiosError(error)) {
    const apiError = error.response?.data as ApiErrorResponse | undefined;
    if (apiError?.validationErrors && Array.isArray(apiError.validationErrors)) {
      apiError.validationErrors.forEach((ve) => {
        if (ve.field && ve.message) {
          result[ve.field] = ve.message;
        }
      });
    }
  }
  return result;
}
