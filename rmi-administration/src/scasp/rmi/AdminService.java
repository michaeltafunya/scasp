package scasp.rmi;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/** Remote administration interface. Only read/administrative operations; no persistence internals are exposed. */
public interface AdminService extends Remote {
    /** Passed by value (Serializable); the remote object itself is passed by reference (stub). */
    record CourseSummary(String code, String title, int capacity, int registered, int waiting) implements Serializable {}

    List<CourseSummary> courseReport() throws RemoteException;
    int totalRegistrations() throws RemoteException;
    List<String> auditTrail() throws RemoteException;
    void announce(String title, String body) throws RemoteException;
}
