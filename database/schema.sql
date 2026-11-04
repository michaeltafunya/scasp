-- SCASP schema (SQLite dialect; standard SQL elsewhere)
CREATE TABLE IF NOT EXISTS students (
  student_id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  programme TEXT
);
CREATE TABLE IF NOT EXISTS courses (
  code TEXT PRIMARY KEY,
  title TEXT NOT NULL,
  capacity INTEGER NOT NULL CHECK (capacity > 0)
);
CREATE TABLE IF NOT EXISTS registrations (
  student_id TEXT NOT NULL REFERENCES students(student_id),
  course_code TEXT NOT NULL REFERENCES courses(code),
  created_at TEXT NOT NULL,
  PRIMARY KEY (student_id, course_code)
);
CREATE TABLE IF NOT EXISTS waitlist (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  student_id TEXT NOT NULL REFERENCES students(student_id),
  course_code TEXT NOT NULL REFERENCES courses(code),
  UNIQUE (student_id, course_code)
);
CREATE TABLE IF NOT EXISTS service_requests (
  request_id INTEGER PRIMARY KEY AUTOINCREMENT,
  student_id TEXT NOT NULL REFERENCES students(student_id),
  category TEXT NOT NULL,
  priority INTEGER NOT NULL,
  status TEXT NOT NULL DEFAULT 'OPEN',
  assigned_to TEXT
);
CREATE TABLE IF NOT EXISTS audit_events (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  actor TEXT NOT NULL,
  action TEXT NOT NULL,
  entity_type TEXT NOT NULL,
  entity_id TEXT NOT NULL,
  occurred_at TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_reg_course ON registrations(course_code);
CREATE INDEX IF NOT EXISTS idx_wait_course ON waitlist(course_code)
