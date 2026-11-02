import React from 'react';

export const Footer: React.FC = () => {
  return (
    <footer className="bg-white border-t border-slate-200/80 py-6 mt-auto">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between gap-4 text-xs text-slate-500">
        <div>
          <span className="font-semibold text-slate-700">e-Do</span> · Система управління особистими завданнями та фокус-сесіями Pomodoro
        </div>
        <div className="flex items-center gap-6">
          <span>Спринт 1 · MVP</span>
          <a
            href="https://github.com/yakukhno02/e-Do"
            target="_blank"
            rel="noopener noreferrer"
            className="hover:text-indigo-600 transition-colors"
          >
            GitHub
          </a>
        </div>
      </div>
    </footer>
  );
};
