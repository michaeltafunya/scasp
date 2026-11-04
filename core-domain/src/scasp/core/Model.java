package scasp.core;

import java.time.Instant;

/** Domain records (immutable). Equality/hashCode come from the record components. */
public final class Model {
    private Model() {}
    public record Student(String id, String name, String programme) {}
    public record Course(String code, String title, int capacity) {}
    public record Registration(String studentId, String courseCode) {}
    public record ServiceRequest(int id, String studentId, String category, int priority, String status)
            implements Comparable<ServiceRequest> {
        @Override public int compareTo(ServiceRequest o) { // higher priority first, then oldest
            int c = Integer.compare(o.priority, priority);
            return c != 0 ? c : Integer.compare(id, o.id);
        }
    }
    public record Announcement(String title, String body, String audience) {}
    public record AuditEvent(String actor, String action, String entity, Instant at) {}
    public record CampusPlace(String name, double lat, double lon) {}
}
