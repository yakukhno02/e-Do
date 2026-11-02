import React, { createContext, useContext, useEffect, useState } from 'react';
import { authApi } from '../api/auth.api';
import { TOKEN_STORAGE_KEY, USER_STORAGE_KEY } from '../api/client';
import type { LoginRequest, RegisterRequest, UserSummary } from '../types/api';

interface AuthContextType {
  user: UserSummary | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (data: LoginRequest) => Promise<void>;
  register: (data: RegisterRequest) => Promise<void>;
  logout: () => Promise<void>;
  setUser: React.Dispatch<React.SetStateAction<UserSummary | null>>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem(TOKEN_STORAGE_KEY));
  const [user, setUser] = useState<UserSummary | null>(() => {
    const saved = localStorage.getItem(USER_STORAGE_KEY);
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch (e) {
        console.error('Failed to parse cached user:', e);
      }
    }
    return null;
  });
  const [isLoading, setIsLoading] = useState<boolean>(true);

  // Initialize auth state
  useEffect(() => {
    const initAuth = async () => {
      const storedToken = localStorage.getItem(TOKEN_STORAGE_KEY);
      if (storedToken) {
        try {
          const profile = await authApi.getProfile();
          setUser({
            id: profile.id,
            name: profile.name,
            username: profile.username,
            email: profile.email,
            role: profile.role,
          });
          localStorage.setItem(
            USER_STORAGE_KEY,
            JSON.stringify({
              id: profile.id,
              name: profile.name,
              username: profile.username,
              email: profile.email,
              role: profile.role,
            })
          );
        } catch (err) {
          console.warn('Could not refresh profile on mount:', err);
        }
      }
      setIsLoading(false);
    };

    initAuth();

    // Listen to 401 unauthorized events from axios interceptor
    const handleUnauthorized = () => {
      setToken(null);
      setUser(null);
    };

    window.addEventListener('auth:unauthorized', handleUnauthorized);
    return () => window.removeEventListener('auth:unauthorized', handleUnauthorized);
  }, []);

  const login = async (credentials: LoginRequest) => {
    const res = await authApi.login(credentials);
    localStorage.setItem(TOKEN_STORAGE_KEY, res.token);
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(res.user));
    setToken(res.token);
    setUser(res.user);
  };

  const register = async (data: RegisterRequest) => {
    const res = await authApi.register(data);
    localStorage.setItem(TOKEN_STORAGE_KEY, res.token);
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(res.user));
    setToken(res.token);
    setUser(res.user);
  };

  const logout = async () => {
    try {
      await authApi.logout();
    } catch (err) {
      console.warn('Error during server logout:', err);
    } finally {
      localStorage.removeItem(TOKEN_STORAGE_KEY);
      localStorage.removeItem(USER_STORAGE_KEY);
      setToken(null);
      setUser(null);
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: !!token && !!user,
        isLoading,
        login,
        register,
        logout,
        setUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
