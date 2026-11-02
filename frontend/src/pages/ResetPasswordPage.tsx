import React, { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { authApi } from '../api/auth.api';
import { extractErrorMessage } from '../api/client';
import { Alert } from '../components/common/Alert';
import { Button } from '../components/common/Button';
import { Input } from '../components/common/Input';

export const ResetPasswordPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') || '';
  const navigate = useNavigate();

  const [password, setPassword] = useState('');
  const [passwordConfirm, setPasswordConfirm] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!token) {
      setError('Токен скидання пароля відсутній або недійсний.');
      return;
    }
    if (password.length < 8 || password.length > 64) {
      setError('Пароль повинен містити від 8 до 64 символів.');
      return;
    }
    if (password !== passwordConfirm) {
      setError('Паролі не співпадають.');
      return;
    }

    try {
      setIsLoading(true);
      await authApi.resetPassword({
        token,
        password,
        passwordConfirm,
      });
      setSuccess(true);
      setTimeout(() => {
        navigate('/login');
      }, 3000);
    } catch (err) {
      setError(extractErrorMessage(err, 'Не вдалося встановити новий пароль'));
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="text-center space-y-1">
        <h2 className="text-2xl font-black text-slate-900 tracking-tight">Новий пароль</h2>
        <p className="text-xs text-slate-500">Введіть та підтвердіть новий пароль для входу</p>
      </div>

      {error && <Alert type="error" message={error} onClose={() => setError(null)} />}
      {success && (
        <Alert
          type="success"
          title="Пароль успішно змінено!"
          message="Всі попередні сесії анульовано. Перенаправляємо на сторінку входу..."
        />
      )}

      {!success && (
        <form onSubmit={handleSubmit} className="space-y-4" noValidate>
          {!token && (
            <div className="p-3 bg-amber-50 border border-amber-200 rounded-xl text-xs text-amber-800">
              Попередження: токен не знайдено в URL. Перевірте посилання з листа.
            </div>
          )}

          <Input
            label="Новий пароль"
            type="password"
            placeholder="Мінімум 8 символів"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            autoFocus
          />

          <Input
            label="Підтвердження нового пароля"
            type="password"
            placeholder="Повторіть пароль"
            value={passwordConfirm}
            onChange={(e) => setPasswordConfirm(e.target.value)}
            required
          />

          <Button
            type="submit"
            variant="primary"
            size="lg"
            className="w-full mt-2"
            isLoading={isLoading}
            disabled={!token}
          >
            Зберегти новий пароль
          </Button>
        </form>
      )}

      <div className="pt-4 border-t border-slate-100 text-center text-xs text-slate-500">
        <Link to="/login" className="font-bold text-indigo-600 hover:text-indigo-700">
          Повернутися до входу
        </Link>
      </div>
    </div>
  );
};
