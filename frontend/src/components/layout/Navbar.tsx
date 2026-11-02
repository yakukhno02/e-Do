import React, { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { CheckSquare, Clock, LogOut, Menu, User, Users, X } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import { Button } from '../common/Button';

export const Navbar: React.FC = () => {
  const { user, isAuthenticated, logout } = useAuth();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const navLinks = [
    { to: '/tasks', label: 'Завдання', icon: <CheckSquare className="w-4 h-4" /> },
    { to: '/pomodoro', label: 'Pomodoro', icon: <Clock className="w-4 h-4" /> },
    { to: '/rooms', label: 'Кімнати', icon: <Users className="w-4 h-4" /> },
  ];

  return (
    <header className="sticky top-0 z-40 bg-white/95 backdrop-blur-md border-b border-slate-200/80 shadow-2xs">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo */}
          <div className="flex items-center gap-8">
            <Link to={isAuthenticated ? '/tasks' : '/'} className="flex items-center gap-2.5 group">
              <div className="w-9 h-9 rounded-xl bg-indigo-600 flex items-center justify-center text-white shadow-md shadow-indigo-200 group-hover:scale-105 transition-transform">
                <Clock className="w-5 h-5" />
              </div>
              <span className="text-xl font-extrabold tracking-tight bg-gradient-to-r from-slate-900 to-indigo-700 bg-clip-text text-transparent">
                e-Do
              </span>
            </Link>

            {/* Desktop Navigation Links */}
            {isAuthenticated && (
              <nav className="hidden md:flex items-center gap-1">
                {navLinks.map((link) => {
                  const isActive = location.pathname === link.to;
                  return (
                    <Link
                      key={link.to}
                      to={link.to}
                      className={`flex items-center gap-2 px-3.5 py-2 rounded-xl text-sm font-semibold transition-colors ${
                        isActive
                          ? 'bg-indigo-50 text-indigo-700'
                          : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                      }`}
                    >
                      {link.icon}
                      {link.label}
                    </Link>
                  );
                })}
              </nav>
            )}
          </div>

          {/* Right side actions */}
          <div className="hidden md:flex items-center gap-3">
            {isAuthenticated ? (
              <div className="flex items-center gap-3">
                <div className="flex items-center gap-2.5 px-3 py-1.5 bg-slate-50 border border-slate-200/80 rounded-xl">
                  <div className="w-7 h-7 rounded-lg bg-indigo-100 text-indigo-700 flex items-center justify-center text-xs font-bold">
                    <User className="w-4 h-4" />
                  </div>
                  <div className="text-left">
                    <p className="text-xs font-bold text-slate-800 leading-tight">
                      {user?.name || user?.username}
                    </p>
                    <p className="text-[10px] text-slate-500 font-medium">
                      @{user?.username} {user?.role === 'ADMIN' && '· Admin'}
                    </p>
                  </div>
                </div>

                <Button
                  variant="ghost"
                  size="sm"
                  onClick={handleLogout}
                  className="text-slate-500 hover:text-rose-600 hover:bg-rose-50"
                  title="Вийти з акаунта"
                >
                  <LogOut className="w-4 h-4 mr-1.5" />
                  Вийти
                </Button>
              </div>
            ) : (
              <div className="flex items-center gap-2">
                <Link to="/login">
                  <Button variant="ghost" size="sm">
                    Увійти
                  </Button>
                </Link>
                <Link to="/register">
                  <Button variant="primary" size="sm">
                    Реєстрація
                  </Button>
                </Link>
              </div>
            )}
          </div>

          {/* Mobile hamburger menu toggle */}
          <div className="flex md:hidden">
            <button
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              className="p-2 rounded-xl text-slate-600 hover:bg-slate-100 transition-colors cursor-pointer"
              aria-label="Перемикач меню"
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile Menu Dropdown */}
      {mobileMenuOpen && (
        <div className="md:hidden border-t border-slate-200 bg-white px-4 pt-3 pb-5 space-y-3 shadow-lg">
          {isAuthenticated ? (
            <>
              <div className="flex items-center gap-3 p-3 bg-slate-50 rounded-xl border border-slate-200/80">
                <div className="w-9 h-9 rounded-xl bg-indigo-100 text-indigo-700 flex items-center justify-center font-bold">
                  {user?.name?.charAt(0) || 'U'}
                </div>
                <div>
                  <div className="text-sm font-bold text-slate-800">{user?.name}</div>
                  <div className="text-xs text-slate-500">@{user?.username} · {user?.email}</div>
                </div>
              </div>

              <div className="space-y-1">
                {navLinks.map((link) => (
                  <Link
                    key={link.to}
                    to={link.to}
                    onClick={() => setMobileMenuOpen(false)}
                    className="flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-semibold text-slate-700 hover:bg-slate-100"
                  >
                    {link.icon}
                    {link.label}
                  </Link>
                ))}
              </div>

              <div className="pt-2 border-t border-slate-100">
                <Button
                  variant="danger"
                  size="sm"
                  onClick={() => {
                    setMobileMenuOpen(false);
                    handleLogout();
                  }}
                  className="w-full justify-center"
                >
                  <LogOut className="w-4 h-4 mr-2" />
                  Вийти із системи
                </Button>
              </div>
            </>
          ) : (
            <div className="space-y-2 pt-2">
              <Link to="/login" onClick={() => setMobileMenuOpen(false)} className="block">
                <Button variant="outline" className="w-full">
                  Увійти
                </Button>
              </Link>
              <Link to="/register" onClick={() => setMobileMenuOpen(false)} className="block">
                <Button variant="primary" className="w-full">
                  Зареєструватися
                </Button>
              </Link>
            </div>
          )}
        </div>
      )}
    </header>
  );
};
