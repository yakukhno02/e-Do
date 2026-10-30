# e-Do

### Архітектура системи · API · дані · план розробки

Технічна специфікація для розробників і AI coding agents. Місце у monorepo: **`docs/ARCHITECTURE.md`**.

У PDF цей документ визначено як єдине джерело істини для розробки e-Do: він узгоджує роботу backend, frontend / DevOps та генерацію коду AI-асистентами.

## Як читати та використовувати

- Розділи 1–10 містять чинну специфікацію, включно з погодженими доповненнями. Розділ 11 зберігає простежуваність усіх 24 рішень.
- Ідентифікатори ARCH/FR/NFR із PDF збережено. Нову роботу позначено окремо; її Story Points ще не оцінено.
- Для AI coding agents: дотримуйтеся DDL, DTO, правил доступу та переходів стану разом. Не відновлюйте застарілу поведінку PDF й не додавайте нові продуктові вимоги без узгодження.
- Наведені версії технологій збережено зі специфікації; оновлення версій не входило до погоджених рішень.
- Посилання на сторінки PDF в навігації позначають походження розділу. Поточний текст доповнено рішеннями команди.
- `credentialsVersion` у JWT/БД і `version` стану кімнати — технічні поля для погодженого відкликання доступу після reset та впорядкування WebSocket-подій; їхній контракт наведено явно.

<a id="contents"></a>

## Навігація

1. [Архітектура, стек і пакети](#section-1) · PDF с. 1–2
2. [Безпека та CORS](#section-2) · PDF с. 2–3
3. [PostgreSQL: повний DDL](#section-3) · PDF с. 3–6
4. [Контракт помилок](#section-4) · PDF с. 6–7
5. [REST API: request, response, валідація](#section-5) · PDF с. 7–17
6. [Синхронізація та React-хук](#section-6) · PDF с. 17–19
7. [TypeScript-контракти](#section-7) · PDF с. 19–21
8. [Docker Compose](#section-8) · PDF с. 21–22
9. [Інтеграція frontend/backend](#section-9) · PDF с. 22–23
10. [Спринти та повний беклог](#section-10) · PDF с. 23–27
11. [Погоджені рішення та відмінності від PDF](#section-11)

<a id="section-1"></a>

## 1. Загальна системна архітектура та стек технологій

### 1.1. Базові технологічні версії

| Частина системи | Технології та домовленості |
|---|---|
| Backend | Java **21 LTS**, Spring Boot **3.3.x**, Spring Web, Spring Data JPA, Spring Security, Hibernate **6.x**, Gradle **8.x**, Flyway |
| База даних | PostgreSQL **16** |
| Frontend | React **18+** (SPA), TypeScript **5.x**, Vite, Axios, React Router **v6** |
| Обмін даними | REST API over HTTPS; JSON, UTF-8; поля JSON — `camelCase`, SQL — `snake_case` |
| Синхронізація MVP | Сервер зберігає точку відліку `startTime`; клієнт опитує сервер кожні **2500 мс** |
| Наступна ітерація | Spring WebSocket + STOMP over SockJS |

### 1.2. Структура каталогів та пакетів

**Backend · Spring Boot · `com.edo`**

```text
com.edo
├── EdoApplication.java
├── config/                  # WebConfig, CorsConfig, SecurityConfig, SwaggerConfig
├── controller/              # AuthController, TaskController, RoomController, etc.
├── dto/
│   ├── request/             # Вхідні DTO (з анотаціями @NotNull, @Size тощо)
│   └── response/            # Вихідні DTO (результати, обгортки)
├── entity/                  # JPA Entities (User, Task, Room, PomodoroSession тощо)
├── enums/                   # UserRole, TaskStatus, TaskPriority, RoomStatus тощо
├── exception/               # Кастомні RuntimeExceptions + GlobalExceptionHandler
├── repository/             # Spring Data JPA інтерфейси
├── security/                # JwtTokenProvider, JwtAuthenticationFilter, CustomUserDetailsService
└── service/                 # Інтерфейси сервісів бізнес-логіки
    └── impl/                # Імплементації сервісів (Transactional логіка)
```

**Frontend · React / TypeScript · `src/`**

```text
src/
├── api/                     # Axios instance, перехоплювачі та функції запитів
│   ├── client.ts            # Базовий клієнт (baseURL, перехоплювач Bearer токена)
│   ├── auth.api.ts
│   ├── tasks.api.ts
│   ├── pomodoro.api.ts
│   ├── rooms.api.ts
│   ├── stats.api.ts
│   └── admin.api.ts
├── assets/                  # SVG, зображення, глобальні стилі
├── components/              # UI компоненти загального призначення (Modal, Button, Input)
├── context/                 # AuthContext, RoomContext
├── hooks/                   # useAuth, usePomodoroTimer, useRoomSync
├── layouts/                 # MainLayout (з Sidebar/Navbar), AuthLayout
├── pages/                   # Сторінки застосунку згідно з маршрутами
├── router/                  # AppRouter, PrivateRoute, AdminRoute
├── types/                   # TypeScript інтерфейси (співпадають 1:1 з Backend DTO)
└── utils/                   # Форматування часу, розрахунок дельти таймера
```

[↑ До змісту](#contents)

---

<a id="section-2"></a>

## 2. Безпека, автентифікація та інваріанти доступу

### 2.1. Аутентифікація через JWT (Stateless)

- Авторизація здійснюється за стандартом RFC 6750 (Bearer Token).
- Токен передається клієнтом у кожному захищеному запиті в HTTP-заголовку:

```http
Authorization: Bearer <JWT_TOKEN>
```

- Структура JWT Claims (Payload):

```jsonc
{
  "sub": "c4b1b36e-57b1-4c12-8874-9b2f6bbd4021", // userId (UUID)
  "email": "user@example.com",
  "username": "developer42",
  "role": "USER", // USER або ADMIN
  "credentialsVersion": 0,
  "iat": 1729000000,
  "exp": 1729086400 // Час життя: 24 години
}
```

- Паролі користувачів хешуються виключно через BCryptPasswordEncoder (фактор складності: 12).
- Відкритий текст пароля суворо заборонено логувати або зберігати в пам'яті довше, ніж це необхідно для автентифікації.

#### Строк дії та logout · U-01, варіант А

- JWT діє **24 години** (`86400000` мс); refresh-токенів і списку відкликаних токенів немає.
- `POST /auth/logout` повертає `204`; frontend видаляє `accessToken`, очищує стан користувача, зупиняє polling і закриває WebSocket. Токен не відкликається на сервері й за звичайного logout залишається чинним до `exp`.
- У кожному захищеному запиті backend перевіряє підпис, `exp`, існування користувача та збіг JWT `credentialsVersion` із `users.credentials_version`. Видалений акаунт або стара версія — `401`.
- Скидання пароля збільшує `credentials_version`, тому всі попередні JWT перестають прийматися. Звичайний logout це поле не змінює.
- Ролі в БД, JWT та DTO: `USER` / `ADMIN`. Spring Security створює authorities `ROLE_USER` / `ROLE_ADMIN`. Джерело чинної ролі — завантажений користувач; зміна профілю не потребує нового JWT.

### 2.2. Захист та ізоляція даних (Data Isolation Contract)

- Головний інваріант безпеки (NFR-06): Користувач не може отримати, оновити чи видалити сутність, яка належить іншому користувачеві.
- Будь-який запит на операцію із ресурсом (Task, PomodoroSession, ProductivityGoal) зобов'язаний витягувати userId із SecurityContextHolder і виконувати пошук:

```java
taskRepository.findByIdAndUserId(taskId, currentUserId)
    .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " +
        taskId));
```

- Якщо ресурс із таким ID існує, але належить іншому користувачу — повертається 404 Not Found (щоб не розкривати наявність чужого ID у системі).
- Адміністративний контур (NFR-07): Ендпоінти /api/v1/admin/** мають пре-авторизацію @PreAuthorize("hasRole('ADMIN')"). Якщо роль не відповідає, Spring Security повертає 403 Forbidden.

### 2.3. Конфігурація CORS та адреса API

| Параметр | Значення |
|---|---|
| Локальні origins | `http://localhost:3000`, `http://localhost:5173` |
| Production origins | Явний список із конфігурації середовища; wildcard не вмикається автоматично |
| Methods | `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS` |
| Headers | `Authorization`, `Content-Type`, `Accept`, `X-Requested-With` |
| Allow credentials | **`false`**; автентифікаційні cookie не використовуються |
| Frontend baseURL | **`/api/v1`** у всіх середовищах |

Vite проксіює `/api` на локальний backend; Nginx робить те саме в Docker/production. Якщо надалі потрібен шаблон `https://*.e-do.app`, застосовувати `allowedOriginPatterns`, а не записувати шаблон як точний origin.

### 2.4. Матриця доступу

| Ресурс / операція | Хто має доступ | Відмова |
|---|---|---|
| Register, login, forgot/reset password | Guest; Bearer не потрібний | `400` за невалідних даних; login також `401` |
| Профіль, особисті задачі, сесії, цілі, статистика | Поточний користувач; власні дані | `401` без чинної автентифікації; чужий або відсутній ресурс — `404` |
| Створення / join кімнати | Авторизований користувач; join за кодом | `404` за невідомого коду; `409` за неприпустимого стану |
| GET/start/stop/leave кімнати | Поточний учасник | Чужа або відсутня кімната — `404`; конфлікт стану — `409` |
| `/admin/**` | Лише `ADMIN` | `403` для автентифікованого неадміністратора |

`ADMIN` користується особистими функціями на тих самих умовах, що й `USER`. Адміністративний перегляд/видалення всіх кімнат та користувачів виконується лише через admin API; роль не дає права керувати чужим таймером через звичайні endpoints.

### 2.5. Відновлення пароля

1. `forgot-password` для синтаксично валідного email завжди повертає однакові `200` та message незалежно від існування акаунта.
2. Для існуючого акаунта сервер генерує криптографічно випадковий одноразовий токен на **30 хвилин**, зберігає лише SHA-256 хеш та надсилає посилання поштою через SMTP. Сирий токен не логувати.
3. `reset-password` атомарно перевіряє токен, строк дії та `used_at`, змінює BCrypt-хеш пароля, збільшує `credentials_version` і позначає використаними всі невикористані reset-токени цього користувача.
4. За некоректного/простроченого/використаного токена — `400` без уточнення причини. Автоматичного входу після reset немає; користувач входить знову.
5. Форма відкривається за URL `${FRONTEND_PUBLIC_URL}/reset-password?token=...`. Налаштування SMTP та публічного frontend URL задаються середовищем.

[↑ До змісту](#contents)

---

<a id="section-3"></a>

## 3. Реляційна модель даних (PostgreSQL DDL)

Схема проєкту містить 6 доменних таблиць і 1 службову таблицю reset-токенів із чіткими обмеженнями цілісності (Constraints), первинними ключами у форматі UUID та індексами для дотримання ліміту часу відповіді (NFR-01 ≤    2 с).

```sql
-- Увімкнення розширення генерації UUID (якщо відсутнє)
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. Таблиця користувачів
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

-- 2. Таблиця особистих завдань
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

-- 3. Таблиця спільних кімнат Pomodoro
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

-- 4. Таблиця учасників кімнат (M:N зв'язок)
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

-- 5. Таблиця Pomodoro-сесій (індивідуальних та спільних)
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

-- 6. Таблиця цілей продуктивності
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

-- 7. Службові токени скидання пароля: сирий токен не зберігається.
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

```

### 3.1. Міграції та аудит

- Початкова схема: `backend/src/main/resources/db/migration/V1__init_schema.sql`; наступні зміни — новими Flyway-міграціями. Hibernate працює з `ddl-auto: validate`.
- Якщо попередню схему вже застосовано в будь-якому середовищі, не переписувати застосовану V1: оформити зміни наступною міграцією. Перед `NOT NULL` для `name` потрібна явна стратегія заповнення наявних рядків; наявні дані репозиторію тут не перевірені.
- `created_at` / `updated_at` заповнює JPA auditing (`@CreatedDate`, `@LastModifiedDate`, `AuditingEntityListener`, `@EnableJpaAuditing`). SQL defaults підтримують вставки; прямі SQL-оновлення мають явно змінювати `updated_at`.
- `state_version` кімнати відображається як `version` у DTO; збільшується при зміні стану, складу учасників чи автора. Polling/GET сам по собі версію не змінює.
- `SHARED` із `room_id = NULL` дозволений для історії видалених кімнат. `INDIVIDUAL` завжди має `room_id = NULL`.
- Обмеження `uq_sessions_one_active_per_user` захищає від одночасного створення двох фокус-сесій навіть за конкурентних запитів; порушення перетворюється на `409`.

[↑ До змісту](#contents)

---

<a id="section-4"></a>

## 4. Специфікація стандарту помилок (Error Contract)

При виникненні помилок клієнт отримує стандартизоване тіло відповіді (NFR-12, NFR-19):

```typescript
// TypeScript Interface: ApiErrorResponse
export interface ApiErrorResponse {
  timestamp: string;      // ISO-8601, наприклад "2026-10-28T14:30:00.000Z"
  status: number;         // HTTP Status Code (400, 401, 403, 404, 409, 500)
  error: string;          // Назва статусу ("Bad Request", "Conflict")
  message: string;        // Зрозумілий опис помилки для користувача
  path: string;           // Шлях запиту, наприклад "/api/v1/auth/login"
  validationErrors?: {    // Заповнюється тільки для 400 при помилках валідації форм
    field: string;        // Назва поля форми ("email", "password")
    message: string;      // "Email must be valid", "Password must be at least 8 chars"
  }[];
}
```

[↑ До змісту](#contents)

---

<a id="section-5"></a>

## 5. Деталізований контракт REST API

[Auth та профіль](#api-1) · [Завдання](#api-2) · [Індивідуальний Pomodoro](#api-3) · [Спільні кімнати](#api-4) · [Статистика та цілі](#api-5) · [Адміністрування](#api-6)

**Чинний контракт:** приклади PDF узгоджено з рішеннями команди. Типи відповіді наведено в розділі 7; правила безпеки — у розділі 2. Методи start/complete/cancel/stop/leave/logout не потребують тіла запиту. Усі часові поля API — ISO-8601 UTC. Для всіх захищених методів діють `401`, перевірка власника/участі та єдиний формат помилки.

### Загальні правила валідації та помилок

- Усі поля request DTO обов'язкові, крім явно опціональних `description` / `deadline` завдання. `null` для обов'язкового поля та невідоме enum-значення — `400`.
- `400`: невалідний JSON, UUID, поля чи query-параметри; `401`: відсутня/нечинна автентифікація; `403`: недостатня admin-роль; `404`: відсутній або недоступний ресурс; `409`: конфлікт стану чи унікальності. Тіло — `ApiErrorResponse`.
- Login/register повертають `AuthResponse` / `RegisterResponse`. Register не приймає роль від клієнта: новий користувач має `USER`.
- Body відсутній для logout, complete/cancel, room start/stop/leave та всіх DELETE. DELETE/logout/reset при успіху не повертають JSON.

### Покажчик API

| Метод | Шлях | Операція |
|---|---|---|
| `POST` | [`/api/v1/auth/register`](#endpoint-01) | Реєстрація нового користувача (FR-02) |
| `POST` | [`/api/v1/auth/login`](#endpoint-02) | Авторизація (FR-03) |
| `POST` | [`/api/v1/auth/forgot-password`](#endpoint-03) | Відновлення пароля (FR-04) |
| `POST` | [`/api/v1/auth/reset-password`](#endpoint-29) | Встановлення нового пароля (FR-04) |
| `POST` | [`/api/v1/auth/logout`](#endpoint-04) | Вихід із системи (FR-06) |
| `GET` | [`/api/v1/users/me`](#endpoint-05) | Отримання профілю поточного користувача (FR-07) |
| `PUT` | [`/api/v1/users/me`](#endpoint-06) | Редагування профілю (FR-08) |
| `GET` | [`/api/v1/tasks`](#endpoint-07) | Отримання списку завдань (FR-10, FR-15) |
| `POST` | [`/api/v1/tasks`](#endpoint-08) | Створення завдання (FR-09, FR-14) |
| `GET` | [`/api/v1/tasks/{id}`](#endpoint-09) | Отримання окремого завдання (FR-11) |
| `PUT` | [`/api/v1/tasks/{id}`](#endpoint-10) | Повне редагування завдання (FR-12) |
| `PATCH` | [`/api/v1/tasks/{id}/status`](#endpoint-11) | Швидка зміна статусу (FR-14) |
| `DELETE` | [`/api/v1/tasks/{id}`](#endpoint-12) | Видалення завдання (FR-13) |
| `GET` | [`/api/v1/pomodoro/sessions`](#endpoint-30) | Повна історія сесій (FR-35) |
| `POST` | [`/api/v1/pomodoro/start`](#endpoint-13) | Запуск індивідуального таймера (FR-16, FR-17) |
| `POST` | [`/api/v1/pomodoro/{sessionId}/complete`](#endpoint-14) | Завершення сесії (успішне — FR-19) |
| `POST` | [`/api/v1/pomodoro/{sessionId}/cancel`](#endpoint-15) | Дострокове завершення/скасування (FR-20) |
| `POST` | [`/api/v1/rooms`](#endpoint-16) | Створення кімнати (FR-22, FR-23) |
| `POST` | [`/api/v1/rooms/join`](#endpoint-17) | Приєднання до кімнати за кодом (FR-24, FR-25) |
| `GET` | [`/api/v1/rooms/{id}`](#endpoint-18) | Отримання поточного стану кімнати (Polling / Sync — FR-26, FR-27, FR-29) |
| `POST` | [`/api/v1/rooms/{id}/start`](#endpoint-19) | Запуск спільної Pomodoro-сесії (FR-28) |
| `POST` | [`/api/v1/rooms/{id}/stop`](#endpoint-20) | Дострокове завершення спільної сесії (FR-31) |
| `POST` | [`/api/v1/rooms/{id}/leave`](#endpoint-22) | Вихід із кімнати (FR-32) |
| `GET` | [`/api/v1/statistics/summary`](#endpoint-23) | Агрегована статистика (FR-35, FR-36, FR-37, FR-38) |
| `POST` | [`/api/v1/goals`](#endpoint-24) | Встановлення або оновлення цілі продуктивності (FR-39, FR-40, FR-41) |
| `GET` | [`/api/v1/admin/users`](#endpoint-25) | Список користувачів (FR-43) |
| `DELETE` | [`/api/v1/admin/users/{userId}`](#endpoint-26) | Видалення користувача (FR-44) |
| `GET` | [`/api/v1/admin/rooms`](#endpoint-27) | Список створених кімнат (FR-45) |
| `DELETE` | [`/api/v1/admin/rooms/{roomId}`](#endpoint-28) | Примусове видалення кімнати (FR-46) |

<a id="api-1"></a>

### 5.1. Модуль аутентифікації та профілю (Auth & Profile)

<a id="endpoint-01"></a>

#### 1. Реєстрація нового користувача (FR-02)

**`POST /api/v1/auth/register`**

**Доступ:** Публічний (Guest)

**Тіло запиту**

```json
{
  "name": "Назар Цебринський",
  "username": "nazarii_dev",
  "email": "nazar@example.com",
  "password": "StrongPassword123!",
  "passwordConfirm": "StrongPassword123!"
}
```

**Валідація (Backend)**

- name: @NotBlank, @Size(min = 2, max = 100)
- username: @NotBlank, @Size(min = 3, max = 50), регулярний вираз ^[a-zA-Z0-9_]+$
- email: @NotBlank, @Email
- password: @NotBlank, @Size(min = 8, max = 64)
- passwordConfirm: повинно збігатися з password

**Відповідь · `201 Created`**

```json
{
  "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "name": "Назар Цебринський",
    "username": "nazarii_dev",
    "email": "nazar@example.com",
    "role": "USER"
  }
}
```

**Помилки**

- 400 Bad Request — невідповідність валідації або password != passwordConfirm.
- 409 Conflict — email або username вже зайнятий ("Username or email already exists").

<a id="endpoint-02"></a>

#### 2. Авторизація (FR-03)

**`POST /api/v1/auth/login`**

**Доступ:** Публічний (Guest)

**Тіло запиту**

```json
{
  "email": "nazar@example.com",
  "password": "StrongPassword123!"
}
```

**Відповідь · `200 OK`**

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "name": "Назар Цебринський",
    "username": "nazarii_dev",
    "email": "nazar@example.com",
    "role": "USER"
  }
}
```

**Помилки**

- 401 Unauthorized — невірний логін або пароль ("Invalid email or password").

<a id="endpoint-03"></a>

#### 3. Відновлення пароля (FR-04)

**`POST /api/v1/auth/forgot-password`**

**Доступ:** Публічний (Guest)

**Тіло запиту**

```json
{
  "email": "nazar@example.com"
}
```

**Відповідь · `200 OK`**

```json
{
  "message": "Password reset instructions have been sent to your email."
}
```

**Помилки**

- `400` — невалідний email. За невідомого email повертаються ті самі `200` і message, що й для існуючого. Одноразовий токен діє 30 хвилин; серверна логіка — розділ 2.5.

<a id="endpoint-04"></a>

#### 4. Вихід із системи (FR-06)

**`POST /api/v1/auth/logout`**

**Доступ:** Авторизований (User / Admin)

**Заголовок:** `Authorization: Bearer <token>`

**Відповідь:** `204 No Content`, без тіла. Logout не відкликає JWT; клієнт видаляє його. Деталі — розділ 2.1.

<a id="endpoint-05"></a>

#### 5. Отримання профілю поточного користувача (FR-07)

**`GET /api/v1/users/me`**

**Доступ:** Авторизований (User / Admin)

**Відповідь · `200 OK`**

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "name": "Назар Цебринський",
  "username": "nazarii_dev",
  "email": "nazar@example.com",
  "role": "USER",
  "completedPomodoros": 28,
  "totalFocusMinutes": 700,
  "completedSharedSessions": 12,
  "goals": {
    "daily": [
      {
        "id": "77777777-aaaa-bbbb-cccc-111122223333",
        "period": "DAILY",
        "type": "POMODORO_COUNT",
        "targetValue": 8,
        "currentValue": 4,
        "progressPercentage": 50.0
      }
    ],
    "weekly": [
      {
        "id": "77777777-aaaa-bbbb-cccc-444455556666",
        "period": "WEEKLY",
        "type": "FOCUS_MINUTES",
        "targetValue": 2400,
        "currentValue": 1200,
        "progressPercentage": 50.0
      }
    ]
  }
}
```

<a id="endpoint-06"></a>

#### 6. Редагування профілю (FR-08)

**`PUT /api/v1/users/me`**

**Доступ:** Авторизований (User / Admin)

**Тіло запиту**

```json
{
  "name": "Назар Нове Ім'я",
  "username": "nazarii_updated"
}
```

**Правила:** email у межах MVP змінювати заборонено. `name`: `@NotBlank`, 2–100 символів; `username`: `@NotBlank`, 3–50 символів, `^[a-zA-Z0-9_]+$`, як при реєстрації. Помилки валідації — `400`.

**Відповідь · `200 OK`:** Повертає оновлений об'єкт профілю (як у GET /me).

**Помилки:** 409 Conflict — новий username вже зайнятий іншим акаунтом.

<a id="endpoint-29"></a>

#### 7. Встановлення нового пароля (FR-04)

**`POST /api/v1/auth/reset-password`**

**Доступ:** публічний; токен reset не є JWT для API.

**Тіло запиту**

```json
{
  "token": "opaque-single-use-reset-token",
  "password": "NewStrongPassword123!",
  "passwordConfirm": "NewStrongPassword123!"
}
```

`token`: непорожній; пароль 8–64 символи, підтвердження має збігатися. **`204 No Content`** після успіху; **`400`** за невалідних даних або непридатного токена. Усі попередні JWT користувача втрачають чинність; далі потрібен login.

<a id="api-2"></a>

### 5.2. Модуль керування завданнями (Task Management)

<a id="endpoint-07"></a>

#### 1. Отримання списку завдань (FR-10, FR-15)

**`GET /api/v1/tasks`**

**Доступ:** Авторизований

**Query Parameters (опціональні фільтри)**

- status: TODO, IN_PROGRESS, DONE
- priority: LOW, MEDIUM, HIGH

**Відповідь · `200 OK`**

```json
[
  {
    "id": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
    "title": "Спроєктувати схему БД",
    "description": "Створити DDL для таблиць users, tasks, rooms",
    "deadline": "2026-10-31T23:59:59Z",
    "status": "IN_PROGRESS",
    "priority": "HIGH",
    "createdAt": "2026-10-28T09:00:00Z",
    "updatedAt": "2026-10-28T11:30:00Z"
  }
]
```

<a id="endpoint-08"></a>

#### 2. Створення завдання (FR-09, FR-14)

**`POST /api/v1/tasks`**

**Доступ:** Авторизований

**Тіло запиту**

```json
{
  "title": "Підготувати презентацію проєкту",
  "description": "Описати архітектуру та демонстрацію MVP",
  "deadline": "2026-11-05T18:00:00Z",
  "status": "TODO",
  "priority": "MEDIUM"
}
```

**Валідація**

- title: @NotBlank, @Size(max = 255)
- status: обов'язкове, одне з: TODO, IN_PROGRESS, DONE
- priority: обов'язкове, одне з: LOW, MEDIUM, HIGH
- deadline: опціональне, валідна ISO-8601 дата в майбутньому або сьогоденні.

**Відповідь · `201 Created`:** Повертає створений об'єкт завдання з присвоєним id.

<a id="endpoint-09"></a>

#### 3. Отримання окремого завдання (FR-11)

**`GET /api/v1/tasks/{id}`**

**Доступ:** Авторизований

**Відповідь · `200 OK`:** Повертає об'єкт завдання.

**Помилки:** 404 Not Found (якщо завдання не існує або належить іншому юзеру).

<a id="endpoint-10"></a>

#### 4. Повне редагування завдання (FR-12)

**`PUT /api/v1/tasks/{id}`**

**Тіло запиту:** Співпадає зі створенням завдання (усі обов'язкові поля).

**Відповідь · `200 OK`:** Оновлений об'єкт завдання.

<a id="endpoint-11"></a>

#### 5. Швидка зміна статусу (FR-14)

**`PATCH /api/v1/tasks/{id}/status`**

**Тіло запиту**

```json
{
  "status": "DONE"
}
```

**Відповідь · `200 OK`:** Оновлений об'єкт завдання.

<a id="endpoint-12"></a>

#### 6. Видалення завдання (FR-13)

**`DELETE /api/v1/tasks/{id}`**

**Відповідь:** `204 No Content (Тіло пусте).`

<a id="api-3"></a>

### 5.3. Модуль індивідуального Pomodoro

<a id="endpoint-13"></a>

#### 1. Запуск індивідуального таймера (FR-16, FR-17)

**`POST /api/v1/pomodoro/start`**

**Тіло запиту**

```json
{
  "durationMinutes": 25,
  "breakDurationMinutes": 5
}
```

**Валідація**

- durationMinutes: @Min(1), @Max(120)
- breakDurationMinutes: @Min(1), @Max(60)

**Відповідь · `201 Created`**

```json
{
  "sessionId": "8f8c7b6a-4d3e-2c1b-0a9f-8e7d6c5b4a3f",
  "type": "INDIVIDUAL",
  "durationMinutes": 25,
  "breakDurationMinutes": 5,
  "startTime": "2026-10-28T12:00:00.000Z",
  "status": "IN_PROGRESS"
}
```

<a id="endpoint-14"></a>

#### 2. Завершення сесії (успішне — FR-19)

**`POST /api/v1/pomodoro/{sessionId}/complete`**

**Відповідь · `200 OK`**

```json
{
  "sessionId": "8f8c7b6a-4d3e-2c1b-0a9f-8e7d6c5b4a3f",
  "status": "COMPLETED",
  "endTime": "2026-10-28T12:25:00.000Z",
  "earnedFocusMinutes": 25
}
```

<a id="endpoint-15"></a>

#### 3. Дострокове завершення/скасування (FR-20)

**`POST /api/v1/pomodoro/{sessionId}/cancel`**

**Відповідь · `200 OK`**

```json
{
  "sessionId": "8f8c7b6a-4d3e-2c1b-0a9f-8e7d6c5b4a3f",
  "status": "CANCELLED",
  "endTime": "2026-10-28T12:14:22.000Z",
  "earnedFocusMinutes": 0
}
```

<a id="api-4"></a>

### 5.4. Модуль спільних Pomodoro-кімнат (Shared Rooms)

<a id="endpoint-16"></a>

#### 1. Створення кімнати (FR-22, FR-23)

**`POST /api/v1/rooms`**

**Тіло запиту**

```json
{
  "name": "KPI IM-41 Study Group",
  "pomodoroDuration": 30,
  "breakDuration": 5
}
```

**Відповідь · `201 Created`**

```json
{
  "id": "e9b1d2c3-1122-3344-5566-778899aabbcc",
  "code": "A7X9K2",
  "name": "KPI IM-41 Study Group",
  "creatorId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "status": "WAITING",
  "pomodoroDuration": 30,
  "breakDuration": 5,
  "currentSessionStart": null,
  "participants": [
    {
      "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "username": "nazarii_dev",
      "name": "Назар Цебринський",
      "isCreator": true
    }
  ],
  "serverTime": "2026-10-28T12:00:00.000Z",
  "version": 0
}
```

**Валідація:** `name` — непорожній, максимум 150 символів; `pomodoroDuration` — 1–120, `breakDuration` — 1–60 хвилин. Сервер створює код із 6 символів зазначеного алфавіту, при конфлікті унікальності повторює генерацію. Автор одразу стає учасником.

<a id="endpoint-17"></a>

#### 2. Приєднання до кімнати за кодом (FR-24, FR-25)

**`POST /api/v1/rooms/join`**

**Тіло запиту**

```json
{
  "code": "A7X9K2"
}
```

**Валідація:** `code`: `@NotBlank`, рівно 6 символів. Перевести до верхнього регістру; алфавіт `ABCDEFGHJKLMNPQRSTUVWXYZ23456789`. Невідповідність — `400`. Приєднання лише до `WAITING` або `FINISHED`; `ACTIVE`/`BREAK` — `409`. Повторний join поточного учасника в дозволеному стані повертає той самий стан без дублювання membership.

**Відповідь · `200 OK`:** Повертає повний стан кімнати (ідентичний до GET /rooms/{id}).

**Помилки**

- 404 Not Found — кімнати з таким кодом не існує ("Room with specified code not found").

<a id="endpoint-18"></a>

#### 3. Отримання поточного стану кімнати (Polling / Sync — FR-26, FR-27, FR-29)

**`GET /api/v1/rooms/{id}`**

**Відповідь · `200 OK`**

```json
{
  "id": "e9b1d2c3-1122-3344-5566-778899aabbcc",
  "code": "A7X9K2",
  "name": "KPI IM-41 Study Group",
  "creatorId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "status": "ACTIVE",
  "pomodoroDuration": 30,
  "breakDuration": 5,
  "currentSessionStart": "2026-10-28T12:30:00.000Z",
  "serverTime": "2026-10-28T12:35:10.000Z",
  "participants": [
    {
      "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "username": "nazarii_dev",
      "name": "Назар Цебринський",
      "isCreator": true
    },
    {
      "userId": "88885f64-5717-4562-b3fc-2c963f661111",
      "username": "mariia_ya",
      "name": "Марія Якухно",
      "isCreator": false
    }
  ],
  "version": 1
}
```

<a id="endpoint-19"></a>

#### 4. Запуск спільної Pomodoro-сесії (FR-28)

**`POST /api/v1/rooms/{id}/start`**

**Правило:** Будь-який поточний учасник запускає кімнату зі стану `WAITING` або `FINISHED`. Сервер атомарно створює по одній `SHARED`-сесії для всіх поточних учасників із єдиним `startTime`. Якщо хоч один має іншу `IN_PROGRESS`-сесію, весь запуск відхиляється з `409`. Повторний start у `ACTIVE`/`BREAK` — `409`.

**Відповідь · `200 OK`:** Повертає оновлений об'єкт кімнати зі статусом ACTIVE та новим currentSessionStart.

<a id="endpoint-20"></a>

#### 5. Дострокове завершення спільної сесії (FR-31)

**`POST /api/v1/rooms/{id}/stop`**

**Правило:** Доступно лише в `ACTIVE`; в іншому стані — `409`. Усі поточні активні сесії цього фокус-циклу атомарно стають `CANCELLED`, нарахування — 0; кімната стає `FINISHED`. Перед командою сервер спочатку застосовує прострочені автоматичні переходи.

**Відповідь · `200 OK`:** Повертає оновлений об'єкт кімнати.

<a id="endpoint-22"></a>

#### 6. Вихід із кімнати (FR-32)

**`POST /api/v1/rooms/{id}/leave`**

**Правило:** Видаляє membership; власна активна shared-сесія стає `CANCELLED` з нульовим нарахуванням. Інші учасники продовжують. Авторство переходить за `joined_at ASC, user_id ASC`. Порожню кімнату видалити; історію решти сесій зберегти з `room_id = NULL`. Усе — в одній транзакції.

**Відповідь · `200 OK`:** {"message": "Left the room successfully"}.

<a id="api-5"></a>

### 5.5. Модуль статистики та цілей продуктивності

<a id="endpoint-23"></a>

#### 1. Агрегована статистика (FR-35, FR-36, FR-37, FR-38)

**`GET /api/v1/statistics/summary`**

**Відповідь · `200 OK`**

```json
{
  "totalCompletedPomodoros": 45,
  "totalFocusMinutes": 1125,
  "totalSharedSessions": 18,
  "recentSessions": [
    {
      "id": "11223344-aaaa-bbbb-cccc-ddddeeeeffff",
      "type": "SHARED",
      "durationMinutes": 25,
      "startTime": "2026-10-28T09:00:00Z",
      "status": "COMPLETED",
      "roomId": "e9b1d2c3-1122-3344-5566-778899aabbcc",
      "breakDurationMinutes": 5,
      "endTime": "2026-10-28T09:25:00Z"
    }
  ]
}
```

<a id="endpoint-24"></a>

#### 2. Встановлення або оновлення цілі продуктивності (FR-39, FR-40, FR-41)

**`POST /api/v1/goals`**

**Тіло запиту**

```jsonc
{
  "period": "DAILY", // 'DAILY' | 'WEEKLY'
  "type": "POMODORO_COUNT", // 'POMODORO_COUNT' | 'FOCUS_MINUTES'
  "targetValue": 8
}
```

**Валідація:** targetValue: @Min(1)

**Відповідь · `200 OK`**

```json
{
  "id": "77777777-aaaa-bbbb-cccc-111122223333",
  "period": "DAILY",
  "type": "POMODORO_COUNT",
  "targetValue": 8,
  "currentValue": 3,
  "progressPercentage": 37.5
}
```

<a id="api-6"></a>

### 5.6. Модуль адміністратора (Admin Only — FR-42 – FR-46)

<a id="endpoint-25"></a>

#### 1. Список користувачів (FR-43)

**`GET /api/v1/admin/users`**

**Доступ:** Тільки користувачі з роллю ADMIN.

**Параметри:** `page=0`, `size=20` (1–100). **Відповідь `200`:** `PageResponse<AdminUserItem>` (розділ 7); сортування `createdAt DESC, id DESC`. Містить базові поля, `createdAt` та агреговану статистику; жодних хешів чи токенів.

**Приклад відповіді**

```json
{
  "content": [
    {
      "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "name": "Назар Цебринський",
      "username": "nazarii_dev",
      "email": "nazar@example.com",
      "role": "USER",
      "createdAt": "2026-10-28T09:00:00Z",
      "completedPomodoros": 28,
      "totalFocusMinutes": 700,
      "completedSharedSessions": 12
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

<a id="endpoint-26"></a>

#### 2. Видалення користувача (FR-44)

**`DELETE /api/v1/admin/users/{userId}`**

**Відповідь:** `204 No Content`, без тіла.

<a id="endpoint-27"></a>

#### 3. Список створених кімнат (FR-45)

**`GET /api/v1/admin/rooms`**

**Параметри:** `page=0`, `size=20` (1–100). **Відповідь `200`:** `PageResponse<AdminRoomItem>` (розділ 7); сортування `createdAt DESC, id DESC`. Усі наявні кімнати незалежно від статусу. Видалені порожні кімнати не повертаються.

**Приклад відповіді**

```json
{
  "content": [
    {
      "id": "e9b1d2c3-1122-3344-5566-778899aabbcc",
      "code": "A7X9K2",
      "name": "KPI IM-41 Study Group",
      "creatorId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "status": "WAITING",
      "pomodoroDuration": 30,
      "breakDuration": 5,
      "createdAt": "2026-10-28T09:00:00Z",
      "participantsCount": 2
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

<a id="endpoint-28"></a>

#### 4. Примусове видалення кімнати (FR-46)

**`DELETE /api/v1/admin/rooms/{roomId}`**

**Відповідь:** `204 No Content`, без тіла.

### 5.7. Єдині правила сесій та кімнат

#### Індивідуальна сесія

- `start`: дозволений лише за відсутності будь-якої `IN_PROGRESS`-сесії користувача; інакше `409`. Під час спільної `BREAK` користувач може почати індивідуальний фокус, але наступний shared-start чекатиме його завершення.
- `complete`: лише для власної `INDIVIDUAL`-сесії після `startTime + durationMinutes`; раніше — `409`. Результат: `COMPLETED`, `endTime` = запланований кінець, `earnedFocusMinutes` = повна тривалість.
- `cancel`: лише для власної `INDIVIDUAL`-сесії, яка ще триває; `endTime` = серверний час скасування, `earnedFocusMinutes = 0`.
- Повторний complete для `COMPLETED` або cancel для `CANCELLED` повертає вже збережений результат без повторного нарахування. Протилежна дія над термінальним станом — `409`.
- Особисті complete/cancel не керують `SHARED`; для таких запитів — `409`, навіть якщо сесія належить поточному користувачу.
- До виконання cancel сервер застосовує завершення за строком: прострочену сесію не можна скасувати заднім числом.
- Індивідуальна перерва — локальний відлік після успішного фокусу на `breakDurationMinutes`, без окремої сесії БД і без фокус-нарахувань; наступний фокус запускає користувач.

#### Спільна кімната

| Поточний стан | Умова / дія | Наступний стан |
|---|---|---|
| `WAITING` | Учасник викликає start | `ACTIVE` |
| `FINISHED` | Учасник викликає start | `ACTIVE` |
| `ACTIVE` | Вичерпано час фокусу | `BREAK`; активні сесії стають `COMPLETED` |
| `ACTIVE` | Учасник викликає stop | `FINISHED`; активні сесії стають `CANCELLED` |
| `BREAK` | Вичерпано час перерви | `WAITING` |

- Join лише в `WAITING` / `FINISHED`; під час `ACTIVE` / `BREAK` — `409`. Leave дозволений у всіх станах.
- Ручного `/rooms/{id}/break` **немає**. Після перерви новий фокус автоматично не починається.
- `currentSessionStart`: початок фокусу в `ACTIVE`, початок перерви в `BREAK`, `null` у `WAITING` / `FINISHED`.
- Після успішного фокусу початок перерви = запланований кінець фокусу. Простій backend не подовжує фокус чи перерву.
- Повна відповідь create/join/GET/start/stop — `RoomDetails` із `serverTime` та `version`; leave повертає message.
- Scheduler перевіряє прострочення раз на секунду; GET стану та команди також застосовують належні переходи перед відповіддю. Після рестарту scheduler опрацьовує пропущені переходи: якщо спливли і фокус, і перерва, кінцевий стан — `WAITING`.
- Транзакційні блокування кімнати/користувачів та частковий унікальний індекс захищають від гонок start/stop/leave/complete. За shared-start блокувати користувачів у стабільному порядку UUID; помилка одного учасника відкочує запуск повністю.
- Стан `COMPLETED` зберігається один раз. Статистика агрегує таблицю сесій, без окремого повторного інкременту лічильників.

#### Видалення адміністратором

- Видалення користувача: спочатку виконати правила leave/передачі авторства для кожної кімнати, потім видалити акаунт та його персональні записи. Активна особиста сесія зникає разом з акаунтом. Існуючі JWT та WebSocket-доступ припиняють працювати.
- Видалення кімнати: спочатку узгодити прострочення; ще активні сесії скасувати без нарахування, потім видалити кімнату. Завершені та скасовані записи залишаються з `room_id = NULL`, тип `SHARED` зберігається.
- Обидві операції транзакційні; відсутній користувач/кімната — `404`, успіх — `204` без тіла.

### 5.8. Обчислення статистики та цілей

- Агрегати ізольовані за поточним користувачем. У розрахунок потрапляють лише `COMPLETED`.
- `totalCompletedPomodoros` / профільне `completedPomodoros` — кількість усіх завершених фокус-сесій, включно зі спільними.
- `totalFocusMinutes` — сума `duration_minutes` завершених сесій; `totalSharedSessions` / профільне `completedSharedSessions` — кількість завершених `SHARED`.
- День: `[00:00 UTC, наступні 00:00 UTC)`; тиждень: `[понеділок 00:00 UTC, наступний понеділок 00:00 UTC)`. Віднесення сесії до періоду — за `endTime`.
- На користувача — максимум 4 цілі: одна для кожної пари `period + type`; POST виконує upsert за цією парою та повертає `200`.
- `POMODORO_COUNT` рахує всі завершені фокус-сесії в періоді; `FOCUS_MINUTES` — суму їхніх хвилин. `progressPercentage = currentValue / targetValue * 100`, може бути понад 100; UI обмежує лише заповнення смуги.
- `goals.daily` / `goals.weekly` — масиви `ProductivityGoal`; за відсутності цілей — `[]`.
- `recentSessions` — останні 10 власних сесій усіх статусів, `startTime DESC, id DESC`.

<a id="endpoint-30"></a>

### 5.9. Повна історія сесій (FR-35)

**`GET /api/v1/pomodoro/sessions?page=0&size=20`**

**Доступ:** авторизований користувач, лише власні записи. Повертаються всі статуси та обидва типи. Сортування: `startTime DESC, id DESC`.

**Параметри:** `page >= 0`; `size` від 1 до 100, типово 20. Невалідні параметри — `400`.

**Відповідь `200`:** `PageResponse<SessionHistoryItem>` (розділ 7). Порожня сторінка — `content: []` та коректні метадані, не `404`.

**Приклад відповіді**

```json
{
  "content": [
    {
      "id": "11223344-aaaa-bbbb-cccc-ddddeeeeffff",
      "roomId": null,
      "type": "SHARED",
      "durationMinutes": 25,
      "breakDurationMinutes": 5,
      "startTime": "2026-10-28T09:00:00Z",
      "endTime": "2026-10-28T09:25:00Z",
      "status": "COMPLETED"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

[↑ До змісту](#contents)

---

<a id="section-6"></a>

## 6. Синхронізація таймера та WebSocket

### 6.1. Єдине джерело часу

Сервер зберігає UTC-точку запуску, тривалість та статус, а не поточну кількість секунд. Завершення фокусу й перерви виконує backend за правилами розділу 5.7. Frontend лише відображає залишок.

Для кожного запиту стану клієнт зберігає час відправлення `t0` і час отримання `t1`; `serverTime` формує сервер біля моменту серіалізації відповіді. Всі значення різниці часу нижче — мілісекунди.

```text
clientMidpoint = (t0 + t1) / 2
clockSkew = serverTime_response - clientMidpoint
adjustedNow = Date.now() + clockSkew
elapsedSeconds = max(0, floor((adjustedNow - currentSessionStart) / 1000))
durationSeconds = (status == BREAK ? breakDuration : pomodoroDuration) * 60
remainingSeconds = max(0, durationSeconds - elapsedSeconds)
```

Це наближена компенсація затримки, яка припускає приблизно симетричний мережевий шлях; абсолютної гарантії на довільній мережі немає. Polling триває кожні **2500 мс**, зокрема при локальному `00:00`. Не накопичувати перекриті запити; за відповіді старішої версії стан не відкотити.

### 6.2. Приклад React-хука `useRoomTimer.ts`

`clockSkew` обчислюється при отриманні HTTP-відповіді за `t0`/`t1`, а не під час render. Приклад не викликає complete/break і не змінює серверний стан.

```typescript
import { useEffect, useState } from 'react';
import type { RoomStatus } from '../types/api';

interface RoomTimerInput {
  status: RoomStatus;
  pomodoroDuration: number;
  breakDuration: number;
  currentSessionStart: string | null;
  clockSkew: number;
}

export function useRoomTimer({
  status,
  pomodoroDuration,
  breakDuration,
  currentSessionStart,
  clockSkew,
}: RoomTimerInput) {
  const [secondsLeft, setSecondsLeft] = useState(0);

  useEffect(() => {
    const totalSeconds =
      (status === 'BREAK' ? breakDuration : pomodoroDuration) * 60;
    const running = status === 'ACTIVE' || status === 'BREAK';
    const start = currentSessionStart
      ? Date.parse(currentSessionStart)
      : Number.NaN;

    const update = () => {
      if (!running) {
        setSecondsLeft(totalSeconds);
        return;
      }
      if (!Number.isFinite(start)) {
        setSecondsLeft(0);
        return;
      }
      const elapsed = Math.max(
        0,
        Math.floor((Date.now() + clockSkew - start) / 1000),
      );
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
```

При поверненні на вкладку `useRoomSync` додатково негайно запитує актуальний стан. Інтервал 250 мс — лише оновлення відображення, не частота HTTP-запитів. `onAutoCompleted` видалено; до підтвердження сервера UI може показувати `00:00`.

### 6.3. Критерій NFR-03

- Порівнювати показання клієнтів, які вже отримали один і той самий актуальний стан `ACTIVE` або `BREAK`: розбіжність має бути **≤ 2 с** у контрольованих умовах тестування.
- Окремо вимірювати затримку отримання нового стану. Polling 2500 мс не гарантує доставку події запуску за 2 с.
- Перевіряти: різні локальні годинники, мережеву затримку, повторне відкриття/активацію вкладки, повторні відповіді, рестарт backend і відсутність клієнтів під час завершення.

### 6.4. WebSocket · розширена версія, FR-34

| Елемент | Контракт |
|---|---|
| Підключення | `/ws`, STOMP over SockJS |
| Автентифікація | Bearer JWT у STOMP `CONNECT`; власний inbound interceptor виконує ті самі перевірки, що й HTTP |
| Підписка | `/topic/rooms/{roomId}`, лише поточні учасники |
| Подія | `ROOM_UPDATED` із `eventId` і повним `RoomDetails`; `version` — у room payload |
| Команди | Start/stop/join/leave залишаються REST-запитами |
| Reconnect | Повторна автентифікація, підписка та негайний GET стану |
| Fallback | При недоступному WebSocket — polling 2500 мс |

Приклад конверта події (канонічний TypeScript-тип):

```typescript
export interface RoomUpdatedEvent {
  eventId: string; // UUID, ідентифікатор події
  type: 'ROOM_UPDATED';
  room: RoomDetails; // включає serverTime та version
}
```

- Події публікувати після commit зміни, не до нього. Polling і REST залишаються способом відновлення істини після пропуску події.
- Клієнт застосовує лише стан із більшою версією; дублі eventId і старі версії ігнорує. GET тієї самої версії може оновити часову калібровку, не відкотити стан.
- Для WebSocket-події немає HTTP-пари `t0`/`t1`: не перераховувати зсув за midpoint із неї; калібрувати через GET, зокрема після reconnect.
- Контролювати не лише SUBSCRIBE, а й доступ при доставці: після leave, видалення акаунта/кімнати, reset пароля або закінчення JWT припинити відповідну підписку/доставку. Чужих учасників не залишати підписаними на спільний topic.
- При видаленні кімнати сервер припиняє її підписки. Клієнт після втрати підписки чи reconnect перевіряє GET; `404` означає, що кімната недоступна.
- Звичайний logout закриває з'єднання на клієнті; варіант А не відкликає копію чинного JWT на іншому клієнті.

[↑ До змісту](#contents)

---

<a id="section-7"></a>

## 7. TypeScript-контракти frontend/backend

Канонічний файл типів: `frontend/src/types/api.ts`. Назви й nullable-поля нижче узгоджені з API. JWT, хеш пароля та reset-токени не входять до DTO користувача.

```typescript
export type UserRole = 'USER' | 'ADMIN';
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH';
export type RoomStatus = 'WAITING' | 'ACTIVE' | 'BREAK' | 'FINISHED';
export type SessionStatus = 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
export type SessionType = 'INDIVIDUAL' | 'SHARED';
export type GoalPeriod = 'DAILY' | 'WEEKLY';
export type GoalType = 'POMODORO_COUNT' | 'FOCUS_MINUTES';

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
export interface RegisterResponse extends AuthResponse { userId: string }
export interface RegisterRequest {
  name: string;
  username: string;
  email: string;
  password: string;
  passwordConfirm: string;
}
export interface LoginRequest { email: string; password: string }
export interface ForgotPasswordRequest { email: string }
export interface ResetPasswordRequest {
  token: string;
  password: string;
  passwordConfirm: string;
}
export interface ProfileUpdateRequest { name: string; username: string }
export interface MessageResponse { message: string }

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
export interface TaskStatusRequest { status: TaskStatus }

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

export interface RoomCreateRequest {
  name: string;
  pomodoroDuration: number;
  breakDuration: number;
}
export interface RoomJoinRequest { code: string }
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
```

Пагінація: номер сторінки починається з 0; `size` — запитаний розмір, `totalPages = ceil(totalElements / size)`. За відсутності результатів: `content: []`, `totalElements: 0`, `totalPages: 0`. Невалідні page/size — `400`.

Профіль і admin-користувач використовують `completedPomodoros` / `completedSharedSessions`; summary використовує `totalCompletedPomodoros` / `totalSharedSessions`. Це свідомо збережені імена різних DTO, однакові за змістом агрегати.

[↑ До змісту](#contents)

---

<a id="section-8"></a>

## 8. Docker, конфігурація та розгортання

### 8.1. Структура monorepo

```text
e-do/
├── docs/ARCHITECTURE.md
├── backend/
│   ├── Dockerfile
│   └── src/main/resources/
│       ├── application.yml
│       └── db/migration/V1__init_schema.sql
├── frontend/
│   ├── Dockerfile
│   ├── nginx.conf
│   └── src/
├── docker-compose.yml
├── .env.example
└── .gitignore
```

Backend — один Spring Boot-застосунок, не набір мікросервісів. Nginx у frontend-контейнері віддає SPA і пересилає `/api` та `/ws` на backend. В development Vite виконує відповідне proxy. Публічний production-доступ — HTTPS через зовнішній TLS reverse proxy; локальна розробка — HTTP.

### 8.2. Кореневий `docker-compose.yml`

```yaml
services:
  postgres-db:
    image: postgres:16-alpine
    container_name: edo-postgres
    restart: always
    environment:
      POSTGRES_DB: edo_db
      POSTGRES_USER: edo_user
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:?Set POSTGRES_PASSWORD}
    ports:
      - "127.0.0.1:5432:5432"
    volumes:
      - edo_postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U edo_user -d edo_db"]
      interval: 5s
      timeout: 5s
      retries: 5

  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    container_name: edo-backend
    restart: always
    depends_on:
      postgres-db:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-dev}
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-db:5432/edo_db
      SPRING_DATASOURCE_USERNAME: edo_user
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD:?Set POSTGRES_PASSWORD}
      SPRING_JPA_HIBERNATE_DDL_AUTO: validate
      SPRING_FLYWAY_ENABLED: "true"
      JWT_SECRET: ${JWT_SECRET:?Set JWT_SECRET}
      JWT_EXPIRATION_MS: "86400000"
      APP_FRONTEND_PUBLIC_URL: ${FRONTEND_PUBLIC_URL:-http://localhost}
      APP_CORS_ALLOWED_ORIGINS: ${CORS_ALLOWED_ORIGINS:-http://localhost:3000,http://localhost:5173}
      SPRING_MAIL_HOST: ${SMTP_HOST:?Set SMTP_HOST}
      SPRING_MAIL_PORT: ${SMTP_PORT:-587}
      SPRING_MAIL_USERNAME: ${SMTP_USERNAME:-}
      SPRING_MAIL_PASSWORD: ${SMTP_PASSWORD:-}
      SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH: ${SMTP_AUTH:-true}
      SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE: ${SMTP_STARTTLS:-true}
      APP_MAIL_FROM: ${MAIL_FROM:?Set MAIL_FROM}
    ports:
      - "127.0.0.1:8080:8080"

  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    container_name: edo-frontend
    restart: always
    depends_on:
      - backend
    ports:
      - "80:80"

volumes:
  edo_postgres_data:
```

БД має постійний volume (NFR-13). Локальні порти БД і backend прив'язані до loopback; контейнери звертаються один до одного через внутрішню мережу Compose. Production ingress налаштовується зовнішнім reverse proxy.

### 8.3. Середовище та профілі

| Змінна | Призначення |
|---|---|
| `POSTGRES_PASSWORD` | Пароль БД; без значення за замовчуванням |
| `JWT_SECRET` | Секрет підпису достатньої криптографічної довжини; без значення за замовчуванням |
| `JWT_EXPIRATION_MS` | 86400000 мс; властивість backend |
| `FRONTEND_PUBLIC_URL` | Публічний frontend URL для reset-посилань |
| `CORS_ALLOWED_ORIGINS` | Явні дозволені origins для конкретного середовища |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD` | Поштова інтеграція |
| `SMTP_AUTH`, `SMTP_STARTTLS`, `MAIL_FROM` | Параметри SMTP та адреса відправника |

`APP_*` і `JWT_*` — власні налаштування застосунку: їх потрібно явно зв'язати з конфігураційними властивостями Spring. `.env.example` документує імена та приклад формату без робочих секретів. Реальний `.env` — у `.gitignore`. Для `test` поштову інтеграцію замінити контрольованим тестовим відправником.

- `dev`: локальні PostgreSQL/proxy та налаштування розробки.
- `test`: ізольована тестова БД/дані; не використовувати dev/prod БД.
- `prod`: зовнішні секрети, явні origins, HTTPS ingress, без локальних облікових даних у коді.
- Flyway виконується до JPA validation. Для reset email потрібні Spring Mail та SMTP-конфігурація; для PostgreSQL — відповідна підтримка Flyway.

### 8.4. Контракт контейнерів і proxy

- Backend Dockerfile збирає Gradle-проєкт на Java 21 і запускає executable Spring Boot JAR на Java 21, порт 8080.
- Frontend Dockerfile встановлює залежності за lockfile, виконує production build Vite й копіює `dist/` у Nginx, порт 80.
- SPA fallback: невідомий frontend-маршрут має віддавати `index.html`; API-помилки не підміняти HTML.
- `/api/v1/...` пересилати на `http://backend:8080/api/v1/...` без видалення `/api`. `/ws` і SockJS-шляхи — на backend зі збереженням шляху; для WebSocket увімкнути Upgrade proxy.
- Vite proxy спрямовує `/api` та `/ws` на `http://localhost:8080`; production-збірка не повинна містити жорстко заданий localhost backend URL.
- TLS-сертифікат, production DNS/домен і конкретний провайдер розгортання задаються при розгортанні; їх не визначає ця специфікація.

### 8.5. CI

GitHub Actions перевіряє backend-тести й збірку, frontend TypeScript-перевірку та збірку. Автоматичного розгортання в цій редакції немає. Unit-тести сервісного шару — JUnit/Mockito (NFR-17); перевірки API/HTTP-кодів та інтерфейсу — за NFR із беклогу.

[↑ До змісту](#contents)

---

<a id="section-9"></a>

## 9. Інтеграція frontend/backend

1. **Авторизація.** Login/register повертають `token`; frontend записує `localStorage.setItem('accessToken', token)` і додає `Authorization: Bearer ...` через Axios Interceptor. За `401` очищує авторизацію. Logout очищує токен і підключення; серверного відкликання звичайного JWT немає.
2. **Клієнт API.** `baseURL = '/api/v1'`, без cookie credentials. Proxy у Vite/Nginx зберігає повний `/api`-шлях.
3. **Завдання.** Фільтри `status` / `priority`; порожній результат — `200` та `[]`. DTO використовують camelCase; дедлайн — ISO-8601.
4. **Кімната.** GET одразу при вході на екран, далі кожні 2500 мс без перекриття запитів; таймер рахується локально від абсолютного часу. Після повернення на вкладку — негайний GET. Старі версії стану не застосовувати.
5. **Перехід фаз.** Backend сам завершує фокус, починає перерву й переводить її у WAITING. Frontend не викликає `/break`, не нараховує статистику й не фіксує shared-complete.
6. **Повторні команди.** Обробляти `409` як конфлікт поточного стану та оновлювати дані. Ідемпотентність індивідуальних complete/cancel — за розділом 5.7.
7. **Цілі та списки.** Цілі в профілі — масиви; history/admin — `PageResponse<T>`. Смуга прогресу максимум 100%, числовий API-прогрес може бути вищим.
8. **Видалення.** Усі DELETE та успішні logout/reset-password — `204`, без JSON. Після leave чи видалення кімнати припинити локальну підписку й polling цього ресурсу.
9. **Reset password.** Forgot повертає однаковий message для невідомого й відомого email; reset успішний один раз, після нього потрібен новий login.
10. **WebSocket.** Лише розширена версія; REST-команди збережено. При reconnect — GET, за недоступності — polling. Втрату права участі не обходити збереженою підпискою.

[↑ До змісту](#contents)

---

<a id="section-10"></a>

## 10. План розробки: Розподіл беклогу на спринти (Sprint Backlog & Work Breakdown Structure)

Базовий беклог PDF містить **73 задачі: 8 ARCH, 46 FR, 19 NFR**, розподілені на 4 спринти по 2 тижні. Нижче збережено всі їхні ідентифікатори та індивідуальні SP; ARCH-06 перенесено зі спринту 3 до спринту 1. Поточний статус виконання ведеться у GitHub Issues/Projects, не в цьому документі.

**Увага до оцінок:** виправлена сума оригінальних задач — **231 SP**. До перенесення ARCH-06 спринт 1 мав 66 SP; після перенесення — 69 SP, спринт 3 — 46 SP. Погоджені доповнення нижче ще не оцінено; 231 SP не є оцінкою всього розширеного обсягу. План на 8 тижнів залишається базовим і потребує перевірки місткості команди після оцінювання доповнень.

### Спринт 1: Фундамент, Авторизація та Базовий Task Manager (MVP - Частина 1)

**Тривалість:** 2 тижні

**Ціль спринту:** Розгорнути та налаштувати багаторівневу архітектуру, підняти реляційну
базу даних PostgreSQL, реалізувати реєстрацію/вхід через JWT-токени, створити API клієнт і забезпечити базовий життєвий цикл особистих завдань (CRUD).

#### Задачі Марії (Backend Developer):

1. **`ARCH-01`** — Ініціалізація Backend (Spring Boot, PostgreSQL) та архітектурний поділ за Component Diagram **[5 SP]**
2. **`ARCH-02`** — Реалізація доменної моделі БД (Entities та Enumerations) за Class Diagram **[5 SP]**
3. **`ARCH-03`** — Розробка Repository Layer (Spring Data JPA / Hibernate) **[3 SP]**
4. **`ARCH-04`** — Розробка модуля Spring Security та Auth Service **[5 SP]**
5. **`FR-02`** — Реєстрація нового облікового запису **[5 SP]**
6. **`FR-03`** — Авторизація користувача за email та паролем **[5 SP]**
7. **`FR-09`** — Створення особистого завдання **[3 SP]**
8. **`NFR-04`** — Криптографічне хешування паролів у базі даних (BCrypt) **[3 SP]**
9. **`NFR-05`** — Обов'язкова перевірка авторизації для захищених REST API **[3 SP]**
10. **`NFR-06`** — Ізоляція персональних даних та завдань між користувачами **[3 SP]**
11. **`ARCH-06`** — Розробка централізованого модуля Exception Handler (@ControllerAdvice) **[3 SP]**

**Разом Backend:** 11 задач | 43 SP

#### Задачі Назара (Frontend Developer / QA / DevOps):

1. **`ARCH-07`** — Ініціалізація Frontend (React), налаштування API Client та CI/CD пайплайну **[5 SP]**
2. **`ARCH-08`** — Проєктування маршрутизації та каркасів UI-компонентів React за Activity та Sequence діаграмами **[5 SP]**
3. **`FR-01`** — Перегляд стартової сторінки гостем **[2 SP]**
4. **`FR-05`** — Доступ авторизованого користувача до публічних сторінок та перенаправлення з форм входу **[2 SP]**
5. **`FR-06`** — Вихід із системи (завершення авторизованої сесії) **[2 SP]**
6. **`FR-10`** — Перегляд списку всіх власних завдань **[3 SP]**
7. **`FR-14`** — Зміна статусу завдання (TODO, IN_PROGRESS, DONE) **[2 SP]**
8. **`NFR-12`** — Інформативні текстові повідомлення про помилки вводу в UI **[3 SP]**
9. **`NFR-18`** — Стандартизація формату обміну REST API (JSON) **[2 SP]**

**Разом Frontend:** 9 задач | 26 SP

### Спринт 2: Індивідуальний Pomodoro та Завершення MVP (MVP - Частина 2)

**Тривалість:** 2 тижні

**Ціль спринту:** Доопрацювати повне керування завданнями (фільтрація, детальний
перегляд, редагування, видалення) та реалізувати повноцінний автономний модуль індивідуального Pomodoro-таймера з налаштуванням тривалості, перервами, автоматичним і достроковим завершенням.

#### Задачі Марії (Backend Developer):

1. **`ARCH-05`** — Розробка сервісного шару та REST Controllers (Task, Pomodoro, User) **[8 SP]**
2. **`FR-08`** — Редагування профілю користувача (ім'я та username) **[3 SP]**
3. **`FR-12`** — Редагування власного завдання **[3 SP]**
4. **`FR-13`** — Видалення власного завдання **[2 SP]**
5. **`FR-17`** — Запуск індивідуального Pomodoro-таймера **[3 SP]**
6. **`FR-19`** — Автоматичне завершення індивідуальної фокус-сесії **[3 SP]**
7. **`FR-20`** — Дострокове завершення індивідуальної Pomodoro-сесії **[2 SP]**
8. **`NFR-13`** — Перевірка збереження даних у PostgreSQL після перезапуску backend **[2 SP]**
9. **`NFR-14`** — Стійкість сервера до некоректних користувацьких запитів **[3 SP]**

**Разом Backend:** 9 задач | 29 SP

#### Задачі Назара (Frontend Developer / QA / DevOps):

1. **`FR-07`** — Перегляд профілю користувача **[3 SP]**
2. **`FR-11`** — Перегляд повної інформації про вибране завдання **[2 SP]**
3. **`FR-15`** — Фільтрація власних завдань за статусом та пріоритетом **[3 SP]**

4. **`FR-16`** — Налаштування тривалості індивідуального Pomodoro та перерви **[2 SP]**
5. **`FR-18`** — Відображення таймера активної Pomodoro-сесії **[3 SP]**
6. **`FR-21`** — Запуск таймера перерви після завершення фокус-сесії **[2 SP]**
7. **`NFR-02`** — Оптимізація часу завантаження основного інтерфейсу (<= 3 сек) **[3 SP]**
8. **`NFR-09`** — Адаптивна верстка інтерфейсу для ширини viewport від 360 px **[5 SP]**
9. **`NFR-10`** — Усунення горизонтального прокручування на основних сторінках **[2 SP]**

**Разом Frontend:** 9 задач | 25 SP

### Спринт 3: Спільні Pomodoro-кімнати та Синхронізація

**Тривалість:** 2 тижні

**Ціль спринту:** Реалізувати створення приватних кімнат, генерацію 6-символьних унікальних
кодів доступу, підключення учасників, спільний запуск/зупинку фокус-сесій та синхронізацію таймера між різними клієнтами з розбіжністю <= 2 секунд.

#### Задачі Марії (Backend Developer):

1. **`FR-22`** — Створення приватної спільної Pomodoro-кімнати **[5 SP]**
2. **`FR-23`** — Автоматична генерація унікального коду кімнати **[2 SP]**
3. **`FR-25`** — Забезпечення приватності спільних кімнат **[2 SP]**
4. **`FR-28`** — Запуск спільної Pomodoro-сесії **[5 SP]**
5. **`FR-30`** — Автоматичне завершення спільної Pomodoro-сесії **[3 SP]**
6. **`FR-31`** — Дострокове завершення спільної сесії будь-яким учасником **[3 SP]**
7. **`FR-32`** — Вихід учасника з кімнати під час активної сесії **[3 SP]**

**Разом Backend:** 7 задач | 23 SP

#### Задачі Назара (Frontend Developer / QA / DevOps):

1. **`FR-24`** — Приєднання до спільної кімнати за кодом **[3 SP]**
2. **`FR-26`** — Перегляд списку поточних учасників кімнати **[2 SP]**
3. **`FR-27`** — Перегляд поточного стану спільної кімнати (WAITING, ACTIVE, BREAK, FINISHED) **[2 SP]**
4. **`FR-29`** — Синхронізація таймера спільної сесії між учасниками **[5 SP]**
5. **`FR-33`** — Проведення спільної перерви в кімнаті **[3 SP]**
6. **`NFR-03`** — Верифікація точності синхронізації спільного Pomodoro (різниця <= 2 с) **[3 SP]**
7. **`NFR-08`** — Захист приватних кімнат від неавторизованого доступу та підбору без коду **[3 SP]**
8. **`NFR-11`** — Організація навігації до основних модулів не більше ніж за 2 переходи **[2 SP]**

**Разом Frontend:** 8 задач | 23 SP

### Спринт 4: Статистика, Цілі, Адмінка, WebSocket та Тестування (Medium / Low)

**Тривалість:** 2 тижні

**Ціль спринту:** Реалізувати накопичення статистики та обчислення фокус-часу,
встановлення цілей продуктивності, адміністративні інструменти аудиту/модерації, розширену синхронізацію через WebSocket та фінальне тестування надійності системи.

#### Задачі Марії (Backend Developer):

1. **`FR-36`** — Обчислення кількості завершених Pomodoro-сесій **[2 SP]**
2. **`FR-37`** — Обчислення загального часу фокусування користувача **[2 SP]**
3. **`FR-38`** — Підрахунок кількості завершених спільних сесій **[2 SP]**
4. **`FR-39`** — Встановлення денної цілі продуктивності **[3 SP]**
5. **`FR-40`** — Встановлення тижневої цілі продуктивності **[3 SP]**
6. **`FR-42`** — Забезпечення доступу адміністратора до всіх користувацьких функцій **[2 SP]**
7. **`FR-44`** — Видалення облікового запису користувача адміністратором **[3 SP]**
8. **`FR-46`** — Видалення спільної кімнати адміністратором **[2 SP]**
9. **`NFR-07`** — Обмеження доступу до адміністративних API лише для ролі ADMIN **[2 SP]**
10. **`NFR-16`** — Перевірка дотримання багаторівневої архітектури Backend **[2 SP]**
11. **`NFR-17`** — Unit-тестування бізнес-логіки сервісного шару (JUnit, Mockito) **[5 SP]**

**Разом Backend:** 11 задач | 28 SP

#### Задачі Назара (Frontend Developer / QA / DevOps):

1. **`FR-04`** — Відновлення забутого пароля **[5 SP]**
2. **`FR-34`** — Синхронізація подій кімнати в реальному часі через WebSocket (Розширена версія) **[8 SP]**
3. **`FR-35`** — Перегляд історії індивідуальних та спільних Pomodoro-сесій **[3 SP]**
4. **`FR-41`** — Перегляд прогресу виконання денних та тижневих цілей **[3 SP]**
5. **`FR-43`** — Перегляд адміністратором списку зареєстрованих користувачів **[3 SP]**
6. **`FR-45`** — Перегляд адміністратором переліку створених кімнат **[3 SP]**
7. **`NFR-01`** — Оптимізація та перевірка часу відповіді REST API (<= 2 секунди) **[3 SP]**
8. **`NFR-15`** — Кросбраузерне тестування (Chrome 120+, Firefox 120+, Safari 17+, Edge 120+) **[3 SP]**
9. **`NFR-19`** — Підтримка та тестування стандартних HTTP-кодів у Postman **[3 SP]**

**Разом Frontend:** 9 задач | 34 SP

### Зведена таблиця розподілу навантаження та Story Points

| Спринт | Тривалість | Напрям фокусу | Задачі Backend | SP Backend | Задачі Frontend | SP Frontend | Загальний SP |
|---|---|---|---:|---:|---:|---:|---:|
| 1 | 2 тижні | Архітектура, Auth, Task CRUD, Error handling | 11 | 43 | 9 | 26 | 69 |
| 2 | 2 тижні | Task Filters, Solo Pomodoro, UX | 9 | 29 | 9 | 25 | 54 |
| 3 | 2 тижні | Shared Rooms, Time Sync | 7 | 23 | 8 | 23 | 46 |
| 4 | 2 тижні | Stats, Goals, Admin, WebSocket, Testing | 11 | 28 | 9 | 34 | 62 |
| Базовий беклог | 8 тижнів плану | 73 задачі PDF із виправленими SP та перенесеним ARCH-06 | 38 | 123 | 35 | 108 | 231 |

### Погоджені доповнення до беклогу

`ADD-*` — технічні задачі цієї редакції, а не нові FR/NFR із лабораторних. SP **не призначено**: команда має оцінити додаткову роботу, уникаючи подвійного обліку з базовими задачами.

| ID | Робота | Відповідальний | Спринт / зв'язок |
|---|---|---|---|
| ADD-01 | Flyway, оновлений DDL, JPA auditing | Марія | 1; ARCH-02/03 |
| ADD-02 | Перевірка існування акаунта, credentialsVersion, одна активна сесія | Марія | 1–2; ARCH-04, NFR-05, FR-17 |
| ADD-03 | Scheduler, рестарт і транзакційні переходи фокусу/перерви | Марія | 2–3; FR-19/30/33 |
| ADD-04 | Повний backend reset-password, reset-токени, SMTP, відкликання старих JWT | Марія | 4; FR-04 |
| ADD-05 | Пагінована історія, admin DTO, масиви цілей і UTC-агрегації | Марія + Назар | 4; FR-35–45 |
| ADD-06 | Dockerfile/proxy, env-профілі, секрети й CI без deploy | Назар; конфігурація backend — Марія | 1, завершення 4; ARCH-07 |
| ADD-07 | Перероблений хук і перевірка midpoint-калібрування | Назар | 3; FR-29, NFR-03 |
| ADD-08 | Автентифікація/авторизація STOMP, події, version, reconnect/fallback | Марія + Назар | 4; FR-34 |
| ADD-09 | Перевірки гонок, повторних команд, видалення та reset без витоку даних | Марія + Назар | 2–4; NFR-06/08/14/17/19 |

FR-04 у списку Назара — UI відновлення пароля; backend і пошта явно віднесені до ADD-04. ARCH-06 має бути готовим уже у спринті 1. Посилання PDF на Component/Class/Activity/Sequence diagrams та Лаб №1/№2 збережено як походження задач; самих цих матеріалів у доступному PDF немає, їх не відновлено за припущеннями.

[↑ До змісту](#contents)

---

<a id="section-11"></a>

## 11. Погоджені рішення та відмінності від PDF

**Статус усіх записів нижче: погоджено користувачем 30 жовтня 2026 року.** Для U-01 обрано варіант А. Цей реєстр замінює попередній список відкритих питань; рішення вже внесено до нормативних розділів документа.

### 11.1. Узгоджені розбіжності C-01–C-08

#### C-01 · name у користувача

*Джерело: DDL `users`, с. 4; API, с. 7–10.*

**Погоджено:** Додано `users.name VARCHAR(100) NOT NULL`; реєстрація/профіль і учасники використовують це поле.

**Чинний контракт:** розділ 3, 5.1, 7.

#### C-02 · Єдина довжина username

*Джерело: DDL, с. 4; реєстрація, с. 7.*

**Погоджено:** БД та API: 3–50 символів, `^[a-zA-Z0-9_]+$`; однакова перевірка register/update.

**Чинний контракт:** розділ 3, 5.1.

#### C-03 · Ролі без префікса у зовнішніх контрактах

*Джерело: JWT, с. 3; DDL та API, с. 4, 7–9.*

**Погоджено:** У БД, JWT та DTO — USER/ADMIN; authorities Spring Security — ROLE_USER/ROLE_ADMIN.

**Чинний контракт:** розділ 2.1, 7.

#### C-04 · Окремі UserSummary та UserProfile

*Джерело: `UserProfile`, с. 19; `GET /users/me`, с. 9.*

**Погоджено:** Auth повертає UserSummary, профіль — статистику і масиви цілей.

**Чинний контракт:** розділ 5.1, 7.

#### C-05 · Єдиний RoomDetails

*Джерело: `RoomDetails`, с. 20; створення кімнати, с. 13.*

**Погоджено:** Create/join/GET/start/stop повертають RoomDetails із serverTime та version.

**Чинний контракт:** розділ 5.4, 7.

#### C-06 · Виправлена формула хука

*Джерело: Формула, с. 17; хук, с. 18.*

**Погоджено:** Elapsed обмежено знизу нулем; callback завершення прибрано, сервер керує завершенням.

**Чинний контракт:** розділ 6.1–6.2.

#### C-07 · Шість символів у коді кімнати

*Джерело: Коди кімнат, с. 13–14, 25.*

**Погоджено:** Великі літери й цифри без 0/O/1/I; нормалізація регістру; UNIQUE і повтор генерації при колізії.

**Чинний контракт:** розділ 3, 5.4.

#### C-08 · Виправлені підсумки SP

*Джерело: Спринт 1, с. 23; зведення, с. 26–27.*

**Погоджено:** Оригінальні задачі: backend 123 SP, frontend 108 SP, разом 231 SP. Перший спринт до перенесення ARCH-06 — 66 SP; після — 69 SP. Додаткова робота не входить до цієї суми.

**Арифметичне уточнення під час фінальної звірки:** у попередній пропозиції було 230 SP. Повна сума рядків також виявила 34 SP frontend у спринті 4 замість 33 у PDF: frontend загалом — 108 SP, спринт 4 — 62 SP, увесь базовий беклог — 231 SP. Індивідуальні SP задач не змінено; виправлено лише підсумки.

**Чинний контракт:** розділ 10.

### 11.2. Узгоджені доповнення U-01–U-16

#### U-01 · Logout — варіант А

*Джерело: Stateless JWT та logout, с. 2–3, 9.*

**Погоджено:** JWT 24 години, без refresh/denylist; logout очищує клієнт. Backend перевіряє існування акаунта; reset пароля анулює старі JWT через credentialsVersion.

**Чинний контракт:** розділ 2.1.

#### U-02 · Повний reset-password

*Джерело: Forgot password, с. 8–9; FR-04, с. 26.*

**Погоджено:** Forgot завжди 200 для валідного email; одноразовий токен на 30 хв, у БД лише хеш; новий reset endpoint, SMTP, інвалідація старих JWT після успіху.

**Чинний контракт:** розділ 2.5, 3, 5.1.

#### U-03 · Матриця доступу

*Джерело: Захищені API, с. 3, 12–17.*

**Погоджено:** Особисті дані — власнику; кімната — учасникам; admin-перегляд/видалення — окремим контуром, без автоматичного керування чужим таймером.

**Чинний контракт:** розділ 2.4.

#### U-04 · Одна активна сесія та повторні команди

*Джерело: Pomodoro complete/cancel, с. 12–13.*

**Погоджено:** Один IN_PROGRESS на користувача; complete за строком, cancel — 0; повтор однакової термінальної дії повертає той самий результат, протилежна — 409.

**Чинний контракт:** розділ 3, 5.7.

#### U-05 · Завершення на backend

*Джерело: FR-19, FR-30, с. 24–25; API та хук, с. 12, 15, 18–19.*

**Погоджено:** Scheduler раз на секунду, перевірки перед командами, відновлення прострочених переходів після рестарту; спільні сесії завершуються атомарно.

**Чинний контракт:** розділ 5.7.

#### U-06 · Автоматична спільна перерва

*Джерело: Життєвий цикл кімнати, с. 13–15.*

**Погоджено:** WAITING/FINISHED → ACTIVE → BREAK → WAITING; stop із ACTIVE → FINISHED. Join лише у WAITING/FINISHED; ручний /break вилучено, новий фокус запускається вручну.

**Чинний контракт:** розділ 5.7.

#### U-07 · Вихід і порожня кімната

*Джерело: Leave, с. 15.*

**Погоджено:** Leave скасовує лише власну активну сесію; передача автора за joined_at, потім UUID; порожня кімната видаляється, історія залишається.

**Чинний контракт:** розділ 5.4, 5.7.

#### U-08 · Контрольоване адміністративне видалення

*Джерело: DDL cascade, с. 5–6; admin delete, с. 16–17.*

**Погоджено:** При видаленні користувача — спочатку leave/передача авторства; при видаленні кімнати — скасування ще активних сесій та збереження історії. creator FK — RESTRICT.

**Чинний контракт:** розділ 3, 5.7.

#### U-09 · UTC, чотири цілі та історія

*Джерело: Статистика та цілі, с. 9, 15–16, 21.*

**Погоджено:** Періоди UTC, тиждень із понеділка, облік за endTime; лише COMPLETED у статистиці; до 4 цілей, масиви у профілі; recent 10, повна history з пагінацією.

**Чинний контракт:** розділ 5.8–5.9, 7.

#### U-10 · Admin DTO та пагінація

*Джерело: Admin API, с. 16–17.*

**Погоджено:** AdminUserItem і AdminRoomItem, PageResponse, default size 20/max 100, без секретів.

**Чинний контракт:** розділ 5.6, 7.

#### U-11 · Єдина адреса API та CORS

*Джерело: CORS, с. 3; Compose, с. 21–22.*

**Погоджено:** Frontend /api/v1; Vite/Nginx proxy, явні origins, allowCredentials false; шаблони лише через allowedOriginPatterns за потреби.

**Чинний контракт:** розділ 2.3, 8.

#### U-12 · Контейнери, секрети та CI

*Джерело: HTTPS та Docker, с. 1, 21–22.*

**Погоджено:** Три сервіси; Nginx SPA/proxy, зовнішній TLS ingress; env-секрети, dev/test/prod, GitHub Actions без автоматичного deploy.

**Чинний контракт:** розділ 8.

#### U-13 · Flyway й обмеження БД

*Джерело: DDL та `ddl-auto: validate`, с. 4–6, 22.*

**Погоджено:** Flyway + validate, JPA auditing; тривалості 1–120/1–60; одна IN_PROGRESS-сесія, історичний SHARED допускає NULL room_id.

**Чинний контракт:** розділ 3.1.

#### U-14 · Калібрування та межі NFR-03

*Джерело: Синхронізація, с. 17–19, 23.*

**Погоджено:** Midpoint HTTP-калібрування, негайний перерахунок, оновлення при поверненні на вкладку; точність показань відокремлена від затримки доставки нового стану.

**Чинний контракт:** розділ 6.1–6.3.

#### U-15 · WebSocket як розширення

*Джерело: WebSocket, с. 1, 26.*

**Погоджено:** /ws, STOMP CONNECT JWT interceptor, membership-захист /topic/rooms/{roomId}, ROOM_UPDATED з eventId і version, REST-команди, reconnect GET, polling fallback.

**Чинний контракт:** розділ 6.4.

#### U-16 · Оновлений план і простежуваність

*Джерело: Беклог, с. 23–27.*

**Погоджено:** ARCH-06 у спринті 1; статуси виконання — у GitHub; відсутні діаграми не відновлено; ADD-задачі виділено для оцінювання командою.

**Чинний контракт:** розділ 10.

### 11.3. Межі погодження та технічне оформлення

- Ця редакція явно змінює PDF: name/username/ролі, DTO цілей та кімнат, forgot/reset password, lifecycle перерви, admin/history pagination, DDL, CORS, формулу таймера та розподіл ARCH-06. Старі приклади не мають пріоритету над цими контрактами.
- Технічні поля `credentials_version`, JWT `credentialsVersion`, `password_reset_tokens` та `state_version` конкретизують погоджені вимоги reset, інвалідації JWT та versioned-подій. Це частина цієї редакції, а не твердження про їхню наявність у вихідному PDF.
- Flyway та Spring Mail внесено для погоджених функцій. DevTools/Actuator не стали обов'язковими залежностями; версії базового стека не оновлювалися.
- Домен, провайдер хостингу/SMTP, робочі секрети та оцінки ADD-задач визначаються при налаштуванні середовища й плануванні. Тут немає вигаданих credentials або гарантованого строку завершення розширеного обсягу.
- Відсутні зовнішні діаграми та матеріали лабораторних не відновлювалися. Реалізація застосунку, застосовані міграції й наявні дані не перевірялися.

[↑ До змісту](#contents)
