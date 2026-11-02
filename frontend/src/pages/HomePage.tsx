import React from 'react';
import { Link as RouterLink } from 'react-router-dom';
import {
  ArrowRight,
  CheckCircle2,
  Clock,
  Sparkles,
  Users,
} from 'lucide-react';
import { Button } from '../components/common/Button';
import { useAuth } from '../hooks/useAuth';

export const HomePage: React.FC = () => {
  const { isAuthenticated, user } = useAuth();

  return (
    <div className="space-y-20 py-6">
      {/* Hero Section */}
      <section className="text-center max-w-4xl mx-auto space-y-6 pt-4">
        <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-indigo-50 border border-indigo-200 text-indigo-700 text-xs font-bold tracking-wide uppercase animate-pulse">
          <Sparkles className="w-3.5 h-3.5" />
          Спринт 1 · Доступна рання версія MVP
        </div>

        <h1 className="text-4xl sm:text-6xl font-black text-slate-900 tracking-tight leading-[1.15]">
          Зосередьтеся на важливому разом із{' '}
          <span className="bg-gradient-to-r from-indigo-600 to-violet-600 bg-clip-text text-transparent">
            e-Do
          </span>
        </h1>

        <p className="text-lg sm:text-xl text-slate-600 max-w-2xl mx-auto leading-relaxed">
          Сучасний таск-менеджер із технікою Pomodoro, спільними кімнатами для синхронної роботи в команді та аналітикою особистої продуктивності.
        </p>

        <div className="flex flex-col sm:flex-row items-center justify-center gap-4 pt-4">
          {isAuthenticated ? (
            <RouterLink to="/tasks">
              <Button size="lg" className="w-full sm:w-auto shadow-lg shadow-indigo-200">
                Перейти до моїх завдань
                <ArrowRight className="w-4 h-4 ml-2" />
              </Button>
            </RouterLink>
          ) : (
            <>
              <RouterLink to="/register">
                <Button size="lg" className="w-full sm:w-auto shadow-lg shadow-indigo-200">
                  Почати безкоштовно
                  <ArrowRight className="w-4 h-4 ml-2" />
                </Button>
              </RouterLink>
              <RouterLink to="/login">
                <Button variant="outline" size="lg" className="w-full sm:w-auto">
                  Увійти в акаунт
                </Button>
              </RouterLink>
            </>
          )}
        </div>

        {isAuthenticated && user && (
          <p className="text-xs text-slate-500 font-medium">
            Ви увійшли як <span className="font-bold text-slate-700">{user.name}</span> (@{user.username})
          </p>
        )}
      </section>

      {/* Feature Preview Cards */}
      <section className="grid grid-cols-1 md:grid-cols-3 gap-8">
        <div className="bg-white p-8 rounded-3xl border border-slate-200/80 shadow-xs hover:shadow-md transition-shadow space-y-4">
          <div className="w-12 h-12 rounded-2xl bg-indigo-50 text-indigo-600 flex items-center justify-center font-bold">
            <CheckCircle2 className="w-6 h-6" />
          </div>
          <h3 className="text-xl font-bold text-slate-900">Керування завданнями</h3>
          <p className="text-sm text-slate-600 leading-relaxed">
            Створюйте завдання, визначайте дедлайни та пріоритети (LOW, MEDIUM, HIGH), швидко змінюйте статуси виконання одним кліком.
          </p>
        </div>

        <div className="bg-white p-8 rounded-3xl border border-slate-200/80 shadow-xs hover:shadow-md transition-shadow space-y-4">
          <div className="w-12 h-12 rounded-2xl bg-violet-50 text-violet-600 flex items-center justify-center font-bold">
            <Clock className="w-6 h-6" />
          </div>
          <h3 className="text-xl font-bold text-slate-900">Індивідуальний Pomodoro</h3>
          <p className="text-sm text-slate-600 leading-relaxed">
            Гнучкі сесії фокусування (від 1 до 120 хв) та перерви (від 1 до 60 хв). Автоматичне нарахування заробленого часу фокусу.
          </p>
        </div>

        <div className="bg-white p-8 rounded-3xl border border-slate-200/80 shadow-xs hover:shadow-md transition-shadow space-y-4">
          <div className="w-12 h-12 rounded-2xl bg-emerald-50 text-emerald-600 flex items-center justify-center font-bold">
            <Users className="w-6 h-6" />
          </div>
          <h3 className="text-xl font-bold text-slate-900">Спільні кімнати фокусу</h3>
          <p className="text-sm text-slate-600 leading-relaxed">
            Створюйте приватні кімнати з унікальним 6-значним кодом. Синхронізація таймера між усіма учасниками з точністю до 2 секунд.
          </p>
        </div>
      </section>

      {/* Tech Architecture highlights */}
      <section className="bg-gradient-to-r from-slate-900 via-indigo-950 to-slate-900 text-white rounded-3xl p-8 sm:p-12 shadow-xl">
        <div className="max-w-3xl space-y-4">
          <span className="text-xs font-bold uppercase tracking-wider text-indigo-400">
            Архітектура та Надійність
          </span>
          <h2 className="text-2xl sm:text-3xl font-extrabold tracking-tight">
            Побудовано за строгими інженерними стандартами
          </h2>
          <p className="text-slate-300 text-sm leading-relaxed">
            Java 21 LTS, Spring Boot 3.3.x з повноцінною ізоляцією даних (NFR-06), BCrypt хешуванням паролів (NFR-04), PostgreSQL 16 з міграціями Flyway та сучасним SPA-клієнтом на React і TypeScript.
          </p>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 pt-4 border-t border-slate-800 text-xs">
            <div>
              <span className="block text-slate-400 font-medium">Автентифікація</span>
              <span className="font-bold text-white">Stateless JWT</span>
            </div>
            <div>
              <span className="block text-slate-400 font-medium">Час відповіді</span>
              <span className="font-bold text-white">≤ 2 сек (NFR-01)</span>
            </div>
            <div>
              <span className="block text-slate-400 font-medium">API контракт</span>
              <span className="font-bold text-white">REST / JSON</span>
            </div>
            <div>
              <span className="block text-slate-400 font-medium">Адаптивність</span>
              <span className="font-bold text-white">від 360 px (NFR-09)</span>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
};
