import React from 'react';
import { Calendar, Edit3, Trash2 } from 'lucide-react';
import type { TaskItem } from '../../types/api';
import { formatDeadlineRelative } from '../../utils/formatters';
import { TaskPriorityBadge, TaskStatusBadge } from '../common/Badge';
import { Button } from '../common/Button';
import { Modal } from '../common/Modal';

interface TaskDetailsModalProps {
  task: TaskItem | null;
  isOpen: boolean;
  onClose: () => void;
  onEdit: (task: TaskItem) => void;
  onDelete: (taskId: string) => void;
}

export const TaskDetailsModal: React.FC<TaskDetailsModalProps> = ({
  task,
  isOpen,
  onClose,
  onEdit,
  onDelete,
}) => {
  if (!task) return null;

  const deadlineInfo = formatDeadlineRelative(task.deadline);

  const formatDate = (isoString?: string | null) => {
    if (!isoString) return '—';
    try {
      return new Date(isoString).toLocaleString('uk-UA', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return isoString;
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Деталі завдання">
      <div className="space-y-6">
        <div className="flex items-center gap-2 flex-wrap">
          <TaskStatusBadge status={task.status} />
          <TaskPriorityBadge priority={task.priority} />
        </div>

        <div>
          <h3 className="text-xl font-bold text-slate-900 leading-snug">{task.title}</h3>
          {task.description ? (
            <p className="text-sm text-slate-600 mt-3 whitespace-pre-wrap leading-relaxed bg-slate-50 p-4 rounded-2xl border border-slate-100">
              {task.description}
            </p>
          ) : (
            <p className="text-sm italic text-slate-400 mt-2">Опис завдання відсутній</p>
          )}
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2 border-t border-slate-100 text-xs">
          <div className="bg-slate-50 p-3 rounded-xl border border-slate-100">
            <span className="text-slate-400 font-semibold block mb-1">Дедлайн:</span>
            <div className="flex items-center gap-1.5 text-slate-700">
              <Calendar className="w-4 h-4 text-slate-400" />
              <span
                className={
                  deadlineInfo.isOverdue
                    ? 'text-rose-600 font-bold'
                    : deadlineInfo.isDueSoon
                    ? 'text-amber-600 font-bold'
                    : 'text-slate-700 font-medium'
                }
              >
                {deadlineInfo.text}
              </span>
            </div>
            {task.deadline && (
              <span className="text-[11px] text-slate-400 block mt-1">
                {formatDate(task.deadline)}
              </span>
            )}
          </div>

          <div className="bg-slate-50 p-3 rounded-xl border border-slate-100 space-y-1">
            <div className="flex items-center justify-between text-slate-500">
              <span>Створено:</span>
              <span className="font-medium text-slate-700">{formatDate(task.createdAt)}</span>
            </div>
            <div className="flex items-center justify-between text-slate-500">
              <span>Оновлено:</span>
              <span className="font-medium text-slate-700">{formatDate(task.updatedAt)}</span>
            </div>
          </div>
        </div>

        <div className="flex items-center justify-between pt-4 border-t border-slate-100">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => {
              onClose();
              onDelete(task.id);
            }}
            className="text-rose-600 hover:bg-rose-50"
          >
            <Trash2 className="w-4 h-4 mr-1.5" />
            Видалити
          </Button>

          <div className="flex items-center gap-2">
            <Button variant="outline" size="sm" onClick={onClose}>
              Закрити
            </Button>
            <Button
              variant="primary"
              size="sm"
              onClick={() => {
                onClose();
                onEdit(task);
              }}
            >
              <Edit3 className="w-4 h-4 mr-1.5" />
              Редагувати
            </Button>
          </div>
        </div>
      </div>
    </Modal>
  );
};
