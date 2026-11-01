// Canonical TypeScript API Contracts as defined in docs/ARCHITECTURE.md (Section 4 & Section 7)

export type UserRole = 'USER' | 'ADMIN';
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH';
export type RoomStatus = 'WAITING' | 'ACTIVE' | 'BREAK' | 'FINISHED';
export type SessionStatus = 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
export type SessionType = 'INDIVIDUAL' | 'SHARED';
export type GoalPeriod = 'DAILY' | 'WEEKLY';
export type GoalType = 'POMODORO_COUNT' | 'FOCUS_MINUTES';

// Error Contract (Section 4)
export interface ApiValidationError {
  field: string;
  message: string;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  validationErrors?: ApiValidationError[];
}

// User & Auth Contracts
export interface UserSummary {
  id: string;
  name: string;
  username: string;
  email: string;
  role: UserRole;
}

export interface AuthResponse {
  token: string;
  user: UserSummary;
}

export interface RegisterResponse extends AuthResponse {
  userId: string;
}

export interface RegisterRequest {
  name: string;
  username: string;
  email: string;
  password: string;
  passwordConfirm: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  password: string;
  passwordConfirm: string;
}

export interface ProfileUpdateRequest {
  name: string;
  username: string;
}

export interface MessageResponse {
  message: string;
}

// Goals & Profile Contracts
export interface ProductivityGoal {
  id: string;
  period: GoalPeriod;
  type: GoalType;
  targetValue: number;
  currentValue: number;
  progressPercentage: number;
}

export interface GoalUpsertRequest {
  period: GoalPeriod;
  type: GoalType;
  targetValue: number;
}

export interface UserProfile extends UserSummary {
  completedPomodoros: number;
  totalFocusMinutes: number;
  completedSharedSessions: number;
  goals: {
    daily: ProductivityGoal[];
    weekly: ProductivityGoal[];
  };
}

// Task Contracts
export interface TaskItem {
  id: string;
  title: string;
  description: string | null;
  deadline: string | null;
  status: TaskStatus;
  priority: TaskPriority;
  createdAt: string;
  updatedAt: string;
}

export interface TaskCreateRequest {
  title: string;
  description?: string;
  deadline?: string;
  status: TaskStatus;
  priority: TaskPriority;
}

export type TaskUpdateRequest = TaskCreateRequest;

export interface TaskStatusRequest {
  status: TaskStatus;
}

export interface TaskFilterParams {
  status?: TaskStatus;
  priority?: TaskPriority;
}

// Pomodoro Contracts
export interface PomodoroStartRequest {
  durationMinutes: number;
  breakDurationMinutes: number;
}

export interface PomodoroStartResponse {
  sessionId: string;
  type: 'INDIVIDUAL';
  durationMinutes: number;
  breakDurationMinutes: number;
  startTime: string;
  status: 'IN_PROGRESS';
}

export interface PomodoroEndResponse {
  sessionId: string;
  status: 'COMPLETED' | 'CANCELLED';
  endTime: string;
  earnedFocusMinutes: number;
}

export interface SessionHistoryItem {
  id: string;
  roomId: string | null;
  type: SessionType;
  durationMinutes: number;
  breakDurationMinutes: number;
  startTime: string;
  endTime: string | null;
  status: SessionStatus;
}

export interface StatisticsSummary {
  totalCompletedPomodoros: number;
  totalFocusMinutes: number;
  totalSharedSessions: number;
  recentSessions: SessionHistoryItem[];
}

// Room Contracts
export interface RoomCreateRequest {
  name: string;
  pomodoroDuration: number;
  breakDuration: number;
}

export interface RoomJoinRequest {
  code: string;
}

export interface RoomParticipant {
  userId: string;
  username: string;
  name: string;
  isCreator: boolean;
}

export interface RoomDetails {
  id: string;
  code: string;
  name: string;
  creatorId: string;
  status: RoomStatus;
  pomodoroDuration: number;
  breakDuration: number;
  currentSessionStart: string | null;
  serverTime: string;
  version: number;
  participants: RoomParticipant[];
}

// Pagination & Admin Contracts
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface AdminUserItem extends UserSummary {
  createdAt: string;
  completedPomodoros: number;
  totalFocusMinutes: number;
  completedSharedSessions: number;
}

export interface AdminRoomItem {
  id: string;
  code: string;
  name: string;
  creatorId: string;
  status: RoomStatus;
  pomodoroDuration: number;
  breakDuration: number;
  createdAt: string;
  participantsCount: number;
}

// WebSocket Event
export interface RoomUpdatedEvent {
  eventId: string;
  type: 'ROOM_UPDATED';
  room: RoomDetails;
}
