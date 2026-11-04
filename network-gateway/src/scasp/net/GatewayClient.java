package scasp.net;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class GatewayClient implements AutoCloseable {
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;

    public GatewayClient(String host, int port) throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(InetAddress.getByName(host), port), 3000);
        socket.setSoTimeout(3000);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
    }

    /** Sends one request line and returns the response line (null if the server closed). */
    public String send(String line) throws IOException {
        out.println(line);
        return in.readLine();
    }

    @Override public void close() throws IOException { socket.close(); }
}
