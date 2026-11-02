import React from 'react';
import { Filter } from 'lucide-react';
import type { TaskPriority, TaskStatus } from '../../types/api';

interface TaskFilterProps {
  statusFilter?: TaskStatus | 'ALL';
  priorityFilter?: TaskPriority | 'ALL';
  onStatusChange: (status: TaskStatus | 'ALL') => void;
  onPriorityChange: (priority: TaskPriority | 'ALL') => void;
  totalCount: number;
}

export const TaskFilter: React.FC<TaskFilterProps> = ({
  statusFilter = 'ALL',
  priorityFilter = 'ALL',
  onStatusChange,
  onPriorityChange,
  totalCount,
}) => {
  const statusTabs: { key: TaskStatus | 'ALL'; label: string }[] = [
    { key: 'ALL', label: 'Усі завдання' },
    { key: 'TODO', label: 'До виконання' },
    { key: 'IN_PROGRESS', label: 'В процесі' },
    { key: 'DONE', label: 'Виконано' },
  ];

  return (
    <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-4 bg-white p-3.5 rounded-2xl border border-slate-200/80 shadow-xs">
      {/* Status tabs */}
      <div className="flex items-center gap-1 overflow-x-auto pb-1 sm:pb-0 scrollbar-none">
        {statusTabs.map((tab) => (
          <button
            key={tab.key}
            type="button"
            onClick={() => onStatusChange(tab.key)}
            className={`px-3 py-1.5 text-xs font-semibold rounded-xl whitespace-nowrap transition-all duration-150 cursor-pointer ${
              statusFilter === tab.key
                ? 'bg-indigo-600 text-white shadow-xs'
                : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Priority & count filter */}
      <div className="flex items-center justify-between sm:justify-end gap-3 border-t sm:border-t-0 pt-2 sm:pt-0 border-slate-100">
        <div className="flex items-center gap-1.5 text-xs text-slate-500">
          <Filter className="w-3.5 h-3.5 text-slate-400" />
          <span>Пріоритет:</span>
          <select
            value={priorityFilter}
            onChange={(e) => onPriorityChange(e.target.value as TaskPriority | 'ALL')}
            className="text-xs font-medium text-slate-800 bg-slate-50 border border-slate-200 rounded-lg px-2 py-1 focus:outline-none focus:ring-1 focus:ring-indigo-500 cursor-pointer"
          >
            <option value="ALL">Всі пріоритети</option>
            <option value="HIGH">Високий</option>
            <option value="MEDIUM">Середній</option>
            <option value="LOW">Низький</option>
          </select>
        </div>

        <span className="text-xs font-semibold text-slate-400 bg-slate-100 px-2 py-0.5 rounded-full">
          {totalCount}
        </span>
      </div>
    </div>
  );
};
