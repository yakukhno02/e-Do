import React from 'react';
import { AlertCircle, CheckCircle2, Info, XCircle } from 'lucide-react';

interface AlertProps {
  type?: 'error' | 'success' | 'info' | 'warning';
  title?: string;
  message: string;
  className?: string;
  onClose?: () => void;
}

export const Alert: React.FC<AlertProps> = ({
  type = 'error',
  title,
  message,
  className = '',
  onClose,
}) => {
  const styles = {
    error: {
      bg: 'bg-rose-50 border-rose-200 text-rose-800',
      icon: <XCircle className="w-5 h-5 text-rose-500 shrink-0 mt-0.5" />,
    },
    success: {
      bg: 'bg-emerald-50 border-emerald-200 text-emerald-800',
      icon: <CheckCircle2 className="w-5 h-5 text-emerald-500 shrink-0 mt-0.5" />,
    },
    warning: {
      bg: 'bg-amber-50 border-amber-200 text-amber-800',
      icon: <AlertCircle className="w-5 h-5 text-amber-500 shrink-0 mt-0.5" />,
    },
    info: {
      bg: 'bg-indigo-50 border-indigo-200 text-indigo-800',
      icon: <Info className="w-5 h-5 text-indigo-500 shrink-0 mt-0.5" />,
    },
  };

  const current = styles[type];

  return (
    <div
      className={`p-4 rounded-xl border flex items-start gap-3 transition-all ${current.bg} ${className}`}
      role="alert"
    >
      {current.icon}
      <div className="flex-1 text-sm">
        {title && <h5 className="font-semibold mb-0.5">{title}</h5>}
        <div className="leading-relaxed">{message}</div>
      </div>
      {onClose && (
        <button
          onClick={onClose}
          className="text-slate-400 hover:text-slate-700 transition-colors cursor-pointer"
          aria-label="Закрити сповіщення"
        >
          <span className="text-xl leading-none">&times;</span>
        </button>
      )}
    </div>
  );
};
