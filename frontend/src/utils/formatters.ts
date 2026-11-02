import type { TaskPriority, TaskStatus } from '../types/api';

export function formatDate(isoDate: string | null | undefined): string {
  if (!isoDate) return 'Без терміну';
  const date = new Date(isoDate);
  if (isNaN(date.getTime())) return 'Невалідна дата';

  return new Intl.DateTimeFormat('uk-UA', {
    day: 'numeric',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

export function formatDeadlineRelative(isoDeadline: string | null | undefined): {
  text: string;
  isOverdue: boolean;
  isDueSoon: boolean;
} {
  if (!isoDeadline) return { text: 'Без дедлайну', isOverdue: false, isDueSoon: false };

  const deadline = new Date(isoDeadline);
  if (isNaN(deadline.getTime())) return { text: 'Некоректна дата', isOverdue: false, isDueSoon: false };

  const now = new Date();
  const diffMs = deadline.getTime() - now.getTime();
  const isOverdue = diffMs < 0;
  const isDueSoon = !isOverdue && diffMs < 24 * 60 * 60 * 1000; // less than 24h

  const formatted = formatDate(isoDeadline);
  if (isOverdue) {
    return { text: `Прострочено (${formatted})`, isOverdue: true, isDueSoon: false };
  }
  if (isDueSoon) {
    return { text: `Сьогодні (${formatted})`, isOverdue: false, isDueSoon: true };
  }
  return { text: formatted, isOverdue: false, isDueSoon: false };
}

export function getStatusLabel(status: TaskStatus): string {
  switch (status) {
    case 'TODO':
      return 'До виконання';
    case 'IN_PROGRESS':
      return 'В процесі';
    case 'DONE':
      return 'Виконано';
  }
}

export function getPriorityLabel(priority: TaskPriority): string {
  switch (priority) {
    case 'LOW':
      return 'Низький';
    case 'MEDIUM':
      return 'Середній';
    case 'HIGH':
      return 'Високий';
  }
}
