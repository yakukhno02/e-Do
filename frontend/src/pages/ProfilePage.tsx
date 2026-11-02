import React from 'react';
import { Clock, Flame, Target } from 'lucide-react';
import { useAuth } from '../hooks/useAuth';

export const ProfilePage: React.FC = () => {
  const { user } = useAuth();

  return (
    <div className="max-w-4xl mx-auto space-y-8 py-6">
      <div className="bg-white p-8 rounded-3xl border border-slate-200 shadow-xs flex flex-col sm:flex-row items-center gap-6">
        <div className="w-20 h-20 rounded-2xl bg-indigo-600 text-white flex items-center justify-center text-3xl font-extrabold shadow-md shadow-indigo-200">
          {user?.name?.charAt(0) || 'U'}
        </div>
        <div className="text-center sm:text-left space-y-1">
          <h1 className="text-2xl font-black text-slate-900">{user?.name}</h1>
          <p className="text-sm text-slate-500">@{user?.username} · {user?.email}</p>
          <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-bold bg-indigo-50 text-indigo-700 mt-2">
            Роль: {user?.role}
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
        <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-2xs space-y-2">
          <div className="flex items-center gap-2 text-indigo-600 text-xs font-bold">
            <Flame className="w-4 h-4" />
            Виконано Pomodoro
          </div>
          <p className="text-3xl font-black text-slate-900">0</p>
          <p className="text-xs text-slate-400">Сесій завершено</p>
        </div>

        <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-2xs space-y-2">
          <div className="flex items-center gap-2 text-violet-600 text-xs font-bold">
            <Clock className="w-4 h-4" />
            Час фокусування
          </div>
          <p className="text-3xl font-black text-slate-900">0 хв</p>
          <p className="text-xs text-slate-400">Загальний час</p>
        </div>

        <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-2xs space-y-2">
          <div className="flex items-center gap-2 text-emerald-600 text-xs font-bold">
            <Target className="w-4 h-4" />
            Спільні сесії
          </div>
          <p className="text-3xl font-black text-slate-900">0</p>
          <p className="text-xs text-slate-400">У командних кімнатах</p>
        </div>
      </div>
    </div>
  );
};
