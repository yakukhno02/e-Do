CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL,
    username VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    credentials_version INT NOT NULL DEFAULT 0,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT chk_user_role CHECK (role IN ('USER', 'ADMIN'))
);

CREATE TABLE tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    deadline TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) NOT NULL DEFAULT 'TODO',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tasks_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_task_status CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE')),
    CONSTRAINT chk_task_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH'))
);
CREATE INDEX idx_tasks_user_id ON tasks(user_id);
CREATE INDEX idx_tasks_user_status_priority ON tasks(user_id, status, priority);

CREATE TABLE rooms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(6) NOT NULL,
    name VARCHAR(150) NOT NULL,
    creator_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    pomodoro_duration INT NOT NULL DEFAULT 25,
    break_duration INT NOT NULL DEFAULT 5,
    current_session_start TIMESTAMP WITH TIME ZONE,
    state_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_rooms_code UNIQUE (code),
    CONSTRAINT fk_rooms_creator FOREIGN KEY (creator_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT chk_room_status CHECK (status IN ('WAITING', 'ACTIVE', 'BREAK', 'FINISHED')),
    CONSTRAINT chk_room_durations CHECK (pomodoro_duration BETWEEN 1 AND 120 AND break_duration BETWEEN 1 AND 60)
);
CREATE INDEX idx_rooms_code ON rooms(code);

CREATE TABLE room_participants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id UUID NOT NULL,
    user_id UUID NOT NULL,
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rp_room FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
    CONSTRAINT fk_rp_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_room_participant UNIQUE (room_id, user_id)
);
CREATE INDEX idx_rp_room_id ON room_participants(room_id);
CREATE INDEX idx_rp_user_id ON room_participants(user_id);

CREATE TABLE pomodoro_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    room_id UUID,
    type VARCHAR(20) NOT NULL DEFAULT 'INDIVIDUAL',
    duration_minutes INT NOT NULL,
    break_duration_minutes INT NOT NULL,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sessions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_sessions_room FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE SET NULL,
    CONSTRAINT chk_session_type CHECK (type IN ('INDIVIDUAL', 'SHARED')),
    CONSTRAINT chk_session_status CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_session_duration CHECK (duration_minutes BETWEEN 1 AND 120),
    CONSTRAINT chk_session_break_duration CHECK (break_duration_minutes BETWEEN 1 AND 60),
    CONSTRAINT chk_session_individual_room CHECK (type <> 'INDIVIDUAL' OR room_id IS NULL)
);
CREATE UNIQUE INDEX uq_sessions_one_active_per_user
    ON pomodoro_sessions(user_id) WHERE status = 'IN_PROGRESS';
CREATE INDEX idx_sessions_user_status ON pomodoro_sessions(user_id, status);
CREATE INDEX idx_sessions_user_start_time ON pomodoro_sessions(user_id, start_time);

CREATE TABLE productivity_goals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    period VARCHAR(20) NOT NULL,
    type VARCHAR(20) NOT NULL,
    target_value INT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_goals_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_goal_period CHECK (period IN ('DAILY', 'WEEKLY')),
    CONSTRAINT chk_goal_type CHECK (type IN ('POMODORO_COUNT', 'FOCUS_MINUTES')),
    CONSTRAINT chk_goal_target CHECK (target_value > 0),
    CONSTRAINT uq_user_period_type UNIQUE (user_id, period, type)
);
CREATE INDEX idx_goals_user_id ON productivity_goals(user_id);

CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reset_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_reset_user_id ON password_reset_tokens(user_id);
