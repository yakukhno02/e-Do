import React, { useState } from 'react';
import type { TaskCreateRequest, TaskPriority, TaskStatus } from '../../types/api';
import { Button } from '../common/Button';
import { Input } from '../common/Input';
import { Modal } from '../common/Modal';

interface CreateTaskModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: TaskCreateRequest) => Promise<void>;
}

export const CreateTaskModal: React.FC<CreateTaskModalProps> = ({
  isOpen,
  onClose,
  onSubmit,
}) => {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [deadline, setDeadline] = useState('');
  const [priority, setPriority] = useState<TaskPriority>('MEDIUM');
  const [status, setStatus] = useState<TaskStatus>('TODO');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  const resetForm = () => {
    setTitle('');
    setDescription('');
    setDeadline('');
    setPriority('MEDIUM');
    setStatus('TODO');
    setErrors({});
  };

  const handleClose = () => {
    resetForm();
    onClose();
  };

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};
    if (!title.trim()) {
      newErrors.title = 'Назва завдання є обовʼязковою';
    } else if (title.trim().length > 255) {
      newErrors.title = 'Назва не повинна перевищувати 255 символів';
    }

    if (deadline) {
      const d = new Date(deadline);
      if (isNaN(d.getTime())) {
        newErrors.deadline = 'Введіть коректну дату та час дедлайну';
      }
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    try {
      setIsSubmitting(true);
      await onSubmit({
        title: title.trim(),
        description: description.trim() || undefined,
        deadline: deadline ? new Date(deadline).toISOString() : undefined,
        priority,
        status,
      });
      handleClose();
    } catch (err: any) {
      if (err?.response?.data?.validationErrors) {
        const backendErrors: Record<string, string> = {};
        err.response.data.validationErrors.forEach((ve: any) => {
          backendErrors[ve.field] = ve.message;
        });
        setErrors(backendErrors);
      } else {
        setErrors({ general: err?.response?.data?.message || 'Не вдалося створити завдання' });
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={handleClose} title="Створити нове завдання">
      <form onSubmit={handleSubmit} className="space-y-4">
        {errors.general && (
          <div className="p-3 text-xs text-rose-700 bg-rose-50 border border-rose-200 rounded-xl">
            {errors.general}
          </div>
        )}

        <Input
          label="Назва завдання"
          placeholder="Наприклад: Підготувати звіт до спринту"
          value={title}
          onChange={(e) => {
            setTitle(e.target.value);
            if (errors.title) setErrors((prev) => ({ ...prev, title: '' }));
          }}
          error={errors.title}
          required
          autoFocus
        />

        <div>
          <label className="block text-xs font-semibold text-slate-700 mb-1.5 uppercase tracking-wider">
            Опис (опціонально)
          </label>
          <textarea
            rows={3}
            placeholder="Деталі або посилання на ресурси..."
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            className="w-full px-3.5 py-2.5 bg-white border border-slate-300 rounded-xl text-sm transition-colors duration-150 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-offset-1 focus:border-indigo-500 focus:ring-indigo-100 resize-none"
          />
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1.5 uppercase tracking-wider">
              Пріоритет
            </label>
            <select
              value={priority}
              onChange={(e) => setPriority(e.target.value as TaskPriority)}
              className="w-full px-3.5 py-2.5 bg-white border border-slate-300 rounded-xl text-sm text-slate-800 focus:outline-none focus:ring-2 focus:ring-offset-1 focus:border-indigo-500 focus:ring-indigo-100 cursor-pointer"
            >
              <option value="LOW">Низький</option>
              <option value="MEDIUM">Середній</option>
              <option value="HIGH">Високий</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1.5 uppercase tracking-wider">
              Початковий статус
            </label>
            <select
              value={status}
              onChange={(e) => setStatus(e.target.value as TaskStatus)}
              className="w-full px-3.5 py-2.5 bg-white border border-slate-300 rounded-xl text-sm text-slate-800 focus:outline-none focus:ring-2 focus:ring-offset-1 focus:border-indigo-500 focus:ring-indigo-100 cursor-pointer"
            >
              <option value="TODO">До виконання (TODO)</option>
              <option value="IN_PROGRESS">В процесі (IN_PROGRESS)</option>
              <option value="DONE">Виконано (DONE)</option>
            </select>
          </div>
        </div>

        <Input
          label="Дедлайн (опціонально)"
          type="datetime-local"
          value={deadline}
          onChange={(e) => {
            setDeadline(e.target.value);
            if (errors.deadline) setErrors((prev) => ({ ...prev, deadline: '' }));
          }}
          error={errors.deadline}
        />

        <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-100">
          <Button type="button" variant="outline" onClick={handleClose}>
            Скасувати
          </Button>
          <Button type="submit" variant="primary" isLoading={isSubmitting}>
            Створити завдання
          </Button>
        </div>
      </form>
    </Modal>
  );
};
