import React from 'react';
import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { AuthLayout } from '../layouts/AuthLayout';
import { MainLayout } from '../layouts/MainLayout';
import { ForgotPasswordPage } from '../pages/ForgotPasswordPage';
import { HomePage } from '../pages/HomePage';
import { LoginPage } from '../pages/LoginPage';
import { NotFoundPage } from '../pages/NotFoundPage';
import { PomodoroPage } from '../pages/PomodoroPage';
import { ProfilePage } from '../pages/ProfilePage';
import { RegisterPage } from '../pages/RegisterPage';
import { ResetPasswordPage } from '../pages/ResetPasswordPage';
import { RoomsPage } from '../pages/RoomsPage';
import { TasksPage } from '../pages/TasksPage';
import { PrivateRoute } from './PrivateRoute';
import { PublicRoute } from './PublicRoute';

export const AppRouter: React.FC = () => {
  return (
    <BrowserRouter>
      <Routes>
        {/* Public & App Pages in MainLayout */}
        <Route element={<MainLayout />}>
          <Route path="/" element={<HomePage />} />
          
          {/* Protected Routes */}
          <Route
            path="/tasks"
            element={
              <PrivateRoute>
                <TasksPage />
              </PrivateRoute>
            }
          />
          <Route
            path="/pomodoro"
            element={
              <PrivateRoute>
                <PomodoroPage />
              </PrivateRoute>
            }
          />
          <Route
            path="/rooms"
            element={
              <PrivateRoute>
                <RoomsPage />
              </PrivateRoute>
            }
          />
          <Route
            path="/profile"
            element={
              <PrivateRoute>
                <ProfilePage />
              </PrivateRoute>
            }
          />

          <Route path="*" element={<NotFoundPage />} />
        </Route>

        {/* Auth Pages in AuthLayout (Guest Only, redirects if already authenticated) */}
        <Route element={<AuthLayout />}>
          <Route
            path="/login"
            element={
              <PublicRoute>
                <LoginPage />
              </PublicRoute>
            }
          />
          <Route
            path="/register"
            element={
              <PublicRoute>
                <RegisterPage />
              </PublicRoute>
            }
          />
          <Route
            path="/forgot-password"
            element={
              <PublicRoute>
                <ForgotPasswordPage />
              </PublicRoute>
            }
          />
          <Route
            path="/reset-password"
            element={
              <PublicRoute>
                <ResetPasswordPage />
              </PublicRoute>
            }
          />
        </Route>
      </Routes>
    </BrowserRouter>
  );
};
