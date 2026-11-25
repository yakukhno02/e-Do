import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  ArrowRight,
  Clock,
  KeyRound,
  LogIn,
  Plus,
  RefreshCw,
  Sparkles,
  Users,
} from 'lucide-react';
import { extractErrorMessage } from '../api/client';
import { roomsApi } from '../api/rooms.api';
import { Alert } from '../components/common/Alert';
import { Badge } from '../components/common/Badge';
import { Button } from '../components/common/Button';
import { Input } from '../components/common/Input';
import { Modal } from '../components/common/Modal';
import type { RoomDetails } from '../types/api';

export const RoomsPage: React.FC = () => {
  const navigate = useNavigate();

  // Rooms list state
  const [rooms, setRooms] = useState<RoomDetails[]>([]);
  const [isLoadingRooms, setIsLoadingRooms] = useState(true);

  // Join Room state
  const [joinCode, setJoinCode] = useState('');
  const [isJoining, setIsJoining] = useState(false);
  const [joinError, setJoinError] = useState<string | null>(null);

  // Create Room Modal state
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [roomName, setRoomName] = useState('');
  const [pomodoroDuration, setPomodoroDuration] = useState(25);
  const [breakDuration, setBreakDuration] = useState(5);
  const [isCreating, setIsCreating] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);

  const loadRooms = async () => {
    try {
      setIsLoadingRooms(true);
      const list = await roomsApi.getRoomsList();
      setRooms(list);
    } catch {
      // ignore
    } finally {
      setIsLoadingRooms(false);
    }
  };

  useEffect(() => {
    loadRooms();
  }, []);

  // Handle Join (FR-24)
  const handleJoin = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    setJoinError(null);

    const cleanCode = joinCode.trim().toUpperCase();
    if (!cleanCode || cleanCode.length !== 6) {
      setJoinError('Код кімнати повинен містити рівно 6 символів');
      return;
    }

    try {
      setIsJoining(true);
      const room = await roomsApi.joinRoom({ code: cleanCode });
      navigate(`/rooms/${room.id}`);
    } catch (err: unknown) {
      setJoinError(extractErrorMessage(err, 'Не вдалося приєднатися до кімнати. Перевірте код.'));
    } finally {
      setIsJoining(false);
    }
  };

  // Handle Create (FR-22 & Fixes Issue 2!)
  const handleCreateRoom = async (e: React.FormEvent) => {
    e.preventDefault();
    setCreateError(null);

    if (!roomName.trim()) {
      setCreateError('Назва кімнати є обовʼязковою');
      return;
    }
    if (pomodoroDuration < 1 || pomodoroDuration > 120) {
      setCreateError('Тривалість Pomodoro повинна бути від 1 до 120 хвилин');
      return;
    }
    if (breakDuration < 1 || breakDuration > 60) {
      setCreateError('Тривалість перерви повинна бути від 1 до 60 хвилин');
      return;
    }

    try {
      setIsCreating(true);
      const newRoom = await roomsApi.createRoom({
        name: roomName.trim(),
        pomodoroDuration,
        breakDuration,
      });
      setIsCreateModalOpen(false);
      navigate(`/rooms/${newRoom.id}`);
    } catch (err: unknown) {
      setCreateError(extractErrorMessage(err, 'Не вдалося створити кімнату'));
    } finally {
      setIsCreating(false);
    }
  };

  const getStatusBadgeVariant = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return 'danger';
      case 'BREAK':
        return 'success';
      case 'WAITING':
        return 'info';
      default:
        return 'default';
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-8 py-4">
      {/* Page Title */}
      <div className="text-center space-y-2">
        <div className="inline-flex items-center gap-1.5 px-3.5 py-1 rounded-full bg-emerald-50 text-emerald-700 text-xs font-bold uppercase tracking-wider border border-emerald-200">
          <Sparkles className="w-3.5 h-3.5" />
          Спільні кімнати Pomodoro · Спринт 3
        </div>
        <h1 className="text-3xl font-black text-slate-900 tracking-tight">Синхронний фокус у кімнатах</h1>
        <p className="text-sm text-slate-500">
          Створюйте спільні сесії або приєднуйтеся за 6-значним кодом для спільної роботи
        </p>
      </div>

      {/* Action Cards: Join Room & Create Room */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Join Room Card (FR-24) */}
        <div className="bg-white p-6 sm:p-8 rounded-3xl border border-slate-200/90 shadow-xs flex flex-col justify-between space-y-5">
          <div className="space-y-3">
            <div className="w-12 h-12 rounded-2xl bg-indigo-50 text-indigo-600 flex items-center justify-center font-bold">
              <KeyRound className="w-6 h-6" />
            </div>
            <h3 className="text-xl font-bold text-slate-900">Приєднатися за кодом</h3>
            <p className="text-xs text-slate-500 leading-relaxed">
              Введіть 6-символьний код, отриманий від колеги, щоб синхронізувати таймер сесії.
            </p>

            {joinError && (
              <Alert type="error" message={joinError} onClose={() => setJoinError(null)} />
            )}
          </div>

          <form onSubmit={handleJoin} className="space-y-3">
            <Input
              placeholder="НАПР. A7X9K2"
              maxLength={6}
              value={joinCode}
              onChange={(e) => {
                setJoinCode(e.target.value.toUpperCase());
                if (joinError) setJoinError(null);
              }}
              className="uppercase tracking-widest text-center font-mono font-black text-lg py-3"
            />
            <Button
              type="submit"
              variant="primary"
              size="lg"
              className="w-full shadow-md shadow-indigo-100"
              isLoading={isJoining}
              disabled={!joinCode.trim()}
            >
              <LogIn className="w-4 h-4 mr-2" />
              Приєднатися до кімнати
            </Button>
          </form>
        </div>

        {/* Create Room Card (FR-22 & Fixes Issue 2!) */}
        <div className="bg-white p-6 sm:p-8 rounded-3xl border border-slate-200/90 shadow-xs flex flex-col justify-between space-y-5">
          <div className="space-y-3">
            <div className="w-12 h-12 rounded-2xl bg-emerald-50 text-emerald-600 flex items-center justify-center font-bold">
              <Plus className="w-6 h-6" />
            </div>
            <h3 className="text-xl font-bold text-slate-900">Створити нову кімнату</h3>
            <p className="text-xs text-slate-500 leading-relaxed">
              Організуйте власну кімнату, оберіть тривалість Pomodoro і запросіть команду за посиланням або кодом.
            </p>
          </div>

          <div className="space-y-3 pt-4">
            <Button
              type="button"
              variant="secondary"
              size="lg"
              onClick={() => {
                setCreateError(null);
                setRoomName('');
                setPomodoroDuration(25);
                setBreakDuration(5);
                setIsCreateModalOpen(true);
              }}
              className="w-full shadow-md"
            >
              <Plus className="w-4 h-4 mr-2" />
              Налаштувати та створити кімнату
            </Button>
          </div>
        </div>
      </div>

      {/* Active / Accessible Rooms List */}
      <div className="space-y-4 pt-4">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-black text-slate-800 flex items-center gap-2">
            <Users className="w-5 h-5 text-indigo-600" />
            Доступні кімнати ({rooms.length})
          </h2>
          <Button variant="ghost" size="sm" onClick={loadRooms} isLoading={isLoadingRooms}>
            <RefreshCw className="w-4 h-4 mr-1.5" />
            Оновити
          </Button>
        </div>

        {rooms.length === 0 ? (
          <div className="bg-white rounded-3xl border border-dashed border-slate-200 p-8 text-center text-xs text-slate-500">
            Наразі немає створених кімнат. Створіть першу кімнату за кнопкою вище!
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {rooms.map((room) => (
              <div
                key={room.id}
                onClick={() => navigate(`/rooms/${room.id}`)}
                className="bg-white rounded-2xl border border-slate-200/80 p-5 shadow-xs hover:shadow-md hover:border-indigo-200 transition-all cursor-pointer group flex flex-col justify-between space-y-4"
              >
                <div className="space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="font-mono text-xs font-black tracking-widest text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded-md">
                      #{room.code}
                    </span>
                    <Badge variant={getStatusBadgeVariant(room.status)}>
                      {room.status}
                    </Badge>
                  </div>
                  <h4 className="font-bold text-slate-900 group-hover:text-indigo-600 transition-colors">
                    {room.name}
                  </h4>
                </div>

                <div className="flex items-center justify-between text-xs text-slate-500 pt-3 border-t border-slate-100">
                  <span className="flex items-center gap-1.5">
                    <Clock className="w-3.5 h-3.5 text-slate-400" />
                    {room.pomodoroDuration} хв / {room.breakDuration} хв
                  </span>
                  <span className="flex items-center gap-1.5 font-semibold text-slate-700">
                    <Users className="w-3.5 h-3.5 text-indigo-500" />
                    {room.participants?.length || 1} учасників
                  </span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Create Room Modal */}
      <Modal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        title="Створення спільної кімнати"
      >
        <form onSubmit={handleCreateRoom} className="space-y-4">
          {createError && <Alert type="error" message={createError} />}

          <Input
            label="Назва кімнати"
            placeholder="Напр. Спринт рев'ю або Командний фокус"
            value={roomName}
            onChange={(e) => {
              setRoomName(e.target.value);
              if (createError) setCreateError(null);
            }}
            required
            autoFocus
          />

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1.5 uppercase tracking-wider">
                Фокус (хв)
              </label>
              <input
                type="number"
                min={1}
                max={120}
                value={pomodoroDuration}
                onChange={(e) => setPomodoroDuration(Number(e.target.value))}
                className="w-full px-3.5 py-2.5 bg-white border border-slate-300 rounded-xl text-sm font-bold text-slate-800 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                required
              />
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1.5 uppercase tracking-wider">
                Перерва (хв)
              </label>
              <input
                type="number"
                min={1}
                max={60}
                value={breakDuration}
                onChange={(e) => setBreakDuration(Number(e.target.value))}
                className="w-full px-3.5 py-2.5 bg-white border border-slate-300 rounded-xl text-sm font-bold text-slate-800 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                required
              />
            </div>
          </div>

          <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-100">
            <Button
              type="button"
              variant="outline"
              onClick={() => setIsCreateModalOpen(false)}
            >
              Скасувати
            </Button>
            <Button
              type="submit"
              variant="primary"
              isLoading={isCreating}
            >
              Створити та увійти
              <ArrowRight className="w-4 h-4 ml-1.5" />
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
