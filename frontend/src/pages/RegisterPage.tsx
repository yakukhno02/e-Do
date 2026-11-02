import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { extractErrorMessage, extractValidationErrors } from '../api/client';
import { Alert } from '../components/common/Alert';
import { Button } from '../components/common/Button';
import { Input } from '../components/common/Input';
import { useAuth } from '../hooks/useAuth';

export const RegisterPage: React.FC = () => {
  const { register } = useAuth();
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    name: '',
    username: '',
    email: '',
    password: '',
    passwordConfirm: '',
  });

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [generalError, setGeneralError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: '' }));
    }
  };

  const validate = (): boolean => {
    const errs: Record<string, string> = {};

    // Name: 2-100 characters
    if (!formData.name.trim()) {
      errs.name = "Ім'я є обов'язковим";
    } else if (formData.name.trim().length < 2 || formData.name.trim().length > 100) {
      errs.name = "Ім'я повинно містити від 2 до 100 символів";
    }

    // Username: 3-50 characters, ^[a-zA-Z0-9_]+$
    if (!formData.username.trim()) {
      errs.username = "Юзернейм є обов'язковим";
    } else if (formData.username.trim().length < 3 || formData.username.trim().length > 50) {
      errs.username = 'Юзернейм повинен містити від 3 до 50 символів';
    } else if (!/^[a-zA-Z0-9_]+$/.test(formData.username.trim())) {
      errs.username = 'Юзернейм може містити лише латинські літери, цифри та символ підкреслення';
    }

    // Email
    if (!formData.email.trim()) {
      errs.email = "Email є обов'язковим";
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email.trim())) {
      errs.email = 'Введіть коректну адресу електронної пошти';
    }

    // Password: 8-64 characters
    if (!formData.password) {
      errs.password = "Пароль є обов'язковим";
    } else if (formData.password.length < 8 || formData.password.length > 64) {
      errs.password = 'Пароль повинен містити від 8 до 64 символів';
    }

    // Password confirm
    if (!formData.passwordConfirm) {
      errs.passwordConfirm = 'Підтвердження пароля є обовʼязковим';
    } else if (formData.password !== formData.passwordConfirm) {
      errs.passwordConfirm = 'Паролі не співпадають';
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
      await register({
        name: formData.name.trim(),
        username: formData.username.trim(),
        email: formData.email.trim(),
        password: formData.password,
        passwordConfirm: formData.passwordConfirm,
      });
      navigate('/tasks');
    } catch (err: unknown) {
      const fieldErrors = extractValidationErrors(err);
      if (Object.keys(fieldErrors).length > 0) {
        setErrors(fieldErrors);
      } else {
        const msg = extractErrorMessage(err, 'Помилка реєстрації');
        setGeneralError(msg);
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="text-center space-y-1">
        <h2 className="text-2xl font-black text-slate-900 tracking-tight">Створити обліковий запис</h2>
        <p className="text-xs text-slate-500">Зареєструйтеся та розпочніть роботу в e-Do</p>
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
          label="Повне ім'я"
          type="text"
          name="name"
          placeholder="Назар Цебринський"
          value={formData.name}
          onChange={handleChange}
          error={errors.name}
          required
          autoFocus
        />

        <Input
          label="Юзернейм (@username)"
          type="text"
          name="username"
          placeholder="nazarii_dev"
          value={formData.username}
          onChange={handleChange}
          error={errors.username}
          helperText="3-50 символів: латиниця, цифри, _"
          required
        />

        <Input
          label="Електронна пошта"
          type="email"
          name="email"
          placeholder="nazar@example.com"
          value={formData.email}
          onChange={handleChange}
          error={errors.email}
          required
        />

        <Input
          label="Пароль"
          type="password"
          name="password"
          placeholder="Мінімум 8 символів"
          value={formData.password}
          onChange={handleChange}
          error={errors.password}
          required
        />

        <Input
          label="Підтвердження пароля"
          type="password"
          name="passwordConfirm"
          placeholder="Повторіть пароль"
          value={formData.passwordConfirm}
          onChange={handleChange}
          error={errors.passwordConfirm}
          required
        />

        <Button
          type="submit"
          variant="primary"
          size="lg"
          className="w-full mt-2"
          isLoading={isLoading}
        >
          Зареєструватися
        </Button>
      </form>

      <div className="pt-4 border-t border-slate-100 text-center text-xs text-slate-500">
        Вже маєте акаунт?{' '}
        <Link
          to="/login"
          className="font-bold text-indigo-600 hover:text-indigo-700 transition-colors"
        >
          Увійти
        </Link>
      </div>
    </div>
  );
};
