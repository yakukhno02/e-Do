import React from 'react';
import type { TaskPriority, TaskStatus } from '../../types/api';
import { getPriorityLabel, getStatusLabel } from '../../utils/formatters';

interface BadgeProps {
  children?: React.ReactNode;
  variant?: 'default' | 'success' | 'warning' | 'danger' | 'info';
  className?: string;
}

export const Badge: React.FC<BadgeProps> = ({
  children,
  variant = 'default',
  className = '',
}) => {
  const variantStyles = {
    default: 'bg-slate-100 text-slate-700 border-slate-200',
    success: 'bg-emerald-50 text-emerald-700 border-emerald-200',
    warning: 'bg-amber-50 text-amber-700 border-amber-200',
    danger: 'bg-rose-50 text-rose-700 border-rose-200',
    info: 'bg-indigo-50 text-indigo-700 border-indigo-200',
  };

  return (
    <span
      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold border ${variantStyles[variant]} ${className}`}
    >
      {children}
    </span>
  );
};

export const TaskStatusBadge: React.FC<{ status: TaskStatus }> = ({ status }) => {
  const map: Record<TaskStatus, 'default' | 'info' | 'success'> = {
    TODO: 'default',
    IN_PROGRESS: 'info',
    DONE: 'success',
  };
  return <Badge variant={map[status]}>{getStatusLabel(status)}</Badge>;
};

export const TaskPriorityBadge: React.FC<{ priority: TaskPriority }> = ({ priority }) => {
  const map: Record<TaskPriority, 'default' | 'warning' | 'danger'> = {
    LOW: 'default',
    MEDIUM: 'warning',
    HIGH: 'danger',
  };
  return <Badge variant={map[priority]}>{getPriorityLabel(priority)}</Badge>;
};
