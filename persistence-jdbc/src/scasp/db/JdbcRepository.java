package scasp.db;

import java.sql.*;
import java.time.Instant;
import java.util.*;

/** JDBC persistence. Every user value goes through a PreparedStatement parameter (no string concatenation). */
public class JdbcRepository {
    private final Db db;
    public JdbcRepository(Db db) { this.db = db; }

    // ---- CRUD ----
    public void addStudent(String id, String name, String programme) throws SQLException {
        try (Connection c = db.connect()) { exec(c, "INSERT INTO students VALUES (?,?,?)", id, name, programme); }
    }
    public void addCourse(String code, String title, int capacity) throws SQLException {
        try (Connection c = db.connect()) { exec(c, "INSERT INTO courses VALUES (?,?,?)", code, title, capacity); }
    }
    public Optional<String> courseTitle(String code) throws SQLException {
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement("SELECT title FROM courses WHERE code=?")) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(rs.getString(1)) : Optional.empty(); }
        }
    }
    public void updateCapacity(String code, int capacity) throws SQLException {
        try (Connection c = db.connect()) { exec(c, "UPDATE courses SET capacity=? WHERE code=?", capacity, code); }
    }
    public void deleteCourse(String code) throws SQLException {
        try (Connection c = db.connect()) { exec(c, "DELETE FROM courses WHERE code=?", code); }
    }

    // ---- Transactions: registration + waiting list + audit are one atomic unit ----
    public String register(String studentId, String code) throws SQLException {
        try (Connection c = db.connect()) {
            c.setAutoCommit(false);
            try {
                Integer cap = one(c, "SELECT capacity FROM courses WHERE code=?", code);
                if (cap == null) throw new IllegalArgumentException("Unknown course " + code);
                int already = one(c, "SELECT (SELECT COUNT(*) FROM registrations WHERE student_id=? AND course_code=?)"
                        + " + (SELECT COUNT(*) FROM waitlist WHERE student_id=? AND course_code=?)", studentId, code, studentId, code);
                if (already > 0) throw new IllegalStateException("Already registered or waitlisted");
                int taken = one(c, "SELECT COUNT(*) FROM registrations WHERE course_code=?", code);
                String result;
                if (taken < cap) {
                    exec(c, "INSERT INTO registrations VALUES (?,?,?)", studentId, code, Instant.now().toString());
                    result = "REGISTERED";
                } else {
                    exec(c, "INSERT INTO waitlist(student_id,course_code) VALUES (?,?)", studentId, code);
                    result = "WAITLISTED";
                }
                audit(c, studentId, result, code);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public void withdraw(String studentId, String code) throws SQLException {
        try (Connection c = db.connect()) {
            c.setAutoCommit(false);
            try {
                if (exec(c, "DELETE FROM registrations WHERE student_id=? AND course_code=?", studentId, code) > 0) {
                    audit(c, studentId, "WITHDRAW", code);
                    try (PreparedStatement ps = c.prepareStatement("SELECT id, student_id FROM waitlist WHERE course_code=? ORDER BY id LIMIT 1")) {
                        ps.setString(1, code);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) { // promote first in line
                                long wid = rs.getLong(1);
                                String next = rs.getString(2);
                                exec(c, "DELETE FROM waitlist WHERE id=?", wid);
                                exec(c, "INSERT INTO registrations VALUES (?,?,?)", next, code, Instant.now().toString());
                                audit(c, next, "PROMOTE", code);
                            }
                        }
                    }
                } else if (exec(c, "DELETE FROM waitlist WHERE student_id=? AND course_code=?", studentId, code) > 0) {
                    audit(c, studentId, "LEAVE_WAITLIST", code);
                } else {
                    throw new IllegalStateException("Not registered or waitlisted");
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    // ---- Reports (joins) ----
    public List<String> registrationReport() throws SQLException {
        return rows("SELECT c.code, c.title, COUNT(r.student_id) FROM courses c LEFT JOIN registrations r ON r.course_code=c.code "
                + "GROUP BY c.code, c.title ORDER BY c.code");
    }
    public List<String> waitlistReport() throws SQLException {
        return rows("SELECT c.code, s.name FROM waitlist w JOIN students s ON s.student_id=w.student_id "
                + "JOIN courses c ON c.code=w.course_code ORDER BY w.id");
    }
    public List<String> studentReport() throws SQLException {
        return rows("SELECT s.name, c.title FROM registrations r JOIN students s ON s.student_id=r.student_id "
                + "JOIN courses c ON c.code=r.course_code ORDER BY s.name, c.title");
    }
    public List<String> auditReport() throws SQLException {
        return rows("SELECT actor, action, entity_id FROM audit_events ORDER BY id");
    }

    // ---- helpers ----
    private List<String> rows(String sql) throws SQLException {
        List<String> out = new ArrayList<>();
        try (Connection c = db.connect(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            int n = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                StringJoiner j = new StringJoiner(" | ");
                for (int i = 1; i <= n; i++) j.add(String.valueOf(rs.getObject(i)));
                out.add(j.toString());
            }
        }
        return out;
    }
    private static void audit(Connection c, String actor, String action, String code) throws SQLException {
        exec(c, "INSERT INTO audit_events(actor,action,entity_type,entity_id,occurred_at) VALUES (?,?,?,?,?)",
                actor, action, "Course", code, Instant.now().toString());
    }
    private static int exec(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
            return ps.executeUpdate();
        }
    }
    private static Integer one(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : null; }
        }
    }
}
