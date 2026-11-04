package scasp;

import java.net.DatagramSocket;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import scasp.core.*;
import scasp.core.Model.*;
import scasp.db.*;
import scasp.net.*;
import scasp.proc.*;
import scasp.rmi.*;

/** Dependency-free test runner: `./build.sh` compiles and runs it. */
public class AllTests {
    static int passed, failed;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS  " : "FAIL  ") + name);
        if (ok) passed++; else failed++;
    }
    static boolean throwsEx(Runnable r, Class<? extends Exception> t) {
        try { r.run(); return false; } catch (Exception e) { return t.isInstance(e); }
    }

    public static void main(String[] args) throws Exception {
        stage1(); stage2(); stage3(); stage5(); stage8();
        System.out.println("\n" + passed + " passed, " + failed + " failed");
        System.exit(failed == 0 ? 0 : 1);
    }

    static void stage1() {
        System.out.println("== Stage 1: collections ==");
        AcademicService s = AcademicService.sample(); // CS101 capacity 2
        check("register", s.register("S1", "CS101").equals("REGISTERED"));
        s.register("S2", "CS101");
        check("capacity -> waitlist", s.register("S3", "CS101").startsWith("WAITLISTED"));
        s.register("S4", "CS101");
        check("waitlist order", s.waitlist("CS101").equals(List.of("S3", "S4")));
        check("duplicate rejected", throwsEx(() -> s.register("S1", "CS101"), IllegalStateException.class));
        check("duplicate waitlist rejected", throwsEx(() -> s.register("S3", "CS101"), IllegalStateException.class));
        check("unknown student rejected", throwsEx(() -> s.register("NOPE", "CS101"), IllegalArgumentException.class));
        s.withdraw("S1", "CS101");
        check("promotion on withdraw", s.count("CS101") == 2 && s.waitlist("CS101").equals(List.of("S4")));
        check("withdraw invalid", throwsEx(() -> s.withdraw("S5", "CS101"), IllegalStateException.class));
        check("record equality", new Registration("a", "b").equals(new Registration("a", "b")));
        check("sort by capacity desc", s.courses(Comparator.comparingInt(Course::capacity).reversed()).get(0).code().equals("CS102"));
        check("search", s.search("data").size() == 1);
        check("defensive copy", throwsEx(() -> s.courses().add(null), UnsupportedOperationException.class));
        s.submitRequest("S1", "TRANSCRIPT", 1);
        s.submitRequest("S2", "APPEAL", 5);
        check("priority queue", s.assignNext("staff").get().category().equals("APPEAL"));
        check("report", s.registrationReport().get("CS101") == 2L);
        check("audit recorded", !s.audit().isEmpty());
    }

    static void stage2() throws Exception {
        System.out.println("== Stage 2: concurrency ==");
        AcademicService s = new AcademicService();
        for (int i = 0; i < 200; i++) s.addStudent(new Student("T" + i, "T" + i, "x"));
        s.addCourse(new Course("C1", "Concurrent", 10));
        CountDownLatch start = new CountDownLatch(1);
        try (RequestProcessor p = RequestProcessor.platform(8, 300)) {
            List<Future<String>> fs = new ArrayList<>();
            for (int i = 0; i < 200; i++) {
                String id = "T" + i;
                fs.add(p.submit(() -> { start.await(); return s.register(id, "C1"); }));
            }
            start.countDown();
            for (Future<String> f : fs) f.get(5, TimeUnit.SECONDS);
            check("never over capacity under 200 threads", s.count("C1") == 10);
            check("waitlist holds the rest", s.waitlist("C1").size() == 190);
            check("stats", p.succeeded() == 200 && p.failures() == 0);
        }
        try (RequestProcessor p = RequestProcessor.platform(1, 1)) {
            CountDownLatch block = new CountDownLatch(1);
            p.submit(() -> { block.await(); return 1; });      // occupies the worker
            Thread.sleep(50);
            p.submit(() -> 2);                                  // fills the queue
            check("back-pressure rejects", throwsEx(() -> p.submit(() -> 3), RejectedExecutionException.class)
                    && p.rejections() == 1);
            block.countDown();
        }
        try (RequestProcessor p = RequestProcessor.platform(1, 1)) {
            boolean timedOut = false;
            try { p.call(() -> { Thread.sleep(5000); return 1; }, 100); } catch (TimeoutException e) { timedOut = true; }
            check("timeout cancels task", timedOut);
        }
        try (RequestProcessor p = RequestProcessor.virtual()) {
            AcademicService v = AcademicService.sample();
            List<Future<String>> fs = new ArrayList<>();
            for (int i = 1; i <= 5; i++) { String id = "S" + i; fs.add(p.submit(() -> v.register(id, "CS102"))); }
            for (Future<String> f : fs) f.get();
            check("virtual threads keep invariants", v.count("CS102") == 3);
        }
        int[] r = RaceDemo.run(5, 300);
        System.out.println("      race evidence: unsafe took " + r[0] + " seats of 5, fixed took " + r[1]);
        check("fixed version never over-books", r[1] == 5);
    }

    static void stage3() throws Exception {
        System.out.println("== Stage 3: networking ==");
        AcademicService s = AcademicService.sample();
        try (RequestProcessor p = RequestProcessor.platform(4, 50);
             GatewayServer g = new GatewayServer(s, p, 0)) {
            try (GatewayClient c = new GatewayClient("localhost", g.port())) {
                check("PING", c.send("PING").equals("OK PONG"));
                check("REGISTER", c.send("REGISTER S1 CS101").equals("OK REGISTERED"));
                check("duplicate -> ERR", c.send("REGISTER S1 CS101").startsWith("ERR"));
                check("malformed -> ERR", c.send("REGISTER S1").startsWith("ERR"));
                check("unknown command -> ERR", c.send("HACK").startsWith("ERR"));
                check("empty line -> ERR", c.send("").startsWith("ERR"));
                check("COURSES", c.send("COURSES").equals("OK CS101,CS102"));
                check("WITHDRAW", c.send("WITHDRAW S1 CS101").equals("OK withdrawn"));
            }
            ExecutorService ex = Executors.newFixedThreadPool(5);
            List<Future<String>> fs = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                String id = "S" + i;
                fs.add(ex.submit(() -> { try (GatewayClient c = new GatewayClient("localhost", g.port())) { return c.send("REGISTER " + id + " CS102"); } }));
            }
            for (Future<String> f : fs) f.get(5, TimeUnit.SECONDS);
            ex.shutdown();
            check("5 concurrent clients, capacity respected", s.count("CS102") == 3);
            GatewayClient dropped = new GatewayClient("localhost", g.port());
            dropped.close(); // abrupt disconnect must not hurt the server
            try (GatewayClient c = new GatewayClient("localhost", g.port())) {
                check("server survives client disconnect", c.send("PING").equals("OK PONG"));
            }
            try (DatagramSocket u = new DatagramSocket(0)) {
                u.setSoTimeout(2000);
                Extras.udpSend("localhost", u.getLocalPort(), "HEARTBEAT");
                check("UDP datagram", Extras.udpReceive(u).equals("HEARTBEAT"));
            }
            com.sun.net.httpserver.HttpServer h = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("localhost", 0), 0);
            h.createContext("/a", x -> { byte[] b = "hello".getBytes(); x.sendResponseHeaders(200, b.length); x.getResponseBody().write(b); x.close(); });
            h.start();
            check("URL fetch ok", Extras.fetch("http://localhost:" + h.getAddress().getPort() + "/a").equals("hello"));
            check("URL fetch 404 safe", Extras.fetch("http://localhost:" + h.getAddress().getPort() + "/missing").startsWith("ERR"));
            h.stop(0);
            check("URL fetch failure safe", Extras.fetch("http://localhost:1/").startsWith("ERR"));
            System.out.println("      host info: " + Extras.hostInfo());
            GatewayClient open = new GatewayClient("localhost", g.port());
            g.close();
            boolean closed;
            try { closed = open.send("PING") == null; } catch (Exception e) { closed = true; }
            check("server shutdown closes clients", closed);
        }
    }

    static void stage5() throws Exception {
        System.out.println("== Stage 5: JDBC ==");
        Path file = Files.createTempFile("scasp", ".db");
        Db db = new Db("jdbc:sqlite:" + file);
        db.runScript(Path.of("database/schema.sql"));
        db.runScript(Path.of("database/sample-data.sql"));
        JdbcRepository r = new JdbcRepository(db);
        r.addStudent("S4", "Student 4", "BSc CS");
        check("CRUD read", r.courseTitle("CS101").orElse("").equals("Intro to Programming"));
        r.updateCapacity("CS102", 4);
        r.deleteCourse("CS102");
        check("CRUD delete", r.courseTitle("CS102").isEmpty());
        check("register", r.register("S1", "CS101").equals("REGISTERED"));
        r.register("S2", "CS101");
        check("waitlist", r.register("S3", "CS101").equals("WAITLISTED"));
        check("duplicate rejected + rolled back", throwsSql(() -> r.register("S1", "CS101")));
        int auditBefore = r.auditReport().size();
        check("unknown student rolls back", throwsSql(() -> r.register("GHOST", "CS101")));
        check("rollback left no audit row", r.auditReport().size() == auditBefore);
        r.withdraw("S1", "CS101");
        check("promotion", r.waitlistReport().isEmpty() && r.studentReport().size() == 2);
        check("join report", r.registrationReport().get(0).equals("CS101 | Intro to Programming | 2"));
        check("audit report", r.auditReport().size() >= 5);
        check("SQL injection stays data", throwsSql(() -> r.register("S1' OR '1'='1", "CS101")));
        Files.deleteIfExists(file);
    }
    interface SqlCall { void run() throws Exception; }
    static boolean throwsSql(SqlCall c) {
        try { c.run(); return false; } catch (Exception e) { return true; }
    }

    static void stage8() throws Exception {
        System.out.println("== Stage 8: RMI ==");
        AcademicService s = AcademicService.sample();
        s.register("S1", "CS101");
        var reg = AdminServer.start(s, 11099);
        String out = AdminClient.report("localhost", 11099);
        check("remote report", out.contains("Total registrations: 1") && out.contains("CS101"));
        java.rmi.server.UnicastRemoteObject.unexportObject(reg, true);
        check("server down -> clear message", AdminClient.report("localhost", 11099).startsWith("Admin server unavailable"));
    }
}
