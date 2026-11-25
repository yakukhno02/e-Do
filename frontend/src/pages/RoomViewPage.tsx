import React, { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import {
  ArrowLeft,
  Check,
  Clock,
  Coffee,
  Copy,
  Crown,
  Flame,
  LogOut,
  Play,
  Square,
  Users,
} from 'lucide-react';
import { extractErrorMessage } from '../api/client';
import { roomsApi } from '../api/rooms.api';
import { Alert } from '../components/common/Alert';
import { RoomStatusBadge } from '../components/common/Badge';
import { Button } from '../components/common/Button';
import { useAuth } from '../hooks/useAuth';
import { usePomodoroTimer } from '../hooks/usePomodoroTimer';
import type { RoomDetails } from '../types/api';
import { playChime } from '../utils/sound';

export const RoomViewPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const navigate = useNavigate();

  const [room, setRoom] = useState<RoomDetails | null>(null);
  const [clockSkew, setClockSkew] = useState<number>(0);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [copied, setCopied] = useState<boolean>(false);
  const [isActionLoading, setIsActionLoading] = useState<boolean>(false);
  const [showLeaveConfirm, setShowLeaveConfirm] = useState<boolean>(false);

  const prevStatusRef = useRef<string | null>(null);
  const isPollingRef = useRef<boolean>(false);

  // Synchronize room state and compute midpoint clock skew
  const fetchRoomState = useCallback(async (isInitial = false) => {
    if (!id || isPollingRef.current) return;
    try {
      isPollingRef.current = true;
      if (isInitial) setIsLoading(true);

      const t0 = Date.now();
      const data = await roomsApi.getRoom(id);
      const t1 = Date.now();

      // Architecture Section 6.1:
      // clientMidpoint = (t0 + t1) / 2
      // clockSkew = serverTime_response - clientMidpoint
      const clientMidpoint = (t0 + t1) / 2;
      const serverTimeParsed = Date.parse(data.serverTime);
      if (Number.isFinite(serverTimeParsed)) {
        const calculatedSkew = serverTimeParsed - clientMidpoint;
        setClockSkew(calculatedSkew);
      }

      // Check if status changed to trigger audio notification
      if (prevStatusRef.current && prevStatusRef.current !== data.status) {
        if (data.status === 'BREAK' || data.status === 'FINISHED') {
          playChime();
        }
      }
      prevStatusRef.current = data.status;

      setRoom(data);
      setError(null);
    } catch (err: any) {
      if (isInitial) {
        setError(extractErrorMessage(err, 'Не вдалося завантажити кімнату'));
      }
    } finally {
      if (isInitial) setIsLoading(false);
      isPollingRef.current = false;
    }
  }, [id]);

  // Initial load and polling every 2500ms
  useEffect(() => {
    fetchRoomState(true);

    const interval = setInterval(() => {
      fetchRoomState(false);
    }, 2500);

    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible') {
        fetchRoomState(false);
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);

    return () => {
      clearInterval(interval);
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, [fetchRoomState]);

  // Hook for real-time timer countdown
  const { secondsLeft, formattedTime } = usePomodoroTimer({
    status: room?.status || 'WAITING',
    pomodoroDuration: room?.pomodoroDuration || 25,
    breakDuration: room?.breakDuration || 5,
    currentSessionStart: room?.currentSessionStart || null,
    clockSkew,
  });

  // Calculate circular progress percentage
  const totalDurationSeconds =
    room?.status === 'BREAK'
      ? (room.breakDuration || 5) * 60
      : (room?.pomodoroDuration || 25) * 60;
  const progressPercent =
    totalDurationSeconds > 0
      ? Math.max(0, Math.min(100, ((totalDurationSeconds - secondsLeft) / totalDurationSeconds) * 100))
      : 0;

  const handleCopyCode = () => {
    if (!room?.code) return;
    navigator.clipboard.writeText(room.code);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleStartSession = async () => {
    if (!id || isActionLoading) return;
    try {
      setIsActionLoading(true);
      setError(null);
      const updated = await roomsApi.startRoomSession(id);
      setRoom(updated);
      prevStatusRef.current = updated.status;
      playChime();
    } catch (err: any) {
      setError(extractErrorMessage(err, 'Не вдалося розпочати спільну сесію'));
    } finally {
      setIsActionLoading(false);
    }
  };

  const handleStopSession = async () => {
    if (!id || isActionLoading) return;
    try {
      setIsActionLoading(true);
      setError(null);
      const updated = await roomsApi.stopRoomSession(id);
      setRoom(updated);
      prevStatusRef.current = updated.status;
    } catch (err: any) {
      setError(extractErrorMessage(err, 'Не вдалося зупинити сесію'));
    } finally {
      setIsActionLoading(false);
    }
  };

  const handleLeaveRoom = async () => {
    if (!id || isActionLoading) return;
    try {
      setIsActionLoading(true);
      await roomsApi.leaveRoom(id);
      navigate('/rooms');
    } catch (err: any) {
      setError(extractErrorMessage(err, 'Не вдалося покинути кімнату'));
      setIsActionLoading(false);
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-[50vh] flex flex-col items-center justify-center">
        <div className="w-12 h-12 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin mb-4" />
        <p className="text-sm font-semibold text-slate-600">Завантаження спільної кімнати...</p>
      </div>
    );
  }

  if (!room) {
    return (
      <div className="max-w-md mx-auto my-12 bg-white p-8 rounded-3xl border border-slate-200 text-center space-y-4">
        <h3 className="text-xl font-bold text-slate-800">Кімнату не знайдено</h3>
        <p className="text-sm text-slate-500">
          Кімната з таким ідентифікатором не існує або була видалена.
        </p>
        <Link to="/rooms">
          <Button variant="primary">Повернутися до кімнат</Button>
        </Link>
      </div>
    );
  }

  const isCreator = room.creatorId === user?.id;
  const isSessionRunning = room.status === 'ACTIVE' || room.status === 'BREAK';

  return (
    <div className="max-w-5xl mx-auto space-y-8 py-2">
      {/* Top Header and Back Navigation */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <Link
            to="/rooms"
            className="w-10 h-10 rounded-2xl bg-white border border-slate-200 flex items-center justify-center text-slate-500 hover:text-slate-800 hover:border-slate-300 transition-colors shadow-2xs"
            title="Назад до списку кімнат"
          >
            <ArrowLeft className="w-5 h-5" />
          </Link>
          <div>
            <div className="flex items-center gap-2.5 flex-wrap">
              <h1 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">
                {room.name}
              </h1>
              <RoomStatusBadge status={room.status} />
              {isCreator && (
                <span className="inline-flex items-center gap-1 text-[11px] font-bold text-amber-700 bg-amber-50 px-2 py-0.5 rounded-full border border-amber-200">
                  <Crown className="w-3 h-3 text-amber-500" />
                  Ви організатор
                </span>
              )}
            </div>
            <p className="text-xs sm:text-sm text-slate-500 mt-0.5">
              Синхронізована Pomodoro-кімната · Фідбек кожні 2.5 сек
            </p>
          </div>
        </div>

        <div className="flex items-center gap-3">
          {/* Room Code Badge with 1-click copy */}
          <div className="flex items-center bg-white border border-indigo-200 rounded-2xl p-1.5 pl-3.5 shadow-2xs">
            <span className="text-xs font-semibold text-slate-500 mr-2">Код:</span>
            <span className="text-base font-black tracking-widest text-indigo-700 font-mono mr-2">
              {room.code}
            </span>
            <button
              onClick={handleCopyCode}
              className="p-1.5 rounded-xl bg-indigo-50 hover:bg-indigo-100 text-indigo-600 transition-colors"
              title="Копіювати код кімнати"
            >
              {copied ? <Check className="w-4 h-4 text-emerald-600" /> : <Copy className="w-4 h-4" />}
            </button>
          </div>

          <Button
            variant="ghost"
            size="md"
            onClick={() => setShowLeaveConfirm(true)}
            className="text-slate-500 hover:text-rose-600 hover:bg-rose-50"
            title="Покинути кімнату"
          >
            <LogOut className="w-4 h-4 mr-1.5" />
            Вийти
          </Button>
        </div>
      </div>

      {error && (
        <Alert type="error" message={error} onClose={() => setError(null)} />
      )}

      {/* Main Grid: Left Timer, Right Participants */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        {/* Timer Panel */}
        <div className="lg:col-span-7 bg-white rounded-3xl border border-slate-200 p-8 shadow-xs flex flex-col items-center text-center relative overflow-hidden">
          {/* Subtle Ambient Background Ring */}
          <div className="absolute -top-24 -left-24 w-72 h-72 rounded-full bg-indigo-50/60 blur-3xl pointer-events-none" />
          <div className="absolute -bottom-24 -right-24 w-72 h-72 rounded-full bg-purple-50/60 blur-3xl pointer-events-none" />

          {/* Mode Pill */}
          <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full text-xs font-bold tracking-wide mb-6 bg-slate-100 text-slate-700">
            {room.status === 'BREAK' ? (
              <>
                <Coffee className="w-4 h-4 text-emerald-600" />
                Спільна перерва
              </>
            ) : room.status === 'ACTIVE' ? (
              <>
                <Flame className="w-4 h-4 text-indigo-600" />
                Спільний фокус
              </>
            ) : room.status === 'FINISHED' ? (
              <>
                <Check className="w-4 h-4 text-slate-500" />
                Сесію завершено
              </>
            ) : (
              <>
                <Clock className="w-4 h-4 text-indigo-600" />
                Очікування запуску
              </>
            )}
          </div>

          {/* SVG Circular Progress Ring */}
          <div className="relative w-64 h-64 sm:w-72 sm:h-72 flex items-center justify-center my-2">
            <svg className="w-full h-full -rotate-90 transform" viewBox="0 0 256 256">
              {/* Background circle */}
              <circle
                cx="128"
                cy="128"
                r="110"
                stroke="currentColor"
                strokeWidth="12"
                className="text-slate-100 fill-transparent"
              />
              {/* Foreground progress circle */}
              <circle
                cx="128"
                cy="128"
                r="110"
                stroke="currentColor"
                strokeWidth="12"
                strokeLinecap="round"
                className={`fill-transparent transition-all duration-300 ${
                  room.status === 'BREAK'
                    ? 'text-emerald-500'
                    : room.status === 'ACTIVE'
                    ? 'text-indigo-600'
                    : 'text-slate-300'
                }`}
                strokeDasharray={2 * Math.PI * 110}
                strokeDashoffset={
                  2 * Math.PI * 110 * (1 - (isSessionRunning ? progressPercent / 100 : 0))
                }
              />
            </svg>

            {/* Center Content */}
            <div className="absolute flex flex-col items-center">
              <span className="text-5xl sm:text-6xl font-black tracking-tight text-slate-900 font-mono">
                {room.status === 'WAITING'
                  ? `${String(room.pomodoroDuration).padStart(2, '0')}:00`
                  : formattedTime}
              </span>
              <span className="text-xs font-semibold uppercase tracking-wider text-slate-400 mt-2">
                {room.status === 'BREAK'
                  ? 'Час відпочинку'
                  : room.status === 'ACTIVE'
                  ? 'Залишилося часу'
                  : 'Готові до старту'}
              </span>
            </div>
          </div>

          {/* Duration Settings Hint */}
          <p className="text-xs text-slate-500 mt-4 mb-6">
            Тривалість сесії: <strong>{room.pomodoroDuration} хв</strong> · Перерва:{' '}
            <strong>{room.breakDuration} хв</strong>
          </p>

          {/* Controls Bar */}
          <div className="flex flex-wrap items-center justify-center gap-3 w-full max-w-sm">
            {room.status === 'WAITING' || room.status === 'FINISHED' ? (
              <Button
                variant="primary"
                size="lg"
                onClick={handleStartSession}
                isLoading={isActionLoading}
                className="w-full shadow-lg shadow-indigo-200"
              >
                <Play className="w-5 h-5 mr-2" />
                {room.status === 'FINISHED' ? 'Почати нову сесію' : 'Запустити для всіх'}
              </Button>
            ) : room.status === 'ACTIVE' ? (
              <Button
                variant="outline"
                size="lg"
                onClick={handleStopSession}
                isLoading={isActionLoading}
                className="w-full border-rose-200 text-rose-600 hover:bg-rose-50 hover:border-rose-300"
              >
                <Square className="w-5 h-5 mr-2" />
                Зупинити достроково
              </Button>
            ) : (
              <div className="text-center py-2">
                <p className="text-sm font-semibold text-emerald-700">
                  Триває спільна перерва. Новий фокус почнеться після завершення відпочинку!
                </p>
              </div>
            )}
          </div>
        </div>

        {/* Participants Panel */}
        <div className="lg:col-span-5 bg-white rounded-3xl border border-slate-200 p-6 shadow-xs space-y-6">
          <div className="flex items-center justify-between border-b border-slate-100 pb-4">
            <div className="flex items-center gap-2">
              <Users className="w-5 h-5 text-indigo-600" />
              <h3 className="text-lg font-bold text-slate-800">Учасники кімнати</h3>
            </div>
            <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-indigo-50 text-indigo-700">
              {room.participants.length} онлайн
            </span>
          </div>

          <div className="space-y-3 max-h-[380px] overflow-y-auto pr-1">
            {room.participants.map((p) => {
              const isCurrentUser = p.userId === user?.id;
              return (
                <div
                  key={p.userId}
                  className={`flex items-center justify-between p-3.5 rounded-2xl border transition-colors ${
                    isCurrentUser
                      ? 'bg-indigo-50/50 border-indigo-200'
                      : 'bg-slate-50/60 border-slate-100'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-xl bg-indigo-600 text-white flex items-center justify-center font-bold text-sm shadow-xs">
                      {p.name.charAt(0)}
                    </div>
                    <div>
                      <div className="flex items-center gap-1.5">
                        <span className="text-sm font-bold text-slate-900 leading-tight">
                          {p.name}
                        </span>
                        {isCurrentUser && (
                          <span className="text-[10px] font-semibold text-indigo-600 bg-indigo-100 px-1.5 py-0.2 rounded-md">
                            Ви
                          </span>
                        )}
                      </div>
                      <span className="text-xs text-slate-500 font-medium">@{p.username}</span>
                    </div>
                  </div>

                  {p.isCreator && (
                    <div className="flex items-center gap-1 text-amber-600 bg-amber-50 px-2 py-0.5 rounded-lg text-xs font-bold border border-amber-200">
                      <Crown className="w-3.5 h-3.5" />
                      Організатор
                    </div>
                  )}
                </div>
              );
            })}
          </div>

          <div className="bg-slate-50 p-4 rounded-2xl border border-slate-200/70 text-xs text-slate-600 space-y-1.5">
            <p className="font-semibold text-slate-800">Правила спільної синхронізації:</p>
            <ul className="list-disc pl-4 space-y-1 text-slate-500">
              <li>Будь-який учасник може запустити таймер з очікування.</li>
              <li>Час таймера звіряється із сервером кожні 2500 мс.</li>
              <li>Перерва вмикається автоматично для всіх учасників одночасно.</li>
            </ul>
          </div>
        </div>
      </div>

      {/* Confirmation Modal to Leave Room */}
      {showLeaveConfirm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-xs">
          <div className="bg-white rounded-3xl max-w-sm w-full p-6 shadow-2xl border border-slate-100 space-y-4">
            <h4 className="text-lg font-bold text-slate-900">Покинути кімнату?</h4>
            <p className="text-xs text-slate-500">
              Ви впевнені, що хочете вийти з цієї кімнати? Якщо ви організатор, права буде передано наступному учаснику.
            </p>
            <div className="flex items-center justify-end gap-3 pt-2">
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setShowLeaveConfirm(false)}
                disabled={isActionLoading}
              >
                Скасувати
              </Button>
              <Button
                variant="primary"
                size="sm"
                onClick={handleLeaveRoom}
                isLoading={isActionLoading}
                className="bg-rose-600 hover:bg-rose-700 text-white"
              >
                Так, покинути
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
