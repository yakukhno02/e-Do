import { useEffect, useState } from 'react';
import type { RoomStatus } from '../types/api';

interface RoomTimerInput {
  status: RoomStatus;
  pomodoroDuration: number;
  breakDuration: number;
  currentSessionStart: string | null;
  clockSkew: number;
}

export function usePomodoroTimer({
  status,
  pomodoroDuration,
  breakDuration,
  currentSessionStart,
  clockSkew,
}: RoomTimerInput) {
  const [secondsLeft, setSecondsLeft] = useState(0);

  useEffect(() => {
    const totalSeconds = (status === 'BREAK' ? breakDuration : pomodoroDuration) * 60;
    const running = status === 'ACTIVE' || status === 'BREAK';
    const start = currentSessionStart ? Date.parse(currentSessionStart) : Number.NaN;

    const update = () => {
      if (!running) {
        setSecondsLeft(totalSeconds);
        return;
      }
      if (!Number.isFinite(start)) {
        setSecondsLeft(0);
        return;
      }
      const elapsed = Math.max(0, Math.floor((Date.now() + clockSkew - start) / 1000));
      setSecondsLeft(Math.max(0, totalSeconds - elapsed));
    };

    const onVisibilityChange = () => {
      if (document.visibilityState === 'visible') update();
    };

    update();
    const intervalId = running ? window.setInterval(update, 250) : undefined;
    document.addEventListener('visibilitychange', onVisibilityChange);
    return () => {
      if (intervalId !== undefined) window.clearInterval(intervalId);
      document.removeEventListener('visibilitychange', onVisibilityChange);
    };
  }, [status, pomodoroDuration, breakDuration, currentSessionStart, clockSkew]);

  const minutes = Math.floor(secondsLeft / 60);
  const seconds = secondsLeft % 60;
  const formattedTime = `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
  return { secondsLeft, formattedTime };
}
