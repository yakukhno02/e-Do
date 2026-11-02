import React from 'react';
import { ArrowRight, KeyRound, Plus, Sparkles } from 'lucide-react';
import { Button } from '../components/common/Button';
import { Input } from '../components/common/Input';

export const RoomsPage: React.FC = () => {
  return (
    <div className="max-w-4xl mx-auto space-y-8 py-6">
      <div className="text-center space-y-2">
        <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-emerald-50 text-emerald-700 text-xs font-bold uppercase tracking-wider border border-emerald-200">
          <Sparkles className="w-3.5 h-3.5" />
          Спільні кімнати Pomodoro · Спринт 3 Scaffold
        </div>
        <h1 className="text-3xl font-black text-slate-900 tracking-tight">Синхронний фокус у кімнатах</h1>
        <p className="text-sm text-slate-500">
          Працюйте разом із колегами та друзями в реальному часі за 6-значним кодом доступу
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 pt-4">
        {/* Join Room Card */}
        <div className="bg-white p-8 rounded-3xl border border-slate-200 shadow-xs space-y-5">
          <div className="w-12 h-12 rounded-2xl bg-indigo-50 text-indigo-600 flex items-center justify-center font-bold">
            <KeyRound className="w-6 h-6" />
          </div>
          <h3 className="text-xl font-bold text-slate-900">Приєднатися до кімнати</h3>
          <p className="text-xs text-slate-500 leading-relaxed">
            Введіть 6-символьний код кімнати, отриманий від організатора, щоб синхронізувати таймер.
          </p>

          <div className="space-y-3">
            <Input
              placeholder="Код кімнати (напр. A7X9K2)"
              maxLength={6}
              className="uppercase tracking-widest text-center font-mono font-bold text-base"
            />
            <Button variant="primary" className="w-full">
              Приєднатися
              <ArrowRight className="w-4 h-4 ml-2" />
            </Button>
          </div>
        </div>

        {/* Create Room Card */}
        <div className="bg-white p-8 rounded-3xl border border-slate-200 shadow-xs space-y-5">
          <div className="w-12 h-12 rounded-2xl bg-emerald-50 text-emerald-600 flex items-center justify-center font-bold">
            <Plus className="w-6 h-6" />
          </div>
          <h3 className="text-xl font-bold text-slate-900">Створити нову кімнату</h3>
          <p className="text-xs text-slate-500 leading-relaxed">
            Створіть приватну кімнату з налаштованою тривалістю Pomodoro і запросіть учасників.
          </p>

          <div className="space-y-3">
            <Input placeholder="Назва кімнати (напр. Спринт рев'ю)" />
            <Button variant="secondary" className="w-full">
              Створити кімнату
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
};
