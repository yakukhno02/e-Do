import React, { useCallback, useEffect, useState } from 'react';
import {
  Calendar,
  CheckCircle2,
  Clock,
  Edit2,
  Flame,
  Plus,
  RefreshCw,
  Shield,
  Target,
  Trophy,
} from 'lucide-react';
import { authApi } from '../api/auth.api';
import { extractErrorMessage } from '../api/client';
import { statsApi } from '../api/stats.api';
import { Alert } from '../components/common/Alert';
import { Button } from '../components/common/Button';
import { Input } from '../components/common/Input';
import { Modal } from '../components/common/Modal';
import { useAuth } from '../hooks/useAuth';
import type { GoalPeriod, GoalType, ProductivityGoal, UserProfile } from '../types/api';

export const ProfilePage: React.FC = () => {
  const { user, setUser } = useAuth();

  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Edit Profile Modal State
  const [isEditProfileOpen, setIsEditProfileOpen] = useState<boolean>(false);
  const [editName, setEditName] = useState<string>('');
  const [editUsername, setEditUsername] = useState<string>('');
  const [editErrors, setEditErrors] = useState<Record<string, string>>({});
  const [isEditingProfile, setIsEditingProfile] = useState<boolean>(false);

  // Set Goal Modal State
  const [isGoalModalOpen, setIsGoalModalOpen] = useState<boolean>(false);
  const [goalPeriod, setGoalPeriod] = useState<GoalPeriod>('DAILY');
  const [goalType, setGoalType] = useState<GoalType>('POMODORO_COUNT');
  const [goalTargetValue, setGoalTargetValue] = useState<string>('8');
  const [goalError, setGoalError] = useState<string | null>(null);
  const [isSavingGoal, setIsSavingGoal] = useState<boolean>(false);

  const loadProfile = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const data = await authApi.getProfile();
      setProfile(data);
    } catch (err: any) {
      setError(extractErrorMessage(err, 'Не вдалося завантажити профіль'));
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadProfile();
  }, [loadProfile]);

  const openEditModal = () => {
    if (profile) {
      setEditName(profile.name);
      setEditUsername(profile.username);
    } else if (user) {
      setEditName(user.name);
      setEditUsername(user.username);
    }
    setEditErrors({});
    setIsEditProfileOpen(true);
  };

  const handleUpdateProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    const errors: Record<string, string> = {};

    if (!editName.trim() || editName.trim().length < 2 || editName.trim().length > 100) {
      errors.name = "Ім'я повинно містити від 2 до 100 символів";
    }

    const usernameRegex = /^[a-zA-Z0-9_]+$/;
    if (!editUsername.trim() || editUsername.trim().length < 3 || editUsername.trim().length > 50) {
      errors.username = 'Username повинен містити від 3 до 50 символів';
    } else if (!usernameRegex.test(editUsername.trim())) {
      errors.username = 'Дозволено лише латинські літери, цифри та знак підкреслення';
    }

    if (Object.keys(errors).length > 0) {
      setEditErrors(errors);
      return;
    }

    try {
      setIsEditingProfile(true);
      setEditErrors({});
      const updated = await authApi.updateProfile({
        name: editName.trim(),
        username: editUsername.trim(),
      });
      setProfile(updated);
      setUser({
        id: updated.id,
        name: updated.name,
        username: updated.username,
        email: updated.email,
        role: updated.role,
      });
      setIsEditProfileOpen(false);
      setSuccessMessage('Профіль успішно оновлено!');
      setTimeout(() => setSuccessMessage(null), 3000);
    } catch (err: any) {
      setEditErrors({
        general: extractErrorMessage(err, 'Не вдалося оновити профіль'),
      });
    } finally {
      setIsEditingProfile(false);
    }
  };

  const handleSaveGoal = async (e: React.FormEvent) => {
    e.preventDefault();
    const val = parseInt(goalTargetValue, 10);
    if (isNaN(val) || val <= 0) {
      setGoalError('Цільове значення повинно бути додатнім числом (більше 0)');
      return;
    }

    try {
      setIsSavingGoal(true);
      setGoalError(null);
      await statsApi.upsertGoal({
        period: goalPeriod,
        type: goalType,
        targetValue: val,
      });
      setIsGoalModalOpen(false);
      setSuccessMessage('Ціль продуктивності успішно збережена!');
      setTimeout(() => setSuccessMessage(null), 3000);
      loadProfile();
    } catch (err: any) {
      setGoalError(extractErrorMessage(err, 'Не вдалося зберегти ціль'));
    } finally {
      setIsSavingGoal(false);
    }
  };

  const formatHours = (minutes: number) => {
    const hrs = Math.floor(minutes / 60);
    const mins = minutes % 60;
    if (hrs === 0) return `${mins} хв`;
    return `${hrs} год ${mins} хв`;
  };

  if (isLoading && !profile) {
    return (
      <div className="min-h-[50vh] flex flex-col items-center justify-center">
        <div className="w-10 h-10 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin mb-4" />
        <p className="text-sm font-semibold text-slate-600">Завантаження профілю...</p>
      </div>
    );
  }

  const currentUser = profile || user;
  const dailyGoals = profile?.goals?.daily || [];
  const weeklyGoals = profile?.goals?.weekly || [];

  return (
    <div className="max-w-5xl mx-auto space-y-8 py-4">
      {/* Messages */}
      {error && <Alert type="error" message={error} onClose={() => setError(null)} />}
      {successMessage && (
        <Alert type="success" message={successMessage} onClose={() => setSuccessMessage(null)} />
      )}

      {/* Header Profile Card (FR-07, FR-08) */}
      <div className="bg-white p-6 sm:p-8 rounded-3xl border border-slate-200 shadow-xs flex flex-col sm:flex-row items-center justify-between gap-6">
        <div className="flex flex-col sm:flex-row items-center gap-6 text-center sm:text-left">
          <div className="w-20 h-20 rounded-2xl bg-indigo-600 text-white flex items-center justify-center text-3xl font-extrabold shadow-md shadow-indigo-200">
            {currentUser?.name?.charAt(0) || 'U'}
          </div>
          <div className="space-y-1">
            <h1 className="text-2xl font-black text-slate-900">{currentUser?.name}</h1>
            <p className="text-sm text-slate-500">
              @{currentUser?.username} · {currentUser?.email}
            </p>
            <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-indigo-50 text-indigo-700 mt-2">
              <Shield className="w-3.5 h-3.5" />
              Роль: {currentUser?.role}
            </div>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <Button variant="outline" size="sm" onClick={loadProfile} title="Оновити дані">
            <RefreshCw className="w-4 h-4" />
          </Button>
          <Button variant="primary" size="md" onClick={openEditModal}>
            <Edit2 className="w-4 h-4 mr-2" />
            Редагувати профіль
          </Button>
        </div>
      </div>

      {/* Productivity Metric Counters */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-5">
        <div className="bg-white p-6 rounded-3xl border border-slate-200/90 shadow-2xs space-y-2 hover:border-indigo-200 transition-colors">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-indigo-600">
              Виконано Pomodoro
            </span>
            <div className="p-2 rounded-xl bg-indigo-50 text-indigo-600">
              <Flame className="w-5 h-5" />
            </div>
          </div>
          <p className="text-3xl font-black text-slate-900">
            {profile?.completedPomodoros ?? 0}
          </p>
          <p className="text-xs text-slate-400">Усі завершені фокус-сесії</p>
        </div>

        <div className="bg-white p-6 rounded-3xl border border-slate-200/90 shadow-2xs space-y-2 hover:border-violet-200 transition-colors">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-violet-600">
              Час фокусування
            </span>
            <div className="p-2 rounded-xl bg-violet-50 text-violet-600">
              <Clock className="w-5 h-5" />
            </div>
          </div>
          <p className="text-3xl font-black text-slate-900">
            {formatHours(profile?.totalFocusMinutes ?? 0)}
          </p>
          <p className="text-xs text-slate-400">Сумарно у стані повної концентрації</p>
        </div>

        <div className="bg-white p-6 rounded-3xl border border-slate-200/90 shadow-2xs space-y-2 hover:border-emerald-200 transition-colors">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-emerald-600">
              Спільні сесії
            </span>
            <div className="p-2 rounded-xl bg-emerald-50 text-emerald-600">
              <Trophy className="w-5 h-5" />
            </div>
          </div>
          <p className="text-3xl font-black text-slate-900">
            {profile?.completedSharedSessions ?? 0}
          </p>
          <p className="text-xs text-slate-400">Сесій у командних кімнатах</p>
        </div>
      </div>

      {/* Goals Section (FR-39, FR-40, FR-41) */}
      <div className="bg-white p-6 sm:p-8 rounded-3xl border border-slate-200 shadow-xs space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-100 pb-5">
          <div>
            <h3 className="text-xl font-bold text-slate-900 flex items-center gap-2.5">
              <Target className="w-6 h-6 text-indigo-600" />
              Цілі продуктивності
            </h3>
            <p className="text-xs sm:text-sm text-slate-500 mt-1">
              Встановлюйте щоденні та щотижневі цілі фокусування та відстежуйте свій прогрес
            </p>
          </div>

          <Button variant="primary" size="sm" onClick={() => setIsGoalModalOpen(true)}>
            <Plus className="w-4 h-4 mr-1.5" />
            Встановити ціль
          </Button>
        </div>

        {/* Daily Goals */}
        <div className="space-y-4">
          <h4 className="text-sm font-bold text-slate-700 uppercase tracking-wider flex items-center gap-2">
            <Calendar className="w-4 h-4 text-slate-400" />
            Денні цілі (Daily)
          </h4>

          {dailyGoals.length === 0 ? (
            <div className="p-4 rounded-2xl bg-slate-50 border border-dashed border-slate-200 text-xs text-slate-500 text-center">
              Денні цілі ще не встановлені. Натисніть «Встановити ціль», щоб створити першу!
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {dailyGoals.map((g) => (
                <GoalCard key={g.id} goal={g} />
              ))}
            </div>
          )}
        </div>

        {/* Weekly Goals */}
        <div className="space-y-4 pt-4 border-t border-slate-100">
          <h4 className="text-sm font-bold text-slate-700 uppercase tracking-wider flex items-center gap-2">
            <Calendar className="w-4 h-4 text-slate-400" />
            Тижневі цілі (Weekly)
          </h4>

          {weeklyGoals.length === 0 ? (
            <div className="p-4 rounded-2xl bg-slate-50 border border-dashed border-slate-200 text-xs text-slate-500 text-center">
              Тижневі цілі ще не встановлені.
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {weeklyGoals.map((g) => (
                <GoalCard key={g.id} goal={g} />
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Edit Profile Modal (FR-08) */}
      <Modal
        isOpen={isEditProfileOpen}
        onClose={() => setIsEditProfileOpen(false)}
        title="Редагування профілю"
      >
        <form onSubmit={handleUpdateProfile} className="space-y-4">
          {editErrors.general && (
            <div className="p-3 text-xs text-rose-700 bg-rose-50 border border-rose-200 rounded-xl">
              {editErrors.general}
            </div>
          )}

          <Input
            label="Ім'я"
            value={editName}
            onChange={(e) => setEditName(e.target.value)}
            error={editErrors.name}
            required
          />

          <Input
            label="Ім'я користувача (Username)"
            value={editUsername}
            onChange={(e) => setEditUsername(e.target.value)}
            error={editErrors.username}
            required
            helperText="Лише латиниця, цифри та символ _ (від 3 до 50 символів)"
          />

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1.5">
              Електронна пошта
            </label>
            <input
              type="text"
              disabled
              value={currentUser?.email || ''}
              className="w-full px-3.5 py-2.5 text-sm bg-slate-100 border border-slate-200 text-slate-400 rounded-xl cursor-not-allowed"
            />
            <p className="text-[11px] text-slate-400 mt-1">
              Email не можна змінювати згідно з безпековою політикою MVP
            </p>
          </div>

          <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-100">
            <Button
              variant="ghost"
              type="button"
              onClick={() => setIsEditProfileOpen(false)}
              disabled={isEditingProfile}
            >
              Скасувати
            </Button>
            <Button variant="primary" type="submit" isLoading={isEditingProfile}>
              Зберегти
            </Button>
          </div>
        </form>
      </Modal>

      {/* Set Goal Modal (FR-39, FR-40, FR-41) */}
      <Modal
        isOpen={isGoalModalOpen}
        onClose={() => setIsGoalModalOpen(false)}
        title="Встановити ціль продуктивності"
      >
        <form onSubmit={handleSaveGoal} className="space-y-4">
          {goalError && (
            <div className="p-3 text-xs text-rose-700 bg-rose-50 border border-rose-200 rounded-xl">
              {goalError}
            </div>
          )}

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                Період
              </label>
              <select
                value={goalPeriod}
                onChange={(e) => setGoalPeriod(e.target.value as GoalPeriod)}
                className="w-full px-3.5 py-2.5 text-sm bg-white border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-600 transition-colors"
              >
                <option value="DAILY">Щоденна (DAILY)</option>
                <option value="WEEKLY">Щотижнева (WEEKLY)</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                Тип цілі
              </label>
              <select
                value={goalType}
                onChange={(e) => {
                  const t = e.target.value as GoalType;
                  setGoalType(t);
                  if (t === 'POMODORO_COUNT') {
                    setGoalTargetValue(goalPeriod === 'DAILY' ? '8' : '40');
                  } else {
                    setGoalTargetValue(goalPeriod === 'DAILY' ? '200' : '1000');
                  }
                }}
                className="w-full px-3.5 py-2.5 text-sm bg-white border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-600 transition-colors"
              >
                <option value="POMODORO_COUNT">Кількість Pomodoro</option>
                <option value="FOCUS_MINUTES">Хвилини фокусу</option>
              </select>
            </div>
          </div>

          <Input
            label={
              goalType === 'POMODORO_COUNT'
                ? 'Цільова кількість сесій (шт)'
                : 'Цільовий фокус-час (хвилин)'
            }
            type="number"
            min="1"
            max="10000"
            value={goalTargetValue}
            onChange={(e) => setGoalTargetValue(e.target.value)}
            required
            helperText={
              goalType === 'POMODORO_COUNT'
                ? 'Наприклад: 8 сесій на день'
                : 'Наприклад: 200 хвилин (3 год 20 хв)'
            }
          />

          <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-100">
            <Button
              variant="ghost"
              type="button"
              onClick={() => setIsGoalModalOpen(false)}
              disabled={isSavingGoal}
            >
              Скасувати
            </Button>
            <Button variant="primary" type="submit" isLoading={isSavingGoal}>
              Зберегти ціль
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

// Sub-component for individual goal progress card
const GoalCard: React.FC<{ goal: ProductivityGoal }> = ({ goal }) => {
  const isCount = goal.type === 'POMODORO_COUNT';
  const unit = isCount ? 'сесій' : 'хв';
  const displayPercentage = Math.round(goal.progressPercentage);
  const clampedWidth = Math.min(100, Math.max(0, goal.progressPercentage));

  return (
    <div className="bg-slate-50/70 p-4 rounded-2xl border border-slate-200/80 space-y-3">
      <div className="flex items-center justify-between">
        <div>
          <span className="text-xs font-bold text-slate-800">
            {isCount ? 'Кількість Pomodoro' : 'Фокус-хвилини'}
          </span>
          <p className="text-[11px] text-slate-500">
            Ціль: <strong>{goal.targetValue} {unit}</strong>
          </p>
        </div>
        <span
          className={`text-xs font-black px-2 py-0.5 rounded-lg ${
            displayPercentage >= 100
              ? 'bg-emerald-100 text-emerald-700'
              : 'bg-indigo-100 text-indigo-700'
          }`}
        >
          {displayPercentage}%
        </span>
      </div>

      {/* Progress Bar (capped at 100% width for visual cleanlyness as per Section 9 item 7) */}
      <div className="w-full bg-slate-200 h-2.5 rounded-full overflow-hidden">
        <div
          className={`h-full rounded-full transition-all duration-500 ${
            displayPercentage >= 100 ? 'bg-emerald-500' : 'bg-indigo-600'
          }`}
          style={{ width: `${clampedWidth}%` }}
        />
      </div>

      <div className="flex items-center justify-between text-[11px] text-slate-500">
        <span>
          Виконано: <strong>{goal.currentValue} {unit}</strong>
        </span>
        {displayPercentage >= 100 && (
          <span className="text-emerald-600 font-semibold flex items-center gap-1">
            <CheckCircle2 className="w-3.5 h-3.5" />
            Досягнуто!
          </span>
        )}
      </div>
    </div>
  );
};
