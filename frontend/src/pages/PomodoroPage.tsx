import React, { useEffect, useRef, useState } from 'react';
import {
  BellOff,
  Clock,
  Coffee,
  Flame,
  ListTodo,
  Pause,
  Play,
  RotateCcw,
  Settings,
  Sparkles,
  StopCircle,
  Volume2,
} from 'lucide-react';
import { pomodoroApi } from '../api/pomodoro.api';
import { tasksApi } from '../api/tasks.api';
import { Alert } from '../components/common/Alert';
import { Button } from '../components/common/Button';
import { Modal } from '../components/common/Modal';
import type { TaskItem } from '../types/api';
import { sound } from '../utils/sound';

export const PomodoroPage: React.FC = () => {
  // Settings
  const [pomodoroMinutes, setPomodoroMinutes] = useState(25);
  const [breakMinutes, setBreakMinutes] = useState(5);
  const [soundEnabled, setSoundEnabled] = useState(true);
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);

  // Timer State
  const [mode, setMode] = useState<'FOCUS' | 'BREAK'>('FOCUS');
  const [isRunning, setIsRunning] = useState(false);
  const [secondsLeft, setSecondsLeft] = useState(25 * 60);
  const [activeSessionId, setActiveSessionId] = useState<string | null>(null);

  // Feedback & Tasks
  const [completionMessage, setCompletionMessage] = useState<string | null>(null);
  const [tasks, setTasks] = useState<TaskItem[]>([]);
  const [selectedTaskId, setSelectedTaskId] = useState<string>('');

  const targetEndTimeRef = useRef<number | null>(null);

  // Load tasks for linking focus session
  useEffect(() => {
    tasksApi.getTasks().then((list) => {
      setTasks(list.filter((t) => t.status !== 'DONE'));
    }).catch(() => {});
  }, []);

  // Sync initial secondsLeft with pomodoroMinutes
  useEffect(() => {
    if (!isRunning) {
      setSecondsLeft(mode === 'FOCUS' ? pomodoroMinutes * 60 : breakMinutes * 60);
    }
  }, [pomodoroMinutes, breakMinutes, mode, isRunning]);

  // Main Timer Countdown Engine (Fixes Issue 1!)
  useEffect(() => {
    let intervalId: number | undefined;

    if (isRunning) {
      if (!targetEndTimeRef.current) {
        targetEndTimeRef.current = Date.now() + secondsLeft * 1000;
      }

      intervalId = window.setInterval(() => {
        const remaining = Math.max(0, Math.ceil((targetEndTimeRef.current! - Date.now()) / 1000));
        setSecondsLeft(remaining);

        if (remaining <= 0) {
          window.clearInterval(intervalId);
          handleSessionComplete();
        }
      }, 250);
    } else {
      targetEndTimeRef.current = null;
    }

    return () => {
      if (intervalId) window.clearInterval(intervalId);
    };
  }, [isRunning]);

  // Handle session auto-completion (FR-19 & FR-21)
  const handleSessionComplete = async () => {
    setIsRunning(false);
    targetEndTimeRef.current = null;

    if (soundEnabled) {
      sound.playCompleteChime();
    }

    if (mode === 'FOCUS') {
      const minutesEarned = pomodoroMinutes;
      if (activeSessionId) {
        try {
          await pomodoroApi.complete(activeSessionId);
        } catch (e) {
          console.warn('Could not report complete to backend:', e);
        }
      }
      setActiveSessionId(null);
      setCompletionMessage(`Чудова робота! Ви завершили фокус-сесію тривалістю ${minutesEarned} хв. Час відпочити!`);

      // Automatically switch to break mode (FR-21)
      setMode('BREAK');
      setSecondsLeft(breakMinutes * 60);
    } else {
      setCompletionMessage('Перерва закінчилася! Готові до нового продуктивного фокусу?');
      setMode('FOCUS');
      setSecondsLeft(pomodoroMinutes * 60);
    }
  };

  // Start or Resume Timer (FR-17)
  const handleStart = async () => {
    setCompletionMessage(null);
    if (!isRunning && mode === 'FOCUS' && !activeSessionId) {
      try {
        const session = await pomodoroApi.start({
          durationMinutes: pomodoroMinutes,
          breakDurationMinutes: breakMinutes,
        });
        setActiveSessionId(session.sessionId);
      } catch (e) {
        console.warn('Could not start session on backend:', e);
      }
    }
    targetEndTimeRef.current = Date.now() + secondsLeft * 1000;
    setIsRunning(true);
  };

  // Pause Timer
  const handlePause = () => {
    setIsRunning(false);
    targetEndTimeRef.current = null;
  };

  // Early cancellation / Stop (FR-20)
  const handleCancel = async () => {
    if (!window.confirm('Ви дійсно бажаєте достроково завершити сесію? Зароблений час не буде зараховано.')) {
      return;
    }
    setIsRunning(false);
    targetEndTimeRef.current = null;

    if (activeSessionId) {
      try {
        await pomodoroApi.cancel(activeSessionId);
      } catch (e) {
        console.warn('Could not cancel on backend:', e);
      }
      setActiveSessionId(null);
    }

    setSecondsLeft(mode === 'FOCUS' ? pomodoroMinutes * 60 : breakMinutes * 60);
    setCompletionMessage('Сесію скасовано.');
  };

  // Reset Timer
  const handleReset = () => {
    setIsRunning(false);
    targetEndTimeRef.current = null;
    setActiveSessionId(null);
    setSecondsLeft(mode === 'FOCUS' ? pomodoroMinutes * 60 : breakMinutes * 60);
  };

  // Switch between Focus and Break modes
  const switchMode = (newMode: 'FOCUS' | 'BREAK') => {
    if (isRunning) {
      if (!window.confirm('Таймер запущено. Змінити режим і скинути прогрес?')) return;
    }
    setIsRunning(false);
    targetEndTimeRef.current = null;
    setMode(newMode);
    setSecondsLeft(newMode === 'FOCUS' ? pomodoroMinutes * 60 : breakMinutes * 60);
    setCompletionMessage(null);
  };

  // Visual calculation
  const totalSeconds = (mode === 'FOCUS' ? pomodoroMinutes : breakMinutes) * 60;
  const progressPercent = Math.min(100, Math.max(0, ((totalSeconds - secondsLeft) / totalSeconds) * 100));

  const minutesDisplay = Math.floor(secondsLeft / 60);
  const secondsDisplay = secondsLeft % 60;
  const formattedTime = `${String(minutesDisplay).padStart(2, '0')}:${String(secondsDisplay).padStart(2, '0')}`;

  return (
    <div className="max-w-3xl mx-auto space-y-8 py-4">
      {/* Page Header */}
      <div className="text-center space-y-2">
        <div className="inline-flex items-center gap-1.5 px-3.5 py-1 rounded-full bg-indigo-50 text-indigo-700 text-xs font-bold uppercase tracking-wider border border-indigo-200">
          <Sparkles className="w-3.5 h-3.5" />
          Індивідуальний Pomodoro · Спринт 2
        </div>
        <h1 className="text-3xl font-black text-slate-900 tracking-tight">Таймер фокусування</h1>
        <p className="text-sm text-slate-500">
          Техніка чергування глибокої концентрації та відновлювальних перерв
        </p>
      </div>

      {completionMessage && (
        <Alert
          type="success"
          message={completionMessage}
          onClose={() => setCompletionMessage(null)}
        />
      )}

      {/* Main Timer Container */}
      <div className="bg-white rounded-3xl border border-slate-200/90 shadow-xl p-8 sm:p-12 text-center space-y-8 relative overflow-hidden">
        {/* Top Controls: Mode Switcher & Sound / Settings */}
        <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="inline-flex p-1 bg-slate-100 rounded-2xl gap-1 shadow-2xs">
            <button
              onClick={() => switchMode('FOCUS')}
              className={`flex items-center gap-2 px-5 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                mode === 'FOCUS'
                  ? 'bg-indigo-600 text-white shadow-sm'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <Flame className="w-4 h-4" />
              Фокус ({pomodoroMinutes} хв)
            </button>
            <button
              onClick={() => switchMode('BREAK')}
              className={`flex items-center gap-2 px-5 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                mode === 'BREAK'
                  ? 'bg-emerald-600 text-white shadow-sm'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <Coffee className="w-4 h-4" />
              Перерва ({breakMinutes} хв)
            </button>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={() => setSoundEnabled(!soundEnabled)}
              className={`p-2 rounded-xl border transition-colors cursor-pointer ${
                soundEnabled
                  ? 'border-indigo-200 text-indigo-600 bg-indigo-50/50'
                  : 'border-slate-200 text-slate-400 hover:text-slate-600'
              }`}
              title={soundEnabled ? 'Звук увімкнено' : 'Звук вимкнено'}
            >
              {soundEnabled ? <Volume2 className="w-4 h-4" /> : <BellOff className="w-4 h-4" />}
            </button>

            <button
              onClick={() => setIsSettingsOpen(true)}
              className="p-2 rounded-xl border border-slate-200 text-slate-600 hover:text-slate-900 hover:bg-slate-50 transition-colors cursor-pointer"
              title="Налаштування тривалості"
            >
              <Settings className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Selected Task Focus Banner */}
        <div className="max-w-md mx-auto">
          {tasks.length > 0 && (
            <div className="flex items-center gap-2 text-xs text-slate-600 bg-slate-50 border border-slate-200 rounded-xl px-3 py-1.5">
              <ListTodo className="w-4 h-4 text-indigo-600 shrink-0" />
              <span className="font-semibold text-slate-500">Завдання:</span>
              <select
                value={selectedTaskId}
                onChange={(e) => setSelectedTaskId(e.target.value)}
                className="bg-transparent text-xs font-bold text-slate-800 focus:outline-none flex-1 truncate cursor-pointer"
              >
                <option value="">(Оберіть завдання для фокусу)</option>
                {tasks.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.title}
                  </option>
                ))}
              </select>
            </div>
          )}
        </div>

        {/* Circular SVG Timer with Live Numbers */}
        <div className="relative w-64 h-64 sm:w-72 sm:h-72 mx-auto flex items-center justify-center">
          <svg className="w-full h-full transform -rotate-90" viewBox="0 0 100 100">
            {/* Background circle */}
            <circle
              cx="50"
              cy="50"
              r="44"
              stroke="currentColor"
              strokeWidth="6"
              className={mode === 'FOCUS' ? 'text-indigo-100' : 'text-emerald-100'}
              fill="transparent"
            />
            {/* Animated progress circle */}
            <circle
              cx="50"
              cy="50"
              r="44"
              stroke="currentColor"
              strokeWidth="6"
              strokeDasharray={276.46}
              strokeDashoffset={276.46 - (276.46 * progressPercent) / 100}
              strokeLinecap="round"
              className={`transition-all duration-300 ${
                mode === 'FOCUS' ? 'text-indigo-600' : 'text-emerald-500'
              }`}
              fill="transparent"
            />
          </svg>

          {/* Time digits in center */}
          <div className="absolute inset-0 flex flex-col items-center justify-center">
            <span className="text-5xl sm:text-6xl font-black tracking-tight text-slate-900 font-mono select-none">
              {formattedTime}
            </span>
            <div className="flex items-center gap-1.5 mt-2">
              <span
                className={`w-2 h-2 rounded-full ${
                  isRunning
                    ? 'bg-emerald-500 animate-pulse'
                    : 'bg-slate-400'
                }`}
              />
              <span className="text-xs font-bold text-slate-400 uppercase tracking-widest">
                {isRunning
                  ? mode === 'FOCUS'
                    ? 'Фокусування...'
                    : 'Перерва...'
                  : 'На паузі'}
              </span>
            </div>
          </div>
        </div>

        {/* Timer Control Buttons */}
        <div className="flex items-center justify-center gap-4 pt-2">
          {!isRunning ? (
            <Button
              size="lg"
              variant="primary"
              onClick={handleStart}
              className="w-44 shadow-lg shadow-indigo-200"
            >
              <Play className="w-5 h-5 mr-2" />
              {secondsLeft < totalSeconds ? 'Продовжити' : 'Старт'}
            </Button>
          ) : (
            <Button
              size="lg"
              variant="secondary"
              onClick={handlePause}
              className="w-44 shadow-md"
            >
              <Pause className="w-5 h-5 mr-2" />
              Пауза
            </Button>
          )}

          {isRunning && mode === 'FOCUS' && (
            <Button
              size="lg"
              variant="danger"
              onClick={handleCancel}
              title="Достроково завершити сесію (FR-20)"
            >
              <StopCircle className="w-5 h-5 mr-1" />
              Скасувати
            </Button>
          )}

          <Button
            size="lg"
            variant="outline"
            onClick={handleReset}
            title="Скинути таймер"
          >
            <RotateCcw className="w-5 h-5 text-slate-600" />
          </Button>
        </div>
      </div>

      {/* Preset Duration Buttons */}
      <div className="bg-white rounded-2xl border border-slate-200/80 p-5 shadow-xs flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-2 text-xs font-bold text-slate-600">
          <Clock className="w-4 h-4 text-indigo-600" />
          Швидкі пресети тривалості:
        </div>

        <div className="flex items-center gap-1.5 flex-wrap">
          {[15, 25, 45, 60].map((mins) => (
            <button
              key={mins}
              onClick={() => {
                if (isRunning) return;
                setPomodoroMinutes(mins);
                if (mode === 'FOCUS') setSecondsLeft(mins * 60);
              }}
              disabled={isRunning}
              className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                pomodoroMinutes === mins
                  ? 'bg-indigo-50 border border-indigo-300 text-indigo-700'
                  : 'bg-slate-50 hover:bg-slate-100 text-slate-600 border border-slate-200'
              } disabled:opacity-50`}
            >
              {mins} хв
            </button>
          ))}
        </div>
      </div>

      {/* Settings Modal (FR-16) */}
      <Modal
        isOpen={isSettingsOpen}
        onClose={() => setIsSettingsOpen(false)}
        title="Налаштування Pomodoro (FR-16)"
      >
        <div className="space-y-5">
          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1.5 uppercase tracking-wider">
              Тривалість фокусу (від 1 до 120 хвилин)
            </label>
            <input
              type="number"
              min={1}
              max={120}
              value={pomodoroMinutes}
              onChange={(e) => {
                const val = Math.max(1, Math.min(120, Number(e.target.value) || 1));
                setPomodoroMinutes(val);
                if (!isRunning && mode === 'FOCUS') setSecondsLeft(val * 60);
              }}
              className="w-full px-3.5 py-2.5 bg-white border border-slate-300 rounded-xl text-sm font-bold text-slate-800 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1.5 uppercase tracking-wider">
              Тривалість перерви (від 1 до 60 хвилин)
            </label>
            <input
              type="number"
              min={1}
              max={60}
              value={breakMinutes}
              onChange={(e) => {
                const val = Math.max(1, Math.min(60, Number(e.target.value) || 1));
                setBreakMinutes(val);
                if (!isRunning && mode === 'BREAK') setSecondsLeft(val * 60);
              }}
              className="w-full px-3.5 py-2.5 bg-white border border-slate-300 rounded-xl text-sm font-bold text-slate-800 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>

          <div className="flex items-center justify-between pt-2">
            <span className="text-sm font-semibold text-slate-700">Звуковий сигнал після завершення:</span>
            <input
              type="checkbox"
              checked={soundEnabled}
              onChange={(e) => setSoundEnabled(e.target.checked)}
              className="w-5 h-5 text-indigo-600 rounded-md focus:ring-indigo-500 cursor-pointer"
            />
          </div>

          <div className="flex justify-end pt-4 border-t border-slate-100">
            <Button variant="primary" onClick={() => setIsSettingsOpen(false)}>
              Зберегти
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
};
