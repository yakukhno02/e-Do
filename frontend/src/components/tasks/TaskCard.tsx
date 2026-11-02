import React, { useState } from 'react';
import { Calendar, CheckCircle2, Clock, Trash2 } from 'lucide-react';
import type { TaskItem, TaskStatus } from '../../types/api';
import { formatDeadlineRelative } from '../../utils/formatters';
import { TaskPriorityBadge, TaskStatusBadge } from '../common/Badge';

interface TaskCardProps {
  task: TaskItem;
  onStatusChange: (taskId: string, newStatus: TaskStatus) => Promise<void>;
  onDelete?: (taskId: string) => Promise<void>;
}

export const TaskCard: React.FC<TaskCardProps> = ({
  task,
  onStatusChange,
  onDelete,
}) => {
  const [isUpdating, setIsUpdating] = useState(false);
  const deadlineInfo = formatDeadlineRelative(task.deadline);

  const handleStatusClick = async (status: TaskStatus) => {
    if (task.status === status || isUpdating) return;
    try {
      setIsUpdating(true);
      await onStatusChange(task.id, status);
    } finally {
      setIsUpdating(false);
    }
  };

  return (
    <div className="bg-white rounded-2xl border border-slate-200/80 p-5 shadow-xs hover:shadow-md transition-all duration-200 flex flex-col justify-between group">
      <div>
        <div className="flex items-start justify-between gap-3 mb-2.5">
          <div className="flex items-center gap-2 flex-wrap">
            <TaskStatusBadge status={task.status} />
            <TaskPriorityBadge priority={task.priority} />
          </div>
          {onDelete && (
            <button
              onClick={() => onDelete(task.id)}
              className="text-slate-300 hover:text-rose-500 p-1 rounded-lg hover:bg-rose-50 transition-colors opacity-0 group-hover:opacity-100 cursor-pointer"
              title="Видалити завдання"
            >
              <Trash2 className="w-4 h-4" />
            </button>
          )}
        </div>

        <h4 className={`text-base font-bold text-slate-800 mb-1.5 leading-snug ${task.status === 'DONE' ? 'line-through text-slate-400' : ''}`}>
          {task.title}
        </h4>

        {task.description && (
          <p className="text-xs text-slate-500 line-clamp-3 mb-3 leading-relaxed">
            {task.description}
          </p>
        )}
      </div>

      <div className="pt-3 border-t border-slate-100 mt-2 space-y-3">
        {/* Deadline info */}
        <div className="flex items-center text-xs text-slate-500 gap-1.5">
          <Calendar className="w-3.5 h-3.5 text-slate-400" />
          <span
            className={
              deadlineInfo.isOverdue
                ? 'text-rose-600 font-semibold'
                : deadlineInfo.isDueSoon
                ? 'text-amber-600 font-semibold'
                : 'text-slate-600'
            }
          >
            {deadlineInfo.text}
          </span>
        </div>

        {/* Quick Status Selector (FR-14) */}
        <div className="flex items-center justify-between gap-1 pt-1 bg-slate-50 p-1 rounded-xl">
          <span className="text-[11px] font-medium text-slate-500 pl-2">Статус:</span>
          <div className="inline-flex rounded-lg shadow-2xs gap-0.5">
            <button
              type="button"
              disabled={isUpdating}
              onClick={() => handleStatusClick('TODO')}
              className={`px-2 py-1 text-[11px] font-medium rounded-lg transition-colors cursor-pointer ${
                task.status === 'TODO'
                  ? 'bg-white text-slate-900 shadow-xs font-semibold'
                  : 'text-slate-500 hover:text-slate-900'
              }`}
            >
              TODO
            </button>
            <button
              type="button"
              disabled={isUpdating}
              onClick={() => handleStatusClick('IN_PROGRESS')}
              className={`px-2 py-1 text-[11px] font-medium rounded-lg transition-colors cursor-pointer flex items-center gap-1 ${
                task.status === 'IN_PROGRESS'
                  ? 'bg-indigo-600 text-white shadow-xs font-semibold'
                  : 'text-slate-500 hover:text-indigo-600'
              }`}
            >
              <Clock className="w-2.5 h-2.5" />
              В процесі
            </button>
            <button
              type="button"
              disabled={isUpdating}
              onClick={() => handleStatusClick('DONE')}
              className={`px-2 py-1 text-[11px] font-medium rounded-lg transition-colors cursor-pointer flex items-center gap-1 ${
                task.status === 'DONE'
                  ? 'bg-emerald-600 text-white shadow-xs font-semibold'
                  : 'text-slate-500 hover:text-emerald-600'
              }`}
            >
              <CheckCircle2 className="w-2.5 h-2.5" />
              Готово
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
