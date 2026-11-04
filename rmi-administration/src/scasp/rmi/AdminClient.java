package scasp.rmi;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

public class AdminClient {
    /** Looks up the service and prints a report; returns a user-safe message if the server is unavailable. */
    public static String report(String host, int port) {
        try {
            AdminService s = (AdminService) LocateRegistry.getRegistry(host, port).lookup("AdminService");
            StringBuilder sb = new StringBuilder("Total registrations: " + s.totalRegistrations());
            s.courseReport().forEach(c -> sb.append("\n").append(c));
            return sb.toString();
        } catch (RemoteException | NotBoundException e) {
            return "Admin server unavailable at " + host + ":" + port;
        }
    }

    public static void main(String[] a) {
        System.out.println(report(a.length > 0 ? a[0] : "localhost", 1099));
    }
}
