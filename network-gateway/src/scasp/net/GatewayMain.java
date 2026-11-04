package scasp.net;

import scasp.core.AcademicService;
import scasp.proc.RequestProcessor;

/** Starts the TCP gateway on port 9090 with sample data. Ctrl+C to stop. */
public class GatewayMain {
    public static void main(String[] a) throws Exception {
        RequestProcessor p = RequestProcessor.platform(4, 100);
        GatewayServer g = new GatewayServer(AcademicService.sample(), p, 9090);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> { try { g.close(); } catch (Exception ignored) { } p.close(); }));
        System.out.println("Gateway listening on " + g.port());
        Thread.currentThread().join();
    }
}
