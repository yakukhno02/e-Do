import React, { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { extractErrorMessage, extractValidationErrors } from '../api/client';
import { Alert } from '../components/common/Alert';
import { Button } from '../components/common/Button';
import { Input } from '../components/common/Input';
import { useAuth } from '../hooks/useAuth';

export const LoginPage: React.FC = () => {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [generalError, setGeneralError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const validate = (): boolean => {
    const errs: Record<string, string> = {};
    if (!email.trim()) {
      errs.email = "Email є обов'язковим";
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      errs.email = 'Введіть коректну адресу електронної пошти';
    }

    if (!password) {
      errs.password = "Пароль є обов'язковим";
    } else if (password.length < 8) {
      errs.password = 'Пароль повинен містити щонайменше 8 символів';
    }

    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setGeneralError(null);

    if (!validate()) return;

    try {
      setIsLoading(true);
      await login({ email: email.trim(), password });
      const redirectTo = searchParams.get('redirect') || '/tasks';
      navigate(redirectTo);
    } catch (err: unknown) {
      const fieldErrors = extractValidationErrors(err);
      if (Object.keys(fieldErrors).length > 0) {
        setErrors(fieldErrors);
      } else {
        const msg = extractErrorMessage(err, 'Невірний email або пароль');
        setGeneralError(msg);
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="text-center space-y-1">
        <h2 className="text-2xl font-black text-slate-900 tracking-tight">Вхід до системи</h2>
        <p className="text-xs text-slate-500">Введіть свої облікові дані для доступу до e-Do</p>
      </div>

      {generalError && (
        <Alert
          type="error"
          message={generalError}
          onClose={() => setGeneralError(null)}
        />
      )}

      <form onSubmit={handleSubmit} className="space-y-4" noValidate>
        <Input
          label="Електронна пошта"
          type="email"
          name="email"
          autoComplete="email"
          placeholder="nazar@example.com"
          value={email}
          onChange={(e) => {
            setEmail(e.target.value);
            if (errors.email) setErrors((prev) => ({ ...prev, email: '' }));
          }}
          error={errors.email}
          required
          autoFocus
        />

        <div>
          <div className="flex items-center justify-between mb-1.5">
            <label
              htmlFor="password"
              className="block text-xs font-semibold text-slate-700 uppercase tracking-wider"
            >
              Пароль <span className="text-rose-500">*</span>
            </label>
            <Link
              to="/forgot-password"
              className="text-xs font-semibold text-indigo-600 hover:text-indigo-700 transition-colors"
            >
              Забули пароль?
            </Link>
          </div>
          <Input
            id="password"
            type="password"
            name="password"
            autoComplete="current-password"
            placeholder="••••••••"
            value={password}
            onChange={(e) => {
              setPassword(e.target.value);
              if (errors.password) setErrors((prev) => ({ ...prev, password: '' }));
            }}
            error={errors.password}
            required
          />
        </div>

        <Button
          type="submit"
          variant="primary"
          size="lg"
          className="w-full mt-2"
          isLoading={isLoading}
        >
          Увійти
        </Button>
      </form>

      <div className="pt-4 border-t border-slate-100 text-center text-xs text-slate-500">
        Ще не маєте акаунта?{' '}
        <Link
          to="/register"
          className="font-bold text-indigo-600 hover:text-indigo-700 transition-colors"
        >
          Зареєструватися
        </Link>
      </div>
    </div>
  );
};
