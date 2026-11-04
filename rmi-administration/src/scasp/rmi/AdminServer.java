package scasp.rmi;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import scasp.core.AcademicService;
import scasp.core.Model.Announcement;

public class AdminServer implements AdminService {
    private final AcademicService svc;
    private AdminServer(AcademicService svc) { this.svc = svc; }

    public List<CourseSummary> courseReport() {
        return svc.courses().stream().map(c -> new CourseSummary(c.code(), c.title(), c.capacity(),
                svc.count(c.code()), svc.waitlist(c.code()).size())).toList();
    }
    public int totalRegistrations() { return svc.totalRegistrations(); }
    public List<String> auditTrail() {
        return svc.audit().stream().map(a -> a.actor() + " " + a.action() + " " + a.entity()).toList();
    }
    public void announce(String title, String body) { svc.publish(new Announcement(title, body, "ALL")); }

    /** Exports the service and binds it as "AdminService". Keep the returned registry; unexport on shutdown. */
    public static Registry start(AcademicService svc, int port) throws RemoteException {
        AdminServer impl = new AdminServer(svc);
        AdminService stub = (AdminService) UnicastRemoteObject.exportObject(impl, 0);
        Registry reg = LocateRegistry.createRegistry(port);
        reg.rebind("AdminService", stub);
        return reg;
    }

    public static void main(String[] a) throws Exception {
        start(AcademicService.sample(), 1099);
        System.out.println("AdminService bound on port 1099");
    }
}
