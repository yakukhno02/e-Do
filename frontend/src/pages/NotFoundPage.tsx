import React from 'react';
import { Link } from 'react-router-dom';
import { Home } from 'lucide-react';
import { Button } from '../components/common/Button';

export const NotFoundPage: React.FC = () => {
  return (
    <div className="min-h-[70vh] flex flex-col items-center justify-center text-center px-4 space-y-4">
      <div className="text-7xl font-black text-indigo-600 font-mono">404</div>
      <h1 className="text-2xl font-bold text-slate-800">Сторінку не знайдено</h1>
      <p className="text-sm text-slate-500 max-w-sm">
        Сторінка, яку ви шукаєте, була видалена, перейменована або тимчасово недоступна.
      </p>
      <Link to="/" className="pt-2">
        <Button variant="primary">
          <Home className="w-4 h-4 mr-2" />
          Повернутися на головну
        </Button>
      </Link>
    </div>
  );
};
