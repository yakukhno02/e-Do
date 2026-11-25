import React, { useCallback, useEffect, useState } from 'react';
import {
  ChevronLeft,
  ChevronRight,
  Clock,
  RefreshCw,
  ShieldAlert,
  Trash2,
  Users,
} from 'lucide-react';
import { adminApi } from '../api/admin.api';
import { extractErrorMessage } from '../api/client';
import { Alert } from '../components/common/Alert';
import { RoomStatusBadge } from '../components/common/Badge';
import { Button } from '../components/common/Button';
import { useAuth } from '../hooks/useAuth';
import type { AdminRoomItem, AdminUserItem, PageResponse } from '../types/api';

type AdminTab = 'users' | 'rooms';

export const AdminPage: React.FC = () => {
  const { user: currentUser } = useAuth();
  const [activeTab, setActiveTab] = useState<AdminTab>('users');

  // Users Tab State
  const [usersPage, setUsersPage] = useState<PageResponse<AdminUserItem>>({
    content: [],
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
  });
  const [usersCurrentPage, setUsersCurrentPage] = useState(0);
  const [isLoadingUsers, setIsLoadingUsers] = useState(true);

  // Rooms Tab State
  const [roomsPage, setRoomsPage] = useState<PageResponse<AdminRoomItem>>({
    content: [],
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
  });
  const [roomsCurrentPage, setRoomsCurrentPage] = useState(0);
  const [isLoadingRooms, setIsLoadingRooms] = useState(true);

  // Alerts & Modals
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const [userToDelete, setUserToDelete] = useState<AdminUserItem | null>(null);
  const [roomToDelete, setRoomToDelete] = useState<AdminRoomItem | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  const loadUsers = useCallback(async (page: number) => {
    try {
      setIsLoadingUsers(true);
      const data = await adminApi.getUsers(page, 10);
      setUsersPage(data);
    } catch (err: any) {
      setError(extractErrorMessage(err, 'Не вдалося завантажити список користувачів'));
    } finally {
      setIsLoadingUsers(false);
    }
  }, []);

  const loadRooms = useCallback(async (page: number) => {
    try {
      setIsLoadingRooms(true);
      const data = await adminApi.getRooms(page, 10);
      setRoomsPage(data);
    } catch (err: any) {
      setError(extractErrorMessage(err, 'Не вдалося завантажити список кімнат'));
    } finally {
      setIsLoadingRooms(false);
    }
  }, []);

  useEffect(() => {
    if (activeTab === 'users') {
      loadUsers(usersCurrentPage);
    } else {
      loadRooms(roomsCurrentPage);
    }
  }, [activeTab, usersCurrentPage, roomsCurrentPage, loadUsers, loadRooms]);

  const handleDeleteUser = async () => {
    if (!userToDelete) return;
    try {
      setIsDeleting(true);
      await adminApi.deleteUser(userToDelete.id);
      setSuccess(`Користувача ${userToDelete.name} (@${userToDelete.username}) успішно видалено`);
      setUserToDelete(null);
      loadUsers(usersCurrentPage);
      setTimeout(() => setSuccess(null), 3000);
    } catch (err: any) {
      setError(extractErrorMessage(err, 'Не вдалося видалити користувача'));
    } finally {
      setIsDeleting(false);
    }
  };

  const handleDeleteRoom = async () => {
    if (!roomToDelete) return;
    try {
      setIsDeleting(true);
      await adminApi.deleteRoom(roomToDelete.id);
      setSuccess(`Кімнату «${roomToDelete.name}» (код: ${roomToDelete.code}) успішно видалено`);
      setRoomToDelete(null);
      loadRooms(roomsCurrentPage);
      setTimeout(() => setSuccess(null), 3000);
    } catch (err: any) {
      setError(extractErrorMessage(err, 'Не вдалося видалити кімнату'));
    } finally {
      setIsDeleting(false);
    }
  };

  const formatDate = (isoString?: string | null) => {
    if (!isoString) return '—';
    try {
      return new Date(isoString).toLocaleDateString('uk-UA', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
      });
    } catch {
      return isoString;
    }
  };

  return (
    <div className="space-y-6 py-4">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2.5">
            <h1 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight flex items-center gap-3">
              <ShieldAlert className="w-8 h-8 text-rose-600" />
              Панель адміністратора
            </h1>
            <span className="px-2.5 py-0.5 rounded-full text-xs font-black bg-rose-50 text-rose-700 border border-rose-200">
              ADMIN ONLY
            </span>
          </div>
          <p className="text-xs sm:text-sm text-slate-500 mt-1">
            Керування зареєстрованими обліковими записами та створеними Pomodoro-кімнатами
          </p>
        </div>

        <Button
          variant="outline"
          size="sm"
          onClick={() => {
            if (activeTab === 'users') loadUsers(usersCurrentPage);
            else loadRooms(roomsCurrentPage);
          }}
          isLoading={isLoadingUsers || isLoadingRooms}
          title="Оновити дані"
        >
          <RefreshCw className="w-4 h-4 mr-1.5" />
          Оновити
        </Button>
      </div>

      {error && <Alert type="error" message={error} onClose={() => setError(null)} />}
      {success && <Alert type="success" message={success} onClose={() => setSuccess(null)} />}

      {/* Tabs Switcher */}
      <div className="flex items-center gap-2 border-b border-slate-200">
        <button
          onClick={() => setActiveTab('users')}
          className={`flex items-center gap-2 px-5 py-3 text-sm font-bold border-b-2 transition-all cursor-pointer ${
            activeTab === 'users'
              ? 'border-indigo-600 text-indigo-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <Users className="w-4 h-4" />
          Користувачі ({usersPage.totalElements})
        </button>

        <button
          onClick={() => setActiveTab('rooms')}
          className={`flex items-center gap-2 px-5 py-3 text-sm font-bold border-b-2 transition-all cursor-pointer ${
            activeTab === 'rooms'
              ? 'border-indigo-600 text-indigo-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <Clock className="w-4 h-4" />
          Спільні кімнати ({roomsPage.totalElements})
        </button>
      </div>

      {/* TAB 1: USERS (FR-43, FR-44) */}
      {activeTab === 'users' && (
        <div className="bg-white rounded-3xl border border-slate-200 shadow-xs overflow-hidden">
          {isLoadingUsers ? (
            <div className="py-20 flex flex-col items-center justify-center">
              <div className="w-8 h-8 border-3 border-indigo-600 border-t-transparent rounded-full animate-spin mb-3" />
              <p className="text-xs font-semibold text-slate-500">Завантаження користувачів...</p>
            </div>
          ) : usersPage.content.length === 0 ? (
            <div className="p-12 text-center text-sm text-slate-500">Користувачів не знайдено</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="bg-slate-50/80 text-[11px] font-bold text-slate-500 uppercase tracking-wider border-b border-slate-100">
                    <th className="py-3 px-6">Користувач</th>
                    <th className="py-3 px-6">Email</th>
                    <th className="py-3 px-6">Роль</th>
                    <th className="py-3 px-6">Реєстрація</th>
                    <th className="py-3 px-6">Pomodoro</th>
                    <th className="py-3 px-6">Фокус</th>
                    <th className="py-3 px-6 text-right">Дія</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 text-xs">
                  {usersPage.content.map((u) => {
                    const isSelf = u.id === currentUser?.id;

                    return (
                      <tr
                        key={u.id}
                        className="hover:bg-slate-50/60 transition-colors font-medium text-slate-700"
                      >
                        <td className="py-4 px-6">
                          <div className="flex items-center gap-3">
                            <div className="w-8 h-8 rounded-xl bg-indigo-600 text-white flex items-center justify-center font-bold text-xs shadow-2xs">
                              {u.name.charAt(0)}
                            </div>
                            <div>
                              <p className="font-bold text-slate-900 leading-tight">{u.name}</p>
                              <p className="text-[11px] text-slate-400">@{u.username}</p>
                            </div>
                          </div>
                        </td>

                        <td className="py-4 px-6 text-slate-600">{u.email}</td>

                        <td className="py-4 px-6">
                          <span
                            className={`px-2 py-0.5 rounded-lg text-[10px] font-extrabold ${
                              u.role === 'ADMIN'
                                ? 'bg-rose-50 text-rose-700 border border-rose-200'
                                : 'bg-slate-100 text-slate-600'
                            }`}
                          >
                            {u.role}
                          </span>
                        </td>

                        <td className="py-4 px-6 text-slate-500">{formatDate(u.createdAt)}</td>

                        <td className="py-4 px-6 font-semibold text-slate-900">
                          {u.completedPomodoros} шт
                        </td>

                        <td className="py-4 px-6 text-slate-600">
                          {Math.round(u.totalFocusMinutes / 60)} год ({u.totalFocusMinutes} хв)
                        </td>

                        <td className="py-4 px-6 text-right">
                          <button
                            onClick={() => setUserToDelete(u)}
                            disabled={isSelf}
                            className={`p-1.5 rounded-xl transition-colors cursor-pointer ${
                              isSelf
                                ? 'text-slate-300 cursor-not-allowed'
                                : 'text-slate-400 hover:text-rose-600 hover:bg-rose-50'
                            }`}
                            title={isSelf ? 'Не можна видалити власний акаунт' : 'Видалити акаунт'}
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}

          {/* Pagination */}
          <div className="p-4 sm:p-6 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
            <span>
              Всього користувачів: <strong>{usersPage.totalElements}</strong>
            </span>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                disabled={usersCurrentPage <= 0 || isLoadingUsers}
                onClick={() => setUsersCurrentPage((p) => Math.max(0, p - 1))}
              >
                <ChevronLeft className="w-4 h-4 mr-1" />
                Попередня
              </Button>
              <Button
                variant="outline"
                size="sm"
                disabled={usersCurrentPage >= usersPage.totalPages - 1 || isLoadingUsers}
                onClick={() => setUsersCurrentPage((p) => p + 1)}
              >
                Наступна
                <ChevronRight className="w-4 h-4 ml-1" />
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* TAB 2: ROOMS (FR-45, FR-46) */}
      {activeTab === 'rooms' && (
        <div className="bg-white rounded-3xl border border-slate-200 shadow-xs overflow-hidden">
          {isLoadingRooms ? (
            <div className="py-20 flex flex-col items-center justify-center">
              <div className="w-8 h-8 border-3 border-indigo-600 border-t-transparent rounded-full animate-spin mb-3" />
              <p className="text-xs font-semibold text-slate-500">Завантаження кімнат...</p>
            </div>
          ) : roomsPage.content.length === 0 ? (
            <div className="p-12 text-center text-sm text-slate-500">Кімнат не знайдено</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="bg-slate-50/80 text-[11px] font-bold text-slate-500 uppercase tracking-wider border-b border-slate-100">
                    <th className="py-3 px-6">Код</th>
                    <th className="py-3 px-6">Назва кімнати</th>
                    <th className="py-3 px-6">Статус</th>
                    <th className="py-3 px-6">Фокус / Перерва</th>
                    <th className="py-3 px-6">Учасників</th>
                    <th className="py-3 px-6">Створено</th>
                    <th className="py-3 px-6 text-right">Дія</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 text-xs">
                  {roomsPage.content.map((r) => (
                    <tr
                      key={r.id}
                      className="hover:bg-slate-50/60 transition-colors font-medium text-slate-700"
                    >
                      <td className="py-4 px-6 font-mono font-black text-indigo-700 tracking-wider">
                        {r.code}
                      </td>

                      <td className="py-4 px-6 font-bold text-slate-900">{r.name}</td>

                      <td className="py-4 px-6">
                        <RoomStatusBadge status={r.status} />
                      </td>

                      <td className="py-4 px-6 text-slate-600">
                        {r.pomodoroDuration} хв / {r.breakDuration} хв
                      </td>

                      <td className="py-4 px-6 font-semibold text-slate-800">
                        {r.participantsCount} чол
                      </td>

                      <td className="py-4 px-6 text-slate-500">{formatDate(r.createdAt)}</td>

                      <td className="py-4 px-6 text-right">
                        <button
                          onClick={() => setRoomToDelete(r)}
                          className="p-1.5 rounded-xl text-slate-400 hover:text-rose-600 hover:bg-rose-50 transition-colors cursor-pointer"
                          title="Примусово видалити кімнату"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {/* Pagination */}
          <div className="p-4 sm:p-6 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
            <span>
              Всього кімнат: <strong>{roomsPage.totalElements}</strong>
            </span>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                disabled={roomsCurrentPage <= 0 || isLoadingRooms}
                onClick={() => setRoomsCurrentPage((p) => Math.max(0, p - 1))}
              >
                <ChevronLeft className="w-4 h-4 mr-1" />
                Попередня
              </Button>
              <Button
                variant="outline"
                size="sm"
                disabled={roomsCurrentPage >= roomsPage.totalPages - 1 || isLoadingRooms}
                onClick={() => setRoomsCurrentPage((p) => p + 1)}
              >
                Наступна
                <ChevronRight className="w-4 h-4 ml-1" />
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Delete User Confirmation Modal (FR-44) */}
      {userToDelete && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-xs">
          <div className="bg-white rounded-3xl max-w-sm w-full p-6 shadow-2xl border border-slate-100 space-y-4">
            <h4 className="text-lg font-bold text-slate-900">Видалити користувача?</h4>
            <p className="text-xs text-slate-500">
              Ви збираєтеся назавжди видалити користувача{' '}
              <strong>
                {userToDelete.name} (@{userToDelete.username})
              </strong>
              . Всі пов'язані завдання та сесії будуть скасовані, а кімнати перепідписані.
            </p>
            <div className="flex items-center justify-end gap-3 pt-2">
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setUserToDelete(null)}
                disabled={isDeleting}
              >
                Скасувати
              </Button>
              <Button
                variant="primary"
                size="sm"
                onClick={handleDeleteUser}
                isLoading={isDeleting}
                className="bg-rose-600 hover:bg-rose-700 text-white"
              >
                Так, видалити
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Delete Room Confirmation Modal (FR-46) */}
      {roomToDelete && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-xs">
          <div className="bg-white rounded-3xl max-w-sm w-full p-6 shadow-2xl border border-slate-100 space-y-4">
            <h4 className="text-lg font-bold text-slate-900">Видалити спільну кімнату?</h4>
            <p className="text-xs text-slate-500">
              Ви збираєтеся примусово видалити кімнату <strong>«{roomToDelete.name}»</strong> (код:{' '}
              {roomToDelete.code}). Усі активні сесії учасників буде скасовано.
            </p>
            <div className="flex items-center justify-end gap-3 pt-2">
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setRoomToDelete(null)}
                disabled={isDeleting}
              >
                Скасувати
              </Button>
              <Button
                variant="primary"
                size="sm"
                onClick={handleDeleteRoom}
                isLoading={isDeleting}
                className="bg-rose-600 hover:bg-rose-700 text-white"
              >
                Так, видалити
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
