import React, { useCallback, useEffect, useState } from 'react';
import {
  BarChart2,
  Calendar,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Clock,
  Filter,
  Flame,
  RefreshCw,
  Trophy,
  Users,
  XCircle,
} from 'lucide-react';
import { extractErrorMessage } from '../api/client';
import { statsApi } from '../api/stats.api';
import { Alert } from '../components/common/Alert';
import { Button } from '../components/common/Button';
import type {
  PageResponse,
  SessionHistoryItem,
  SessionStatus,
  SessionType,
  StatisticsSummary,
} from '../types/api';

export const StatsPage: React.FC = () => {
  const [summary, setSummary] = useState<StatisticsSummary | null>(null);
  const [historyPage, setHistoryPage] = useState<PageResponse<SessionHistoryItem>>({
    content: [],
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
  });

  const [isLoadingSummary, setIsLoadingSummary] = useState<boolean>(true);
  const [isLoadingHistory, setIsLoadingHistory] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Filters
  const [typeFilter, setTypeFilter] = useState<SessionType | 'ALL'>('ALL');
  const [statusFilter, setStatusFilter] = useState<SessionStatus | 'ALL'>('ALL');
  const [currentPage, setCurrentPage] = useState<number>(0);
  const [pageSize, setPageSize] = useState<number>(10);

  const loadSummary = useCallback(async () => {
    try {
      setIsLoadingSummary(true);
      const data = await statsApi.getSummary();
      setSummary(data);
    } catch (err: any) {
      setError(extractErrorMessage(err, 'Не вдалося завантажити загальну статистику'));
    } finally {
      setIsLoadingSummary(false);
    }
  }, []);

  const loadHistory = useCallback(async (page: number, size: number) => {
    try {
      setIsLoadingHistory(true);
      const data = await statsApi.getSessionHistory(page, size);
      setHistoryPage(data);
    } catch (err: any) {
      setError(extractErrorMessage(err, 'Не вдалося завантажити історію сесій'));
    } finally {
      setIsLoadingHistory(false);
    }
  }, []);

  useEffect(() => {
    loadSummary();
  }, [loadSummary]);

  useEffect(() => {
    loadHistory(currentPage, pageSize);
  }, [currentPage, pageSize, loadHistory]);

  const formatMinutesToHours = (totalMins: number) => {
    const hrs = Math.floor(totalMins / 60);
    const mins = totalMins % 60;
    if (hrs === 0) return `${mins} хв`;
    return `${hrs} год ${mins} хв`;
  };

  const formatDate = (isoString?: string | null) => {
    if (!isoString) return '—';
    try {
      return new Date(isoString).toLocaleString('uk-UA', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return isoString;
    }
  };

  // Filter content client-side if local data or combined
  const filteredSessions = historyPage.content.filter((s) => {
    if (typeFilter !== 'ALL' && s.type !== typeFilter) return false;
    if (statusFilter !== 'ALL' && s.status !== statusFilter) return false;
    return true;
  });

  return (
    <div className="space-y-8 py-4">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight flex items-center gap-3">
            <BarChart2 className="w-8 h-8 text-indigo-600" />
            Статистика та історія
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 mt-1">
            Аналітика продуктивності, завершених Pomodoro та історії фокус-сесій
          </p>
        </div>

        <Button
          variant="outline"
          size="sm"
          onClick={() => {
            loadSummary();
            loadHistory(currentPage, pageSize);
          }}
          isLoading={isLoadingSummary || isLoadingHistory}
          title="Оновити дані"
        >
          <RefreshCw className="w-4 h-4 mr-1.5" />
          Оновити
        </Button>
      </div>

      {error && <Alert type="error" message={error} onClose={() => setError(null)} />}

      {/* Summary KPI Cards (FR-36, FR-37, FR-38) */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-5">
        <div className="bg-white p-6 rounded-3xl border border-slate-200/90 shadow-2xs space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-indigo-600">
              Всього Pomodoro
            </span>
            <div className="p-2 rounded-xl bg-indigo-50 text-indigo-600">
              <Flame className="w-5 h-5" />
            </div>
          </div>
          <p className="text-3xl font-black text-slate-900">
            {summary?.totalCompletedPomodoros ?? 0}
          </p>
          <p className="text-xs text-slate-400">Успішно завершених фокус-сесій</p>
        </div>

        <div className="bg-white p-6 rounded-3xl border border-slate-200/90 shadow-2xs space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-violet-600">
              Загальний фокус-час
            </span>
            <div className="p-2 rounded-xl bg-violet-50 text-violet-600">
              <Clock className="w-5 h-5" />
            </div>
          </div>
          <p className="text-3xl font-black text-slate-900">
            {formatMinutesToHours(summary?.totalFocusMinutes ?? 0)}
          </p>
          <p className="text-xs text-slate-400">Часу без відволікань</p>
        </div>

        <div className="bg-white p-6 rounded-3xl border border-slate-200/90 shadow-2xs space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-emerald-600">
              Спільні сесії
            </span>
            <div className="p-2 rounded-xl bg-emerald-50 text-emerald-600">
              <Trophy className="w-5 h-5" />
            </div>
          </div>
          <p className="text-3xl font-black text-slate-900">
            {summary?.totalSharedSessions ?? 0}
          </p>
          <p className="text-xs text-slate-400">Сесій у спільних кімнатах</p>
        </div>
      </div>

      {/* History Table Container (FR-35) */}
      <div className="bg-white rounded-3xl border border-slate-200 shadow-xs overflow-hidden">
        {/* Table Header and Filters */}
        <div className="p-6 border-b border-slate-100 flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <h3 className="text-lg font-bold text-slate-900 flex items-center gap-2">
              <Calendar className="w-5 h-5 text-indigo-600" />
              Журнал фокус-сесій
            </h3>
            <p className="text-xs text-slate-500 mt-0.5">
              Повний перелік індивідуальних та спільних сесій
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            {/* Type Filter */}
            <div className="flex items-center gap-1.5 text-xs text-slate-500">
              <Filter className="w-3.5 h-3.5 text-slate-400" />
              <span>Тип:</span>
              <select
                value={typeFilter}
                onChange={(e) => setTypeFilter(e.target.value as any)}
                className="bg-slate-50 border border-slate-200 rounded-xl px-2.5 py-1.5 text-xs font-semibold text-slate-700 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              >
                <option value="ALL">Всі типи</option>
                <option value="INDIVIDUAL">Індивідуальні</option>
                <option value="SHARED">Спільні кімнати</option>
              </select>
            </div>

            {/* Status Filter */}
            <div className="flex items-center gap-1.5 text-xs text-slate-500">
              <span>Статус:</span>
              <select
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value as any)}
                className="bg-slate-50 border border-slate-200 rounded-xl px-2.5 py-1.5 text-xs font-semibold text-slate-700 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              >
                <option value="ALL">Всі статуси</option>
                <option value="COMPLETED">Завершені (COMPLETED)</option>
                <option value="CANCELLED">Скасовані (CANCELLED)</option>
              </select>
            </div>
          </div>
        </div>

        {/* Table Body */}
        {isLoadingHistory ? (
          <div className="py-20 flex flex-col items-center justify-center">
            <div className="w-8 h-8 border-3 border-indigo-600 border-t-transparent rounded-full animate-spin mb-3" />
            <p className="text-xs font-semibold text-slate-500">Завантаження журналу...</p>
          </div>
        ) : filteredSessions.length === 0 ? (
          <div className="p-12 text-center space-y-2">
            <Clock className="w-10 h-10 text-slate-300 mx-auto" />
            <h4 className="text-sm font-bold text-slate-700">Записів не знайдено</h4>
            <p className="text-xs text-slate-400 max-w-sm mx-auto">
              За вашим фільтром або в системі ще немає збережених сесій Pomodoro. Запустіть таймер, щоб з'явилися перші дані!
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-50/80 text-[11px] font-bold text-slate-500 uppercase tracking-wider border-b border-slate-100">
                  <th className="py-3 px-6">Тип</th>
                  <th className="py-3 px-6">Фокус</th>
                  <th className="py-3 px-6">Перерва</th>
                  <th className="py-3 px-6">Початок</th>
                  <th className="py-3 px-6">Кінець</th>
                  <th className="py-3 px-6">Статус</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-xs">
                {filteredSessions.map((session) => {
                  const isCompleted = session.status === 'COMPLETED';
                  const isShared = session.type === 'SHARED';

                  return (
                    <tr
                      key={session.id}
                      className="hover:bg-slate-50/60 transition-colors font-medium text-slate-700"
                    >
                      <td className="py-4 px-6">
                        <div className="flex items-center gap-2">
                          <div
                            className={`p-1.5 rounded-lg ${
                              isShared
                                ? 'bg-indigo-50 text-indigo-600'
                                : 'bg-slate-100 text-slate-600'
                            }`}
                          >
                            {isShared ? <Users className="w-3.5 h-3.5" /> : <Flame className="w-3.5 h-3.5" />}
                          </div>
                          <span className="font-semibold text-slate-900">
                            {isShared ? 'Спільна кімната' : 'Індивідуальна'}
                          </span>
                        </div>
                      </td>

                      <td className="py-4 px-6 font-semibold text-slate-900">
                        {session.durationMinutes} хв
                      </td>

                      <td className="py-4 px-6 text-slate-500">
                        {session.breakDurationMinutes} хв
                      </td>

                      <td className="py-4 px-6 text-slate-500 whitespace-nowrap">
                        {formatDate(session.startTime)}
                      </td>

                      <td className="py-4 px-6 text-slate-500 whitespace-nowrap">
                        {formatDate(session.endTime)}
                      </td>

                      <td className="py-4 px-6">
                        <span
                          className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold ${
                            isCompleted
                              ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                              : 'bg-rose-50 text-rose-700 border border-rose-200'
                          }`}
                        >
                          {isCompleted ? (
                            <>
                              <CheckCircle2 className="w-3 h-3" />
                              Завершено
                            </>
                          ) : (
                            <>
                              <XCircle className="w-3 h-3" />
                              Скасовано
                            </>
                          )}
                        </span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination Bar */}
        <div className="p-4 sm:p-6 border-t border-slate-100 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs text-slate-500">
          <div>
            Всього записів:{' '}
            <strong className="text-slate-800">{historyPage.totalElements}</strong> · Сторінка{' '}
            <strong className="text-slate-800">{historyPage.page + 1}</strong> з{' '}
            <strong className="text-slate-800">{Math.max(1, historyPage.totalPages)}</strong>
          </div>

          <div className="flex items-center gap-3 flex-wrap">
            <div className="flex items-center gap-1.5">
              <span>Показувати:</span>
              <select
                value={pageSize}
                onChange={(e) => {
                  setPageSize(Number(e.target.value));
                  setCurrentPage(0);
                }}
                className="bg-slate-50 border border-slate-200 rounded-lg px-2 py-1 text-xs font-semibold text-slate-700"
              >
                <option value={10}>10</option>
                <option value={20}>20</option>
                <option value={50}>50</option>
              </select>
            </div>

            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                disabled={currentPage <= 0 || isLoadingHistory}
                onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
              >
                <ChevronLeft className="w-4 h-4 mr-1" />
                Попередня
              </Button>

              <Button
                variant="outline"
                size="sm"
                disabled={
                  currentPage >= historyPage.totalPages - 1 ||
                  historyPage.totalPages === 0 ||
                  isLoadingHistory
                }
                onClick={() => setCurrentPage((p) => p + 1)}
              >
                Наступна
                <ChevronRight className="w-4 h-4 ml-1" />
              </Button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
