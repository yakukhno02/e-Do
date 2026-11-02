import React, { useCallback, useEffect, useState } from 'react';
import {
  CheckCircle2,
  Clock,
  ListTodo,
  Plus,
  RefreshCw,
} from 'lucide-react';
import { extractErrorMessage } from '../api/client';
import { tasksApi } from '../api/tasks.api';
import { Alert } from '../components/common/Alert';
import { Button } from '../components/common/Button';
import { CreateTaskModal } from '../components/tasks/CreateTaskModal';
import { TaskCard } from '../components/tasks/TaskCard';
import { TaskFilter } from '../components/tasks/TaskFilter';
import type { TaskCreateRequest, TaskItem, TaskPriority, TaskStatus } from '../types/api';

export const TasksPage: React.FC = () => {
  const [tasks, setTasks] = useState<TaskItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);

  const [statusFilter, setStatusFilter] = useState<TaskStatus | 'ALL'>('ALL');
  const [priorityFilter, setPriorityFilter] = useState<TaskPriority | 'ALL'>('ALL');

  const loadTasks = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const data = await tasksApi.getTasks({
        status: statusFilter !== 'ALL' ? statusFilter : undefined,
        priority: priorityFilter !== 'ALL' ? priorityFilter : undefined,
      });
      setTasks(data);
    } catch (err) {
      setError(extractErrorMessage(err, 'Не вдалося завантажити завдання'));
    } finally {
      setIsLoading(false);
    }
  }, [statusFilter, priorityFilter]);

  useEffect(() => {
    loadTasks();
  }, [loadTasks]);

  // Handle status update (FR-14)
  const handleStatusChange = async (taskId: string, newStatus: TaskStatus) => {
    // Optimistic UI update
    setTasks((prev) =>
      prev.map((t) => (t.id === taskId ? { ...t, status: newStatus } : t))
    );

    try {
      await tasksApi.updateStatus(taskId, newStatus);
    } catch (err) {
      setError(extractErrorMessage(err, 'Не вдалося оновити статус завдання'));
      // Rollback by refreshing
      loadTasks();
    }
  };

  // Handle task creation
  const handleCreateTask = async (data: TaskCreateRequest) => {
    const created = await tasksApi.createTask(data);
    setTasks((prev) => [created, ...prev]);
  };

  // Handle task deletion
  const handleDeleteTask = async (taskId: string) => {
    if (!window.confirm('Ви впевнені, що хочете видалити це завдання?')) return;
    try {
      await tasksApi.deleteTask(taskId);
      setTasks((prev) => prev.filter((t) => t.id !== taskId));
    } catch (err) {
      setError(extractErrorMessage(err, 'Не вдалося видалити завдання'));
    }
  };

  // Counts
  const todoCount = tasks.filter((t) => t.status === 'TODO').length;
  const inProgressCount = tasks.filter((t) => t.status === 'IN_PROGRESS').length;
  const doneCount = tasks.filter((t) => t.status === 'DONE').length;

  return (
    <div className="space-y-6">
      {/* Header with Title and Create Action */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight flex items-center gap-3">
            <ListTodo className="w-8 h-8 text-indigo-600" />
            Особисті завдання
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 mt-1">
            Керуйте власними завданнями, дедлайнами та пріоритетами
          </p>
        </div>

        <div className="flex items-center gap-3">
          <Button
            variant="outline"
            size="md"
            onClick={loadTasks}
            isLoading={isLoading}
            title="Оновити список"
          >
            <RefreshCw className="w-4 h-4" />
          </Button>

          <Button
            variant="primary"
            size="md"
            onClick={() => setIsModalOpen(true)}
            className="shadow-md shadow-indigo-200"
          >
            <Plus className="w-4 h-4 mr-1.5" />
            Нове завдання
          </Button>
        </div>
      </div>

      {/* Metric Counters Banner */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 sm:gap-4">
        <div className="bg-white p-4 rounded-2xl border border-slate-200/80 shadow-2xs">
          <span className="text-xs font-semibold text-slate-500">Всього завдань</span>
          <p className="text-2xl font-black text-slate-900 mt-0.5">{tasks.length}</p>
        </div>
        <div className="bg-white p-4 rounded-2xl border border-slate-200/80 shadow-2xs">
          <div className="flex items-center gap-1.5 text-xs font-semibold text-slate-500">
            <span className="w-2 h-2 rounded-full bg-slate-400"></span>
            До виконання
          </div>
          <p className="text-2xl font-black text-slate-700 mt-0.5">{todoCount}</p>
        </div>
        <div className="bg-white p-4 rounded-2xl border border-indigo-200 shadow-2xs bg-indigo-50/20">
          <div className="flex items-center gap-1.5 text-xs font-semibold text-indigo-600">
            <Clock className="w-3.5 h-3.5" />
            В процесі
          </div>
          <p className="text-2xl font-black text-indigo-600 mt-0.5">{inProgressCount}</p>
        </div>
        <div className="bg-white p-4 rounded-2xl border border-emerald-200 shadow-2xs bg-emerald-50/20">
          <div className="flex items-center gap-1.5 text-xs font-semibold text-emerald-600">
            <CheckCircle2 className="w-3.5 h-3.5" />
            Виконано
          </div>
          <p className="text-2xl font-black text-emerald-600 mt-0.5">{doneCount}</p>
        </div>
      </div>

      {error && (
        <Alert
          type="error"
          message={error}
          onClose={() => setError(null)}
        />
      )}

      {/* Filter Bar */}
      <TaskFilter
        statusFilter={statusFilter}
        priorityFilter={priorityFilter}
        onStatusChange={setStatusFilter}
        onPriorityChange={setPriorityFilter}
        totalCount={tasks.length}
      />

      {/* Tasks List */}
      {isLoading && tasks.length === 0 ? (
        <div className="py-20 flex flex-col items-center justify-center text-center">
          <div className="w-10 h-10 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin mb-4"></div>
          <p className="text-sm font-semibold text-slate-600">Завантаження ваших завдань...</p>
        </div>
      ) : tasks.length === 0 ? (
        <div className="bg-white rounded-3xl border border-dashed border-slate-300 p-12 text-center space-y-4">
          <div className="w-14 h-14 rounded-2xl bg-indigo-50 text-indigo-600 flex items-center justify-center mx-auto">
            <ListTodo className="w-7 h-7" />
          </div>
          <div className="max-w-sm mx-auto">
            <h3 className="text-lg font-bold text-slate-800">Завдань не знайдено</h3>
            <p className="text-xs text-slate-500 mt-1">
              {statusFilter !== 'ALL' || priorityFilter !== 'ALL'
                ? 'Немає завдань, що відповідають обраним фільтрам. Спробуйте змінити фільтр.'
                : 'У вас ще немає створених завдань. Створіть перше завдання та почніть фокусуватися!'}
            </p>
          </div>
          <Button variant="primary" onClick={() => setIsModalOpen(true)}>
            <Plus className="w-4 h-4 mr-1.5" />
            Створити перше завдання
          </Button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {tasks.map((task) => (
            <TaskCard
              key={task.id}
              task={task}
              onStatusChange={handleStatusChange}
              onDelete={handleDeleteTask}
            />
          ))}
        </div>
      )}

      {/* Modal for creating a new task */}
      <CreateTaskModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSubmit={handleCreateTask}
      />
    </div>
  );
};
