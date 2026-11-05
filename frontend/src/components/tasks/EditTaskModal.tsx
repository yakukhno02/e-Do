import React, { useEffect, useState } from 'react';
import type { TaskItem, TaskPriority, TaskStatus, TaskUpdateRequest } from '../../types/api';
import { Button } from '../common/Button';
import { Input } from '../common/Input';
import { Modal } from '../common/Modal';

interface EditTaskModalProps {
  task: TaskItem | null;
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (taskId: string, data: TaskUpdateRequest) => Promise<void>;
}

export const EditTaskModal: React.FC<EditTaskModalProps> = ({
  task,
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

  useEffect(() => {
    if (task) {
      setTitle(task.title);
      setDescription(task.description || '');
      setDeadline(task.deadline ? task.deadline.slice(0, 16) : '');
      setPriority(task.priority);
      setStatus(task.status);
      setErrors({});
    }
  }, [task]);

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
    if (!task || !validate()) return;

    try {
      setIsSubmitting(true);
      await onSubmit(task.id, {
        title: title.trim(),
        description: description.trim() || undefined,
        deadline: deadline ? new Date(deadline).toISOString() : undefined,
        priority,
        status,
      });
      onClose();
    } catch (err: any) {
      if (err?.response?.data?.validationErrors) {
        const backendErrors: Record<string, string> = {};
        err.response.data.validationErrors.forEach((ve: any) => {
          backendErrors[ve.field] = ve.message;
        });
        setErrors(backendErrors);
      } else {
        setErrors({ general: err?.response?.data?.message || 'Не вдалося оновити завдання' });
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Редагувати завдання">
      <form onSubmit={handleSubmit} className="space-y-4">
        {errors.general && (
          <div className="p-3 text-xs text-rose-700 bg-rose-50 border border-rose-200 rounded-xl">
            {errors.general}
          </div>
        )}

        <Input
          label="Назва завдання"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          error={errors.title}
          required
        />

        <div>
          <label className="block text-xs font-semibold text-slate-700 mb-1.5">
            Опис (опціонально)
          </label>
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            rows={3}
            className="w-full px-3.5 py-2.5 text-sm bg-white border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-600 transition-colors placeholder:text-slate-400"
            placeholder="Додайте деталі, кроки або нотатки..."
          />
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1.5">
              Статус
            </label>
            <select
              value={status}
              onChange={(e) => setStatus(e.target.value as TaskStatus)}
              className="w-full px-3.5 py-2.5 text-sm bg-white border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-600 transition-colors"
            >
              <option value="TODO">До виконання (TODO)</option>
              <option value="IN_PROGRESS">В процесі (IN PROGRESS)</option>
              <option value="DONE">Виконано (DONE)</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1.5">
              Пріоритет
            </label>
            <select
              value={priority}
              onChange={(e) => setPriority(e.target.value as TaskPriority)}
              className="w-full px-3.5 py-2.5 text-sm bg-white border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-600 transition-colors"
            >
              <option value="LOW">Низький</option>
              <option value="MEDIUM">Середній</option>
              <option value="HIGH">Високий</option>
            </select>
          </div>
        </div>

        <Input
          label="Дедлайн"
          type="datetime-local"
          value={deadline}
          onChange={(e) => setDeadline(e.target.value)}
          error={errors.deadline}
        />

        <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-100">
          <Button variant="ghost" type="button" onClick={onClose} disabled={isSubmitting}>
            Скасувати
          </Button>
          <Button variant="primary" type="submit" isLoading={isSubmitting}>
            Зберегти зміни
          </Button>
        </div>
      </form>
    </Modal>
  );
};
