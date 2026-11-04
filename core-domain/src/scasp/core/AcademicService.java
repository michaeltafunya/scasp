package scasp.core;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import scasp.core.Model.*;

/**
 * In-memory core. All public methods are synchronized so the registration invariants
 * (unique registration, capacity) hold under concurrent use (Stage 2).
 * Invariants: a student is registered OR waitlisted for a course, never both; registered <= capacity.
 */
public class AcademicService {
    private final Map<String, Student> students = new HashMap<>();
    private final Map<String, Course> courses = new TreeMap<>();
    private final Set<Registration> regs = new LinkedHashSet<>();
    private final Map<String, Deque<String>> waitlists = new HashMap<>();
    private final PriorityQueue<ServiceRequest> requests = new PriorityQueue<>();
    private final List<Announcement> announcements = new ArrayList<>();
    private final List<AuditEvent> audit = new ArrayList<>();
    private int nextRequestId = 1;

    public synchronized void addStudent(Student s) { students.put(s.id(), s); }

    public synchronized void addCourse(Course c) {
        if (c.capacity() <= 0) throw new IllegalArgumentException("Capacity must be positive");
        courses.put(c.code(), c);
    }

    public synchronized String register(String studentId, String code) {
        if (!students.containsKey(studentId)) throw new IllegalArgumentException("Unknown student " + studentId);
        Course c = courses.get(code);
        if (c == null) throw new IllegalArgumentException("Unknown course " + code);
        Registration r = new Registration(studentId, code);
        Deque<String> w = waitlists.computeIfAbsent(code, k -> new ArrayDeque<>());
        if (regs.contains(r) || w.contains(studentId)) throw new IllegalStateException("Already registered or waitlisted");
        if (count(code) < c.capacity()) {
            regs.add(r);
            log(studentId, "REGISTER", code);
            return "REGISTERED";
        }
        w.addLast(studentId);
        log(studentId, "WAITLIST", code);
        return "WAITLISTED:" + w.size();
    }

    public synchronized void withdraw(String studentId, String code) {
        Deque<String> w = waitlists.computeIfAbsent(code, k -> new ArrayDeque<>());
        if (regs.remove(new Registration(studentId, code))) {
            log(studentId, "WITHDRAW", code);
            String next = w.pollFirst(); // promote next student in line
            if (next != null) {
                regs.add(new Registration(next, code));
                log(next, "PROMOTE", code);
            }
        } else if (w.remove(studentId)) {
            log(studentId, "LEAVE_WAITLIST", code);
        } else {
            throw new IllegalStateException("Not registered or waitlisted");
        }
    }

    public synchronized int count(String code) {
        return (int) regs.stream().filter(r -> r.courseCode().equals(code)).count();
    }

    public synchronized List<String> waitlist(String code) {
        return List.copyOf(waitlists.getOrDefault(code, new ArrayDeque<>()));
    }

    /** Defensive copy, sorted by the given comparator. */
    public synchronized List<Course> courses(Comparator<Course> order) {
        return courses.values().stream().sorted(order).toList();
    }
    public List<Course> courses() { return courses(Comparator.comparing(Course::code)); }

    public synchronized List<Course> search(String text) {
        String t = text.toLowerCase();
        return courses.values().stream()
                .filter(c -> c.code().toLowerCase().contains(t) || c.title().toLowerCase().contains(t))
                .sorted(Comparator.comparing(Course::title)).toList();
    }

    public synchronized int submitRequest(String studentId, String category, int priority) {
        if (!students.containsKey(studentId)) throw new IllegalArgumentException("Unknown student " + studentId);
        int id = nextRequestId++;
        requests.add(new ServiceRequest(id, studentId, category, priority, "OPEN"));
        log(studentId, "REQUEST", String.valueOf(id));
        return id;
    }

    /** Takes the highest-priority open request and marks it ASSIGNED (OPEN -> ASSIGNED). */
    public synchronized Optional<ServiceRequest> assignNext(String staff) {
        ServiceRequest r = requests.poll();
        if (r == null) return Optional.empty();
        log(staff, "ASSIGN", String.valueOf(r.id()));
        return Optional.of(new ServiceRequest(r.id(), r.studentId(), r.category(), r.priority(), "ASSIGNED"));
    }

    public synchronized void publish(Announcement a) { announcements.add(a); log("staff", "ANNOUNCE", a.title()); }
    public synchronized List<Announcement> announcements(String audience) {
        return announcements.stream().filter(a -> a.audience().equals(audience) || a.audience().equals("ALL")).toList();
    }

    public synchronized Map<String, Long> registrationReport() {
        return regs.stream().collect(Collectors.groupingBy(Registration::courseCode, TreeMap::new, Collectors.counting()));
    }
    public synchronized int totalRegistrations() { return regs.size(); }
    public synchronized List<AuditEvent> audit() { return List.copyOf(audit); }

    public static List<CampusPlace> campusPlaces() {
        return List.of(new CampusPlace("Library", -15.3875, 28.3228),
                new CampusPlace("Registry", -15.3880, 28.3235),
                new CampusPlace("Cafeteria", -15.3870, 28.3220));
    }

    private void log(String actor, String action, String entity) {
        audit.add(new AuditEvent(actor, action, entity, Instant.now()));
    }

    /** Fictional sample data. */
    public static AcademicService sample() {
        AcademicService s = new AcademicService();
        for (int i = 1; i <= 5; i++) s.addStudent(new Student("S" + i, "Student " + i, "BSc CS"));
        s.addCourse(new Course("CS101", "Intro to Programming", 2));
        s.addCourse(new Course("CS102", "Data Structures", 3));
        return s;
    }
}
