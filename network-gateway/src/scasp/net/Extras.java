package scasp.net;

import java.io.IOException;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** UDP announcements and a safe URL fetch. */
public class Extras {
    public static void udpSend(String host, int port, String msg) throws IOException {
        try (DatagramSocket s = new DatagramSocket()) {
            byte[] b = msg.getBytes(StandardCharsets.UTF_8);
            s.send(new DatagramPacket(b, b.length, InetAddress.getByName(host), port));
        }
    }

    public static String udpReceive(DatagramSocket s) throws IOException {
        DatagramPacket p = new DatagramPacket(new byte[512], 512);
        s.receive(p);
        return new String(p.getData(), 0, p.getLength(), StandardCharsets.UTF_8);
    }

    /** GET a URL; returns the body, or "ERR ..." on any failure or non-200 status. */
    public static String fetch(String url) {
        try {
            HttpClient c = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
            HttpResponse<String> r = c.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(3)).build(),
                    HttpResponse.BodyHandlers.ofString());
            return r.statusCode() == 200 ? r.body() : "ERR status " + r.statusCode();
        } catch (Exception e) {
            return "ERR " + e.getClass().getSimpleName();
        }
    }

    public static String hostInfo() throws IOException {
        InetAddress a = InetAddress.getLocalHost();
        return a.getHostName() + " / " + a.getHostAddress();
    }
}
