import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { authApi } from '../api/auth.api';
import { extractErrorMessage } from '../api/client';
import { Alert } from '../components/common/Alert';
import { Button } from '../components/common/Button';
import { Input } from '../components/common/Input';

export const ForgotPasswordPage: React.FC = () => {
  const [email, setEmail] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccessMessage(null);

    if (!email.trim() || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      setError('Введіть коректну адресу електронної пошти');
      return;
    }

    try {
      setIsLoading(true);
      const res = await authApi.forgotPassword({ email: email.trim() });
      setSuccessMessage(res.message || 'Інструкції для скидання пароля надіслано на вашу електронну пошту.');
    } catch (err) {
      setError(extractErrorMessage(err, 'Помилка надсилання інструкцій'));
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="text-center space-y-1">
        <h2 className="text-2xl font-black text-slate-900 tracking-tight">Відновлення пароля</h2>
        <p className="text-xs text-slate-500">Введіть email для отримання посилання на скидання пароля</p>
      </div>

      {error && <Alert type="error" message={error} onClose={() => setError(null)} />}
      {successMessage && <Alert type="success" message={successMessage} />}

      {!successMessage ? (
        <form onSubmit={handleSubmit} className="space-y-4" noValidate>
          <Input
            label="Електронна пошта"
            type="email"
            placeholder="nazar@example.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            autoFocus
          />

          <Button type="submit" variant="primary" size="lg" className="w-full mt-2" isLoading={isLoading}>
            Надіслати посилання
          </Button>
        </form>
      ) : (
        <div className="text-center pt-2">
          <Link to="/login">
            <Button variant="outline" className="w-full">
              Повернутися до входу
            </Button>
          </Link>
        </div>
      )}

      <div className="pt-4 border-t border-slate-100 text-center text-xs text-slate-500">
        Згадали пароль?{' '}
        <Link to="/login" className="font-bold text-indigo-600 hover:text-indigo-700">
          Увійти
        </Link>
      </div>
    </div>
  );
};
