package scasp.web;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import scasp.core.AcademicService;

/** Creates the shared service once at startup and exposes it as a context attribute. */
@WebListener
public class AppContext implements ServletContextListener {
    @Override public void contextInitialized(ServletContextEvent e) {
        e.getServletContext().setAttribute("svc", AcademicService.sample());
    }
}
