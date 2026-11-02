import React from 'react';
import { Link, Outlet } from 'react-router-dom';
import { Clock } from 'lucide-react';

export const AuthLayout: React.FC = () => {
  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-100 via-indigo-50/40 to-slate-100 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md text-center">
        <Link to="/" className="inline-flex items-center gap-2.5 group mb-4">
          <div className="w-11 h-11 rounded-2xl bg-indigo-600 flex items-center justify-center text-white shadow-lg shadow-indigo-300 group-hover:scale-105 transition-transform">
            <Clock className="w-6 h-6" />
          </div>
          <span className="text-3xl font-extrabold tracking-tight bg-gradient-to-r from-slate-900 via-indigo-900 to-indigo-700 bg-clip-text text-transparent">
            e-Do
          </span>
        </Link>
        <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">
          Фокус · Завдання · Результат
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md px-4 sm:px-0">
        <div className="bg-white py-8 px-6 sm:px-10 shadow-xl shadow-slate-200/60 rounded-3xl border border-slate-200/70">
          <Outlet />
        </div>
      </div>
    </div>
  );
};
