package scasp.net;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import scasp.core.AcademicService;
import scasp.core.Model.Course;
import scasp.proc.RequestProcessor;

/**
 * TCP gateway. Protocol: one UTF-8 text line per message ending in LF.
 * Requests:  PING | COURSES | REGISTER <studentId> <code> | WITHDRAW <studentId> <code> | REPORT | QUIT
 * Responses: "OK <text>" or "ERR <reason>".
 * Each command runs on the Stage 2 RequestProcessor; each client connection has its own virtual thread.
 */
public class GatewayServer implements AutoCloseable {
    private final AcademicService svc;
    private final RequestProcessor proc;
    private final ServerSocket server;
    private final ExecutorService clients = Executors.newVirtualThreadPerTaskExecutor();
    private final Set<Socket> open = ConcurrentHashMap.newKeySet();

    public GatewayServer(AcademicService svc, RequestProcessor proc, int port) throws IOException {
        this.svc = svc;
        this.proc = proc;
        this.server = new ServerSocket(port);
        clients.submit(this::acceptLoop);
    }

    public int port() { return server.getLocalPort(); }

    private void acceptLoop() {
        while (!server.isClosed()) {
            try {
                Socket s = server.accept();
                s.setSoTimeout(5000); // idle clients are dropped
                open.add(s);
                clients.submit(() -> serve(s));
            } catch (IOException e) {
                break;
            }
        }
    }

    private void serve(Socket s) {
        try (s;
             var in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
             var out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true)) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.trim().equalsIgnoreCase("QUIT")) { out.println("OK bye"); break; }
                out.println(handle(line));
            }
        } catch (IOException ignored) { // timeout or disconnect
        } finally {
            open.remove(s);
        }
    }

    private String handle(String line) {
        try {
            String[] p = line.trim().split("\\s+");
            return proc.call(() -> exec(p), 2000);
        } catch (Exception e) {
            return "ERR " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }

    private String exec(String[] p) {
        return switch (p[0].toUpperCase()) {
            case "PING" -> "OK PONG";
            case "COURSES" -> "OK " + svc.courses().stream().map(Course::code).collect(Collectors.joining(","));
            case "REGISTER" -> { need(p, 3); yield "OK " + svc.register(p[1], p[2]); }
            case "WITHDRAW" -> { need(p, 3); svc.withdraw(p[1], p[2]); yield "OK withdrawn"; }
            case "REPORT" -> "OK " + svc.registrationReport();
            default -> throw new IllegalArgumentException("Unknown command");
        };
    }

    private static void need(String[] p, int n) {
        if (p.length != n) throw new IllegalArgumentException("Expected " + (n - 1) + " arguments");
    }

    @Override public void close() throws IOException {
        server.close();
        for (Socket s : open) { try { s.close(); } catch (IOException ignored) { } }
        clients.shutdownNow();
    }
}
