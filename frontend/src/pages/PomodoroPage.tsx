import React, { useState } from 'react';
import { Play, RotateCcw, Sparkles } from 'lucide-react';
import { Button } from '../components/common/Button';

export const PomodoroPage: React.FC = () => {
  const [pomodoroMinutes] = useState(25);
  const [breakMinutes] = useState(5);
  const [mode, setMode] = useState<'FOCUS' | 'BREAK'>('FOCUS');
  const [isRunning, setIsRunning] = useState(false);
  const [timeLeft, setTimeLeft] = useState(25 * 60);

  const minutes = Math.floor(timeLeft / 60);
  const seconds = timeLeft % 60;
  const formatted = `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;

  const toggleTimer = () => {
    setIsRunning(!isRunning);
  };

  const resetTimer = () => {
    setIsRunning(false);
    setTimeLeft(mode === 'FOCUS' ? pomodoroMinutes * 60 : breakMinutes * 60);
  };

  return (
    <div className="max-w-2xl mx-auto space-y-8 py-6 text-center">
      <div className="space-y-2">
        <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-violet-50 text-violet-700 text-xs font-bold uppercase tracking-wider border border-violet-200">
          <Sparkles className="w-3.5 h-3.5" />
          Індивідуальний Pomodoro · Спринт 2 Scaffold
        </div>
        <h1 className="text-3xl font-black text-slate-900 tracking-tight">Таймер продуктивності</h1>
        <p className="text-sm text-slate-500">25 хвилин фокусу, 5 хвилин заслуженого відпочинку</p>
      </div>

      {/* Mode Switcher */}
      <div className="inline-flex p-1.5 bg-slate-100 rounded-2xl gap-1">
        <button
          onClick={() => {
            setMode('FOCUS');
            setIsRunning(false);
            setTimeLeft(pomodoroMinutes * 60);
          }}
          className={`px-6 py-2 rounded-xl text-sm font-bold transition-all cursor-pointer ${
            mode === 'FOCUS'
              ? 'bg-white text-indigo-700 shadow-sm'
              : 'text-slate-600 hover:text-slate-900'
          }`}
        >
          Фокус ({pomodoroMinutes} хв)
        </button>
        <button
          onClick={() => {
            setMode('BREAK');
            setIsRunning(false);
            setTimeLeft(breakMinutes * 60);
          }}
          className={`px-6 py-2 rounded-xl text-sm font-bold transition-all cursor-pointer ${
            mode === 'BREAK'
              ? 'bg-white text-emerald-700 shadow-sm'
              : 'text-slate-600 hover:text-slate-900'
          }`}
        >
          Перерва ({breakMinutes} хв)
        </button>
      </div>

      {/* Circular Timer Display */}
      <div className="bg-white p-12 rounded-3xl border border-slate-200 shadow-xl max-w-md mx-auto space-y-6">
        <div className="w-64 h-64 mx-auto rounded-full border-8 border-indigo-100 flex flex-col items-center justify-center bg-gradient-to-b from-indigo-50/50 to-white relative">
          <span className="text-6xl font-black tracking-tight text-slate-900 font-mono">
            {formatted}
          </span>
          <span className="text-xs font-bold text-slate-400 uppercase tracking-widest mt-2">
            {isRunning ? 'Сесія триває' : 'Очікує запуску'}
          </span>
        </div>

        {/* Action Controls */}
        <div className="flex items-center justify-center gap-4">
          <Button
            size="lg"
            variant={isRunning ? 'secondary' : 'primary'}
            onClick={toggleTimer}
            className="w-40 shadow-lg"
          >
            {isRunning ? (
              <>Призупинити</>
            ) : (
              <>
                <Play className="w-4 h-4 mr-2" />
                Старт
              </>
            )}
          </Button>

          <Button size="lg" variant="outline" onClick={resetTimer} title="Скинути таймер">
            <RotateCcw className="w-5 h-5 text-slate-600" />
          </Button>
        </div>
      </div>
    </div>
  );
};
